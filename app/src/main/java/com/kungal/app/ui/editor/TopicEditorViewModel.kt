package com.kungal.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.TopicCategorySections
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.TopicCreate
import com.kungal.core.model.TopicDraftCreate
import com.kungal.core.model.TopicPatch
import com.kungal.core.model.TopicSource
import com.kungal.core.model.UploadedImage
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** 发布/编辑话题的界面状态。 */
data class EditorState(
    val loading: Boolean = false,
    val title: String = "",
    val content: String = "",
    /** null = 尚未选择分类（发帖前必选） */
    val category: String? = null,
    val sections: Set<String> = emptySet(),
    val isNsfw: Boolean = false,
    /** public / login / role / users */
    val accessScope: String = "public",
    val accessRoles: Set<String> = emptySet(),
    /** 逗号或空格分隔的用户 id */
    val accessUserIds: String = "",
    val images: List<UploadedImage> = emptyList(),
    /** 选为封面的图片 hash（最多 9 张） */
    val coverHashes: List<String> = emptyList(),
    val uploading: Boolean = false,
    val saving: Boolean = false,
    val savingDraft: Boolean = false,
    val error: String? = null,
    val message: String? = null,
    /** 保存成功后的话题 id；界面消费后导航 */
    val publishedTopicId: String? = null,
    val editingTopicId: String? = null,
)

