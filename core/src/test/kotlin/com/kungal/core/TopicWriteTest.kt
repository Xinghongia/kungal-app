package com.kungal.core

import com.kungal.core.api.TopicApi
import com.kungal.core.model.TopicEngagement
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** 写通道协议测试：反应 PUT/DELETE、发回复（幂等键强制）、浏览计数 204。 */
class TopicWriteTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TopicApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        api = TopicApi(KungalClient(baseUrl = baseUrl, session = session))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private val engagementJson = """
    {"object":"topic_engagement","topic_id":"4462","like_count":15,"dislike_count":0,
     "favorite_count":0,"upvote_count":1,"upvoted_at":null,"reactions":[
       {"reaction":"like","count":15,"reactors":[],"viewer":{"has_reacted":true}}],
     "viewer":{"has_liked":true}}
    """.trimIndent()

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `点赞走 PUT 反应端点并解析互动快照`() = runBlocking {
        server.enqueue(ok(engagementJson))
        val result = api.react("4462", "like", active = true)

        assertEquals(15, result.likeCount)
        assertEquals(true, result.viewer?.hasLiked)
        assertEquals(true, result.reactions[0].viewer?.hasReacted)

        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/topics/4462/reactions/like", request.url.encodedPath)
        assertEquals("kungal_session=t", request.headers["Cookie"])
    }

    @Test
    fun `取消点赞走 DELETE 同一端点`() = runBlocking {
        server.enqueue(ok(engagementJson))
        api.react("4462", "like", active = false)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/topics/4462/reactions/like", request.url.encodedPath)
    }

    @Test
    fun `发回复带强制幂等键与 markdown 请求体`() = runBlocking {
        val replyJson = """{"object":"reply","id":"17799","topic_id":"4462","floor":16,
            "author":{"object":"user","id":"1","name":"me"},"content":{"object":"document","children":[]},
            "created_at":"2026-10-17T00:00:00Z"}"""
        server.enqueue(MockResponse.Builder().code(201).addHeader("Content-Type", "application/json").body(replyJson).build())

        val reply = api.createReply("4462", "你好 **世界**", "01J-KEY-1")

        assertEquals("17799", reply.id)
        assertEquals(16, reply.floor)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/topics/4462/replies", request.url.encodedPath)
        assertEquals("01J-KEY-1", request.headers["Idempotency-Key"])
        val body = KungalJson.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("你好 **世界**", body["content_markdown"]!!.jsonPrimitive.content)
    }

    @Test
    fun `浏览计数接受 204`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        api.markView("4462")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/topics/4462/views", request.url.encodedPath)
    }

    @Test
    fun `浏览计数失败抛出异常`() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(400)
                .addHeader("Content-Type", "application/problem+json")
                .body("""{"title":"Bad Request","status":400,"detail":"bad","code":"BAD_REQUEST"}""")
                .build(),
        )
        try {
            api.markView("4462")
            org.junit.Assert.fail("应当抛出 KungalApiException")
        } catch (e: com.kungal.core.net.KungalApiException) {
            assertEquals(400, e.httpStatus)
        }
    }

    @Test
    fun `互动快照在字段缺省时用默认值`() {
        val minimal = KungalJson.decodeFromString<TopicEngagement>("""{"object":"topic_engagement","topic_id":"1"}""")
        assertEquals(0, minimal.likeCount)
        assertTrue(minimal.reactions.isEmpty())
        assertEquals(JsonPrimitive("topic_engagement"), JsonPrimitive(minimal.obj))
    }
}
