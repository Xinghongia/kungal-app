package com.kungal.app.ui.galgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.EditForm
import com.kungal.core.model.EditProposalSummary
import com.kungal.core.model.Work
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import java.util.UUID

data class GalgameEditState(
    val loading: Boolean = true,
    val error: String? = null,
    val work: Work? = null,
    val form: EditForm? = null,
    val values: Map<String, JsonElement> = emptyMap(),
    val initialValues: Map<String, JsonElement> = emptyMap(),
    val names: EditNames = EditNames(),
    val openProposals: List<EditProposalSummary> = emptyList(),
    val proposalsError: String? = null,
    val canReviewQueue: Boolean = false,
    val submitting: Boolean = false,
    val submittingProposalId: String? = null,
    val submitError: String? = null,
) {
    private val editableKeys: Set<String> get() = form?.fields?.map { it.key }?.toSet() ?: emptySet()

    /** 有改动的字段（只算 schema 暴露的可编辑字段）。 */
    val dirtyKeys: List<String>
        get() = editableKeys.filter { values[it] != initialValues[it] }.sorted()

    val dirtyCount: Int get() = dirtyKeys.size
}

class GalgameEditViewModel(
    private val app: KungalApp,
    private val workId: String,
    private val includeNsfw: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(GalgameEditState())
    val state: StateFlow<GalgameEditState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            coroutineScope {
                val formDeferred = async { runCatching { app.editApi.form(workId) } }
                val workDeferred = async { runCatching { app.galgameApi.work(workId, includeNsfw) } }
                val proposalsDeferred = async {
                    runCatching { app.editApi.proposals(workId, state = "open", limit = 100) }
                }
                val permissionsDeferred = async { runCatching { app.meApi.permissions() } }

                val form = formDeferred.await().getOrElse { error ->
                    _state.update { it.copy(loading = false, error = friendlyError(error)) }
                    return@coroutineScope
                }
                val work = workDeferred.await().getOrNull()
                val proposalsResult = proposalsDeferred.await()
                val canReview = permissionsDeferred.await().getOrNull()
                    ?.permissions?.contains("galgame.edit_proposal.review") == true

                val names = work?.let { namesFromWork(it) } ?: EditNames()
                val initial = form.fieldValues.toMap()
                _state.update {
                    it.copy(
                        loading = false,
                        error = null,
                        work = work,
                        form = form,
                        values = initial,
                        initialValues = initial,
                        names = names,
                        openProposals = proposalsResult.getOrNull()?.items ?: emptyList(),
                        proposalsError = proposalsResult.exceptionOrNull()?.let(::friendlyError),
                        canReviewQueue = canReview,
                    )
                }
                // 表单里引用的实体可能不在词条读面里（例如提案里的系列），异步补名
                val resolved = resolveEditNames(app, names, editNameIdsFrom(form.fieldValues))
                _state.update { it.copy(names = resolved) }
            }
        }
    }

    fun updateValue(key: String, value: JsonElement) {
        _state.update { it.copy(values = it.values + (key to value), submitError = null) }
    }

    fun resetChanges() {
        _state.update { it.copy(values = it.initialValues, submitError = null) }
    }

    /** 提交前校验；返回 null 表示可以提交，否则返回中文错误。 */
    fun validate(): String? {
        val state = _state.value
        val form = state.form ?: return "编辑表单尚未加载"
        val values = state.values
        for (field in form.fields) {
            when (field.key) {
                "catalog.work.titles" -> {
                    val rows = values[field.key].asArrayOrEmpty().mapNotNull { it.asObjectOrNull() }
                    if (rows.none { it.int("kind") == 0 && it.string("title").isNotBlank() }) {
                        return "标题至少需要一条官方名"
                    }
                    if (rows.any { it.string("title").isBlank() }) {
                        return "标题不能为空：请填写或删除空行"
                    }
                }
                "catalog.work.credits" -> {
                    val rows = values[field.key].asArrayOrEmpty().mapNotNull { it.asObjectOrNull() }
                    if (rows.any { it.int("credit_name_id") <= 0 || it.int("role_id") <= 0 }) {
                        return "本站署名缺少 Staff 名义或职务"
                    }
                }
                "catalog.work.covers", "catalog.work.screenshots" -> {
                    val rows = values[field.key].asArrayOrEmpty().mapNotNull { it.asObjectOrNull() }
                    if (rows.any { it.string("image_hash").isBlank() }) {
                        return "图片行缺少 image_hash"
                    }
                }
            }
        }
        if (state.dirtyKeys.isEmpty()) return "还没有修改任何字段"
        return null
    }

    fun submit(note: String) {
        val state = _state.value
        if (state.submitting) return
        validate()?.let { problem ->
            _state.update { it.copy(submitError = problem) }
            return
        }
        val patch = JsonObject(
            state.dirtyKeys.associateWith { key -> state.values[key] ?: JsonNull },
        )
        viewModelScope.launch {
            _state.update { it.copy(submitting = true, submitError = null) }
            runCatching {
                app.editApi.createProposal(
                    workId = workId,
                    patch = patch,
                    note = note.trim().ifBlank { null },
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            }.onSuccess { result ->
                _state.update { it.copy(submitting = false) }
                AppMessages.show(
                    if (result.proposal.state == "merged") "修改已生效" else "提案已提交，等待审核",
                )
                load()
            }.onFailure { error ->
                _state.update {
                    it.copy(submitting = false, submitError = friendlyError(error))
                }
                AppMessages.show(friendlyError(error))
            }
        }
    }

    fun withdraw(proposalId: String) {
        if (_state.value.submittingProposalId != null) return
        viewModelScope.launch {
            _state.update { it.copy(submittingProposalId = proposalId) }
            runCatching {
                val etag = runCatching { app.editApi.proposal(proposalId).etag }.getOrNull()
                app.editApi.updateProposal(proposalId, "withdrawn", ifMatch = etag)
            }.onSuccess {
                AppMessages.show("提案已撤回")
                _state.update { it.copy(submittingProposalId = null) }
                refreshProposals()
            }.onFailure { error ->
                _state.update { it.copy(submittingProposalId = null) }
                AppMessages.show(friendlyError(error))
            }
        }
    }

    private fun refreshProposals() {
        viewModelScope.launch {
            runCatching { app.editApi.proposals(workId, state = "open", limit = 100) }
                .onSuccess { page ->
                    _state.update { it.copy(openProposals = page.items, proposalsError = null) }
                }
                .onFailure { error ->
                    _state.update { it.copy(proposalsError = friendlyError(error)) }
                }
        }
    }

    /** 上传编辑图片，返回 hash；失败返回 null（提示已由调用方显示）。 */
    suspend fun uploadImage(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
        preset: String,
    ): String? = runCatching {
        app.editApi.uploadImage(
            bytes = bytes,
            fileName = fileName,
            contentType = contentType,
            preset = preset,
            idempotencyKey = UUID.randomUUID().toString(),
        ).hash
    }.onFailure { AppMessages.show(friendlyError(it)) }.getOrNull()
}
