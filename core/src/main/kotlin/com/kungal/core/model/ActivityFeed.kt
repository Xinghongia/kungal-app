package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 全站动态（GET /activities）。网页首页话题卡片的收藏数来自
 * `topic_digest.favorite_count`——话题列表接口本身不返回该字段。
 *
 * 其他字段按 activity_type 只有一个详情块非空：话题类带 topic/reply/comment，
 * Galgame 类带 work/rating/resource/quiz，站务类带 todo/update_log。
 */
@Serializable
data class FeedActivity(
    @SerialName("object") val obj: String = "activity",
    val id: String = "",
    @SerialName("activity_type") val activityType: String = "",
    val path: String = "",
    @SerialName("occurred_at") val occurredAt: String = "",
    val performer: UserRef? = null,
    @SerialName("excerpt_markdown") val excerptMarkdown: String = "",
    val topic: Topic? = null,
    @SerialName("topic_digest") val topicDigest: TopicDigest? = null,
    val reply: ActivityReply? = null,
    val comment: ActivityComment? = null,
    val work: WorkRef? = null,
    @SerialName("work_digest") val workDigest: WorkDigest? = null,
    @SerialName("work_stats") val workStats: WorkStats? = null,
    @SerialName("work_revision") val workRevision: WorkRevision? = null,
    @SerialName("galgame_rating") val galgameRating: ActivityRating? = null,
    val resource: ActivityResource? = null,
    val quiz: ActivityQuiz? = null,
    val toolset: ActivityToolset? = null,
    val todo: ActivityTodo? = null,
    @SerialName("update_log") val updateLog: ActivityUpdateLog? = null,
)

@Serializable
data class TopicDigest(
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("excerpt_markdown") val excerptMarkdown: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
)

/** 活动里的回复（topic_reply_creation / best_answer_set）。 */
@Serializable
data class ActivityReply(
    @SerialName("reply_id") val replyId: String = "",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("topic_title") val topicTitle: String = "",
    val floor: Int = 0,
    val content: ContentDocument? = null,
    val quoted: QuotedReply? = null,
)

/** 活动里的评论（topic_comment_creation）。 */
@Serializable
data class ActivityComment(
    @SerialName("comment_id") val commentId: String = "",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("topic_title") val topicTitle: String = "",
    val quoted: QuotedReply? = null,
)

/** 被引用回复的摘要（楼层 + Markdown 摘录）。 */
@Serializable
data class QuotedReply(
    val floor: Int = 0,
    @SerialName("excerpt_markdown") val excerptMarkdown: String = "",
)

/** 活动里的评分摘要（galgame_rating_creation）。 */
@Serializable
data class ActivityRating(
    @SerialName("rating_id") val ratingId: String = "",
    val overall: Int = 0,
    @SerialName("play_status") val playStatus: String? = null,
    val recommend: String = "",
    @SerialName("spoiler_level") val spoilerLevel: String = "none",
    @SerialName("short_summary") val shortSummary: String = "",
    @SerialName("like_count") val likeCount: Int = 0,
)

/** 活动里的下载资源摘要（galgame_resource_creation）。 */
@Serializable
data class ActivityResource(
    @SerialName("resource_id") val resourceId: String = "",
    @SerialName("resource_type") val resourceType: String = "",
    @SerialName("resource_platforms") val resourcePlatforms: List<String> = emptyList(),
    @SerialName("resource_languages") val resourceLanguages: List<String> = emptyList(),
    val size: String = "",
    val note: String? = null,
    @SerialName("like_count") val likeCount: Int = 0,
)

/** 活动里的题目摘要（galgame_quiz_creation）。 */
@Serializable
data class ActivityQuiz(
    @SerialName("quiz_id") val quizId: String = "",
    val category: String = "",
    @SerialName("question_type") val questionType: String = "",
    val difficulty: Int = 0,
    @SerialName("answer_count") val answerCount: Int = 0,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("description_excerpt") val descriptionExcerpt: String = "",
)

/** 活动里的工具集摘要（toolset_resource_creation）。 */
@Serializable
data class ActivityToolset(
    @SerialName("toolset_id") val toolsetId: String = "",
    val title: String = "",
)

/** 活动里的待办状态（todo_creation）。 */
@Serializable
data class ActivityTodo(
    @SerialName("todo_id") val todoId: String = "",
    val state: String = "",
)

/** 活动里的更新日志版本（update_log_creation）。 */
@Serializable
data class ActivityUpdateLog(
    @SerialName("update_log_id") val updateLogId: String = "",
    @SerialName("release_version") val releaseVersion: String = "",
)

/** 作品简介摘要（galgame_creation / galgame_edit / galgame_pr_creation）。 */
@Serializable
data class WorkDigest(
    @SerialName("developer_names") val developerNames: List<String> = emptyList(),
    @SerialName("intro_excerpt") val introExcerpt: String? = null,
    val release: String? = null,
)

/** 作品计数（galgame_creation）。 */
@Serializable
data class WorkStats(
    @SerialName("resource_count") val resourceCount: Int = 0,
    @SerialName("like_count") val likeCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
)

/** 编辑产生的修订号（galgame_edit）。 */
@Serializable
data class WorkRevision(
    @SerialName("revision_number") val revisionNumber: Int? = null,
    @SerialName("legacy_revision_id") val legacyRevisionId: String? = null,
)
