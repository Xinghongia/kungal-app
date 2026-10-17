package com.kungal.app.ui.toolset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.api.ToolsetListQuery
import com.kungal.core.model.ToolsetSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToolsetListState(
    val query: String = "",
    val type: String? = null,
    val platform: String? = null,
    val release: String? = null,
    val sortField: String = "resource_updated",
    val sortOrder: String = "desc",
    val items: List<ToolsetSummary> = emptyList(),
    val page: Int = 0,
    val total: Int = 0,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

/** 工具集列表（BP-03 U4，对应网页 /toolset）。 */
class ToolsetListViewModel(private val app: KungalApp) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 24
    }

    private val _state = MutableStateFlow(ToolsetListState())
    val state: StateFlow<ToolsetListState> = _state.asStateFlow()

    private var loadJob: Job? = null

    init {
        loadPage(1)
    }

    fun setQuery(value: String) {
        val trimmed = value.trim()
        if (_state.value.query == trimmed) return
        _state.update { it.copy(query = trimmed) }
        loadPage(1)
    }

    fun setType(value: String?) = updateFilters { it.copy(type = value) }

    fun setPlatform(value: String?) = updateFilters { it.copy(platform = value) }

    fun setRelease(value: String?) = updateFilters { it.copy(release = value) }

    fun setSortField(value: String) = updateFilters { it.copy(sortField = value) }

    fun setSortOrder(value: String) = updateFilters { it.copy(sortOrder = value) }

    fun resetFilters() = updateFilters {
        it.copy(type = null, platform = null, release = null, sortField = "resource_updated", sortOrder = "desc")
    }

    fun refresh() = loadPage(1, pull = true)

    fun retry() = loadPage(1)

    /** 页码分页：直接打开某一页（替换当前列表）。 */
    fun goToPage(target: Int) {
        val current = _state.value
        if (current.loading || target < 1 || target == current.page) return
        loadPage(target)
    }

    private fun updateFilters(block: (ToolsetListState) -> ToolsetListState) {
        _state.update(block)
        loadPage(1)
    }

    private fun loadPage(targetPage: Int, pull: Boolean = false) {
        loadJob?.cancel()
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
            val snapshot = _state.value
            runCatching {
                app.toolsetApi.list(
                    ToolsetListQuery(
                        page = targetPage,
                        limit = PAGE_SIZE,
                        sort = toolsetSortToken(snapshot.sortField, snapshot.sortOrder),
                        toolsetType = snapshot.type,
                        platform = snapshot.platform,
                        releaseChannel = snapshot.release,
                        q = snapshot.query.ifBlank { null },
                    ),
                )
            }.onSuccess { page ->
                _state.update { prev ->
                    prev.copy(
                        items = page.items,
                        page = targetPage,
                        total = page.total,
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
