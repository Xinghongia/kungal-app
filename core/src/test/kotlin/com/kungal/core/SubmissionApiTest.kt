package com.kungal.core

import com.kungal.core.api.SubmissionApi
import com.kungal.core.model.SubmissionIntroduction
import com.kungal.core.model.SubmissionTitle
import com.kungal.core.model.WorkSubmissionCreate
import com.kungal.core.net.KungalApiException
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class SubmissionApiTest {

    private lateinit var server: MockWebServer
    private lateinit var submissions: SubmissionApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        val client = KungalClient(baseUrl = baseUrl, session = session)
        submissions = SubmissionApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String, code: Int = 200, etag: String? = null, location: String? = null) =
        MockResponse.Builder()
            .code(code)
            .addHeader("Content-Type", "application/json")
            .apply {
                etag?.let { addHeader("ETag", it) }
                location?.let { addHeader("Location", it) }
            }
            .body(body)
            .build()

    @Test
    fun `候选搜索传关键词与 NSFW 姿态并解析认领状态`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_submission_candidates.json")))
        val page = submissions.candidates(
            query = "示例",
            includeNsfw = true,
            cursor = "cur_1",
            limit = 12,
        )
        assertEquals(2, page.items.size)
        assertEquals("cur_candidate_2", page.nextCursor)
        assertEquals("229", page.items[0].workSummary.id)
        assertEquals("live", page.items[0].state)
        assertEquals("pending", page.items[1].state)
        assertTrue(page.items[1].workSummary.isNsfw)
        assertFalse(page.items[1].workSummary.isPublished)

        val request = server.takeRequest()
        assertEquals("/api/v1/work-submission-candidates", request.url.encodedPath)
        assertEquals("示例", request.url.queryParameter("q"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
        assertEquals("cur_1", request.url.queryParameter("cursor"))
        assertEquals("12", request.url.queryParameter("limit"))
    }

    @Test
    fun `我的投稿按状态收窄并解析拒绝原因与操作能力`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_submissions_mine.json")))
        val page = submissions.mySubmissions(
            states = listOf("pending", "declined", "draft"),
            includeTotal = true,
            limit = 20,
        )
        assertEquals(2, page.items.size)
        assertEquals(2L, page.total)
        assertEquals("cur_mine_2", page.nextCursor)
        assertEquals("declined", page.items[0].state)
        assertEquals("介绍太短，请补充后再提交", page.items[0].lastEvent?.note)
        assertEquals("审核员", page.items[0].lastEvent?.actor?.name)
        assertTrue(page.items[0].viewer?.canSubmit == true)
        assertEquals("draft", page.items[1].state)
        assertTrue(page.items[1].viewer?.canDelete == true)

        val request = server.takeRequest()
        assertEquals("/api/v1/me/work-submissions", request.url.encodedPath)
        assertEquals("pending,declined,draft", request.url.queryParameter("state"))
        assertEquals("true", request.url.queryParameter("include_total"))
        assertEquals("20", request.url.queryParameter("limit"))
    }

    @Test
    fun `新建投稿发送完整请求体并读取 Location 与 ETag`() = runBlocking {
        server.enqueue(
            ok(
                Fixtures.text("galgame_submission_created.json"),
                code = 201,
                etag = "\"claim4501.1\"",
                location = "/api/v1/work-submissions/4501",
            ),
        )
        val result = submissions.create(
            request = WorkSubmissionCreate(
                titles = listOf(
                    SubmissionTitle("ja", "サンプル作品"),
                    SubmissionTitle("zh-Hans", "示例作品"),
                ),
                aliases = listOf("Sample Work"),
                introductions = listOf(SubmissionIntroduction("zh-Hans", "介绍正文")),
                originalLanguage = "ja",
                contentRating = "r18",
                isNsfw = true,
                releaseDate = "2025-01-31",
                releaseDatePrecision = "day",
                bannerHash = "a".repeat(64),
            ),
            idempotencyKey = "ik-create",
        )
        assertEquals("\"claim4501.1\"", result.etag)
        assertEquals("/api/v1/work-submissions/4501", result.location)
        assertEquals("4501", result.body.workId)
        assertEquals("pending", result.body.state)
        assertTrue(result.body.hasBannerAttached)
        assertEquals("r18", result.body.contentRating)
        assertTrue(result.body.viewer?.canWithdraw == true)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/work-submissions", request.url.encodedPath)
        assertEquals("ik-create", request.headers["Idempotency-Key"])
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"locale\":\"ja\""))
        assertTrue(body.contains("\"title\":\"サンプル作品\""))
        assertTrue(body.contains("\"aliases\":[\"Sample Work\"]"))
        assertTrue(body.contains("\"original_language\":\"ja\""))
        assertTrue(body.contains("\"content_rating\":\"r18\""))
        assertTrue(body.contains("\"is_nsfw\":true"))
        assertTrue(body.contains("\"release_date\":\"2025-01-31\""))
        assertTrue(body.contains("\"is_duplicate_confirmed\":false"))
        assertTrue(body.contains("\"banner_hash\":\"${"a".repeat(64)}\""))
    }

    @Test
    fun `重复投稿返回 DUPLICATE_SUSPECTS 时抛带问题码的异常`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(409)
                .addHeader("Content-Type", "application/problem+json")
                .body(
                    """
                    {"type":"https://developer.nextmoe.dev/problems/catalog/duplicate-suspects",
                     "title":"Duplicate suspects","status":409,
                     "detail":"Live works share a submitted title.",
                     "instance":"/api/v1/work-submissions",
                     "code":"DUPLICATE_SUSPECTS",
                     "suspects":[{"id":"229","display_name":"示例作品"}]}
                    """.trimIndent(),
                )
                .build(),
        )
        try {
            submissions.create(
                request = WorkSubmissionCreate(
                    titles = listOf(SubmissionTitle("zh-Hans", "示例作品")),
                    originalLanguage = "zh-Hans",
                    contentRating = "all_ages",
                    isNsfw = false,
                ),
                idempotencyKey = "ik-duplicate",
            )
            fail("应当抛出 KungalApiException")
        } catch (e: KungalApiException) {
            assertEquals(409, e.httpStatus)
            assertEquals("DUPLICATE_SUSPECTS", e.problem?.code)
            assertFalse(e.requiresLogin)
        }
    }

    @Test
    fun `投稿详情读取 ETag 并用 If-Match 撤回与删除`() = runBlocking {
        server.enqueue(
            ok(
                Fixtures.text("galgame_submission_detail.json"),
                etag = "\"claim4501.2\"",
            ),
        )
        val detail = submissions.submission("4501")
        assertEquals("\"claim4501.2\"", detail.etag)
        assertEquals("draft", detail.body.state)
        assertEquals("4501", detail.body.workId)
        assertEquals(2L, detail.body.actedCount)
        assertTrue(detail.body.viewer?.canSubmit == true)
        assertEquals("/api/v1/work-submissions/4501", server.takeRequest().url.encodedPath)

        server.enqueue(
            ok(
                Fixtures.text("galgame_submission_detail.json"),
                etag = "\"claim4501.3\"",
            ),
        )
        val moved = submissions.updateState(
            workId = "4501",
            state = "pending",
            ifMatch = "\"claim4501.2\"",
        )
        assertEquals("\"claim4501.3\"", moved.etag)
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertEquals("/api/v1/work-submissions/4501", patch.url.encodedPath)
        assertEquals("\"claim4501.2\"", patch.headers["If-Match"])
        assertTrue(patch.body!!.utf8().contains("\"state\":\"pending\""))

        server.enqueue(MockResponse.Builder().code(204).build())
        submissions.deleteDraft(workId = "4501", ifMatch = "\"claim4501.3\"")
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/work-submissions/4501", delete.url.encodedPath)
        assertEquals("\"claim4501.3\"", delete.headers["If-Match"])
        assertEquals(0, delete.body?.size ?: 0)
    }

    @Test
    fun `审阅拒绝带说明`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_submission_detail.json")))
        submissions.updateState(
            workId = "4501",
            state = "declined",
            note = "介绍太短",
            ifMatch = "\"claim4501.4\"",
        )
        val request = server.takeRequest()
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"state\":\"declined\""))
        assertTrue(body.contains("介绍太短"))
        assertEquals("\"claim4501.4\"", request.headers["If-Match"])
    }
}
