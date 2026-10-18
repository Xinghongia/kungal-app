package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 题目详情（GET /quizzes/{id}）。solution 仅在已作答/作者/管理可见。 */
@Serializable
data class QuizDetail(
    @SerialName("object") val obj: String = "quiz",
    val id: String = "",
    val prompt: ContentDocument? = null,
    @SerialName("quiz_type") val quizType: String = "single",
    @SerialName("quiz_category") val quizCategory: String = "other",
    val difficulty: Int = 0,
    @SerialName("spoiler_level") val spoilerLevel: String = "none",
    val author: UserRef = UserRef(),
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("answer_count") val answerCount: Int = 0,
    @SerialName("correct_count") val correctCount: Int = 0,
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    @SerialName("quality_average") val qualityAverage: Double? = null,
    @SerialName("quality_count") val qualityCount: Int = 0,
    @SerialName("comment_count") val commentCount: Int = 0,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    @SerialName("bumped_at") val bumpedAt: String = "",
    val content: ContentDocument? = null,
    val choices: List<String> = emptyList(),
    val solution: QuizSolution? = null,
    @SerialName("is_work_hidden") val isWorkHidden: Boolean = false,
    val works: List<WorkSummary> = emptyList(),
    val viewer: QuizViewer? = null,
)

@Serializable
data class QuizViewer(
    val answer: QuizAnswer? = null,
    @SerialName("has_answered") val hasAnswered: Boolean = false,
    @SerialName("has_favorited") val hasFavorited: Boolean = false,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("quality_rating") val qualityRating: Int? = null,
)

/** 答案（作答记录/本人作答）。 */
@Serializable
data class QuizAnswer(
    @SerialName("object") val obj: String = "quiz_answer",
    val id: String = "",
    @SerialName("quiz_id") val quizId: String = "",
    val answerer: UserRef = UserRef(),
    @SerialName("is_correct") val isCorrect: Boolean? = null,
    val submission: QuizSubmission? = null,
    @SerialName("answered_at") val answeredAt: String = "",
)

/** 答案键（解析与正确选项）。 */
@Serializable
data class QuizSolution(
    @SerialName("object") val obj: String = "quiz_solution",
    @SerialName("correct_choice_indexes") val correctChoiceIndexes: List<Int> = emptyList(),
    @SerialName("is_statement_true") val isStatementTrue: Boolean? = null,
    val explanation: ContentDocument? = null,
)

/** 作答请求体（POST /quizzes/{id}/answers）。判断题用 is_statement_true。 */
@Serializable
data class QuizSubmission(
    @SerialName("choice_indexes") val choiceIndexes: List<Int> = emptyList(),
    @SerialName("is_statement_true") val isStatementTrue: Boolean? = null,
)

@Serializable
data class QuizAnswerResult(
    @SerialName("object") val obj: String = "quiz_answer_result",
    val answer: QuizAnswer? = null,
    val solution: QuizSolution? = null,
)

/** 作答记录（游标分页；未解锁时 submission/is_correct 为 null）。 */
@Serializable
data class QuizAnswerPage(
    @SerialName("object") val obj: String = "list",
    val items: List<QuizAnswer> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    val total: Int = 0,
)

/** 收藏/取消收藏的返回体。 */
@Serializable
data class QuizEngagement(
    @SerialName("object") val obj: String = "quiz_engagement",
    @SerialName("quiz_id") val quizId: String = "",
    @SerialName("favorite_count") val favoriteCount: Int = 0,
    val viewer: QuizEngagementViewer? = null,
)

@Serializable
data class QuizEngagementViewer(
    @SerialName("has_favorited") val hasFavorited: Boolean = false,
)

/** 批量收藏态（GET /me/quiz-states），missing 为查不到的题目 id。 */
@Serializable
data class QuizStates(
    @SerialName("object") val obj: String = "list",
    val items: List<QuizState> = emptyList(),
    val missing: List<String> = emptyList(),
)

@Serializable
data class QuizState(
    @SerialName("quiz_id") val quizId: String = "",
    @SerialName("has_favorited") val hasFavorited: Boolean = false,
)

/** 质量评分返回体。 */
@Serializable
data class QuizQuality(
    @SerialName("object") val obj: String = "quiz_quality",
    @SerialName("quiz_id") val quizId: String = "",
    @SerialName("quality_average") val qualityAverage: Double? = null,
    @SerialName("quality_count") val qualityCount: Int = 0,
    val viewer: QuizQualityViewer? = null,
)

@Serializable
data class QuizQualityViewer(
    @SerialName("quality_rating") val qualityRating: Int? = null,
)

/** 质量评分请求体（1–10）。 */
@Serializable
data class QuizQualityBody(val rating: Int)

/** 出题/编辑源数据（GET /quizzes/{id}/source，仅 can_edit 可读）。 */
@Serializable
data class QuizSource(
    @SerialName("object") val obj: String = "quiz_source",
    @SerialName("quiz_id") val quizId: String = "",
    @SerialName("quiz_type") val quizType: String = "single",
    @SerialName("quiz_category") val quizCategory: String = "other",
    val difficulty: Int = 1,
    @SerialName("spoiler_level") val spoilerLevel: String = "none",
    @SerialName("prompt_text") val promptText: String = "",
    @SerialName("description_markdown") val descriptionMarkdown: String = "",
    @SerialName("explanation_markdown") val explanationMarkdown: String = "",
    val choices: List<String> = emptyList(),
    @SerialName("correct_choice_indexes") val correctChoiceIndexes: List<Int> = emptyList(),
    @SerialName("is_statement_true") val isStatementTrue: Boolean? = null,
    @SerialName("work_ids") val workIds: List<String> = emptyList(),
    @SerialName("is_work_hidden") val isWorkHidden: Boolean = false,
)

/** 出题请求体（POST /quizzes，Idempotency-Key 必填）。 */
@Serializable
data class QuizCreate(
    @SerialName("quiz_type") val quizType: String,
    @SerialName("quiz_category") val quizCategory: String,
    val difficulty: Int,
    @SerialName("spoiler_level") val spoilerLevel: String = "none",
    @SerialName("prompt_text") val promptText: String,
    @SerialName("description_markdown") val descriptionMarkdown: String = "",
    @SerialName("explanation_markdown") val explanationMarkdown: String = "",
    val choices: List<String> = emptyList(),
    @SerialName("correct_choice_indexes") val correctChoiceIndexes: List<Int> = emptyList(),
    @SerialName("is_statement_true") val isStatementTrue: Boolean? = null,
    @SerialName("work_ids") val workIds: List<String> = emptyList(),
    @SerialName("is_work_hidden") val isWorkHidden: Boolean = false,
)

/** 编辑请求体（PATCH /quizzes/{id}）：只发要改的字段，null 表示不改。 */
@Serializable
data class QuizPatch(
    @SerialName("quiz_type") val quizType: String? = null,
    @SerialName("quiz_category") val quizCategory: String? = null,
    val difficulty: Int? = null,
    @SerialName("spoiler_level") val spoilerLevel: String? = null,
    @SerialName("prompt_text") val promptText: String? = null,
    @SerialName("description_markdown") val descriptionMarkdown: String? = null,
    @SerialName("explanation_markdown") val explanationMarkdown: String? = null,
    val choices: List<String>? = null,
    @SerialName("correct_choice_indexes") val correctChoiceIndexes: List<Int>? = null,
    @SerialName("is_statement_true") val isStatementTrue: Boolean? = null,
    @SerialName("work_ids") val workIds: List<String>? = null,
    @SerialName("is_work_hidden") val isWorkHidden: Boolean? = null,
)
