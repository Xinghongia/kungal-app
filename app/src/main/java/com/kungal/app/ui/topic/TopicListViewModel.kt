package com.kungal.app.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.core.model.Section
import com.kungal.core.model.Topic
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/** 排序字段（与官方网站话题列表一致，见 apps/web/app/components/topic/Container.vue）。 */
data class SortField(val key: String, val label: String)

val TopicSortFields = listOf(
    SortField("bumped", "更新时间"),
    SortField("created", "创建时间"),
    SortField("views_1d", "日浏览数"),
    SortField("views_7d", "周浏览数"),
    SortField("views_30d", "月浏览数"),
    SortField("views", "总浏览数"),
)

class TopicListViewModel(
    private val app: KungalApp,
    defaultSort: String = "bumped_desc",
) : ViewModel() {

    data class State(
        val loading: Boolean = false,
        val refreshing: Boolean = false,
        val items: List<Topic> = emptyList(),
        val nextCursor: String? = null,
        val endReached: Boolean = false,
        /** null = 全部话题；否则为站点 TopicCategory（galgame / technique / others） */
        val category: String? = null,
        /** 子分类（版块）键，如 g-news；null = 该分类全部版块 */
        val section: String? = null,
        val sections: List<Section> = emptyList(),
        val sectionsLoading: Boolean = false,
        val sortField: String = "bumped",
        val sortOrder: String = "desc",
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var sectionsJob: Job? = null

    init {
        parseDefaultSort(defaultSort)?.let { (field, order) ->
            _state.update { it.copy(sortField = field, sortOrder = order) }
        }
        refresh()
    }

    fun refresh() = load(reset = true)

    fun loadMore() {
        val current = _state.value
        if (current.loading || current.nextCursor == null) return
        load(reset = false)
    }

    fun setCategory(category: String?) {
        if (_state.value.category == category) return
        _state.update {
            it.copy(category = category, section = null, sections = emptyList())
        }
        loadSections()
        load(reset = true)
    }

    fun setSection(section: String?) {
        if (_state.value.section == section) return
        _state.update { it.copy(section = section) }
        load(reset = true)
    }

    fun setSortField(field: String) {
        if (_state.value.sortField == field) return
        _state.update { it.copy(sortField = field) }
        load(reset = true)
    }

    fun setSortOrder(order: String) {
        if (_state.value.sortOrder == order) return
        _state.update { it.copy(sortOrder = order) }
        load(reset = true)
    }

    private fun loadSections() {
        val category = _state.value.category ?: return
        sectionsJob?.cancel()
        sectionsJob = viewModelScope.launch {
            _state.update { it.copy(sectionsLoading = true) }
            runCatching { app.sectionApi.list(category) }
                .onSuccess { page ->
                    _state.update {
                        // 请求期间分类可能已被切换，丢弃过期结果
                        if (it.category != category) it
                        else it.copy(sections = page.items, sectionsLoading = false)
                    }
                }
                .onFailure { e ->
                    if (e is kotlinx.coroutines.CancellationException) return@onFailure
                    _state.update { it.copy(sectionsLoading = false) }
                }
        }
    }

    private fun load(reset: Boolean) {
        val current = _state.value
        if (reset && current.loading) {
            loadJob?.cancel()
        }
        if (!reset && (current.loading || current.nextCursor == null)) return
        val category = current.category
        val section = current.section
        val sort = "${current.sortField}_${current.sortOrder}"
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(
                    loading = true,
                    refreshing = reset && it.items.isNotEmpty(),
                    error = null,
                    items = if (reset) emptyList() else it.items,
                    nextCursor = if (reset) null else it.nextCursor,
                )
            }
            runCatching {
                app.topicApi.list(
                    cursor = if (reset) null else current.nextCursor,
                    limit = 20,
                    sort = sort,
                    category = category,
                    section = section,
                )
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
                if (e is kotlinx.coroutines.CancellationException) return@onFailure
                _state.update {
                    it.copy(loading = false, refreshing = false, error = friendlyError(e))
                }
            }
        }
    }

    private fun parseDefaultSort(token: String): Pair<String, String>? {
        if (token.isBlank()) return null
        val order = when {
            token.endsWith("_asc") -> "asc"
            token.endsWith("_desc") -> "desc"
            else -> return null
        }
        val field = token.removeSuffix("_asc").removeSuffix("_desc")
        if (TopicSortFields.none { it.key == field }) return null
        return field to order
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
