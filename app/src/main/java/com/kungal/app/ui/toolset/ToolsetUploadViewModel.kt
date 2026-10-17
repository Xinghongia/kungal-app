package com.kungal.app.ui.toolset

import android.net.Uri
import android.provider.OpenableColumns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.ToolsetUpload
import com.kungal.core.model.ToolsetUploadCompletePart
import com.kungal.core.model.ToolsetUploadCreate
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Call
import java.io.IOException
import java.io.InputStream
import java.util.UUID
import kotlin.math.min
import kotlin.math.roundToInt

/** 已选中的本地压缩包（SAF Uri）。 */
data class ToolsetPickedFile(
    val uri: Uri,
    val name: String,
    val size: Long,
    val contentType: String,
)

enum class ToolsetUploadStatus { IDLE, INITIALIZING, UPLOADING, COMPLETING, COMPLETED }

data class ToolsetUploadState(
    val quota: ToolsetUploadQuota? = null,
    val quotaLoading: Boolean = false,
    val quotaError: String? = null,
    val records: List<ToolsetUploadRecord> = emptyList(),
    val picked: ToolsetPickedFile? = null,
    /** 命中的未完成记录：开始上传时走服务端续传。 */
    val resumedRecord: ToolsetUploadRecord? = null,
    val uploadId: String? = null,
    val status: ToolsetUploadStatus = ToolsetUploadStatus.IDLE,
    val progress: Int = 0,
    val artifactId: String? = null,
    val artifactName: String = "",
    val artifactSize: Long = 0,
    val error: String? = null,
) {
    val uploadInProgress: Boolean
        get() = status == ToolsetUploadStatus.INITIALIZING ||
            status == ToolsetUploadStatus.UPLOADING ||
            status == ToolsetUploadStatus.COMPLETING
}

/**
 * 工具集文件上传（BP-03 U6）：配额校验、单文件/分片 PUT、ETag 完成、
 * 服务端已传分片 + DataStore 记录的断点续传、中止；失败如实提示，不自动重试。
 */
