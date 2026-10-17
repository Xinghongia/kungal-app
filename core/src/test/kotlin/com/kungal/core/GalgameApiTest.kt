package com.kungal.core

import com.kungal.core.api.GalgameApi
import com.kungal.core.api.LibraryWorkQuery
import com.kungal.core.api.WorkListQuery
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

class GalgameApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: GalgameApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        api = GalgameApi(KungalClient(baseUrl = baseUrl, session = session))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `资源资料库列表解析真实样本并传筛选参数`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_works_list.json")))
        val query = WorkListQuery(
            page = 2,
            limit = 2,
            sort = "view_7d_desc",
            resourcePlatforms = listOf("win", "and"),
            resourceLanguages = listOf("zh-cn"),
            gameType = "moe",
            releasedFrom = "2020",
            includeNsfw = true,
            excludeTagTrees = listOf("1333"),
        )
        val page = api.works(query)

        assertEquals(4833, page.total)
        assertEquals("eq", page.totalRelation)
        assertEquals(2, page.items.size)
        val first = page.items[0]
        assertEquals("4219", first.id)
        assertFalse(first.isNsfw)
        assertTrue(first.isPublished)
        assertEquals("166", first.maker?.id)
        assertEquals(listOf("win"), first.resourcePlatforms)
        assertEquals(listOf("zh-cn"), first.resourceLanguages)
        assertEquals("2012-03-29", first.releaseDate)
        assertEquals("day", first.releaseDatePrecision)
        assertTrue(first.localized.containsKey("zh-Hans"))

        val request = server.takeRequest()
        assertEquals("/api/v1/works", request.url.encodedPath)
        assertEquals("2", request.url.queryParameter("page"))
        assertEquals("2", request.url.queryParameter("limit"))
        assertEquals("view_7d_desc", request.url.queryParameter("sort"))
        assertEquals(listOf("win", "and"), request.url.queryParameterValues("resource_platforms"))
        assertEquals(listOf("zh-cn"), request.url.queryParameterValues("resource_languages"))
        assertEquals("moe", request.url.queryParameter("game_type"))
        assertEquals("2020", request.url.queryParameter("released_from"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
        assertEquals(listOf("1333"), request.url.queryParameterValues("exclude_tag_trees"))
        assertNull(request.url.queryParameter("resource_type"))
    }

    @Test
    fun `信息资料库支持关键词与年份且总数可能是下限`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_library_list.json")))
        val page = api.libraryWorks(
            LibraryWorkQuery(
                page = 1,
                limit = 2,
                q = "root",
                sort = "released_desc",
                releasedFrom = "2019",
                releasedTo = "2020-03",
                includeAllOriginalLanguages = true,
            ),
        )

        assertEquals(10000, page.total)
        assertEquals("gte", page.totalRelation)
        assertEquals("260", page.items[0].id)
        assertEquals(7.8, page.items[0].ratingScore ?: 0.0, 0.001)
        assertEquals(4, page.items[0].ratingCount)

        val request = server.takeRequest()
        assertEquals("/api/v1/library-works", request.url.encodedPath)
        assertEquals("root", request.url.queryParameter("q"))
        assertEquals("released_desc", request.url.queryParameter("sort"))
        assertEquals("2019", request.url.queryParameter("released_from"))
        assertEquals("2020-03", request.url.queryParameter("released_to"))
        assertEquals("true", request.url.queryParameter("include_all_original_languages"))
    }

    @Test
    fun `收录月份按年降序解析`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_collected_months.json")))
        val months = api.collectedMonths(includeNsfw = true)

        assertEquals(31, months.items.size)
        assertTrue(months.items.first().year >= months.items.last().year)
        assertEquals("true", server.takeRequest().url.queryParameter("include_nsfw"))
    }

    @Test
    fun `标签屏蔽游标翻页并给出排除列表`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[{"object":"tag_block","id":"12","mode":"tree",
                   "created_at":"2026-10-01T00:00:00Z"}],"next_cursor":"c2"}""",
            ),
        )
        server.enqueue(
            ok(
                """{"object":"list","items":[{"object":"tag_block","id":"34","mode":"exact",
                   "created_at":"2026-10-02T00:00:00Z"}]}""",
            ),
        )

        val blocks = api.tagBlocks()

        assertEquals(listOf("12", "34"), blocks.map { it.id })
        assertEquals(listOf("tree", "exact"), blocks.map { it.mode })
        assertEquals("/api/v1/me/blocked-tags", server.takeRequest().url.encodedPath)
        assertEquals("c2", server.takeRequest().url.queryParameter("cursor"))
    }

    @Test
    fun `查询构造器省略空筛选`() {
        val query = WorkListQuery().toQuery()
        assertEquals(1, query["page"])
        assertEquals(24, query["limit"])
        assertEquals("resource_updated_desc", query["sort"])
        assertEquals(false, query["include_nsfw"])
        assertNull(query["resource_type"])
        assertNull(query["resource_platforms"])
        assertFalse(query.containsKey("include_resourceless"))
        assertFalse(query.containsKey("exclude_tag_ids"))
    }
}
