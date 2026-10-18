package com.kungal.app.ui.galgame

import com.kungal.core.model.ReleaseCalendarMonth
import com.kungal.core.model.WorkSummary
import java.time.YearMonth

/**
 * 月历的纯展示逻辑：月份网格、按日聚合、今日/过去/未来状态。
 * 与网页 components/galgame/calendar/Month.vue、MonthList.vue 的口径一致：
 * 日期精度为月或没有发售日的作品归入「本月内 · 日期待定」，
 * 今日有作品时置顶，其余按日升序。
 */

val calendarWeekdays: List<String> = listOf("日", "一", "二", "三", "四", "五", "六")
private val weekdayNames: List<String> = listOf("周日", "周一", "周二", "周三", "周四", "周五", "周六")

enum class CalendarDayStatus { PAST, TODAY, FUTURE }

data class CalendarDayRow(
    val day: Int,
    val weekday: String,
    val isBucket: Boolean,
    val status: CalendarDayStatus,
    val items: List<WorkSummary>,
)

/** "2026-10" → "2026 年 10 月"；无法解析时原样返回。 */
fun calendarMonthLabel(month: String): String {
    val parts = month.split('-')
    val year = parts.getOrNull(0)?.toIntOrNull() ?: return month
    val monthNumber = parts.getOrNull(1)?.toIntOrNull() ?: return month
    return "$year 年 $monthNumber 月"
}

/** 周日开头的 7 列网格；首尾用 null 补齐。非法月份返回空列表。 */
fun calendarMonthCells(calendarMonth: String): List<Int?> {
    val ym = runCatching { YearMonth.parse(calendarMonth) }.getOrNull() ?: return emptyList()
    val cells = mutableListOf<Int?>()
    repeat(ym.atDay(1).dayOfWeek.value % 7) { cells.add(null) }
    for (day in 1..ym.lengthOfMonth()) cells.add(day)
    while (cells.size % 7 != 0) cells.add(null)
    return cells
}

/** 作品在月历里的「日」；日期精度为月、缺日期或不属于本月时返回 null（归入待定）。 */
fun calendarReleaseDay(work: WorkSummary, calendarMonth: String): Int? {
    if (work.releaseDatePrecision == "month") return null
    val date = work.releaseDate ?: return null
    if (date.length < 10 || !date.startsWith("$calendarMonth-")) return null
    return date.substring(8, 10).toIntOrNull()
}

fun calendarDayRows(data: ReleaseCalendarMonth): List<CalendarDayRow> {
    val todayMonth = data.todayDate.take(7)
    val todayDay = if (todayMonth == data.calendarMonth) {
        data.todayDate.drop(8).take(2).toIntOrNull()
    } else {
        null
    }
    val monthCmp = data.calendarMonth.compareTo(todayMonth)

    fun statusOf(day: Int): CalendarDayStatus = when {
        monthCmp < 0 -> CalendarDayStatus.PAST
        monthCmp > 0 -> CalendarDayStatus.FUTURE
        day == todayDay -> CalendarDayStatus.TODAY
        todayDay != null && day < todayDay -> CalendarDayStatus.PAST
        else -> CalendarDayStatus.FUTURE
    }

    val byDay = sortedMapOf<Int, MutableList<WorkSummary>>()
    val bucket = mutableListOf<WorkSummary>()
    data.items.forEach { work ->
        val day = calendarReleaseDay(work, data.calendarMonth)
        if (day == null) {
            bucket += work
        } else {
            byDay.getOrPut(day) { mutableListOf() } += work
        }
    }

    val ym = runCatching { YearMonth.parse(data.calendarMonth) }.getOrNull()

    fun rowOf(day: Int, items: List<WorkSummary>): CalendarDayRow {
        val weekday = ym?.atDay(day.coerceIn(1, ym.lengthOfMonth()))?.dayOfWeek?.value
            ?.let { weekdayNames[it % 7] }
            .orEmpty()
        return CalendarDayRow(day, weekday, false, statusOf(day), items)
    }

    val rows = mutableListOf<CalendarDayRow>()
    if (todayDay != null && byDay.containsKey(todayDay)) {
        rows += rowOf(todayDay, byDay.getValue(todayDay))
    }
    byDay.forEach { (day, items) ->
        if (day != todayDay) rows += rowOf(day, items)
    }
    if (bucket.isNotEmpty()) {
        rows += CalendarDayRow(0, "", true, CalendarDayStatus.FUTURE, bucket)
    }
    return rows
}

/** 网格角标用：日 → 作品数（不含待定桶）。 */
fun calendarDayCounts(rows: List<CalendarDayRow>): Map<Int, Int> =
    rows.filterNot { it.isBucket }.associate { it.day to it.items.size }
