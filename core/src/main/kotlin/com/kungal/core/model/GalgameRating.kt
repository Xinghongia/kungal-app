package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 评分与游玩记录（BP-02 U4）以及通用评论墙。 */

/** 列表里的评分摘要已含 viewer；这里补齐登录用户视角。 */
@Serializable
data class RatingViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
)

/** 评分详情（GET /ratings/{id}）：摘要 + 作品摘要 + 最近点赞者。 */
@Serializable
data class RatingDetail(
    @SerialName("object") val obj: String = "rating",
    val id: String = "",
    val work: WorkRef? = null,
    val author: UserRef = UserRef(),
    val recommend: String = "neutral",
    val overall: Int = 0,
    @SerialName("game_types") val gameTypes: List<String> = emptyList(),
    @SerialName("play_status") val playStatus: String? = null,
    @SerialName("spoiler_level") val spoilerLevel: String = "none",
    @SerialName("short_summary") val shortSummary: String = "",
    @SerialName("aspect_scores") val aspectScores: AspectScores = AspectScores(),
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val viewer: RatingViewer? = null,
    @SerialName("work_summary") val workSummary: WorkSummary? = null,
    val likers: List<UserRef> = emptyList(),
)

@Serializable
data class RatingEngagement(
    @SerialName("object") val obj: String = "rating_engagement",
    @SerialName("rating_id") val ratingId: String = "",
    @SerialName("like_count") val likeCount: Int = 0,
    val viewer: RatingEngagementViewer = RatingEngagementViewer(),
)

@Serializable
data class RatingEngagementViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
)

/** 本人对一部作品的记录（GET /works/{id}/record）。 */
@Serializable
data class WorkRecord(
    @SerialName("object") val obj: String = "work_record",
    @SerialName("work_id") val workId: String = "",
    @SerialName("play_state") val playState: String? = null,
    val minutes: Int = 0,
    @SerialName("rating_summary") val ratingSummary: RatingSummary? = null,
)

@Serializable
data class RecordRatingBody(
    val recommend: String,
    val overall: Int,
    @SerialName("game_types") val gameTypes: List<String>,
    @SerialName("spoiler_level") val spoilerLevel: String,
    @SerialName("short_summary") val shortSummary: String = "",
    @SerialName("aspect_scores") val aspectScores: AspectScores = AspectScores(),
)

@Serializable
data class WorkRecordPutBody(
    val minutes: Int? = null,
    @SerialName("play_state") val playState: String? = null,
    @SerialName("rating_input") val ratingInput: RecordRatingBody? = null,
)

@Serializable
data class PlaytimePutBody(
    val minutes: Int? = null,
    @SerialName("play_state") val playState: String? = null,
)

@Serializable
data class RatingCreateBody(
    @SerialName("work_id") val workId: String,
    val recommend: String,
    val overall: Int,
    @SerialName("game_types") val gameTypes: List<String>,
    @SerialName("play_status") val playStatus: String,
    @SerialName("spoiler_level") val spoilerLevel: String,
    @SerialName("short_summary") val shortSummary: String = "",
    @SerialName("aspect_scores") val aspectScores: AspectScores = AspectScores(),
)

@Serializable
data class RatingPatchBody(
    val recommend: String? = null,
    val overall: Int? = null,
    @SerialName("game_types") val gameTypes: List<String>? = null,
    @SerialName("play_status") val playStatus: String? = null,
    @SerialName("spoiler_level") val spoilerLevel: String? = null,
    @SerialName("short_summary") val shortSummary: String? = null,
    @SerialName("aspect_scores") val aspectScores: AspectScores? = null,
)

// ---------- 评论墙 ----------

@Serializable
data class WallCommentViewer(
    @SerialName("has_liked") val hasLiked: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("can_like") val canLike: Boolean = true,
    @SerialName("can_flag") val canFlag: Boolean = true,
)

@Serializable
data class WallComment(
    @SerialName("object") val obj: String = "wall_comment",
    val id: String = "",
    @SerialName("subject_type") val subjectType: String = "",
    @SerialName("subject_id") val subjectId: String = "",
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    @SerialName("root_comment_id") val rootCommentId: String? = null,
    val author: UserRef = UserRef(),
    val addressee: UserRef? = null,
    val state: String = "visible",
    val content: ContentDocument = ContentDocument(),
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("is_edited_by_moderator") val isEditedByModerator: Boolean = false,
    val viewer: WallCommentViewer? = null,
)

@Serializable
data class WallCommentList(
    @SerialName("object") val obj: String = "list",
    val items: List<WallComment> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class WallCommentSource(
    @SerialName("object") val obj: String = "wall_comment_source",
    @SerialName("wall_comment_id") val wallCommentId: String = "",
    @SerialName("content_markdown") val contentMarkdown: String = "",
)

@Serializable
data class WallCommentCreateBody(
    @SerialName("subject_type") val subjectType: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("parent_comment_id") val parentCommentId: String? = null,
    @SerialName("content_markdown") val contentMarkdown: String,
)

@Serializable
data class WallCommentPatchBody(
    @SerialName("content_markdown") val contentMarkdown: String,
)

@Serializable
data class WallCommentFlagBody(
    @SerialName("flag_reason") val flagReason: String,
    val note: String? = null,
)
