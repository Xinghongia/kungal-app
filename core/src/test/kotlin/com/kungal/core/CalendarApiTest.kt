package com.kungal.core

import com.kungal.core.api.CalendarApi
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

class CalendarApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: CalendarApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = CalendarApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `单月月历解析并按参数请求`() = runBlocking {
        server.enqueue(ok(Fixtures.text("release_calendar_month.json")))
        val data = api.month(
            month = "2026-10",
            includeNsfw = true,
            includeDoujin = true,
            excludeTagIds = listOf("11", "12"),
            excludeTagTrees = listOf("21"),
        )
        assertEquals("release_calendar_month", data.obj)
        assertEquals("2026-10", data.calendarMonth)
        assertEquals("2026-10-17", data.todayDate)
        assertEquals(15, data.items.size)
        assertEquals(15, data.itemCount)
        assertFalse(data.isTruncated)
        assertTrue(data.hasPrev)
        assertTrue(data.hasNext)
        assertEquals("2026-09", data.prevMonth)
        assertEquals("2026-11", data.nextMonth)
        assertNull(data.minMonth)
        assertNull(data.maxMonth)
        val first = data.items.first()
        assertEquals("270416", first.id)
        assertEquals("2026-10-01", first.releaseDate)
        assertEquals("day", first.releaseDatePrecision)
        assertEquals("Sakata Haruto", first.maker?.displayName)
        assertTrue(first.cover?.url?.startsWith("https://image.kungal.iloveren.link/") == true)
        assertFalse(first.isPublished)

        val request = server.takeRequest()
        assertEquals("/api/v1/release-calendar", request.url.encodedPath)
        assertEquals("2026-10", request.url.queryParameter("month"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
        assertEquals("true", request.url.queryParameter("include_doujin"))
        assertEquals(listOf("11", "12"), request.url.queryParameterValues("exclude_tag_ids"))
        assertEquals(listOf("21"), request.url.queryParameterValues("exclude_tag_trees"))
    }

    @Test
    fun `未发售按月分组`() = runBlocking {
        server.enqueue(ok(Fixtures.text("release_calendar_upcoming.json")))
        val data = api.upcoming(includeNsfw = false, includeDoujin = false)
        assertEquals("release_calendar_upcoming", data.obj)
        assertEquals("2026-10-17", data.todayDate)
        assertEquals(6, data.entries.size)
        assertEquals(38, data.itemCount)
        val first = data.entries.first()
        assertEquals("2026-10", first.calendarMonth)
        assertEquals(15, first.items.size)
        assertFalse(first.isTruncated)

        val request = server.takeRequest()
        assertEquals("/api/v1/release-calendar/upcoming", request.url.encodedPath)
        assertEquals("false", request.url.queryParameter("include_nsfw"))
        assertEquals("false", request.url.queryParameter("include_doujin"))
        assertNull(request.url.queryParameter("month"))
    }

    @Test
    fun `年内待定按年份请求`() = runBlocking {
        server.enqueue(ok(Fixtures.text("release_calendar_pending.json")))
        val data = api.pending(year = 2026, includeDoujin = true)
        assertEquals("release_calendar_pending", data.obj)
        assertEquals(2026, data.year)
        assertEquals(37, data.items.size)
        assertEquals(37, data.itemCount)
        assertFalse(data.isTruncated)
        assertEquals("4827", data.items.first().id)

        val request = server.takeRequest()
        assertEquals("/api/v1/release-calendar/pending", request.url.encodedPath)
        assertEquals("2026", request.url.queryParameter("year"))
        assertEquals("true", request.url.queryParameter("include_doujin"))
    }

    @Test
    fun `发售日期未定`() = runBlocking {
        server.enqueue(ok(Fixtures.text("release_calendar_tba.json")))
        val data = api.tba()
        assertEquals("release_calendar_tba", data.obj)
        assertEquals(157, data.itemCount)
        assertEquals(157, data.items.size)
        assertNull(data.items.first().releaseDate)
        assertNull(data.items.first().releaseDatePrecision)

        val request = server.takeRequest()
        assertEquals("/api/v1/release-calendar/tba", request.url.encodedPath)
    }

    @Test
    fun `今日发售判断`() = runBlocking {
        server.enqueue(ok(Fixtures.text("release_calendar_today.json")))
        val data = api.today(includeNsfw = true)
        assertEquals("release_calendar_today", data.obj)
        assertEquals("2026-10-17", data.todayDate)
        assertFalse(data.hasRelease)
        assertEquals(8397, data.expiresIn)

        val request = server.takeRequest()
        assertEquals("/api/v1/release-calendar/today", request.url.encodedPath)
        assertEquals("true", request.url.queryParameter("include_nsfw"))
        assertNull(request.url.queryParameter("include_doujin"))
    }
}
