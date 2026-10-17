package com.kungal.app.ui.galgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.core.api.LibraryWorkQuery
import com.kungal.core.api.WorkListQuery
import com.kungal.core.model.TagBlock
import com.kungal.core.model.WorkCollectedMonth
import com.kungal.core.model.WorkSummary
import com.kungal.app.ui.topic.friendlyError
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class GalgameListMode { RESOURCE, LIBRARY }

/**
 * 资料库筛选状态。资源资料库用完整轴；信息资料库只用排序/年份
 * （与网页 /galgame 与 /gallib 的实际筛选面一致）。
 */
data class GalgameFilterState(
    val sortField: String = "time",
    val sortOrder: String = "desc",
    val platforms: Set<String> = emptySet(),
    val runtimes: Set<String> = emptySet(),
    val gameType: String? = null,
    val resourceType: String? = null,
    val languages: Set<String> = emptySet(),
    val includeProviders: Set<String> = emptySet(),
    val excludeOnlyProviders: Set<String> = emptySet(),
    val minRating: Int = 0,
    val minRatingCount: Int = 0,
    val releasedFrom: String = "",
    val releasedTo: String = "",
    val releasedMonths: Set<Int> = emptySet(),
    val collectedFrom: String = "",
    val collectedTo: String = "",
    val collectedMonths: Set<Int> = emptySet(),
    val includeAllOriginalLanguages: Boolean = false,
) {
    /** 有多少个「更多筛选」条件生效（用于筛选按钮上的计数）。 */
    val advancedCount: Int
        get() = listOf(
            includeProviders.isNotEmpty(),
            excludeOnlyProviders.isNotEmpty(),
            minRating > 0,
            minRatingCount > 0,
            releasedFrom.isNotBlank() || releasedTo.isNotBlank(),
            releasedMonths.isNotEmpty(),
            collectedFrom.isNotBlank() || collectedTo.isNotBlank(),
            collectedMonths.isNotEmpty(),
            includeAllOriginalLanguages,
        ).count { it }

    val hasAnyFilter: Boolean
        get() = platforms.isNotEmpty() || runtimes.isNotEmpty() || gameType != null ||
            resourceType != null || languages.isNotEmpty() || advancedCount > 0
}

data class GalgameListState(
    val filters: GalgameFilterState = GalgameFilterState(),
    val items: List<WorkSummary> = emptyList(),
    val page: Int = 0,
    val total: Int = 0,
    val totalRelation: String? = null,
    val collectedMonths: List<WorkCollectedMonth> = emptyList(),
    val loading: Boolean = false,
    val refreshing: Boolean = false,
    val loadingMore: Boolean = false,
    val endReached: Boolean = false,
    val error: String? = null,
)

