package com.kungal.core

import com.kungal.core.api.NewsApi
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

class NewsApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: NewsApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = NewsApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `情报流按游标与筛选请求`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_items.json")))
        val page = api.items(
            cursor = "cur_abc",
            limit = 7,
            includeTotal = true,
            lane = "column",
            source = "ymgal",
            year = 2026,
            month = 10,
        )
        assertEquals("list", page.obj)
        assertEquals(7, page.items.size)
        assertEquals(4751, page.total)
        assertTrue(page.nextCursor?.startsWith("cur_") == true)
        val first = page.items.first()
        assertEquals("4760", first.id)
        assertEquals("galgame_hihyou", first.newsSource)
        assertEquals("news", first.lane)
        assertFalse(first.hasBody)
        assertNull(first.submitter)
        assertEquals("2026-10-11T09:20:51Z", first.publishedAt)
        assertEquals(1125, first.banner?.width)
        assertEquals(750, first.banner?.height)
        assertTrue(first.sourceUrl.startsWith("https://"))
        assertTrue(first.content.children.isEmpty())

        val request = server.takeRequest()
        assertEquals("/api/v1/news-items", request.url.encodedPath)
        assertEquals("cur_abc", request.url.queryParameter("cursor"))
        assertEquals("7", request.url.queryParameter("limit"))
        assertEquals("true", request.url.queryParameter("include_total"))
        assertEquals("column", request.url.queryParameter("lane"))
        assertEquals("ymgal", request.url.queryParameter("news_source"))
        assertEquals("2026", request.url.queryParameter("year"))
        assertEquals("10", request.url.queryParameter("month"))
    }

    @Test
    fun `没有年份时不发送月份`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_items.json")))
        api.items(month = 10)
        val request = server.takeRequest()
        assertNull(request.url.queryParameter("year"))
        assertNull(request.url.queryParameter("month"))
    }

    @Test
    fun `来源目录`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_sources.json")))
        val sources = api.sources()
        assertEquals(3, sources.size)
        assertEquals("community", sources.first().key)
        assertEquals("galgame_hihyou", sources[1].key)
        assertTrue(sources[1].attribution.isNotBlank())
        assertTrue(sources[1].homepageUrl.startsWith("https://"))
        assertTrue(sources[1].columnUrl.startsWith("https://"))
        assertNull(sources.first().forumAccount)

        val request = server.takeRequest()
        assertEquals("/api/v1/news-sources", request.url.encodedPath)
    }

    @Test
    fun `归档索引与按年月份`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_archive.json")))
        val index = api.archive()
        assertEquals("news_archive", index.obj)
        assertEquals(5, index.years.size)
        assertEquals(2026, index.years.first().year)
        assertEquals(943, index.years.first().count)
        assertTrue(index.months.isEmpty())

        server.enqueue(ok(Fixtures.text("news_archive_year.json")))
        val year = api.archive(year = 2026, lane = "news", source = "ymgal")
        assertEquals(10, year.months.size)
        assertEquals(1, year.months.first().month)
        assertEquals(61, year.months.first().count)

        assertEquals("/api/v1/news-archive", server.takeRequest().url.encodedPath)
        val request = server.takeRequest()
        assertEquals("2026", request.url.queryParameter("year"))
        assertEquals("news", request.url.queryParameter("lane"))
        assertEquals("ymgal", request.url.queryParameter("news_source"))
    }

    @Test
    fun `单月概览与逐日计数`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_month.json")))
        val month = api.month(2026, 10, lane = "news")
        assertEquals("news_month", month.obj)
        assertEquals(2026, month.year)
        assertEquals(10, month.month)
        assertEquals(55, month.itemCount)
        assertEquals(31, month.days.size)
        assertEquals(1, month.days.first().day)
        assertEquals(2, month.days.first().count)
        assertEquals(30, month.days[3].count)

        val request = server.takeRequest()
        assertEquals("/api/v1/news-archive/2026/10", request.url.encodedPath)
        assertEquals("news", request.url.queryParameter("lane"))
    }

    @Test
    fun `单月条目按页码请求`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_month_items.json")))
        val page = api.monthItems(2026, 10, page = 1, limit = 5, day = 11)
        assertEquals(5, page.items.size)
        assertEquals(55, page.total)
        assertEquals("eq", page.totalRelation)
        assertEquals("4760", page.items.first().id)

        val request = server.takeRequest()
        assertEquals("/api/v1/news-archive/2026/10/items", request.url.encodedPath)
        assertEquals("1", request.url.queryParameter("page"))
        assertEquals("5", request.url.queryParameter("limit"))
        assertEquals("11", request.url.queryParameter("day"))
        assertNull(request.url.queryParameter("lane"))
    }

    @Test
    fun `情报详情`() = runBlocking {
        server.enqueue(ok(Fixtures.text("news_detail.json")))
        val item = api.item("4760")
        assertEquals("news_item", item.obj)
        assertEquals("4760", item.id)
        assertEquals("galgame_hihyou", item.newsSource)
        assertFalse(item.hasBody)
        assertEquals("https://www.bilibili.com/read/cv53384810", item.sourceUrl)
        assertTrue(item.preview.isNotBlank())
        assertTrue(item.content.children.isEmpty())

        assertEquals("/api/v1/news-items/4760", server.takeRequest().url.encodedPath)
    }
}
