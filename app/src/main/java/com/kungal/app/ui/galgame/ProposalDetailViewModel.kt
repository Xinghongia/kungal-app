package com.kungal.app.ui.galgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.EditForm
import com.kungal.core.model.EditProposal
import com.kungal.core.model.Work
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import java.util.UUID

data class ProposalDetailState(
    val loading: Boolean = true,
    val error: String? = null,
    val proposal: EditProposal? = null,
    val etag: String? = null,
    val form: EditForm? = null,
    val work: Work? = null,
    val names: EditNames = EmptyNames,
    val acting: Boolean = false,
    val actionError: String? = null,
)

private val EmptyNames = EditNames()

class ProposalDetailViewModel(
    private val app: KungalApp,
    private val proposalId: String,
    private val includeNsfw: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(ProposalDetailState())
    val state: StateFlow<ProposalDetailState> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { app.editApi.proposal(proposalId) }
                .onFailure { error ->
                    _state.update { it.copy(loading = false, error = friendlyError(error)) }
                }
                .onSuccess { response ->
                    val proposal = response.body
                    val work = runCatching {
                        app.galgameApi.work(proposal.workId, includeNsfw)
                    }.getOrNull()
                    val form = runCatching { app.editApi.form(proposal.workId) }.getOrNull()
                    var names = work?.let { namesFromWork(it) } ?: EditNames()
                    names = resolveEditNames(
                        app,
                        names,
                        editNameIdsFrom(proposal.effectivePatch) + editNameIdsFrom(form?.fieldValues),
                    )
                    _state.update {
                        it.copy(
                            loading = false,
                            error = null,
                            proposal = proposal,
                            etag = response.etag,
                            form = form,
                            work = work,
                            names = names,
                        )
                    }
                }
        }
    }

    /** 先保存修正（若有），再合并提案。成功返回 true。 */
    fun amendAndMerge(
        note: String,
        overrides: Map<String, JsonElement>,
        rejected: Set<String>,
    ): Boolean {
        val state = _state.value
        val proposal = state.proposal ?: return false
        if (state.acting) return false
        val effective = proposal.effectivePatch
        val set = JsonObject(
            overrides.filter { (key, value) -> value != effective[key] },
        )
        val unset = rejected.toList()
        viewModelScope.launch {
            _state.update { it.copy(acting = true, actionError = null) }
            var amended = false
            runCatching {
                var etag = state.etag
                if (set.isNotEmpty() || unset.isNotEmpty()) {
                    val amendment = app.editApi.amendProposal(
                        proposalId = proposal.id,
                        set = set,
                        unset = unset,
                        note = note.trim().ifBlank { null },
                        ifMatch = etag,
                        idempotencyKey = UUID.randomUUID().toString(),
                    )
                    amended = true
                    etag = amendment.etag ?: etag
                }
                app.editApi.updateProposal(
                    proposalId = proposal.id,
                    state = "merged",
                    note = note.trim().ifBlank { null },
                    ifMatch = etag,
                )
            }.onSuccess {
                _state.update { it.copy(acting = false) }
                AppMessages.show(
                    if (set.isNotEmpty() || unset.isNotEmpty()) "已修正并合并（双方署名）" else "提案已合并",
                )
                _state.update { it.copy(proposal = it.proposal?.copy(state = "merged")) }
            }.onFailure { error ->
                _state.update { it.copy(acting = false, actionError = friendlyError(error)) }
                AppMessages.show(friendlyError(error))
                // 修正可能已经落库；刷新提案，避免再次点合并时重复提交修正
                if (amended) load()
            }
        }
        return true
    }

    fun decline(note: String): Boolean {
        val state = _state.value
        val proposal = state.proposal ?: return false
        if (state.acting) return false
        if (note.isBlank()) {
            _state.update { it.copy(actionError = "拒绝提案需要填写审核说明") }
            return false
        }
        viewModelScope.launch {
            _state.update { it.copy(acting = true, actionError = null) }
            runCatching {
                app.editApi.updateProposal(proposal.id, "declined", note = note.trim(), ifMatch = state.etag)
            }.onSuccess { response ->
                _state.update {
                    it.copy(acting = false, proposal = response.body, etag = response.etag ?: it.etag)
                }
                AppMessages.show("提案已拒绝")
            }.onFailure { error ->
                _state.update { it.copy(acting = false, actionError = friendlyError(error)) }
                AppMessages.show(friendlyError(error))
            }
        }
        return true
    }

    fun withdraw(): Boolean {
        val state = _state.value
        val proposal = state.proposal ?: return false
        if (state.acting) return false
        viewModelScope.launch {
            _state.update { it.copy(acting = true, actionError = null) }
            runCatching {
                app.editApi.updateProposal(proposal.id, "withdrawn", ifMatch = state.etag)
            }.onSuccess { response ->
                _state.update {
                    it.copy(acting = false, proposal = response.body, etag = response.etag ?: it.etag)
                }
                AppMessages.show("提案已撤回")
            }.onFailure { error ->
                _state.update { it.copy(acting = false, actionError = friendlyError(error)) }
                AppMessages.show(friendlyError(error))
            }
        }
        return true
    }

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

private operator fun EditNameIds.plus(other: EditNameIds): EditNameIds = EditNameIds(
    tags = tags + other.tags,
    companies = companies + other.companies,
    engines = engines + other.engines,
    series = series + other.series,
    characters = characters + other.characters,
    staff = staff + other.staff,
)