class TopicEditorViewModel(
    private val app: KungalApp,
    private val topicId: String? = null,
    private val draftId: String? = null,
) : ViewModel() {

    private val _state = MutableStateFlow(EditorState(editingTopicId = topicId))
    val state: StateFlow<EditorState> = _state.asStateFlow()

    /** 幂等键在失败重试时保持不变，成功后才更换（与网页一致）。 */
    private var publishKey = UUID.randomUUID().toString()
    private var draftKey = UUID.randomUUID().toString()

    init {
        when {
            topicId != null -> loadTopic(topicId)
            draftId != null -> loadDraft(draftId)
        }
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }
    fun consumeError() = _state.update { it.copy(error = null) }
    fun consumePublished() = _state.update { it.copy(publishedTopicId = null) }

    fun setTitle(value: String) = _state.update { it.copy(title = value) }
    fun setContent(value: String) = _state.update { it.copy(content = value) }
    fun setNsfw(value: Boolean) = _state.update { it.copy(isNsfw = value) }
    fun setAccessScope(value: String) = _state.update { it.copy(accessScope = value) }
    fun setAccessUserIds(value: String) = _state.update { it.copy(accessUserIds = value) }

    fun setCategory(value: String) {
        val allowed = TopicCategorySections[value].orEmpty().map { it.key }.toSet()
        _state.update {
            it.copy(category = value, sections = it.sections.intersect(allowed).take(3).toSet())
        }
    }

    fun toggleSection(key: String) {
        _state.update {
            val next = if (key in it.sections) it.sections - key else {
                if (it.sections.size >= 3) it.sections else it.sections + key
            }
            it.copy(sections = next)
        }
    }

    fun toggleRole(role: String) {
        _state.update {
            it.copy(accessRoles = if (role in it.accessRoles) it.accessRoles - role else it.accessRoles + role)
        }
    }

    fun toggleCover(hash: String) {
        _state.update {
            it.copy(
                coverHashes = when {
                    hash in it.coverHashes -> it.coverHashes - hash
                    it.coverHashes.size >= 9 -> it.coverHashes
                    else -> it.coverHashes + hash
                },
            )
        }
    }

    fun removeImage(hash: String) {
        _state.update {
            it.copy(
                images = it.images.filterNot { image -> image.hash == hash },
                coverHashes = it.coverHashes - hash,
            )
        }
    }

    /** 上传图片并把 Markdown 图片语法追加到正文末尾；可再设为封面。 */
    fun uploadImage(bytes: ByteArray, fileName: String, contentType: String) {
        if (bytes.isEmpty()) return
        _state.update { it.copy(uploading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                app.imageApi.upload(
                    bytes = bytes,
                    fileName = fileName,
                    contentType = contentType,
                    purpose = "content",
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            }.onSuccess { image ->
                _state.update {
                    it.copy(
                        uploading = false,
                        images = it.images + image,
                        content = it.content.trimEnd() + "\n\n![图片](${image.url})\n",
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(uploading = false, error = friendlyError(e)) }
            }
        }
    }

    private fun loadTopic(id: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { app.topicApi.topicSource(id) }
                .onSuccess { applySource(it) }
                .onFailure { e ->
                    val hint = if (e is KungalApiException && e.httpStatus == 403) {
                        "没有编辑该话题的权限，或话题不存在。"
                    } else {
                        friendlyError(e)
                    }
                    _state.update { it.copy(loading = false, error = hint) }
                }
        }
    }

    private fun applySource(source: TopicSource) {
        _state.update {
            it.copy(
                loading = false,
                editingTopicId = source.topicId,
                title = source.title,
                content = source.contentMarkdown,
                category = source.category.ifBlank { null },
                sections = source.sections.toSet(),
                isNsfw = source.isNsfw,
                accessScope = source.accessScope,
                accessRoles = source.accessGrants.roles.toSet(),
                accessUserIds = source.accessGrants.users.joinToString(",") { user -> user.id },
                images = source.coverImages.map { image ->
                    UploadedImage(url = image.url, hash = image.hash, width = image.width, height = image.height)
                },
                coverHashes = source.coverImages.map { it.hash },
            )
        }
    }

    private fun loadDraft(id: String) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { app.topicApi.draft(id) }
                .onSuccess { draft ->
                    _state.update {
                        it.copy(
                            loading = false,
                            title = draft.title,
                            content = draft.contentMarkdown,
                            category = draft.category,
                            sections = draft.sections.toSet(),
                            isNsfw = draft.isNsfw,
                            coverHashes = draft.coverImageHashes,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e)) }
                }
        }
    }

    fun saveDraft() {
        val s = _state.value
        if (s.savingDraft || s.saving) return
        if (s.title.isBlank() && s.content.isBlank()) {
            _state.update { it.copy(error = "草稿还是空的，写点内容再保存吧") }
            return
        }
        _state.update { it.copy(savingDraft = true, error = null) }
        viewModelScope.launch {
            runCatching {
                app.topicApi.createDraft(
                    TopicDraftCreate(
                        title = s.title,
                        contentMarkdown = s.content,
                        category = s.category,
                        sections = s.sections.toList(),
                        isNsfw = s.isNsfw,
                        coverImageHashes = s.coverHashes,
                    ),
                    idempotencyKey = draftKey,
                )
            }.onSuccess {
                draftKey = UUID.randomUUID().toString()
                _state.update { it.copy(savingDraft = false, message = "草稿已保存，可在「草稿箱」继续编辑") }
            }.onFailure { e ->
                _state.update { it.copy(savingDraft = false, error = friendlyError(e)) }
            }
        }
    }

    fun submit() {
        val s = _state.value
        if (s.saving || s.savingDraft) return
        val validation = validate(s)
        if (validation != null) {
            _state.update { it.copy(error = validation) }
            return
        }
        _state.update { it.copy(saving = true, error = null) }
        viewModelScope.launch {
            runCatching {
                if (s.editingTopicId != null) {
                    app.topicApi.updateTopic(s.editingTopicId, toPatch(s))
                } else {
                    app.topicApi.createTopic(toCreate(s), idempotencyKey = publishKey)
                }
            }.onSuccess { topic ->
                publishKey = UUID.randomUUID().toString()
                _state.update { it.copy(saving = false, publishedTopicId = topic.id) }
            }.onFailure { e ->
                _state.update { it.copy(saving = false, error = friendlyError(e)) }
            }
        }
    }

    private fun validate(s: EditorState): String? = when {
        s.title.isBlank() -> "请填写话题标题"
        s.content.isBlank() -> "请填写话题正文"
        s.category == null -> "请选择话题分类"
        s.sections.isEmpty() -> "请至少选择一个版块"
        s.accessScope == "role" && s.accessRoles.isEmpty() -> "请至少选择一个可见角色"
        s.accessScope == "users" && parseUserIds(s.accessUserIds).isEmpty() -> "请至少填写一位可见用户"
        else -> null
    }

    private fun parseUserIds(raw: String): List<String> =
        raw.split(',', '，', ' ', '\n').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

    private fun toCreate(s: EditorState) = TopicCreate(
        title = s.title.trim(),
        contentMarkdown = s.content,
        category = s.category.orEmpty(),
        sections = s.sections.toList(),
        isNsfw = s.isNsfw,
        coverImageHashes = s.coverHashes,
        accessScope = s.accessScope,
        accessRoles = if (s.accessScope == "role") s.accessRoles.toList() else null,
        accessUserIds = if (s.accessScope == "users") parseUserIds(s.accessUserIds) else null,
    )

    private fun toPatch(s: EditorState) = TopicPatch(
        title = s.title.trim(),
        contentMarkdown = s.content,
        category = s.category,
        sections = s.sections.toList(),
        isNsfw = s.isNsfw,
        coverImageHashes = s.coverHashes,
        accessScope = s.accessScope,
        accessRoles = if (s.accessScope == "role") s.accessRoles.toList() else null,
        accessUserIds = if (s.accessScope == "users") parseUserIds(s.accessUserIds) else null,
    )
}
