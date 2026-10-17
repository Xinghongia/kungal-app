package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 回复互动快照（回复反应写操作的返回体）。 */
@Serializable
data class ReplyEngagement(
    @SerialName("object") val obj: String = "reply_engagement",
    @SerialName("reply_id") val replyId: String = "",
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("dislike_count") val dislikeCount: Int = 0,
    val reactions: List<ReactionSummary> = emptyList(),
    val viewer: ReplyViewer? = null,
)

/** 反应历史里的一条记录（谁在什么时候对某条内容做了什么反应）。 */
@Serializable
data class ReactionRecord(
    @SerialName("object") val obj: String = "reaction",
    val id: String = "",
    val reaction: String = "",
    val reactor: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
)

/** 话题订阅（通知级别与已读楼层）。 */
@Serializable
data class TopicSubscription(
    @SerialName("object") val obj: String = "topic_subscription",
    val id: String = "",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("notification_level") val notificationLevel: String = "normal",
    @SerialName("last_read_floor") val lastReadFloor: Int = 0,
)

/** 订阅列表里的一行：话题摘要 + 未读数与已读楼层。 */
@Serializable
data class SubscribedTopic(
    @SerialName("object") val obj: String = "subscribed_topic",
    val id: String = "",
    val topic: Topic? = null,
    @SerialName("last_read_floor") val lastReadFloor: Int = 0,
    @SerialName("first_unread_floor") val firstUnreadFloor: Int? = null,
    @SerialName("unread_reply_count") val unreadReplyCount: Int = 0,
    @SerialName("last_reply_at") val lastReplyAt: String = "",
)

/** 推话题的记录。 */
@Serializable
data class TopicUpvote(
    @SerialName("object") val obj: String = "topic_upvote",
    val id: String = "",
    @SerialName("topic_id") val topicId: String = "",
    val upvoter: UserRef = UserRef(),
    val note: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

/** 单条话题的本人状态（收藏、自己点过的反应 token）。 */
@Serializable
data class TopicState(
    @SerialName("object") val obj: String = "topic_state",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("has_favorited") val hasFavorited: Boolean = false,
    @SerialName("reaction_tokens") val reactionTokens: List<String> = emptyList(),
)

@Serializable
data class TopicStates(
    @SerialName("object") val obj: String = "list",
    val items: List<TopicState> = emptyList(),
    val missing: List<String> = emptyList(),
)

/** 编辑话题时读取的 Markdown 源文（GET /topics/{id}/source）。 */
@Serializable
data class TopicSource(
    @SerialName("object") val obj: String = "topic_source",
    @SerialName("topic_id") val topicId: String = "",
    val title: String = "",
    @SerialName("content_markdown") val contentMarkdown: String = "",
    val category: String = "",
    val sections: List<String> = emptyList(),
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    @SerialName("cover_images") val coverImages: List<ImageInfo> = emptyList(),
    @SerialName("access_scope") val accessScope: String = "public",
    @SerialName("access_grants") val accessGrants: AccessGrants = AccessGrants(),
)

@Serializable
data class AccessGrants(
    val roles: List<String> = emptyList(),
    val users: List<UserRef> = emptyList(),
)

@Serializable
data class ReplySource(
    @SerialName("object") val obj: String = "reply_source",
    @SerialName("reply_id") val replyId: String = "",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("content_markdown") val contentMarkdown: String = "",
)

@Serializable
data class CommentSource(
    @SerialName("object") val obj: String = "comment_source",
    @SerialName("comment_id") val commentId: String = "",
    val text: String = "",
)

/** 话题草稿（完整内容）。 */
@Serializable
data class TopicDraft(
    @SerialName("object") val obj: String = "topic_draft",
    val id: String = "",
    val title: String = "",
    @SerialName("content_markdown") val contentMarkdown: String = "",
    val category: String? = null,
    val sections: List<String> = emptyList(),
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    @SerialName("cover_image_hashes") val coverImageHashes: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

/** 草稿列表里的摘要。 */
@Serializable
data class TopicDraftSummary(
    @SerialName("object") val obj: String = "topic_draft_summary",
    val id: String = "",
    val title: String = "",
    val summary: String = "",
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
)

@Serializable
data class TopicCreate(
    val title: String,
    @SerialName("content_markdown") val contentMarkdown: String,
    val category: String,
    val sections: List<String>,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("cover_image_hashes") val coverImageHashes: List<String>? = null,
    @SerialName("access_scope") val accessScope: String = "public",
    @SerialName("access_roles") val accessRoles: List<String>? = null,
    @SerialName("access_user_ids") val accessUserIds: List<String>? = null,
)

/** 部分更新：只发送非 null 字段。 */
@Serializable
data class TopicPatch(
    val title: String? = null,
    @SerialName("content_markdown") val contentMarkdown: String? = null,
    val category: String? = null,
    val sections: List<String>? = null,
    @SerialName("is_nsfw") val isNsfw: Boolean? = null,
    @SerialName("cover_image_hashes") val coverImageHashes: List<String>? = null,
    @SerialName("access_scope") val accessScope: String? = null,
    @SerialName("access_roles") val accessRoles: List<String>? = null,
    @SerialName("access_user_ids") val accessUserIds: List<String>? = null,
    /** published / hidden */
    val state: String? = null,
)

@Serializable
data class TopicDraftCreate(
    val title: String = "",
    @SerialName("content_markdown") val contentMarkdown: String = "",
    val category: String? = null,
    val sections: List<String> = emptyList(),
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    @SerialName("cover_image_hashes") val coverImageHashes: List<String> = emptyList(),
)

@Serializable
data class ReplyPatch(
    @SerialName("content_markdown") val contentMarkdown: String,
)

@Serializable
data class CommentCreate(
    val text: String,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
)

@Serializable
data class CommentPatch(val text: String)

@Serializable
data class SubscriptionSet(
    @SerialName("notification_level") val notificationLevel: String,
)

@Serializable
data class ReadMarker(val floor: Int)

@Serializable
data class UpvoteCreate(val note: String? = null)

@Serializable
data class PinReplyBody(@SerialName("reply_id") val replyId: String)
