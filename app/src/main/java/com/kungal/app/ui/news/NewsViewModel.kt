package com.kungal.app.ui.news

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.NewsItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 情报列表筛选：lane/source 为 null 表示全部；year 与 month 分开可选。 */
data class NewsFilters(
    val lane: String? = null,
    val source: String? = null,
    val year: Int? = null,
    val month: Int? = null,
) {
    val isFiltered: Boolean get() = lane != null || source != null || year != null || month != null
}

data class NewsFeedState(
    val items: List<NewsItem> = emptyList(),
    val total: Int = 0,
    val loading: Boolean = true,
    val loadingMore: Boolean = false,
    val ended: Boolean = false,
    val error: String? = null,
)

/**
 * 情报总览的游标列表（网页 /news）：累积条目后由界面按 (日期, 来源) 分组，
 * 这样跨页的同一组不会被拆成两个抬头。
 */
class NewsFeedViewModel(private val app: KungalApp) : ViewModel() {

    private val _state = MutableStateFlow(NewsFeedState())
    val state: StateFlow<NewsFeedState> = _state.asStateFlow()

    private var filters = NewsFilters()
    private var loadedFilters: NewsFilters? = null
    private var cursor: String? = null
    private var generation = 0
    private var loadJob: Job? = null

    /** 筛选变化时重来；同一组筛选已经加载过（或正在加载）时不重复请求。 */
    fun load(newFilters: NewsFilters, force: Boolean = false) {
        if (!force && loadedFilters == newFilters) return
        loadedFilters = newFilters
        filters = newFilters
        generation++
        loadJob?.cancel()
        cursor = null
        _state.value = NewsFeedState(loading = true)
        val gen = generation
        loadJob = viewModelScope.launch {
            runCatching { fetch(cursor = null) }
                .onSuccess { page ->
                    if (gen != generation) return@onSuccess
                    cursor = page.nextCursor
                    _state.value = NewsFeedState(
                        items = page.items,
                        total = page.total ?: 0,
                        loading = false,
                        ended = page.nextCursor == null,
                    )
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    if (gen != generation) return@onFailure
                    _state.value = NewsFeedState(loading = false, error = friendlyError(e))
                }
        }
    }

    fun loadMore() {
        val current = _state.value
        val next = cursor ?: return
        if (current.loading || current.loadingMore || current.ended) return
        val gen = generation
        _state.update { it.copy(loadingMore = true, error = null) }
        viewModelScope.launch {
            runCatching { fetch(cursor = next) }
                .onSuccess { page ->
                    if (gen != generation) return@onSuccess
                    cursor = page.nextCursor
                    _state.update {
                        it.copy(
                            items = it.items + page.items,
                            total = page.total ?: it.total,
                            loadingMore = false,
                            ended = page.nextCursor == null,
                        )
                    }
                }
                .onFailure { e ->
                    if (e is CancellationException) throw e
                    if (gen != generation) return@onFailure
                    _state.update { it.copy(loadingMore = false, error = friendlyError(e)) }
                }
        }
    }

    fun retry() = load(filters, force = true)

    private suspend fun fetch(cursor: String?) = app.newsApi.items(
        cursor = cursor,
        limit = NEWS_PAGE_SIZE,
        includeTotal = true,
        lane = filters.lane,
        source = filters.source,
        year = filters.year,
        month = filters.month,
    )
}

const val NEWS_PAGE_SIZE = 20
