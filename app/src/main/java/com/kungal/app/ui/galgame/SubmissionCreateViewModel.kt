package com.kungal.app.ui.galgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.WorkSubmissionCreate
import com.kungal.core.model.WorkSubmissionCreated
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class SubmissionCreateState(
    val step: Int = 1,
    val draft: SubmissionFormDraft = SubmissionFormDraft(),
    val submitting: Boolean = false,
    /** 首次提交撞上 DUPLICATE_SUSPECTS 后，等待作者确认的请求体。 */
    val duplicateConfirm: WorkSubmissionCreate? = null,
    /** 创建成功；界面据此跳转。 */
    val submitted: WorkSubmissionCreated? = null,
)

/**
 * 新建 Galgame 申请的向导：四步表单、封面上传、重复确认与提交。
 * 首次提交与确认后重发都用新的幂等键（409 时服务端没有写入）。
 */
class SubmissionCreateViewModel(
    private val app: KungalApp,
    initialTitle: String,
) : ViewModel() {

    companion object {
        const val LAST_STEP = 4
    }

    private val _state = MutableStateFlow(
        SubmissionCreateState(
            draft = if (initialTitle.isBlank()) {
                SubmissionFormDraft()
            } else {
                SubmissionFormDraft(titles = SubmissionFormDraft().titles + ("zh-Hans" to initialTitle))
            },
        ),
    )
    val state: StateFlow<SubmissionCreateState> = _state.asStateFlow()

    fun setTitle(tag: String, value: String) =
        updateDraft { it.copy(titles = it.titles + (tag to value)) }

    fun setIntroduction(tag: String, value: String) =
        updateDraft { it.copy(introductions = it.introductions + (tag to value)) }

    fun addAlias(raw: String) {
        val alias = raw.trim()
        if (alias.isEmpty()) return
        if (alias.length > 500) {
            AppMessages.show("每个别名最多 500 个字符")
            return
        }
        if (_state.value.draft.aliases.size >= 100) {
            AppMessages.show("别名最多 100 个")
            return
        }
        if (_state.value.draft.aliases.contains(alias)) {
            AppMessages.show("这个别名已经添加过了")
            return
        }
        updateDraft { it.copy(aliases = it.aliases + alias) }
    }

    fun removeAlias(alias: String) =
        updateDraft { it.copy(aliases = it.aliases - alias) }

    fun setContentLimit(value: String) = updateDraft { it.copy(contentLimit = value) }

    fun setAgeLimitR18(value: Boolean) = updateDraft { it.copy(ageLimitR18 = value) }

    fun setOriginalLanguage(value: String) = updateDraft { it.copy(originalLanguage = value) }

    fun setReleaseDate(value: String) =
        updateDraft { it.copy(releaseDate = value, releaseDateTba = false) }

    fun setReleaseDateTba(value: Boolean) =
        updateDraft { it.copy(releaseDateTba = value) }

    fun setBannerHash(hash: String?) = updateDraft { it.copy(bannerHash = hash) }

    fun goTo(step: Int) {
        if (step in 1..LAST_STEP) _state.update { it.copy(step = step) }
    }

    fun next() = goTo(_state.value.step + 1)

    fun previous() = goTo(_state.value.step - 1)

    fun submit() {
        val current = _state.value
        if (current.submitting) return
        val validation = validateSubmissionDraft(current.draft)
        if (validation != null) {
            AppMessages.show(validation.message)
            goTo(validation.step)
            return
        }
        create(buildSubmissionRequest(current.draft), confirmed = false)
    }

    /** 作者在「资料库已有同名作品」提示里确认是不同作品后重发。 */
    fun confirmDuplicate() {
        val request = _state.value.duplicateConfirm ?: return
        _state.update { it.copy(duplicateConfirm = null) }
        create(request, confirmed = true)
    }

    fun cancelDuplicate() = _state.update { it.copy(duplicateConfirm = null) }

    fun consumeSubmitted() = _state.update { it.copy(submitted = null) }

    private fun create(request: WorkSubmissionCreate, confirmed: Boolean) {
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            val body = if (confirmed) request.copy(isDuplicateConfirmed = true) else request
            runCatching {
                app.submissionApi.create(body, idempotencyKey = UUID.randomUUID().toString())
            }.onSuccess { response ->
                val created = response.body
                if (body.bannerHash != null && !created.hasBannerAttached) {
                    AppMessages.show("封面未能附加到作品，可在资料编辑页补充封面")
                }
                _state.update { it.copy(submitting = false, submitted = created) }
            }.onFailure { error ->
                _state.update { it.copy(submitting = false) }
                val code = (error as? KungalApiException)?.problem?.code
                if (code == "DUPLICATE_SUSPECTS" && !confirmed) {
                    _state.update { it.copy(duplicateConfirm = request) }
                } else {
                    AppMessages.show(friendlyError(error))
                }
            }
        }
    }

    private fun updateDraft(block: (SubmissionFormDraft) -> SubmissionFormDraft) =
        _state.update { it.copy(draft = block(it.draft)) }
}
