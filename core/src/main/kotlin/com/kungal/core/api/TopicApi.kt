package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.Reply
import com.kungal.core.model.ReplyCreate
import com.kungal.core.model.Topic
import com.kungal.core.model.TopicEngagement
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

    /**
     * 对话题添加/移除一种反应（如 like）。返回服务端权威的互动快照。
     * 匿名调用会得到 401（与网页一致：需要登录）。
     */
    suspend fun react(topicId: String, reaction: String, active: Boolean): TopicEngagement = when (active) {
        true -> client.put("/topics/$topicId/reactions/$reaction", TopicEngagement.serializer())
        false -> client.delete("/topics/$topicId/reactions/$reaction", TopicEngagement.serializer())
    }

    /** 发表回复。idempotencyKey 必填（服务端强制），同一 key 24h 内重放返回首次结果。 */
    suspend fun createReply(topicId: String, markdown: String, idempotencyKey: String): Reply {
        val body = KungalJson.encodeToString(ReplyCreate.serializer(), ReplyCreate(markdown))
        return client.postJson(
            path = "/topics/$topicId/replies",
            body = body,
            idempotencyKey = idempotencyKey,
            deserializer = Reply.serializer(),
        )
    }

    /** 浏览计数（与网页打开话题时一致）；失败可静默。 */
    suspend fun markView(topicId: String) {
        client.postNoBody("/topics/$topicId/views")
    }
}
