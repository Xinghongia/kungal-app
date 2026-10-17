package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.Reply
import com.kungal.core.model.Topic
import com.kungal.core.net.PageList

/** 话题与回复的读取接口（对应 v1 /topics、/topics/{id}、/topics/{id}/replies）。 */
class TopicApi(private val client: KungalClient) {

    suspend fun list(
        cursor: String? = null,
        limit: Int = 20,
        sort: String? = null,
        category: String? = null,
        section: String? = null,
        includeNsfw: Boolean = false,
    ): PageList<Topic> = client.get(
        path = "/topics",
        query = mapOf(
            "cursor" to cursor,
            "limit" to limit,
            "sort" to sort,
            "category" to category,
            "section" to section,
            "include_nsfw" to includeNsfw,
        ),
        deserializer = PageList.serializer(Topic.serializer()),
    )

    suspend fun detail(topicId: String): Topic = client.get(
        path = "/topics/$topicId",
        deserializer = Topic.serializer(),
    )

    suspend fun replies(
        topicId: String,
        cursor: String? = null,
        limit: Int = 20,
        sort: String? = null,
        fromFloor: Int? = null,
    ): PageList<Reply> = client.get(
        path = "/topics/$topicId/replies",
        query = mapOf(
            "cursor" to cursor,
            "limit" to limit,
            "sort" to sort,
            "from_floor" to fromFloor,
        ),
        deserializer = PageList.serializer(Reply.serializer()),
    )
}
