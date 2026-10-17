package com.kungal.core

import com.kungal.core.api.SocialApi
import com.kungal.core.model.ReportCreate
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SocialApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SocialApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        api = SocialApi(KungalClient(baseUrl = baseUrl, session = session))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `关注状态解析`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"user_follow_state","id":"2","user_id":"2","is_following":true,
                   "is_followed_by":false,"notify_level":"all"}""",
            ),
        )
        val state = api.followState("2")
        assertTrue(state.isFollowing)
        assertFalse(state.isFollowedBy)
        assertEquals("/api/v1/me/following/2", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `关注与取消关注使用 PUT、DELETE`() = runBlocking {
        val body = """{"object":"user_follow_state","id":"2","user_id":"2","is_following":true,"is_followed_by":false,"notify_level":"all"}"""
        server.enqueue(ok(body))
        api.follow("2")
        assertEquals("PUT", server.takeRequest().method)

        val off = """{"object":"user_follow_state","id":"2","user_id":"2","is_following":false,"is_followed_by":false,"notify_level":null}"""
        server.enqueue(ok(off))
        val state = api.unfollow("2")
        assertFalse(state.isFollowing)
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `举报原因列表`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[{"object":"report_reason","key":"spam","display_name":"垃圾广告"},
                   {"object":"report_reason","key":"abuse","display_name":"人身攻击"}]}""",
            ),
        )
        val reasons = api.reportReasons()
        assertEquals(2, reasons.items.size)
        assertEquals("垃圾广告", reasons.items[0].displayName)
        assertEquals("/api/v1/report-reasons", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `举报发送带幂等键的 JSON 并接受 204`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        api.report(
            ReportCreate(
                subjectKind = "forum_reply",
                subjectId = "15715",
                reasonKey = "spam",
                note = "广告",
                subjectUrl = "https://www.kungal.com/topic/3938?reply=1",
            ),
            idempotencyKey = "01J-r",
        )
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/reports", request.url.encodedPath)
        assertEquals("01J-r", request.headers["Idempotency-Key"])
        assertEquals(
            """{"subject_kind":"forum_reply","subject_id":"15715","reason_key":"spam","note":"广告","subject_url":"https://www.kungal.com/topic/3938?reply=1"}""",
            request.body!!.utf8(),
        )
    }
}
