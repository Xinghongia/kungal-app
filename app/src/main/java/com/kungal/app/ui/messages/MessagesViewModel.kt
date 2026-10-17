package com.kungal.app.ui.messages

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.NotificationSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MessagesViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = false,
        val refreshing: Boolean = false,
        val summary: NotificationSummary? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun refresh() {
        if (!app.isLoggedIn) {
            _state.value = State()
            return
        }
        // 已有汇总时静默刷新，避免每次回到消息页都闪加载态；下拉刷新显示指示器
        val hasSummary = _state.value.summary != null
        _state.update {
            it.copy(
                loading = !hasSummary,
                refreshing = hasSummary,
                error = null,
            )
        }
        viewModelScope.launch {
            runCatching { app.notificationApi.summary() }
                .onSuccess { summary ->
                    _state.value = State(loading = false, refreshing = false, summary = summary)
                    app.refreshMessageBadge()
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(loading = false, refreshing = false, error = friendlyError(e))
                    }
                }
        }
    }
}
