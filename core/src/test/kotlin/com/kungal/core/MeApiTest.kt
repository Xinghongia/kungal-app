package com.kungal.core

import com.kungal.core.api.MeApi
import com.kungal.core.model.Account
import com.kungal.core.model.Me
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
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
}
