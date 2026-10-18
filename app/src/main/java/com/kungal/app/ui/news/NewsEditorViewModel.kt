package com.kungal.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.NewsSubmission
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class NewsEditorState(
    val loading: Boolean = false,
    val error: String? = null,
    val submission: NewsSubmission? = null,
    val saving: Boolean = false,
)

/**
 * 情报投稿表单（BP-05 U6）。新建与编辑共用：有 [submissionId] 时先拉取原稿，
 * 只有 pending / published 可以编辑。封面按网页做法延迟到提交时才上传，
 * 中途放弃不消耗每日额度。
 */
class NewsEditorViewModel(
    private val app: KungalApp,
    private val submissionId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(NewsEditorState(loading = submissionId != null))
    val state: StateFlow<NewsEditorState> = _state.asStateFlow()

    private var createKey = UUID.randomUUID().toString()

    init {
        if (submissionId != null) load()
    }

    fun retry() = load()

    private fun load() {
        val id = submissionId ?: return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { app.newsSubmissionApi.submission(id) }
                .onSuccess { item -> _state.update { it.copy(loading = false, submission = item) } }
                .onFailure { e -> _state.update { it.copy(loading = false, error = friendlyError(e)) } }
        }
    }

    /**
     * 校验 →（按需上传封面）→ 新建或按差异更新。
     * [removeBanner] 表示用户清掉了已有封面；[pendingBanner] 是新选的本地图片。
     */
    fun save(
        values: NewsFormValues,
        pendingBanner: PreparedBanner?,
        removeBanner: Boolean,
        onResult: (success: Boolean, created: Boolean, message: String) -> Unit,
    ) {
        if (_state.value.saving) return
        validateNewsForm(values)?.let { message ->
            onResult(false, false, message)
            return
        }
        _state.update { it.copy(saving = true) }
        viewModelScope.launch {
            val existing = _state.value.submission
            runCatching {
                var bannerHash = existing?.banner?.hash.orEmpty()
                if (pendingBanner != null) {
                    bannerHash = app.newsSubmissionApi.uploadBanner(
                        bytes = pendingBanner.bytes,
                        fileName = pendingBanner.fileName,
                        contentType = pendingBanner.contentType,
                        idempotencyKey = UUID.randomUUID().toString(),
                    ).hash
                } else if (removeBanner) {
                    bannerHash = ""
                }
                if (existing == null) {
                    val created = app.newsSubmissionApi.create(
                        body = newsCreateBody(values, bannerHash),
                        idempotencyKey = createKey,
                    )
                    createKey = UUID.randomUUID().toString()
                    created to true
                } else {
                    val patch = newsPatchBody(existing, values, bannerHash)
                    val saved = if (patch == null) existing else app.newsSubmissionApi.update(existing.id, patch)
                    saved to false
                }
            }.onSuccess { (item, created) ->
                _state.update { it.copy(saving = false, submission = item) }
                val message = when {
                    created -> "投稿成功，审核通过后会出现在 Galgame 情报中"
                    item.state == "published" -> "已保存，情报将重新进入审核"
                    else -> "已保存"
                }
                onResult(true, created, message)
            }.onFailure { e ->
                _state.update { it.copy(saving = false) }
                onResult(false, false, friendlyError(e))
            }
        }
    }
}
