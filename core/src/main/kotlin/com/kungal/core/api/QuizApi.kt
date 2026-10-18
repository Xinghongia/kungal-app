package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.QuizAnswerPage
import com.kungal.core.model.QuizAnswerResult
import com.kungal.core.model.QuizCreate
import com.kungal.core.model.QuizDetail
import com.kungal.core.model.QuizEngagement
import com.kungal.core.model.QuizPatch
import com.kungal.core.model.QuizQuality
import com.kungal.core.model.QuizQualityBody
import com.kungal.core.model.QuizSource
import com.kungal.core.model.QuizStates
import com.kungal.core.model.QuizSubmission
import com.kungal.core.model.QuizSummary
import com.kungal.core.net.PagedList

/**
 * Galgame 题库（BP-05 U3/U4）。读公开；作答/收藏/质量评分/出题/编辑/删除需登录。
 * 作答与出题带 Idempotency-Key；重复作答返回 409 ALREADY_EXISTS，作者自答 403
 * SELF_ANSWER_FORBIDDEN，未作答评质量 403 QUIZ_ANSWER_REQUIRED。
 */
class QuizApi(private val client: KungalClient) {

    /** 题库列表（页码分页，网页 50/页）。 */
    suspend fun list(
        page: Int = 1,
        limit: Int = 50,
        workId: String? = null,
        authorId: String? = null,
        quizType: String? = null,
        quizCategory: String? = null,
        difficulty: Int? = null,
        spoilerLevel: String? = null,
        includeNsfw: Boolean = false,
        sort: String = "bumped_at_desc",
    ): PagedList<QuizSummary> = client.get(
        path = "/quizzes",
        query = buildMap {
            put("page", page)
            put("limit", limit)
            put("work_id", workId)
            put("author_id", authorId)
            put("quiz_type", quizType)
            put("quiz_category", quizCategory)
            put("difficulty", difficulty)
            put("spoiler_level", spoilerLevel)
            put("include_nsfw", includeNsfw)
            put("sort", sort)
        },
        deserializer = PagedList.serializer(QuizSummary.serializer()),
    )

    suspend fun detail(quizId: String): QuizDetail = client.get(
        path = "/quizzes/$quizId",
        deserializer = QuizDetail.serializer(),
    )

    /** 提交答案；返回正确答案与解析。 */
    suspend fun answer(
        quizId: String,
        submission: QuizSubmission,
        idempotencyKey: String,
    ): QuizAnswerResult = client.postJson(
        path = "/quizzes/$quizId/answers",
        body = KungalJson.encodeToString(QuizSubmission.serializer(), submission),
        idempotencyKey = idempotencyKey,
        deserializer = QuizAnswerResult.serializer(),
    )

    /** 作答记录（游标，网页 20/条）；未解锁时 submission/is_correct 为空。 */
    suspend fun answers(
        quizId: String,
        cursor: String? = null,
        limit: Int = 20,
    ): QuizAnswerPage = client.get(
        path = "/quizzes/$quizId/answers",
        query = mapOf("cursor" to cursor, "limit" to limit),
        deserializer = QuizAnswerPage.serializer(),
    )

    suspend fun setFavorite(quizId: String, favorited: Boolean): QuizEngagement {
        val path = "/quizzes/$quizId/favorite"
        return if (favorited) {
            client.put(path, QuizEngagement.serializer())
        } else {
            client.delete(path, QuizEngagement.serializer())
        }
    }

    /** 批量读取本人收藏态（1–100 个 id），供动态卡片等列表初始化。 */
    suspend fun states(quizIds: List<String>): QuizStates = client.get(
        path = "/me/quiz-states",
        query = mapOf("quiz_ids" to quizIds.joinToString(",")),
        deserializer = QuizStates.serializer(),
    )

    /** 质量评分 1–10（须已作答）。 */
    suspend fun rateQuality(quizId: String, rating: Int): QuizQuality = client.putJson(
        path = "/quizzes/$quizId/quality-rating",
        body = KungalJson.encodeToString(QuizQualityBody.serializer(), QuizQualityBody(rating)),
        deserializer = QuizQuality.serializer(),
    )

    /** 编辑源数据（需 can_edit）。 */
    suspend fun source(quizId: String): QuizSource = client.get(
        path = "/quizzes/$quizId/source",
        deserializer = QuizSource.serializer(),
    )

    /** 出题；返回完整题目。 */
    suspend fun create(body: QuizCreate, idempotencyKey: String): QuizDetail = client.postJson(
        path = "/quizzes",
        body = KungalJson.encodeToString(QuizCreate.serializer(), body),
        idempotencyKey = idempotencyKey,
        deserializer = QuizDetail.serializer(),
    )

    suspend fun update(quizId: String, body: QuizPatch): QuizDetail = client.patchJson(
        path = "/quizzes/$quizId",
        body = KungalJson.encodeToString(QuizPatch.serializer(), body),
        deserializer = QuizDetail.serializer(),
    )

    suspend fun delete(quizId: String) = client.deleteNoBody("/quizzes/$quizId")

    /** 本人作答过的题（/me/answered-quizzes）。 */
    suspend fun myAnswered(page: Int = 1, limit: Int = 50): PagedList<QuizSummary> = client.get(
        path = "/me/answered-quizzes",
        query = mapOf("page" to page, "limit" to limit),
        deserializer = PagedList.serializer(QuizSummary.serializer()),
    )
}
