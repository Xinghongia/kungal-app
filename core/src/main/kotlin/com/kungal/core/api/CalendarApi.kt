package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.ReleaseCalendarMonth
import com.kungal.core.model.ReleaseCalendarPending
import com.kungal.core.model.ReleaseCalendarTba
import com.kungal.core.model.ReleaseCalendarToday
import com.kungal.core.model.ReleaseCalendarUpcoming

/**
 * 发售月历（BP-05 U2）。网页默认只列原语言为日语的商业作品；
 * [includeDoujin] 打开后加入同人社团作品。标签屏蔽与资料库列表一致。
 */
class CalendarApi(private val client: KungalClient) {

    suspend fun month(
        month: String? = null,
        includeNsfw: Boolean = false,
        includeDoujin: Boolean = false,
        excludeTagIds: List<String> = emptyList(),
        excludeTagTrees: List<String> = emptyList(),
    ): ReleaseCalendarMonth = client.get(
        path = "/release-calendar",
        query = commonQuery(includeNsfw, includeDoujin, excludeTagIds, excludeTagTrees)
            .plus("month" to month),
        deserializer = ReleaseCalendarMonth.serializer(),
    )

    suspend fun upcoming(
        includeNsfw: Boolean = false,
        includeDoujin: Boolean = false,
        excludeTagIds: List<String> = emptyList(),
        excludeTagTrees: List<String> = emptyList(),
    ): ReleaseCalendarUpcoming = client.get(
        path = "/release-calendar/upcoming",
        query = commonQuery(includeNsfw, includeDoujin, excludeTagIds, excludeTagTrees),
        deserializer = ReleaseCalendarUpcoming.serializer(),
    )

    suspend fun pending(
        year: Int? = null,
        includeNsfw: Boolean = false,
        includeDoujin: Boolean = false,
        excludeTagIds: List<String> = emptyList(),
        excludeTagTrees: List<String> = emptyList(),
    ): ReleaseCalendarPending = client.get(
        path = "/release-calendar/pending",
        query = commonQuery(includeNsfw, includeDoujin, excludeTagIds, excludeTagTrees)
            .plus("year" to year),
        deserializer = ReleaseCalendarPending.serializer(),
    )

    suspend fun tba(
        includeNsfw: Boolean = false,
        includeDoujin: Boolean = false,
        excludeTagIds: List<String> = emptyList(),
        excludeTagTrees: List<String> = emptyList(),
    ): ReleaseCalendarTba = client.get(
        path = "/release-calendar/tba",
        query = commonQuery(includeNsfw, includeDoujin, excludeTagIds, excludeTagTrees),
        deserializer = ReleaseCalendarTba.serializer(),
    )

    /** 今日发售判断；该端点不接受同人开关（与网页一致）。 */
    suspend fun today(
        includeNsfw: Boolean = false,
        excludeTagIds: List<String> = emptyList(),
        excludeTagTrees: List<String> = emptyList(),
    ): ReleaseCalendarToday = client.get(
        path = "/release-calendar/today",
        query = buildMap {
            put("include_nsfw", includeNsfw)
            if (excludeTagIds.isNotEmpty()) put("exclude_tag_ids", excludeTagIds)
            if (excludeTagTrees.isNotEmpty()) put("exclude_tag_trees", excludeTagTrees)
        },
        deserializer = ReleaseCalendarToday.serializer(),
    )

    private fun commonQuery(
        includeNsfw: Boolean,
        includeDoujin: Boolean,
        excludeTagIds: List<String>,
        excludeTagTrees: List<String>,
    ): Map<String, Any?> = buildMap {
        put("include_nsfw", includeNsfw)
        put("include_doujin", includeDoujin)
        if (excludeTagIds.isNotEmpty()) put("exclude_tag_ids", excludeTagIds)
        if (excludeTagTrees.isNotEmpty()) put("exclude_tag_trees", excludeTagTrees)
    }
}
