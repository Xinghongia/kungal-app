package com.kungal.app.ui.util

import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 服务端时间为 ISO-8601 UTC（如 2026-10-13T04:56:25Z）。 */
fun relativeTime(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        val time = Instant.parse(iso)
        val delta = Duration.between(time, Instant.now())
        when {
            delta.isNegative -> formatDate(time)
            delta.toMinutes() < 1 -> "刚刚"
            delta.toHours() < 1 -> "${delta.toMinutes()} 分钟前"
            delta.toDays() < 1 -> "${delta.toHours()} 小时前"
            delta.toDays() < 30 -> "${delta.toDays()} 天前"
            else -> formatDate(time)
        }
    }.getOrDefault(iso)
}

private fun formatDate(time: Instant): String =
    DateTimeFormatter.ofPattern("yyyy-MM-dd").withZone(ZoneId.systemDefault()).format(time)
