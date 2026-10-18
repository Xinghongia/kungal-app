package com.kungal.app.ui.galgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.ReleaseCalendarMonth
import com.kungal.core.model.ReleaseCalendarPending
import com.kungal.core.model.ReleaseCalendarTba
import com.kungal.core.model.ReleaseCalendarUpcoming
import com.kungal.core.model.TagBlock
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 网页 /galgame-calendar 的四个视图。 */
enum class CalendarView(val wire: String, val label: String) {
    MONTH("month", "月历"),
    UPCOMING("upcoming", "未发售"),
    PENDING("pending", "年内待定"),
    TBA("tba", "发售日期未定"),
    ;

    companion object {
        fun fromWire(value: String?): CalendarView =
            entries.firstOrNull { it.wire == value } ?: MONTH
    }
}

data class CalendarState(
    val view: CalendarView = CalendarView.MONTH,
    val includeDoujin: Boolean = false,
    val month: ReleaseCalendarMonth? = null,
    val upcoming: ReleaseCalendarUpcoming? = null,
    val pending: ReleaseCalendarPending? = null,
    val tba: ReleaseCalendarTba? = null,
    val loading: Boolean = true,
    val refreshing: Boolean = false,
    val error: String? = null,
)

/**
 * 发售月历（BP-05 U2）。四个视图各自缓存当前参数下的结果，切换 tab 不重复请求；
 * 内容姿态、同人开关或标签屏蔽变化时清空缓存重新取。
 */
