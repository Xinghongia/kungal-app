package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 话题互动状态的权威快照（反应/收藏/点赞写操作的返回体）。 */
@Serializable
data class TopicEngagement(
    @SerialName("object") val obj: String = "topic_engagement",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("dislike_count") val dislikeCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("upvote_count") val upvoteCount: Int = 0,
    @SerialName("upvoted_at") val upvotedAt: String? = null,
    val reactions: List<ReactionSummary> = emptyList(),
    val viewer: TopicViewer? = null,
)

/** 发回复的请求体（POST /topics/{id}/replies）。 */
@Serializable
data class ReplyCreate(
    @SerialName("content_markdown") val contentMarkdown: String,
)
