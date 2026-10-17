package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.FeedActivity
import com.kungal.core.net.PageList

/**
 * 全站动态（/activities）。当前用于话题列表补充收藏数
 * （话题摘要不含 favorite_count，网页首页卡片取自动态的 topic_digest）。
 */
class ActivityApi(private val client: KungalClient) {

    suspend fun activities(
        cursor: String? = null,
        limit: Int = 20,
        activityTypes: List<String> = emptyList(),
        sort: String? = null,
        topicSections: String? = null,
        includeNsfw: Boolean = false,
    ): PageList<FeedActivity> = client.get(
        path = "/activities",
        query = buildMap {
            put("cursor", cursor)
            put("limit", limit)
            if (activityTypes.isNotEmpty()) put("activity_types", activityTypes)
            put("sort", sort)
            put("topic_sections", topicSections)
            put("include_nsfw", includeNsfw)
        },
        deserializer = PageList.serializer(FeedActivity.serializer()),
    )
}
