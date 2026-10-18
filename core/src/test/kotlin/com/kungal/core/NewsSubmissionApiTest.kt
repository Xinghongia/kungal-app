package com.kungal.core

import com.kungal.core.api.NewsSubmissionApi
import com.kungal.core.model.NewsSubmissionCreate
import com.kungal.core.model.NewsSubmissionPatch
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

class NewsSubmissionApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: NewsSubmissionApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = NewsSubmissionApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    private fun created(body: String) = MockResponse.Builder()
        .code(201).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `我的投稿按游标请求`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_my_submissions.json")))
        val page = api.mySubmissions(cursor = "cur_abc", limit = 20)
        assertEquals(3, page.items.size)
        assertEquals("cur_eyJ2IjoxLCJzIjoicHVibGlzaGVkX2Rlc2MifQ", page.nextCursor)
        val pending = page.items.first()
        assertEquals("4790", pending.id)
        assertEquals("pending", pending.state)
        assertEquals("community", pending.newsSource)
        assertEquals("1280", pending.banner?.width.toString())
        assertEquals(720, pending.banner?.height)
        assertTrue(pending.contentMarkdown.startsWith("## 新作公开"))
        val published = page.items[1]
        assertEquals("column", published.lane)
        assertEquals("https://example.com/original", published.sourceUrl)
        assertNull(published.banner)
        assertEquals("rejected", page.items[2].state)

        val request = server.takeRequest()
        assertEquals("/api/v1/me/news-submissions", request.url.encodedPath)
        assertEquals("cur_abc", request.url.queryParameter("cursor"))
        assertEquals("20", request.url.queryParameter("limit"))
    }

    @Test
    fun `单条投稿`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_submission_detail.json")))
        val item = api.submission("4781")
        assertEquals("4781", item.id)
        assertEquals("published", item.state)
        assertEquals("column", item.lane)
        assertEquals("正文第一段。\n\n正文第二段。", item.contentMarkdown)
        assertEquals("/api/v1/me/news-submissions/4781", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `创建投稿带幂等键并省略空字段`() = runBlocking {
        server.enqueue(created(Fixtures.text("news_submission_detail.json")))
        val item = api.create(
            NewsSubmissionCreate(
                title = "标题",
                preview = "导语",
                contentMarkdown = "正文",
                sourceUrl = null,
                bannerImageHash = null,
            ),
            idempotencyKey = "11111111-2222-3333-4444-555555555555",
        )
        assertEquals("4781", item.id)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/me/news-submissions", request.url.encodedPath)
        assertEquals(
            "11111111-2222-3333-4444-555555555555",
            request.headers["Idempotency-Key"],
        )
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("标题", body["title"]!!.jsonPrimitive.content)
        assertEquals("导语", body["preview"]!!.jsonPrimitive.content)
        assertEquals("正文", body["content_markdown"]!!.jsonPrimitive.content)
        assertFalse(body.containsKey("source_url"))
        assertFalse(body.containsKey("banner_image_hash"))
    }

    @Test
    fun `更新只发变化字段 移除封面发空串`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_submission_detail.json")))
        api.update(
            "4781",
            NewsSubmissionPatch(title = "新标题", bannerImageHash = ""),
        )
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/me/news-submissions/4781", request.url.encodedPath)
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("新标题", body["title"]!!.jsonPrimitive.content)
        assertEquals("", body["banner_image_hash"]!!.jsonPrimitive.content)
        assertFalse(body.containsKey("preview"))
        assertFalse(body.containsKey("content_markdown"))
        assertFalse(body.containsKey("source_url"))
        assertFalse(body.containsKey("state"))
    }

    @Test
    fun `撤回只发送 withdrawn`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_submission_detail.json")))
        api.withdraw("4781")
        val body = Json.parseToJsonElement(server.takeRequest().body!!.utf8()).jsonObject
        assertEquals(1, body.size)
        assertEquals("withdrawn", body["state"]!!.jsonPrimitive.content)
    }

    @Test
    fun `封面上传走 multipart 并解析哈希`() = runBlocking {
        server.enqueue(created(Fixtures.text("news_banner_upload.json")))
        val image = api.uploadBanner(
            bytes = ByteArray(16) { it.toByte() },
            fileName = "banner.jpg",
            contentType = "image/jpeg",
            idempotencyKey = "aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee",
        )
        assertEquals(
            "abcdef0123456789abcdef0123456789abcdef0123456789abcdef0123456789",
            image.hash,
        )
        assertEquals(1280, image.width)
        assertEquals(720, image.height)

        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/me/news-submission-images", request.url.encodedPath)
        assertEquals("aaaaaaaa-bbbb-cccc-dddd-eeeeeeeeeeee", request.headers["Idempotency-Key"])
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("filename=\"banner.jpg\""))
        assertTrue(body.contains("name=\"file\""))
    }
}