class CalendarViewModel(
    private val app: KungalApp,
    initialView: CalendarView,
    initialMonth: String?,
    initialYear: Int?,
    includeNsfw: Boolean,
    includeDoujin: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(
        CalendarState(view = initialView, includeDoujin = includeDoujin),
    )
    val state: StateFlow<CalendarState> = _state.asStateFlow()

    private var nsfw = includeNsfw
    /** null = 服务端当前月。 */
    private var requestedMonth: String? = initialMonth?.takeIf { it.matches(Regex("""\d{4}-\d{2}""")) }
    /** null = 服务端当前年。 */
    private var requestedPendingYear: Int? = initialYear?.takeIf { it > 0 }
    private var tagBlocks: List<TagBlock> = emptyList()
    private var loadJob: Job? = null

    private val monthCache = mutableMapOf<String, ReleaseCalendarMonth>()
    private var upcomingCache: ReleaseCalendarUpcoming? = null
    private val pendingCache = mutableMapOf<Int, ReleaseCalendarPending>()
    private var tbaCache: ReleaseCalendarTba? = null

    init {
        viewModelScope.launch {
            if (app.isLoggedIn) {
                runCatching { app.galgameApi.tagBlocks() }
                    .onSuccess { tagBlocks = it }
            }
            load(_state.value.view)
        }
    }

    fun selectView(view: CalendarView) {
        if (view == _state.value.view) return
        _state.update { it.copy(view = view) }
        load(view)
    }

    fun setIncludeNsfw(value: Boolean) {
        if (value == nsfw) return
        nsfw = value
        clearCachesAndData()
        load(_state.value.view, force = true)
    }

    fun setIncludeDoujin(value: Boolean) {
        if (value == _state.value.includeDoujin) return
        _state.update { it.copy(includeDoujin = value) }
        clearCachesAndData()
        load(_state.value.view, force = true)
    }

    fun refresh() = load(_state.value.view, force = true, pull = true)

    fun retry() = load(_state.value.view, force = true)

    fun prevMonth() {
        val data = _state.value.month ?: return
        if (!data.hasPrev || data.prevMonth == null) return
        requestedMonth = data.prevMonth
        load(CalendarView.MONTH)
    }

    fun nextMonth() {
        val data = _state.value.month ?: return
        if (!data.hasNext || data.nextMonth == null) return
        requestedMonth = data.nextMonth
        load(CalendarView.MONTH)
    }

    fun goToday() {
        if (requestedMonth == null) return
        requestedMonth = null
        load(CalendarView.MONTH)
    }

    fun prevYear() {
        val year = _state.value.pending?.year ?: return
        if (year <= 1) return
        requestedPendingYear = year - 1
        load(CalendarView.PENDING)
    }

    fun nextYear() {
        val year = _state.value.pending?.year ?: return
        requestedPendingYear = year + 1
        load(CalendarView.PENDING)
    }

    private fun clearCachesAndData() {
        monthCache.clear()
        upcomingCache = null
        pendingCache.clear()
        tbaCache = null
        _state.update {
            it.copy(month = null, upcoming = null, pending = null, tba = null, error = null)
        }
    }

    private fun hasData(view: CalendarView): Boolean = when (view) {
        CalendarView.MONTH -> _state.value.month != null
        CalendarView.UPCOMING -> _state.value.upcoming != null
        CalendarView.PENDING -> _state.value.pending != null
        CalendarView.TBA -> _state.value.tba != null
    }

    /** 命中缓存时直接显示，不发请求。 */
    private fun applyCached(view: CalendarView): Boolean {
        when (view) {
            CalendarView.MONTH -> {
                val data = monthCache[requestedMonth ?: CURRENT_KEY] ?: return false
                _state.update { it.copy(month = data) }
            }
            CalendarView.UPCOMING -> {
                val data = upcomingCache ?: return false
                _state.update { it.copy(upcoming = data) }
            }
            CalendarView.PENDING -> {
                val data = pendingCache[requestedPendingYear ?: CURRENT_KEY_INT] ?: return false
                _state.update { it.copy(pending = data) }
            }
            CalendarView.TBA -> {
                val data = tbaCache ?: return false
                _state.update { it.copy(tba = data) }
            }
        }
        return true
    }

    private fun load(view: CalendarView, force: Boolean = false, pull: Boolean = false) {
        loadJob?.cancel()
        if (!force && applyCached(view)) {
            _state.update { it.copy(loading = false, refreshing = false, error = null) }
            return
        }
        val keepContent = hasData(view)
        loadJob = viewModelScope.launch {
            _state.update {
                it.copy(loading = !keepContent, refreshing = pull, error = null)
            }
            runCatching { fetch(view) }
                .onFailure { e ->
                    if (e is CancellationException) return@onFailure
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            error = friendlyError(e),
                        )
                    }
                }
        }
    }

    private suspend fun fetch(view: CalendarView) {
        val excludeIds = tagBlocks.filter { it.mode == "exact" }.map { it.id }
        val excludeTrees = tagBlocks.filter { it.mode != "exact" }.map { it.id }
        when (view) {
            CalendarView.MONTH -> {
                val data = app.calendarApi.month(
                    month = requestedMonth,
                    includeNsfw = nsfw,
                    includeDoujin = _state.value.includeDoujin,
                    excludeTagIds = excludeIds,
                    excludeTagTrees = excludeTrees,
                )
                monthCache[requestedMonth ?: CURRENT_KEY] = data
                _state.update { it.copy(month = data, loading = false, refreshing = false) }
            }
            CalendarView.UPCOMING -> {
                val data = app.calendarApi.upcoming(
                    includeNsfw = nsfw,
                    includeDoujin = _state.value.includeDoujin,
                    excludeTagIds = excludeIds,
                    excludeTagTrees = excludeTrees,
                )
                upcomingCache = data
                _state.update { it.copy(upcoming = data, loading = false, refreshing = false) }
            }
            CalendarView.PENDING -> {
                val data = app.calendarApi.pending(
                    year = requestedPendingYear,
                    includeNsfw = nsfw,
                    includeDoujin = _state.value.includeDoujin,
                    excludeTagIds = excludeIds,
                    excludeTagTrees = excludeTrees,
                )
                pendingCache[requestedPendingYear ?: CURRENT_KEY_INT] = data
                _state.update { it.copy(pending = data, loading = false, refreshing = false) }
            }
            CalendarView.TBA -> {
                val data = app.calendarApi.tba(
                    includeNsfw = nsfw,
                    includeDoujin = _state.value.includeDoujin,
                    excludeTagIds = excludeIds,
                    excludeTagTrees = excludeTrees,
                )
                tbaCache = data
                _state.update { it.copy(tba = data, loading = false, refreshing = false) }
            }
        }
    }

    private companion object {
        const val CURRENT_KEY = "current"
        const val CURRENT_KEY_INT = 0
    }
}