class ToolsetUploadViewModel(
    private val app: KungalApp,
    private val toolsetId: String,
) : ViewModel() {

    private val store = ToolsetUploadStore(app)
    private val _state = MutableStateFlow(ToolsetUploadState())
    val state = _state.asStateFlow()

    private var currentCall: Call? = null
    private var cancelRequested = false
    private var createKey: String? = null

    private class UploadCancelled : Exception()

    init {
        refresh()
    }

    /** 拉取额度（/me + /me/permissions）与本地续传记录。 */
    fun refresh() {
        viewModelScope.launch {
            val records = runCatching { store.list(toolsetId) }.getOrDefault(emptyList())
            _state.update { it.copy(records = records, quotaLoading = true, quotaError = null) }
            runCatching {
                val me = app.meApi.me()
                val permissions = runCatching { app.meApi.permissions().permissions }
                    .getOrDefault(emptyList())
                ToolsetUploadQuota(
                    moemoepoint = me.moemoepoint,
                    usedToday = me.toolsetUploadTodayBytes,
                    bypass = "toolset.upload_bypass" in permissions,
                )
            }.onSuccess { quota ->
                _state.update { it.copy(quota = quota, quotaLoading = false) }
            }.onFailure { e ->
                _state.update { it.copy(quotaLoading = false, quotaError = friendlyError(e)) }
            }
        }
    }

    /** 用户从系统文件选择器拿到 Uri；读取名称与大小并做前置校验。 */
    fun onFilePicked(uri: Uri) {
        viewModelScope.launch {
            val quota = _state.value.quota
            if (quota == null) {
                _state.update { it.copy(error = "正在读取上传额度，请稍候再选文件") }
                return@launch
            }
            val picked = runCatching { readPickedFile(uri) }
                .getOrElse { e ->
                    _state.update { it.copy(error = e.message ?: "无法读取所选文件") }
                    return@launch
                }
            val problem = checkToolsetUploadFile(picked.name, picked.size, quota)
            if (problem != null) {
                _state.update { it.copy(error = problem) }
                return@launch
            }
            val pending = _state.value.records.firstOrNull {
                !it.completed && it.name == picked.name && it.size == picked.size
            }
            createKey = UUID.randomUUID().toString()
            _state.update {
                it.copy(
                    picked = picked,
                    resumedRecord = pending,
                    uploadId = null,
                    status = ToolsetUploadStatus.IDLE,
                    progress = pending?.progress ?: 0,
                    artifactId = null,
                    artifactName = "",
                    artifactSize = 0,
                    error = null,
                )
            }
        }
    }

    fun clearPicked() {
        if (_state.value.uploadInProgress) return
        _state.update {
            it.copy(
                picked = null,
                resumedRecord = null,
                uploadId = null,
                status = ToolsetUploadStatus.IDLE,
                progress = 0,
                artifactId = null,
                artifactName = "",
                artifactSize = 0,
                error = null,
            )
        }
    }

    fun startUpload() {
        val snapshot = _state.value
        if (snapshot.uploadInProgress) return
        val picked = snapshot.picked
        if (picked == null) {
            _state.update { it.copy(error = "请先选择压缩包") }
            return
        }
        if (!app.isLoggedIn) {
            _state.update { it.copy(error = "请先登录") }
            return
        }
        cancelRequested = false
        viewModelScope.launch {
            _state.update {
                it.copy(
                    status = ToolsetUploadStatus.INITIALIZING,
                    progress = 0,
                    error = null,
                    artifactId = null,
                )
            }
            try {
                val resumed = snapshot.resumedRecord
                if (resumed != null) resumeSession(picked, resumed) else createSession(picked)
                checkCancelled()
            } catch (e: UploadCancelled) {
                handleCancelled()
            } catch (e: Throwable) {
                if (cancelRequested) {
                    handleCancelled()
                } else {
                    _state.update {
                        it.copy(status = ToolsetUploadStatus.IDLE, error = friendlyError(e))
                    }
                }
            } finally {
                currentCall = null
                cancelRequested = false
            }
        }
    }

    /** 中止进行中的上传：取消当前请求，删除服务端会话与本地断点记录。 */
    fun cancelUpload() {
        val status = _state.value.status
        // 正在完成（服务端 PATCH）时不响应取消，避免把已完成的会话删掉
        if (status != ToolsetUploadStatus.INITIALIZING && status != ToolsetUploadStatus.UPLOADING) return
        cancelRequested = true
        currentCall?.cancel()
    }

    /** 从断点记录继续：先校验原文件仍可读且大小一致。 */
    fun resumeRecord(record: ToolsetUploadRecord) {
        if (record.completed) {
            useFinishedRecord(record)
            return
        }
        if (_state.value.uploadInProgress) return
        val uri = runCatching { Uri.parse(record.uri) }.getOrNull()
        if (uri == null || record.uri.isBlank()) {
            _state.update { it.copy(error = "找不到原文件，请重新选择文件上传") }
            return
        }
        viewModelScope.launch {
            val quota = _state.value.quota
            if (quota == null) {
                _state.update { it.copy(error = "正在读取上传额度，请稍候再试") }
                return@launch
            }
            val picked = runCatching { readPickedFile(uri) }
                .getOrElse {
                    _state.update { state -> state.copy(error = "原文件已无法读取，请重新选择文件上传") }
                    return@launch
                }
            if (picked.size != record.size || picked.name != record.name) {
                _state.update { it.copy(error = "原文件已变化，请重新选择文件上传") }
                return@launch
            }
            checkToolsetUploadFile(picked.name, picked.size, quota)?.let { problem ->
                _state.update { it.copy(error = problem) }
                return@launch
            }
            _state.update {
                it.copy(
                    picked = picked,
                    resumedRecord = record,
                    uploadId = record.id,
                    status = ToolsetUploadStatus.IDLE,
                    progress = record.progress,
                    artifactId = null,
                    artifactName = "",
                    artifactSize = 0,
                    error = null,
                )
            }
            startUpload()
        }
    }

    /** 使用此前已上传完成的文件（网页 FinishedList 的「使用」）。 */
    fun useFinishedRecord(record: ToolsetUploadRecord) {
        if (_state.value.uploadInProgress) return
        _state.update {
            it.copy(
                picked = null,
                resumedRecord = null,
                uploadId = record.id,
                status = ToolsetUploadStatus.COMPLETED,
                progress = 100,
                artifactId = record.id,
                artifactName = record.name,
                artifactSize = record.size,
                error = null,
            )
        }
    }

    /** 删除本地断点记录；未完成的会话同时向服务端中止。 */
    fun removeRecord(record: ToolsetUploadRecord) {
        if (_state.value.uploadInProgress && _state.value.uploadId == record.id) return
        viewModelScope.launch {
            if (!record.completed) {
                runCatching { app.toolsetApi.abortUpload(toolsetId, record.id) }
            }
            runCatching { store.remove(toolsetId, record.id) }
            _state.update { state ->
                val cleared = state.uploadId == record.id && !state.uploadInProgress
                state.copy(
                    records = state.records.filterNot { it.id == record.id },
                    uploadId = if (cleared) null else state.uploadId,
                    status = if (cleared) ToolsetUploadStatus.IDLE else state.status,
                    progress = if (cleared) 0 else state.progress,
                    artifactId = if (state.artifactId == record.id) null else state.artifactId,
                    picked = if (cleared) null else state.picked,
                    resumedRecord = if (cleared) null else state.resumedRecord,
                )
            }
        }
    }

    /** 发布成功：该 artifact 已被消费，后续不能重复发布。 */
    fun markPublished() {
        val id = _state.value.artifactId ?: return
        viewModelScope.launch { runCatching { store.remove(toolsetId, id) } }
        _state.update {
            it.copy(
                records = it.records.filterNot { record -> record.id == id },
                artifactId = null,
                picked = null,
                resumedRecord = null,
                status = ToolsetUploadStatus.IDLE,
                progress = 0,
            )
        }
    }

    /** 发布时服务端判定 artifact 失效或已发布：丢掉这个结果，重新上传。 */
    fun invalidateArtifact() {
        val id = _state.value.artifactId ?: return
        viewModelScope.launch { runCatching { store.remove(toolsetId, id) } }
        _state.update {
            it.copy(
                records = it.records.filterNot { record -> record.id == id },
                artifactId = null,
                picked = null,
                resumedRecord = null,
                uploadId = null,
                status = ToolsetUploadStatus.IDLE,
                progress = 0,
            )
        }
    }

    // ---------- 上传流程 ----------

    private suspend fun createSession(picked: ToolsetPickedFile) {
        val created = app.toolsetApi.createUpload(
            toolsetId = toolsetId,
            request = ToolsetUploadCreate(
                filename = picked.name,
                fileSize = picked.size,
                contentType = picked.contentType,
            ),
            idempotencyKey = createKey ?: UUID.randomUUID().toString().also { createKey = it },
        )
        createKey = null
        saveRecord(picked, created.id, 0, completed = false)
        _state.update { state ->
            state.copy(
                uploadId = created.id,
                // 失败后重试时走服务端续传，避免重复创建上传会话
                resumedRecord = state.records.firstOrNull { it.id == created.id },
            )
        }
        uploadSession(created, picked)
    }

    private suspend fun resumeSession(picked: ToolsetPickedFile, record: ToolsetUploadRecord) {
        val session = try {
            app.toolsetApi.upload(toolsetId, record.id)
        } catch (e: KungalApiException) {
            if (e.httpStatus == 404) {
                runCatching { store.remove(toolsetId, record.id) }
                _state.update {
                    it.copy(
                        records = it.records.filterNot { r -> r.id == record.id },
                        resumedRecord = null,
                    )
                }
                createKey = UUID.randomUUID().toString()
                createSession(picked)
                return
            }
            throw e
        }
        if (session.state == "completed") {
            markCompleted(picked, session.id, session.fileSize.takeIf { it > 0 } ?: picked.size)
            return
        }
        saveRecord(picked, session.id, record.progress, completed = false)
        _state.update { it.copy(uploadId = session.id) }
        uploadSession(session, picked)
    }

    private suspend fun uploadSession(session: ToolsetUpload, picked: ToolsetPickedFile) {
        if (session.isMultipart) uploadMultipart(session, picked) else uploadSingle(session, picked)
        checkCancelled()
    }

    private suspend fun uploadSingle(session: ToolsetUpload, picked: ToolsetPickedFile) {
        val url = session.uploadUrl ?: throw IOException("服务端没有返回上传地址")
        _state.update { it.copy(status = ToolsetUploadStatus.UPLOADING, progress = 0) }
        var lastPercent = 0
        put(
            url = url,
            length = picked.size,
            contentType = picked.contentType,
            openStream = { openFile(picked.uri) },
            onProgress = { sent ->
                val progress = percentOf(sent, picked.size)
                if (progress != lastPercent) {
                    lastPercent = progress
                    _state.update { it.copy(progress = progress) }
                }
            },
        )
        checkCancelled()
        complete(session.id, parts = emptyList(), picked)
    }

    private suspend fun uploadMultipart(session: ToolsetUpload, picked: ToolsetPickedFile) {
        val partSize = session.partSize?.takeIf { it > 0 } ?: (5 * UPLOAD_MB)
        val doneParts = session.uploadedParts
            .map { ToolsetUploadCompletePart(it.partNumber, it.etag) }
            .toMutableList()
        var doneBytes = session.uploadedParts.sumOf { it.byteCount }
        var lastPercent = percentOf(doneBytes, picked.size)
        _state.update {
            it.copy(status = ToolsetUploadStatus.UPLOADING, progress = lastPercent)
        }
        val fresh = mutableListOf<ToolsetUploadCompletePart>()
        for (part in session.partUrls.sortedBy { it.partNumber }) {
            checkCancelled()
            val offset = (part.partNumber - 1) * partSize
            val length = min(partSize, picked.size - offset)
            if (length <= 0) continue
            val baseBytes = doneBytes
            val etag = put(
                url = part.url,
                length = length,
                contentType = picked.contentType,
                openStream = { openRange(picked.uri, offset, length) },
                onProgress = { sent ->
                    val progress = percentOf(baseBytes + sent, picked.size)
                    if (progress != lastPercent) {
                        lastPercent = progress
                        _state.update { it.copy(progress = progress) }
                    }
                },
            )
            if (etag.isNullOrBlank()) throw IOException("服务端没有返回第 ${part.partNumber} 片的 ETag")
            doneBytes += length
            fresh += ToolsetUploadCompletePart(part.partNumber, etag)
            saveRecord(picked, session.id, percentOf(doneBytes, picked.size), completed = false)
            _state.update { it.copy(progress = percentOf(doneBytes, picked.size)) }
        }
        checkCancelled()
        complete(session.id, parts = (doneParts + fresh).sortedBy { it.partNumber }, picked)
    }

    private suspend fun put(
        url: String,
        length: Long,
        contentType: String,
        openStream: () -> InputStream,
        onProgress: ((Long) -> Unit)? = null,
    ): String? = app.client.putExternalStream(
        url = url,
        contentLength = length,
        contentType = contentType,
        openStream = openStream,
        onProgress = onProgress,
        onCall = { call -> currentCall = call },
    )

    private suspend fun complete(
        uploadId: String,
        parts: List<ToolsetUploadCompletePart>,
        picked: ToolsetPickedFile,
    ) {
        _state.update { it.copy(status = ToolsetUploadStatus.COMPLETING, progress = 100) }
        val done = app.toolsetApi.completeUpload(toolsetId, uploadId, parts)
        markCompleted(picked, done.id.ifBlank { uploadId }, done.fileSize.takeIf { it > 0 } ?: picked.size)
    }

    private suspend fun markCompleted(picked: ToolsetPickedFile, uploadId: String, size: Long) {
        saveRecord(picked, uploadId, 100, completed = true)
        _state.update {
            it.copy(
                status = ToolsetUploadStatus.COMPLETED,
                progress = 100,
                uploadId = uploadId,
                artifactId = uploadId,
                artifactName = picked.name,
                artifactSize = size,
                error = null,
            )
        }
    }

    private suspend fun handleCancelled() {
        val id = _state.value.uploadId
        if (id != null) {
            runCatching { app.toolsetApi.abortUpload(toolsetId, id) }
            runCatching { store.remove(toolsetId, id) }
        }
        _state.update {
            it.copy(
                records = if (id == null) it.records else it.records.filterNot { r -> r.id == id },
                status = ToolsetUploadStatus.IDLE,
                progress = 0,
                uploadId = null,
                resumedRecord = null,
                artifactId = null,
                error = null,
            )
        }
    }

    private fun checkCancelled() {
        if (cancelRequested) throw UploadCancelled()
    }

    private suspend fun saveRecord(
        picked: ToolsetPickedFile,
        uploadId: String,
        progress: Int,
        completed: Boolean,
    ) {
        val record = ToolsetUploadRecord(
            id = uploadId,
            name = picked.name,
            size = picked.size,
            uri = picked.uri.toString(),
            progress = progress,
            updatedAt = System.currentTimeMillis(),
            completed = completed,
        )
        runCatching { store.upsert(toolsetId, record) }
        _state.update { state ->
            state.copy(
                records = (state.records.filterNot { it.id == record.id } + record)
                    .sortedByDescending { it.updatedAt },
            )
        }
    }

    // ---------- 本地文件读取 ----------

    private suspend fun readPickedFile(uri: Uri): ToolsetPickedFile = withContext(Dispatchers.IO) {
        val resolver = app.contentResolver
        var name: String? = null
        var size = -1L
        resolver.query(
            uri,
            arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE),
            null,
            null,
            null,
        )?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0 && !cursor.isNull(nameIndex)) name = cursor.getString(nameIndex)
                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) size = cursor.getLong(sizeIndex)
            }
        }
        if (size < 0) throw IOException("无法读取文件大小，请换一个文件管理器重试")
        ToolsetPickedFile(
            uri = uri,
            name = name ?: uri.lastPathSegment ?: "upload.zip",
            size = size,
            contentType = resolver.getType(uri) ?: "application/octet-stream",
        )
    }

    private fun openFile(uri: Uri): InputStream =
        app.contentResolver.openInputStream(uri) ?: throw IOException("无法读取所选文件")

    /** 打开 [offset, offset+length) 片段：先跳过前缀，再用限长流封住尾部。 */
    private fun openRange(uri: Uri, offset: Long, length: Long): InputStream {
        val input = openFile(uri)
        var remainingSkip = offset
        while (remainingSkip > 0) {
            val skipped = runCatching { input.skip(remainingSkip) }.getOrDefault(0)
            if (skipped > 0) {
                remainingSkip -= skipped
                continue
            }
            if (input.read() == -1) {
                input.close()
                throw IOException("文件内容已变化，无法继续上传")
            }
            remainingSkip -= 1
        }
        return BoundedInputStream(input, length)
    }

    private class BoundedInputStream(
        private val inner: InputStream,
        private var remaining: Long,
    ) : InputStream() {
        override fun read(): Int {
            if (remaining <= 0) return -1
            val value = inner.read()
            if (value != -1) remaining -= 1
            return value
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (remaining <= 0) return -1
            val wanted = min(length.toLong(), remaining).toInt()
            val read = inner.read(buffer, offset, wanted)
            if (read > 0) remaining -= read
            return read
        }

        override fun close() = inner.close()
    }

    private fun percentOf(done: Long, total: Long): Int =
        if (total <= 0) 0 else ((done * 100.0) / total).roundToInt().coerceIn(0, 100)
}
