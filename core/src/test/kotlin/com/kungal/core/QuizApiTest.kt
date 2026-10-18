package com.kungal.core

import com.kungal.core.api.QuizApi
import com.kungal.core.model.QuizAnswerResult
import com.kungal.core.model.QuizCreate
import com.kungal.core.model.QuizEngagement
import com.kungal.core.model.QuizPatch
import com.kungal.core.model.QuizQuality
import com.kungal.core.model.QuizSubmission
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class QuizApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: QuizApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = QuizApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String, code: Int = 200) = MockResponse.Builder()
        .code(code).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `题库列表解析摘要与筛选参数`() = runBlocking {
        server.enqueue(ok(Fixtures.text("quiz_list.json")))
        val page = api.list(
            page = 2,
            limit = 50,
            workId = "211",
            quizType = "single",
            quizCategory = "trivia",
            difficulty = 3,
            sort = "answer_count_desc",
            includeNsfw = true,
        )
        assertEquals(276, page.total)
        assertEquals("eq", page.totalRelation)
        assertEquals(2, page.items.size)
        val first = page.items.first()
        assertEquals("295", first.id)
        assertEquals("single", first.quizType)
        assertEquals("trivia", first.quizCategory)
        assertEquals(1, first.difficulty)
        assertEquals(16, first.answerCount)
        assertEquals(6, first.correctCount)
        assertEquals("2026-10-16T23:57:42Z", first.bumpedAt)
        assertNull(first.viewer)

        val request = server.takeRequest()
        assertEquals("/api/v1/quizzes", request.url.encodedPath)
        assertEquals("2", request.url.queryParameter("page"))
        assertEquals("50", request.url.queryParameter("limit"))
        assertEquals("211", request.url.queryParameter("work_id"))
        assertEquals("single", request.url.queryParameter("quiz_type"))
        assertEquals("trivia", request.url.queryParameter("quiz_category"))
        assertEquals("3", request.url.queryParameter("difficulty"))
        assertEquals("answer_count_desc", request.url.queryParameter("sort"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
    }

    @Test
    fun `题目详情解析选项作品与隐藏作品`() = runBlocking {
        server.enqueue(ok(Fixtures.text("quiz_detail.json")))
        val quiz = api.detail("295")
        assertEquals("295", quiz.id)
        assertEquals("single", quiz.quizType)
        assertEquals(listOf("芒果", "苹果", "香蕉", "葡萄"), quiz.choices)
        assertNull(quiz.solution)
        assertFalse(quiz.isWorkHidden)
        assertEquals(1, quiz.works.size)
        assertEquals("211", quiz.works.first().id)
        assertNull(quiz.viewer)

        val request = server.takeRequest()
        assertEquals("/api/v1/quizzes/295", request.url.encodedPath)
    }

    @Test
    fun `作答带幂等键并解析答案与解析`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"quiz_answer_result",
                   "answer":{"object":"quiz_answer","id":"a1","quiz_id":"295",
                     "answerer":{"object":"user","id":"1","name":"鲲","avatar":null,"avatar_frame":null},
                     "is_correct":true,
                     "submission":{"choice_indexes":[1],"is_statement_true":null},
                     "answered_at":"2026-10-18T01:00:00Z"},
                   "solution":{"object":"quiz_solution","correct_choice_indexes":[1],
                     "is_statement_true":null,
                     "explanation":{"object":"document","children":[]}}}""",
                201,
            ),
        )
        val result: QuizAnswerResult = api.answer(
            quizId = "295",
            submission = QuizSubmission(choiceIndexes = listOf(1)),
            idempotencyKey = "key-1",
        )
        assertEquals(true, result.answer?.isCorrect)
        assertEquals(listOf(1), result.solution?.correctChoiceIndexes)

        val request = server.takeRequest()
        assertEquals("/api/v1/quizzes/295/answers", request.url.encodedPath)
        assertEquals("key-1", request.headers["Idempotency-Key"])
        assertEquals(
            """{"choice_indexes":[1]}""",
            request.body?.utf8(),
        )
    }

    @Test
    fun `判断题作答只发 is_statement_true`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"quiz_answer_result","answer":null,
                   "solution":{"object":"quiz_solution","correct_choice_indexes":[],
                     "is_statement_true":true,"explanation":{"object":"document","children":[]}}}""",
                201,
            ),
        )
        api.answer(
            quizId = "300",
            submission = QuizSubmission(choiceIndexes = emptyList(), isStatementTrue = true),
            idempotencyKey = "key-2",
        )
        val body = server.takeRequest().body?.utf8() ?: ""
        val json = Json.parseToJsonElement(body).jsonObject
        assertEquals("[]", json["choice_indexes"].toString())
        assertEquals("true", json["is_statement_true"]?.jsonPrimitive?.content)
    }

    @Test
    fun `作答记录游标分页`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","total":3,"items":[
                     {"object":"quiz_answer","id":"a1","quiz_id":"295",
                      "answerer":{"object":"user","id":"2","name":"莲","avatar":null,"avatar_frame":null},
                      "is_correct":null,"submission":null,"answered_at":"2026-10-18T01:00:00Z"},
                     {"object":"quiz_answer","id":"a2","quiz_id":"295",
                      "answerer":{"object":"user","id":"3","name":"音","avatar":null,"avatar_frame":null},
                      "is_correct":true,
                      "submission":{"choice_indexes":[1],"is_statement_true":null},
                      "answered_at":"2026-10-18T02:00:00Z"}],
                   "next_cursor":"cur_2"}""",
            ),
        )
        val page = api.answers("295", cursor = "cur_1", limit = 20)
        assertEquals(3, page.total)
        assertEquals("cur_2", page.nextCursor)
        assertNull(page.items[0].isCorrect)
        assertNull(page.items[0].submission)
        assertEquals(listOf(1), page.items[1].submission?.choiceIndexes)

        val request = server.takeRequest()
        assertEquals("cur_1", request.url.queryParameter("cursor"))
        assertEquals("20", request.url.queryParameter("limit"))
    }

    @Test
    fun `收藏与取消收藏`() = runBlocking {
        val body = """{"object":"quiz_engagement","quiz_id":"295","favorite_count":3,
                      "viewer":{"has_favorited":%s}}"""
        server.enqueue(ok(body.format("true")))
        server.enqueue(ok(body.format("false")))

        val added = api.setFavorite("295", true)
        assertEquals(3, added.favoriteCount)
        assertTrue(added.viewer?.hasFavorited == true)
        assertEquals("PUT", server.takeRequest().method)

        val removed = api.setFavorite("295", false)
        assertFalse(removed.viewer?.hasFavorited == true)
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `质量评分提交 1 到 10`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"quiz_quality","quiz_id":"295","quality_average":8.5,
                   "quality_count":2,"viewer":{"quality_rating":9}}""",
            ),
        )
        val quality: QuizQuality = api.rateQuality("295", 9)
        assertEquals(8.5, quality.qualityAverage!!, 0.001)
        assertEquals(9, quality.viewer?.qualityRating)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/quizzes/295/quality-rating", request.url.encodedPath)
        assertEquals("""{"rating":9}""", request.body?.utf8())
    }

    @Test
    fun `出题带幂等键并解析返回题目`() = runBlocking {
        server.enqueue(ok(Fixtures.text("quiz_detail.json"), 201))
        val created = api.create(
            QuizCreate(
                quizType = "judge",
                quizCategory = "plot",
                difficulty = 5,
                spoilerLevel = "portion",
                promptText = "判断题",
                explanationMarkdown = "解析",
                isStatementTrue = true,
                workIds = listOf("211"),
                isWorkHidden = true,
            ),
            idempotencyKey = "create-key",
        )
        assertEquals("295", created.id)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("create-key", request.headers["Idempotency-Key"])
        val json = Json.parseToJsonElement(request.body?.utf8() ?: "").jsonObject
        assertEquals("judge", json["quiz_type"]?.jsonPrimitive?.content)
        assertEquals("true", json["is_statement_true"]?.jsonPrimitive?.content)
        assertEquals("true", json["is_work_hidden"]?.jsonPrimitive?.content)
        assertEquals("[\"211\"]", json["work_ids"].toString())
        assertFalse(json.containsKey("choices") && json["choices"].toString() != "[]")
    }

    @Test
    fun `编辑只发送改动字段`() = runBlocking {
        server.enqueue(ok(Fixtures.text("quiz_detail.json")))
        api.update(
            "295",
            QuizPatch(promptText = "新题干", difficulty = 7, isStatementTrue = false),
        )
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/quizzes/295", request.url.encodedPath)
        val json = Json.parseToJsonElement(request.body?.utf8() ?: "").jsonObject
        assertEquals(setOf("prompt_text", "difficulty", "is_statement_true"), json.keys)
        assertEquals("新题干", json["prompt_text"]?.jsonPrimitive?.content)
        assertEquals("7", json["difficulty"]?.jsonPrimitive?.content)
        assertEquals("false", json["is_statement_true"]?.jsonPrimitive?.content)
    }

    @Test
    fun `编辑源数据`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"quiz_source","quiz_id":"295","quiz_type":"multiple",
                   "quiz_category":"music","difficulty":6,"spoiler_level":"serious",
                   "prompt_text":"题干","description_markdown":"描述",
                   "explanation_markdown":"解析","choices":["A","B","C"],
                   "correct_choice_indexes":[0,2],"is_statement_true":null,
                   "work_ids":["211","212"],"is_work_hidden":true}""",
            ),
        )
        val source = api.source("295")
        assertEquals("multiple", source.quizType)
        assertEquals(listOf(0, 2), source.correctChoiceIndexes)
        assertEquals(listOf("211", "212"), source.workIds)
        assertTrue(source.isWorkHidden)
    }

    @Test
    fun `删除题目 204`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        api.delete("295")
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/quizzes/295", request.url.encodedPath)
    }

    @Test
    fun `我的答题`() = runBlocking {
        server.enqueue(ok(Fixtures.text("quiz_list.json")))
        val page = api.myAnswered(page = 1, limit = 50)
        assertEquals(2, page.items.size)
        val request = server.takeRequest()
        assertEquals("/api/v1/me/answered-quizzes", request.url.encodedPath)
    }
}
