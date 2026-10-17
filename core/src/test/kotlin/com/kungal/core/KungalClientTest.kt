package com.kungal.core

import com.kungal.core.api.TopicApi
import com.kungal.core.net.KungalApiException
import com.kungal.core.net.PageList
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class KungalClientTest {

    private lateinit var server: MockWebServer
    private lateinit var session: SessionHolder
    private lateinit var client: KungalClient
    private lateinit var api: TopicApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        session = SessionHolder()
        client = KungalClient(baseUrl = baseUrl, session = session)
        api = TopicApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private fun problem(code: Int, body: String, retryAfter: String? = null) = MockResponse.Builder()
        .code(code)
        .addHeader("Content-Type", "application/problem+json")
        .apply { if (retryAfter != null) addHeader("Retry-After", retryAfter) }
        .body(body)
        .build()

    @Test
    fun `登录态请求带 Cookie 头与查询参数`() = runBlocking {
        session.restore(Session(cookie = "kungal_session=secret-token"))
        server.enqueue(ok(Fixtures.text("topics_list.json")))

        val page = api.list(cursor = "cur_abc", limit = 5)

        assertEquals(3, page.items.size)
        val request = server.takeRequest()
        assertEquals("/api/v1/topics", request.url.encodedPath)
        assertEquals("cur_abc", request.url.queryParameter("cursor"))
        assertEquals("5", request.url.queryParameter("limit"))
        assertEquals("false", request.url.queryParameter("include_nsfw"))
        assertEquals("kungal_session=secret-token", request.headers["Cookie"])
        assertEquals("application/json", request.headers["Accept"])
        assertTrue(request.headers["User-Agent"]!!.startsWith("kungal-app/"))
    }

    @Test
    fun `匿名请求不带 Cookie 头`() = runBlocking {
        server.enqueue(ok(Fixtures.text("topics_list.json")))
        api.list()
        assertNull(server.takeRequest().headers["Cookie"])
    }

    @Test
    fun `401 转为 KungalApiException 且 requiresLogin`() = runBlocking {
        server.enqueue(problem(401, Fixtures.text("problem_401.json")))
        try {
            api.detail("4462")
            fail("应当抛出 KungalApiException")
        } catch (e: KungalApiException) {
            assertEquals(401, e.httpStatus)
            assertEquals("MISSING_CREDENTIAL", e.problem?.code)
            assertTrue(e.requiresLogin)
            assertEquals("The request has no credentials.", e.message)
        }
    }

    @Test
    fun `429 带 Retry-After`() = runBlocking {
        val body = """{"type":"t","title":"Too many requests","status":429,"detail":"慢一点","code":"RATE_LIMITED"}"""
        server.enqueue(problem(429, body, retryAfter = "7"))
        try {
            api.list()
            fail("应当抛出 KungalApiException")
        } catch (e: KungalApiException) {
            assertEquals(429, e.httpStatus)
            assertTrue(e.isRateLimited)
            assertEquals(7L, e.retryAfterSeconds)
            assertEquals("慢一点", e.message)
        }
    }

    @Test
    fun `postJson 发送幂等键与 JSON 体`() = runBlocking {
        server.enqueue(ok("""{"ok":true}"""))
        val result = client.postJson(
            path = "/replies",
            body = """{"topic_id":"4462","content":"hi"}""",
            idempotencyKey = "01J-abc",
            deserializer = JsonElement.serializer(),
        )
        assertEquals(JsonPrimitive(true), (result as kotlinx.serialization.json.JsonObject)["ok"])
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/replies", request.url.encodedPath)
        assertEquals("01J-abc", request.headers["Idempotency-Key"])
        assertEquals("application/json; charset=utf-8", request.headers["Content-Type"])
        assertEquals("""{"topic_id":"4462","content":"hi"}""", request.body!!.utf8())
    }

    @Test
    fun `错误响应体不是 problem 时按状态码兜底`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(502).body("<html>Bad Gateway</html>").build())
        try {
            api.list()
            fail("应当抛出 KungalApiException")
        } catch (e: KungalApiException) {
            assertEquals(502, e.httpStatus)
            assertNull(e.problem)
            assertTrue(e.message!!.contains("502"))
        }
    }

    @Test
    fun `成功响应体解析失败时给出可读错误`() = runBlocking {
        server.enqueue(ok("not-json"))
        try {
            api.list()
            fail("应当抛出 KungalApiException")
        } catch (e: KungalApiException) {
            assertNull(e.problem)
            assertTrue(e.message!!.startsWith("响应解析失败"))
        }
    }

    @Test
    fun `PageList 反序列化泛型`() {
        val page = KungalJson.decodeFromString<PageList<JsonElement>>("""{"object":"list","items":[{"a":1}]}""")
        assertEquals(1, page.items.size)
        assertNull(page.nextCursor)
        assertTrue(!page.hasMore)
    }
}
