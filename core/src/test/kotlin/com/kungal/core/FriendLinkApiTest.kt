package com.kungal.core

import com.kungal.core.api.FriendLinkApi
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class FriendLinkApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: FriendLinkApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = FriendLinkApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `友情链接按分类请求并解析`() = runBlocking {
        server.enqueue(ok(Fixtures.text("friend_links.json")))
        val page = api.list(cursor = "cur_links", limit = 100, category = "galgame")
        assertEquals(43, page.items.size)
        assertNull(page.nextCursor)
        assertEquals(
            setOf("official", "galgame", "others"),
            page.items.map { it.friendLinkCategory }.toSet(),
        )
        assertEquals(3, page.items.count { it.state == "down" })

        val first = page.items.first()
        assertEquals("1", first.id)
        assertEquals("official", first.friendLinkCategory)
        assertEquals("鲲 Galgame 论坛", first.title)
        assertEquals("https://www.kungal.com", first.url)
        assertTrue(first.description.isNotBlank())
        assertTrue(first.banner?.url?.startsWith("https://image.kungal.iloveren.link/") == true)

        val request = server.takeRequest()
        assertEquals("/api/v1/friend-links", request.url.encodedPath)
        assertEquals("cur_links", request.url.queryParameter("cursor"))
        assertEquals("100", request.url.queryParameter("limit"))
        assertEquals("galgame", request.url.queryParameter("friend_link_category"))
    }

    @Test
    fun `未指定分类时不发送参数`() = runBlocking {
        server.enqueue(ok(Fixtures.text("friend_links.json")))
        api.list()
        val request = server.takeRequest()
        assertNull(request.url.queryParameter("friend_link_category"))
        assertEquals("100", request.url.queryParameter("limit"))
    }
}
