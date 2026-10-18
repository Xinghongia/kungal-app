package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 发售月历·单月（GET /release-calendar）。日期按日本时间计算。 */
@Serializable
data class ReleaseCalendarMonth(
    @SerialName("object") val obj: String = "release_calendar_month",
    @SerialName("calendar_month") val calendarMonth: String = "",
    @SerialName("today_date") val todayDate: String = "",
    val items: List<WorkSummary> = emptyList(),
    @SerialName("prev_month") val prevMonth: String? = null,
    @SerialName("next_month") val nextMonth: String? = null,
    @SerialName("has_prev") val hasPrev: Boolean = false,
    @SerialName("has_next") val hasNext: Boolean = false,
    @SerialName("min_month") val minMonth: String? = null,
    @SerialName("max_month") val maxMonth: String? = null,
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("is_truncated") val isTruncated: Boolean = false,
)

@Serializable
data class ReleaseCalendarUpcomingEntry(
    @SerialName("calendar_month") val calendarMonth: String = "",
    val items: List<WorkSummary> = emptyList(),
    @SerialName("is_truncated") val isTruncated: Boolean = false,
)

/** 发售月历·未发售（GET /release-calendar/upcoming），按月份分组，单月上限 500。 */
@Serializable
data class ReleaseCalendarUpcoming(
    @SerialName("object") val obj: String = "release_calendar_upcoming",
    @SerialName("today_date") val todayDate: String = "",
    val entries: List<ReleaseCalendarUpcomingEntry> = emptyList(),
    @SerialName("item_count") val itemCount: Int = 0,
)

/** 发售月历·年内待定（GET /release-calendar/pending）：只知年份、月份待定。 */
@Serializable
data class ReleaseCalendarPending(
    @SerialName("object") val obj: String = "release_calendar_pending",
    val year: Int = 0,
    val items: List<WorkSummary> = emptyList(),
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("is_truncated") val isTruncated: Boolean = false,
)

/** 发售月历·发售日期未定（GET /release-calendar/tba）。 */
@Serializable
data class ReleaseCalendarTba(
    @SerialName("object") val obj: String = "release_calendar_tba",
    val items: List<WorkSummary> = emptyList(),
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("is_truncated") val isTruncated: Boolean = false,
)

/** 今日发售判断（GET /release-calendar/today）。 */
@Serializable
data class ReleaseCalendarToday(
    @SerialName("object") val obj: String = "release_calendar_today",
    @SerialName("today_date") val todayDate: String = "",
    @SerialName("has_release") val hasRelease: Boolean = false,
    @SerialName("expires_in") val expiresIn: Int = 0,
)
