package com.kungal.core

import com.kungal.core.api.NotificationApi
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class NotificationApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: NotificationApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        api = NotificationApi(KungalClient(baseUrl = baseUrl, session = session))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String, code: Int = 200) = MockResponse.Builder()
        .code(code).addHeader("Content-Type", "application/json").body(body).build()

    private val notificationJson = """
        {"object":"notification","id":"501","notification_type":"liked",
         "actor":{"object":"user","id":"9","name":"阿瞳","avatar":null,"avatar_frame":null},
         "actor_count":3,"item_count":1,"path":"/topic/42",
         "excerpt_markdown":"**好耶**","origin":"local","is_read":false,
         "created_at":"2026-10-17T03:04:05Z"}
    """.trimIndent()

    @Test
    fun `通知列表携带分区类型游标并解析`() = runBlocking {
        server.enqueue(ok("""{"object":"list","items":[$notificationJson],"next_cursor":"cur_2"}"""))
        val page = api.notifications(isMuted = true, cursor = "cur_1", limit = 30, type = "liked")
        assertEquals(1, page.items.size)
        val item = page.items[0]
        assertEquals("501", item.id)
        assertEquals("liked", item.notificationType)
        assertEquals("阿瞳", item.actor.name)
        assertEquals(3, item.actorCount)
        assertEquals("/topic/42", item.path)
        assertEquals("**好耶**", item.excerptMarkdown)
        assertFalse(item.isRead)
        assertEquals("2026-10-17T03:04:05Z", item.createdAt)
        assertEquals("cur_2", page.nextCursor)

        val request = server.takeRequest()
        assertEquals("/api/v1/me/notifications", request.url.encodedPath)
        assertEquals("true", request.url.queryParameter("is_muted"))
        assertEquals("liked", request.url.queryParameter("notification_type"))
        assertEquals("cur_1", request.url.queryParameter("cursor"))
        assertEquals("30", request.url.queryParameter("limit"))
    }

    @Test
    fun `未静音列表默认不传类型`() = runBlocking {
        server.enqueue(ok("""{"object":"list","items":[]}"""))
        val page = api.notifications()
        assertTrue(page.items.isEmpty())
        assertNull(page.nextCursor)
        val request = server.takeRequest()
        assertEquals("false", request.url.queryParameter("is_muted"))
        assertNull(request.url.queryParameter("notification_type"))
    }

    @Test
    fun `未读汇总解析并计算角标`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"notification_summary","unread_count":4,"muted_unread_count":2,
                   "direct_message_unread_count":3,"is_direct_message_muted":false,
                   "latest":$notificationJson}""",
            ),
        )
        val summary = api.summary()
        assertEquals(4, summary.unreadCount)
        assertEquals(2, summary.mutedUnreadCount)
        assertEquals(3, summary.directMessageUnreadCount)
        assertEquals("501", summary.latest?.id)
        assertEquals(7, summary.badgeCount)
        assertEquals("/api/v1/me/notifications/summary", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `静音私信时角标不计私信`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"notification_summary","unread_count":4,"muted_unread_count":2,
                   "direct_message_unread_count":3,"is_direct_message_muted":true,"latest":null}""",
            ),
        )
        assertEquals(4, api.summary().badgeCount)
    }

    @Test
    fun `已读标记发送分区与上界`() = runBlocking {
        server.enqueue(
            ok("""{"object":"notification_read_marker","marked_count":5,"unread_count":0}"""),
        )
        val result = api.markRead(upToId = "501", isMuted = false)
        assertEquals(5, result.markedCount)
        assertEquals(0, result.unreadCount)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/me/notifications/read-marker", request.url.encodedPath)
        val body = Json.parseToJsonElement(request.body!!.utf8()).toString()
        assertEquals("""{"up_to_id":"501","is_muted":false}""", body)
    }

    @Test
    fun `删除通知为无响应体 DELETE`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(204).build())
        api.deleteNotification("501")
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/me/notifications/501", request.url.encodedPath)
    }

    @Test
    fun `通知偏好读取与单类型静音取消`() = runBlocking {
        server.enqueue(ok("""{"object":"notification_preferences","muted_types":[]}"""))
        assertTrue(api.preferences().mutedTypes.isEmpty())
        assertEquals(
            "/api/v1/me/notification-preferences",
            server.takeRequest().url.encodedPath,
        )

        server.enqueue(ok("""{"object":"notification_preferences","muted_types":["liked"]}"""))
        assertEquals(listOf("liked"), api.muteType("liked").mutedTypes)
        val mute = server.takeRequest()
        assertEquals("PUT", mute.method)
        assertEquals("/api/v1/me/notification-preferences/muted-types/liked", mute.url.encodedPath)

        server.enqueue(ok("""{"object":"notification_preferences","muted_types":[]}"""))
        assertTrue(api.unmuteType("liked").mutedTypes.isEmpty())
        val unmute = server.takeRequest()
        assertEquals("DELETE", unmute.method)
        assertEquals("/api/v1/me/notification-preferences/muted-types/liked", unmute.url.encodedPath)
    }

    @Test
    fun `关注墙列表解析作品与网站`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[
                   {"object":"followed_wall","subject_type":"galgame","subject_id":"77",
                    "work":{"object":"work","id":"77","display_name":"星之梦","latin":"planetarian",
                      "localized":{},"cover":null,"is_nsfw":false},"website":null},
                   {"object":"followed_wall","subject_type":"website","subject_id":"88",
                    "work":null,"website":{"object":"website","id":"88","host":"example.com",
                      "title":"示例站","icon":null,"is_nsfw":false}}],"next_cursor":"cur_w"}""",
            ),
        )
        val page = api.followedWalls(limit = 30)
        assertEquals(2, page.items.size)
        assertEquals("galgame", page.items[0].subjectType)
        assertEquals("星之梦", page.items[0].work?.displayName)
        assertEquals("example.com", page.items[1].website?.host)
        assertEquals("cur_w", page.nextCursor)
        val request = server.takeRequest()
        assertEquals("/api/v1/me/walls", request.url.encodedPath)
        assertEquals("30", request.url.queryParameter("limit"))
    }

    @Test
    fun `关注墙状态与关注取消`() = runBlocking {
        server.enqueue(
            ok("""{"object":"wall_state","id":"77","subject_type":"galgame",
                   "subject_id":"77","is_following":true}"""),
        )
        assertTrue(api.followWall("galgame", "77").isFollowing)
        val follow = server.takeRequest()
        assertEquals("PUT", follow.method)
        assertEquals("/api/v1/me/walls/galgame/77/follow", follow.url.encodedPath)

        server.enqueue(
            ok("""{"object":"wall_state","id":"77","subject_type":"galgame",
                   "subject_id":"77","is_following":false}"""),
        )
        assertFalse(api.unfollowWall("galgame", "77").isFollowing)
        val unfollow = server.takeRequest()
        assertEquals("DELETE", unfollow.method)
        assertEquals("/api/v1/me/walls/galgame/77/follow", unfollow.url.encodedPath)
    }
}