class GalgameListViewModel(
    private val app: KungalApp,
    val mode: GalgameListMode,
    includeNsfw: Boolean,
) : ViewModel() {

    companion object {
        const val PAGE_SIZE = 24
    }

    private val _state = MutableStateFlow(
        GalgameListState(
            filters = defaultFilters(mode),
            loading = true,
        ),
    )
    val state: StateFlow<GalgameListState> = _state.asStateFlow()

    private var includeNsfw = includeNsfw
    private var tagBlocks: List<TagBlock> = emptyList()
    private var loadJob: Job? = null

    init {
        viewModelScope.launch {
            if (app.isLoggedIn) {
                runCatching { app.galgameApi.tagBlocks() }
                    .onSuccess { tagBlocks = it }
            }
            runCatching { app.galgameApi.collectedMonths(includeNsfw) }
                .onSuccess { months -> _state.update { it.copy(collectedMonths = months.items) } }
            load(reset = true)
        }
    }

    fun setIncludeNsfw(value: Boolean) {
        if (includeNsfw == value) return
        includeNsfw = value
        viewModelScope.launch {
            runCatching { app.galgameApi.collectedMonths(includeNsfw) }
                .onSuccess { months -> _state.update { it.copy(collectedMonths = months.items) } }
        }
        load(reset = true)
    }

    fun refresh() = load(reset = true, pull = true)

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.loadingMore || current.endReached) return
        load(reset = false)
    }

    fun setSortField(field: String) = updateFilters { it.copy(sortField = field) }

    fun setSortOrder(order: String) = updateFilters { it.copy(sortOrder = order) }

    fun togglePlatform(value: String) = updateFilters {
        it.copy(platforms = it.platforms.toggle(value))
    }

    fun toggleRuntime(value: String) = updateFilters {
        it.copy(runtimes = it.runtimes.toggle(value))
    }

    fun setGameType(value: String?) = updateFilters { it.copy(gameType = value) }

    fun setResourceType(value: String?) = updateFilters { it.copy(resourceType = value) }

    fun toggleLanguage(value: String) = updateFilters {
        it.copy(languages = it.languages.toggle(value))
    }

    fun clearPlatforms() = updateFilters { it.copy(platforms = emptySet()) }

    fun clearRuntimes() = updateFilters { it.copy(runtimes = emptySet()) }

    fun clearLanguages() = updateFilters { it.copy(languages = emptySet()) }

    fun resetFilters() = updateFilters { defaultFilters(mode) }

    /** 资源资料库「更多筛选」一次性应用（年份文本等由弹层统一提交）。 */
    fun applyAdvanced(filters: GalgameFilterState) = updateFilters { filters }

    private fun updateFilters(block: (GalgameFilterState) -> GalgameFilterState) {
        _state.update { it.copy(filters = block(it.filters)) }
        load(reset = true)
    }

    private fun load(reset: Boolean, pull: Boolean = false) {
        val current = _state.value
        if (reset) {
            loadJob?.cancel()
        } else if (current.loading || current.loadingMore || current.endReached) {
            return
        }
        val nextPage = if (reset) 1 else current.page + 1
        val filters = current.filters
        loadJob = viewModelScope.launch {
            _state.update {
                when {
                    reset && pull && it.items.isNotEmpty() -> it.copy(
                        refreshing = true,
                        error = null,
                    )
                    reset -> it.copy(
                        loading = true,
                        refreshing = false,
                        error = null,
                        items = emptyList(),
                        page = 0,
                        total = 0,
                        endReached = false,
                    )
                    else -> it.copy(loadingMore = true, error = null)
                }
            }
            runCatching {
                when (mode) {
                    GalgameListMode.RESOURCE -> app.galgameApi.works(
                        buildWorkQuery(filters, nextPage),
                    )
                    GalgameListMode.LIBRARY -> app.galgameApi.libraryWorks(
                        buildLibraryQuery(filters, nextPage),
                    )
                }
            }.onSuccess { page ->
                _state.update { prev ->
                    val merged = if (reset) page.items else prev.items + page.items
                    prev.copy(
                        items = merged,
                        page = nextPage,
                        total = page.total,
                        totalRelation = page.totalRelation,
                        loading = false,
                        refreshing = false,
                        loadingMore = false,
                        endReached = page.items.isEmpty() || merged.size >= page.total,
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

    private fun buildWorkQuery(filters: GalgameFilterState, page: Int): WorkListQuery {
        val (platforms, runtimes) = splitPlatformAxis(filters.platforms, filters.runtimes)
        return WorkListQuery(
            page = page,
            limit = PAGE_SIZE,
            sort = workSortToken(filters.sortField, filters.sortOrder),
            resourceType = filters.resourceType,
            resourcePlatforms = platforms,
            resourceRuntimes = runtimes,
            resourceLanguages = filters.languages.toList(),
            gameType = filters.gameType,
            resourceProviders = filters.includeProviders.toList(),
            excludedSoleProviders = filters.excludeOnlyProviders.toList(),
            releasedFrom = filters.releasedFrom.ifBlank { null },
            releasedTo = filters.releasedTo.ifBlank { null },
            releasedMonths = filters.releasedMonths.sorted(),
            collectedFrom = filters.collectedFrom.ifBlank { null },
            collectedTo = filters.collectedTo.ifBlank { null },
            collectedMonths = filters.collectedMonths.sorted(),
            minRating = filters.minRating.takeIf { it > 0 }?.toDouble(),
            minRatingCount = filters.minRatingCount.takeIf { it > 0 },
            includeNsfw = includeNsfw,
            includeAllOriginalLanguages = filters.includeAllOriginalLanguages,
            excludeTagIds = tagBlocks.filter { it.mode == "exact" }.map { it.id },
            excludeTagTrees = tagBlocks.filter { it.mode != "exact" }.map { it.id },
        )
    }

    private fun buildLibraryQuery(filters: GalgameFilterState, page: Int): LibraryWorkQuery =
        LibraryWorkQuery(
            page = page,
            limit = PAGE_SIZE,
            sort = librarySortToken(filters.sortField, filters.sortOrder),
            releasedFrom = filters.releasedFrom.ifBlank { null },
            releasedTo = filters.releasedTo.ifBlank { null },
            includeNsfw = includeNsfw,
            includeAllOriginalLanguages = filters.includeAllOriginalLanguages,
            excludeTagIds = tagBlocks.filter { it.mode == "exact" }.map { it.id },
            excludeTagTrees = tagBlocks.filter { it.mode != "exact" }.map { it.id },
        )

    private fun defaultFilters(mode: GalgameListMode) = GalgameFilterState(
        sortField = if (mode == GalgameListMode.RESOURCE) "time" else "popularity",
    )

    private fun Set<String>.toggle(value: String): Set<String> =
        if (value in this) this - value else this + value
}
