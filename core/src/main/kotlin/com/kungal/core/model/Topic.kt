package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 话题。列表页返回子集（无 content/viewer 等），详情页返回全量；
 * 一个模型吃两种响应，缺省字段用默认值（列表页消费方不得依赖 content 非空）。
 */
@Serializable
data class Topic(
    @SerialName("object") val obj: String = "topic",
    val id: String = "",
    val title: String = "",
    val state: String = "",
    @SerialName("hidden_by") val hiddenBy: String? = null,
    @SerialName("access_scope") val accessScope: String = "public",
    val category: String = "",
    val sections: List<String> = emptyList(),
    @SerialName("cover_images") val coverImages: List<ImageInfo> = emptyList(),
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    val author: UserRef = UserRef(),
    @SerialName("author_moemoepoint") val authorMoemoepoint: Int = 0,
    val content: ContentDocument? = null,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("dislike_count") val dislikeCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("upvote_count") val upvoteCount: Int = 0,
    @SerialName("reply_count") val replyCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    val reactions: List<ReactionSummary> = emptyList(),
    @SerialName("mini_apps") val miniApps: List<String> = emptyList(),
    @SerialName("has_best_answer") val hasBestAnswer: Boolean = false,
    @SerialName("pinned_reply") val pinnedReply: Reply? = null,
    @SerialName("best_answer") val bestAnswer: Reply? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("bumped_at") val bumpedAt: String = "",
    @SerialName("upvoted_at") val upvotedAt: String? = null,
    val viewer: TopicViewer? = null,
)

/** 当前用户在该话题上的状态与权限；匿名访问时为 null。 */
@Serializable
data class TopicViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("has_upvoted") val hasUpvoted: Boolean = false,
    @SerialName("has_disliked") val hasDisliked: Boolean = false,
    @SerialName("has_favorited") val hasFavorited: Boolean = false,
    @SerialName("can_like") val canLike: Boolean = false,
    @SerialName("can_upvote") val canUpvote: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_hide") val canHide: Boolean = false,
    @SerialName("can_unhide") val canUnhide: Boolean = false,
    @SerialName("can_pin_reply") val canPinReply: Boolean = false,
    @SerialName("can_set_best_answer") val canSetBestAnswer: Boolean = false,
)
