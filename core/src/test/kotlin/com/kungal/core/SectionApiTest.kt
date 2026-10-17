package com.kungal.core

import com.kungal.core.api.SectionApi
import com.kungal.core.model.Section
import com.kungal.core.net.PageList
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class SectionApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SectionApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = SectionApi(KungalClient(baseUrl = baseUrl, session = SessionHolder().apply { restore(Session("kungal_session=t")) }))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `真实样本解析 27 个版块并按分类过滤参数`() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(200)
                .addHeader("Content-Type", "application/json")
                .body(Fixtures.text("sections.json"))
                .build(),
        )
        val page = api.list("galgame")

        assertEquals(27, page.items.size)
        val first = page.items[0]
        assertEquals("g-walkthrough", first.section)
        assertEquals("galgame", first.category)
        assertEquals(61, first.topicCount)
        assertEquals(177732L, first.viewCount)
        assertNotNull(first.latestTopic)
        assertEquals("4453", first.latestTopic!!.id)
        assertEquals("setup安装后打不开", first.latestTopic!!.title)

        val counts = page.items.groupingBy { it.category }.eachCount()
        assertEquals(7, counts["galgame"])
        assertEquals(11, counts["technique"])
        assertEquals(9, counts["others"])

        val request = server.takeRequest()
        assertEquals("/api/v1/sections", request.url.encodedPath)
        assertEquals("galgame", request.url.queryParameter("category"))
    }

    @Test
    fun `无分类参数时不带 query`() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(200)
                .addHeader("Content-Type", "application/json")
                .body("""{"object":"list","items":[]}""")
                .build(),
        )
        val page = api.list()
        assertTrue(page.items.isEmpty())
        val request = server.takeRequest()
        assertEquals("/api/v1/sections", request.url.encodedPath)
        assertEquals(null, request.url.queryParameter("category"))
    }

    @Test
    fun `版块默认值`() {
        val section = KungalJson.decodeFromString<Section>("""{"object":"section","section":"g-news","category":"galgame"}""")
        assertEquals(0, section.topicCount)
        assertEquals(0L, section.viewCount)
        assertEquals(null, section.latestTopic)
    }

    @Test
    fun `PageList 泛型解析版块`() {
        val page = KungalJson.decodeFromString<PageList<Section>>(
            """{"object":"list","items":[{"section":"o-daily","category":"others"}]}""",
        )
        assertEquals("o-daily", page.items[0].section)
    }
}
