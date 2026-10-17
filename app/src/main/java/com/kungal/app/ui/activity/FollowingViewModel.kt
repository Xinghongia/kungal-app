package com.kungal.app.ui.activity

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.FollowingActivityGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FollowingViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = false,
        val groups: List<FollowingActivityGroup> = emptyList(),
        val nextCursor: String? = null,
        val error: String? = null,
        val unseenCount: Int = 0,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun refresh() {
        if (!app.isLoggedIn) {
            _state.update { it.copy(error = "登录后查看关注动态") }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { app.topicApi.followingSummary() }
                .onSuccess { summary -> _state.update { it.copy(unseenCount = summary.unseenCount) } }
            runCatching { app.topicApi.followingActivities(limit = 20) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(loading = false, groups = page.items, nextCursor = page.nextCursor)
                    }
                    // 打开列表后标记已读（服务端只前进不后退）
                    runCatching { app.topicApi.markFollowingSeen() }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e)) }
                }
        }
    }

    fun loadMore() {
        val cursor = _state.value.nextCursor ?: return
        if (_state.value.loading) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.followingActivities(cursor = cursor, limit = 20) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            loading = false,
                            groups = it.groups + page.items,
                            nextCursor = page.nextCursor,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e)) }
                }
        }
    }
}
