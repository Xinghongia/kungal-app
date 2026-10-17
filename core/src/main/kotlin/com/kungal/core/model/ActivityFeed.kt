package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 全站动态（GET /activities）。网页首页话题卡片的收藏数来自
 * `topic_digest.favorite_count`——话题列表接口本身不返回该字段。
 */
@Serializable
data class FeedActivity(
    @SerialName("object") val obj: String = "activity",
    val id: String = "",
    @SerialName("activity_type") val activityType: String = "",
    val path: String = "",
    @SerialName("occurred_at") val occurredAt: String = "",
    val performer: UserRef = UserRef(),
    val topic: Topic? = null,
    @SerialName("topic_digest") val topicDigest: TopicDigest? = null,
)

@Serializable
data class TopicDigest(
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("excerpt_markdown") val excerptMarkdown: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
)
