package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 楼层回复。comments 为楼中楼嵌套评论（一层）。 */
@Serializable
data class Reply(
    @SerialName("object") val obj: String = "reply",
    val id: String = "",
    @SerialName("topic_id") val topicId: String = "",
    val floor: Int = 0,
    val author: UserRef = UserRef(),
    @SerialName("author_moemoepoint") val authorMoemoepoint: Int = 0,
    val content: ContentDocument? = null,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("dislike_count") val dislikeCount: Int = 0,
    val reactions: List<ReactionSummary> = emptyList(),
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("is_best_answer") val isBestAnswer: Boolean = false,
    val comments: List<ReplyComment> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    val viewer: ReplyViewer? = null,
)

/** 当前用户在该回复上的状态与权限；匿名访问时为 null。 */
@Serializable
data class ReplyViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("has_disliked") val hasDisliked: Boolean = false,
    @SerialName("can_like") val canLike: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
)

/** 楼中楼评论（Comment）：挂在某个回复楼层下。 */
@Serializable
data class ReplyComment(
    @SerialName("object") val obj: String = "comment",
    val id: String = "",
    @SerialName("reply_id") val replyId: String = "",
    @SerialName("reply_floor") val replyFloor: Int = 0,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    val author: UserRef = UserRef(),
    @SerialName("in_reply_to_user") val inReplyToUser: UserRef? = null,
    val content: ContentDocument? = null,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    val viewer: CommentViewer? = null,
)

@Serializable
data class CommentViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("can_like") val canLike: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
)
