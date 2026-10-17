package com.kungal.core

import com.kungal.core.api.MeApi
import com.kungal.core.model.Account
import com.kungal.core.model.Me
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MeApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: MeApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        api = MeApi(KungalClient(baseUrl = baseUrl, session = session))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `me 字段解析`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"me","id":"1207","moemoepoint":4321,"has_checked_in_today":true,
                   "has_unread_messages":false,"is_creator":true,"toolset_upload_today_bytes":0}""",
            ),
        )
        val me = api.me()
        assertEquals("1207", me.id)
        assertEquals(4321, me.moemoepoint)
        assertTrue(me.hasCheckedInToday)
        assertFalse(me.hasUnreadMessages)
        assertTrue(me.isCreator)
        assertEquals("/api/v1/me", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `account 字段解析（含 content_stance）`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"account","id":"1207","name":"鲲","roles":["user","creator"],
                   "avatar":{"url":"https://image.kungal.iloveren.link/aa/bb/x.webp","hash":"h"},
                   "content_stance":{"is_adult_confirmed":true,"nsfw_display":"blur"}}""",
            ),
        )
        val account = api.account()
        assertEquals("鲲", account.name)
        assertEquals(listOf("user", "creator"), account.roles)
        assertEquals(64, "a".repeat(64).length)
        assertTrue(account.contentStance?.isAdultConfirmed == true)
        assertEquals("blur", account.contentStance?.nsfwDisplay)
        assertEquals("/api/v1/me/account", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `account 缺省字段用默认值`() {
        val account = KungalJson.decodeFromString<Account>("""{"object":"account","id":"1"}""")
        assertNull(account.name)
        assertTrue(account.roles.isEmpty())
        assertNull(account.contentStance)
    }

    @Test
    fun `setNsfwDisplay 发送 PUT 与 JSON 体`() = runBlocking {
        server.enqueue(
            ok("""{"object":"nsfw_display","nsfw_display":"show"}"""),
        )
        val result = api.setNsfwDisplay("show")
        assertEquals("show", result.nsfwDisplay)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/me/nsfw-display", request.url.encodedPath)
        assertEquals("application/json; charset=utf-8", request.headers["Content-Type"])
        assertEquals("""{"nsfw_display":"show"}""", request.body!!.utf8())
    }

    @Test
    fun `setNsfwDisplay 缺少 preferences scope 时抛出问题码`() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(403)
                .addHeader("Content-Type", "application/problem+json")
                .body("""{"title":"Scope required","status":403,"code":"SCOPE_REQUIRED"}""")
                .build(),
        )
        try {
            api.setNsfwDisplay("blur")
            org.junit.Assert.fail("应当失败")
        } catch (e: com.kungal.core.net.KungalApiException) {
            assertEquals(403, e.httpStatus)
            assertEquals("SCOPE_REQUIRED", e.problem?.code)
        }
    }

    @Test
    fun `me 需登录——401 会抛出 requiresLogin`() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(401)
                .addHeader("Content-Type", "application/problem+json")
                .body("""{"title":"Missing credential","status":401,"code":"MISSING_CREDENTIAL"}""")
                .build(),
        )
        try {
            api.me()
            org.junit.Assert.fail("应当失败")
        } catch (e: com.kungal.core.net.KungalApiException) {
            assertTrue(e.requiresLogin)
        }
    }

    @Test
    fun `Me 默认值`() {
        val me = KungalJson.decodeFromString<Me>("""{"object":"me","id":"1"}""")
        assertEquals(0, me.moemoepoint)
        assertFalse(me.hasCheckedInToday)
    }

    @Test
    fun `云端偏好读写保留未知键并带 If-Match`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"preferences","version":4,
                   "written_at":"2026-10-17T06:00:00Z",
                   "doc":{"show_platform":true,"feed_tabs":[{"id":"topic"}],
                     "android_note":"keep"}}""",
            ),
        )
        val prefs = api.preferences()
        assertEquals(4, prefs.version)
        assertEquals("2026-10-17T06:00:00Z", prefs.writtenAt)
        assertEquals("true", prefs.doc["show_platform"]!!.jsonPrimitive.content)
        assertTrue(prefs.doc.containsKey("feed_tabs"))
        var request = server.takeRequest()
        assertEquals("/api/v1/me/preferences", request.url.encodedPath)

        val merged = buildJsonObject {
            put("show_platform", false)
            putJsonArray("feed_tabs") { add(buildJsonObject { put("id", "topic") }) }
            put("android_note", "keep")
        }
        server.enqueue(
            ok(
                """{"object":"preferences","version":5,
                   "written_at":"2026-10-17T06:01:00Z","doc":{}}""",
            ),
        )
        val saved = api.savePreferences(merged, version = prefs.version)
        assertEquals(5, saved.version)
        request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/me/preferences", request.url.encodedPath)
        assertEquals("\"4\"", request.headers["If-Match"])
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        val doc = body["doc"]!!.jsonObject
        assertEquals("false", doc["show_platform"]!!.jsonPrimitive.content)
        assertTrue(doc.containsKey("feed_tabs"))
        assertEquals("keep", doc["android_note"]!!.jsonPrimitive.content)
    }
}
