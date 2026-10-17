package com.kungal.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.TopicDraftSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class DraftsViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val items: List<TopicDraftSummary> = emptyList(),
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun consumeMessage() = _state.update { it.copy(message = null) }

    fun refresh() {
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.drafts(limit = 30) }
                .onSuccess { page ->
                    _state.update { it.copy(loading = false, items = page.items) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, message = friendlyError(e)) }
                }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { app.topicApi.deleteDraft(id) }
                .onSuccess {
                    _state.update {
                        it.copy(items = it.items.filterNot { draft -> draft.id == id }, message = "草稿已删除")
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(message = friendlyError(e)) }
                }
        }
    }
}
