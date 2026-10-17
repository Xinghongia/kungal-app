package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.TopicRankingEntry
import com.kungal.core.model.UserRankingEntry
import com.kungal.core.model.WorkRankingEntry
import com.kungal.core.net.PageList

/**
 * 排行榜（/rankings/users、/rankings/topics、/rankings/works）。
 * 站点是 top-N 列表（limit 1–100，默认 50），按 sort 降序，respond 用统一列表信封。
 */
class RankingApi(private val client: KungalClient) {

    suspend fun users(
        sort: String = "moemoepoint_desc",
        limit: Int = 50,
    ): PageList<UserRankingEntry> = client.get(
        path = "/rankings/users",
        query = mapOf("sort" to sort, "limit" to limit),
        deserializer = PageList.serializer(UserRankingEntry.serializer()),
    )

    suspend fun topics(
        sort: String = "views_desc",
        limit: Int = 50,
        includeNsfw: Boolean = false,
    ): PageList<TopicRankingEntry> = client.get(
        path = "/rankings/topics",
        query = mapOf("sort" to sort, "limit" to limit, "include_nsfw" to includeNsfw),
        deserializer = PageList.serializer(TopicRankingEntry.serializer()),
    )

    suspend fun works(
        sort: String = "views_desc",
        limit: Int = 50,
        includeNsfw: Boolean = false,
        includeResourceless: Boolean = false,
        includeAllOriginalLanguages: Boolean = false,
    ): PageList<WorkRankingEntry> = client.get(
        path = "/rankings/works",
        query = mapOf(
            "sort" to sort,
            "limit" to limit,
            "include_nsfw" to includeNsfw,
            "include_resourceless" to includeResourceless,
            "include_all_original_languages" to includeAllOriginalLanguages,
        ),
        deserializer = PageList.serializer(WorkRankingEntry.serializer()),
    )
}
