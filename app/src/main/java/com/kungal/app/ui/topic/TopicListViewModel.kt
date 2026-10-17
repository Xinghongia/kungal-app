package com.kungal.app.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.core.model.Topic
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

class TopicListViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = false,
        val refreshing: Boolean = false,
        val items: List<Topic> = emptyList(),
        val nextCursor: String? = null,
        val endReached: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() = load(reset = true)

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.nextCursor == null) return
        load(reset = false)
    }

    private fun load(reset: Boolean) {
        val current = _state.value
        if (reset && current.loading) return
        viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    refreshing = reset && it.items.isNotEmpty(),
                    error = null,
                )
            }
            runCatching {
                app.topicApi.list(cursor = if (reset) null else current.nextCursor, limit = 20)
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        items = if (reset) page.items else it.items + page.items,
                        nextCursor = page.nextCursor,
                        endReached = page.nextCursor == null,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, refreshing = false, error = friendlyError(e)) }
            }
        }
    }
}

fun friendlyError(e: Throwable): String = when (e) {
    is KungalApiException -> when {
        e.requiresLogin -> "登录已过期，请重新登录"
        e.isRateLimited -> "操作太频繁，请稍后再试"
        else -> e.message ?: "请求失败"
    }
    is IOException -> "网络连接失败，请检查网络后重试"
    else -> e.message ?: "未知错误"
}
