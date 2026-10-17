package com.kungal.app.ui.galgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.api.ResourceListQuery
import com.kungal.core.model.GalgameResource
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResourceListState(
    val query: String = "",
    val items: List<GalgameResource> = emptyList(),
    val page: Int = 0,
    val total: Int = 0,
    val totalRelation: String? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

/** 全站 Galgame 资源列表（BP-03 U1，对应网页 /galgame/resource）。 */
class ResourceListViewModel(
    private val app: KungalApp,
    includeNsfw: Boolean,
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 50
    }

    private val _state = MutableStateFlow(ResourceListState(loading = true))
    val state: StateFlow<ResourceListState> = _state.asStateFlow()

    private var includeNsfw = includeNsfw
    private var loadJob: Job? = null

    init {
        loadPage(1)
    }

    fun setIncludeNsfw(value: Boolean) {
        if (includeNsfw == value) return
        includeNsfw = value
        loadPage(1)
    }

    /** 搜索词（由界面防抖后调用）。 */
    fun setQuery(value: String) {
        val trimmed = value.trim()
        if (_state.value.query == trimmed) return
        _state.update { it.copy(query = trimmed) }
        loadPage(1)
    }

    fun refresh() = loadPage(1, pull = true)

    /** 页码分页：直接打开某一页（替换当前列表）。 */
    fun goToPage(target: Int) {
        val current = _state.value
        if (current.loading || target < 1 || target == current.page) return
        loadPage(target)
    }

    fun retry() = loadPage(1)

    private fun loadPage(targetPage: Int, pull: Boolean = false) {
        loadJob?.cancel()
        val query = _state.value.query
        loadJob = viewModelScope.launch {
            _state.update {
                if (pull && it.items.isNotEmpty()) {
                    it.copy(refreshing = true, error = null)
                } else {
                    it.copy(
                        loading = true,
                        refreshing = false,
                        error = null,
                        items = emptyList(),
                        total = 0,
                        endReached = false,
                    )
                }
            }
            runCatching {
                app.resourceApi.list(
                    ResourceListQuery(
                        page = targetPage,
                        limit = PAGE_SIZE,
                        q = query.ifBlank { null },
                        includeNsfw = includeNsfw,
                    ),
                )
            }.onSuccess { page ->
                _state.update { prev ->
                    prev.copy(
                        items = page.items,
                        page = targetPage,
                        total = page.total,
                        totalRelation = page.totalRelation,
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        endReached = page.items.isEmpty() ||
                            targetPage * PAGE_SIZE >= page.total,
                        error = null,
                    )
                }
            }.onFailure { e ->
                if (e is kotlinx.coroutines.CancellationException) return@onFailure
                _state.update {
                    it.copy(
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        error = friendlyError(e),
                    )
                }
            }
        }
    }
}
