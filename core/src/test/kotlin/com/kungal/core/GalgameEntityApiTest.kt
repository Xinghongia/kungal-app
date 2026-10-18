package com.kungal.core

import com.kungal.core.api.CollectionApi
import com.kungal.core.api.EntityWorkQuery
import com.kungal.core.api.GalgameEntityApi
import com.kungal.core.model.CollectionPatchBody
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

class GalgameEntityApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: GalgameEntityApi
    private lateinit var collections: CollectionApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        val client = KungalClient(baseUrl = baseUrl, session = session)
        api = GalgameEntityApi(client)
        collections = CollectionApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String, code: Int = 200) = MockResponse.Builder()
        .code(code).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `标签列表详情与作品列表`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_tag_list.json")))
        val page = api.tags(q = "恋爱", page = 2, limit = 3, includeNsfw = true)
        assertEquals(2578, page.total)
        assertEquals(3, page.items.size)
        assertEquals("1333", page.items[0].id)
        assertEquals("content", page.items[0].tagKind)
        assertTrue(page.items[0].descendantCount > 0)
        val request = server.takeRequest()
        assertEquals("/api/v1/tags", request.url.encodedPath)
        assertEquals("恋爱", request.url.queryParameter("q"))
        assertEquals("2", request.url.queryParameter("page"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))

        server.enqueue(ok(Fixtures.text("galgame_tag_detail.json")))
        val tag = api.tag("1333")
        assertEquals(8, tag.subtags.size)
        assertEquals(9, tag.descendantCount)
        assertEquals(21932, tag.catalogWorkCount)
        assertFalse(tag.isHidden)
        assertEquals("/api/v1/tags/1333", server.takeRequest().url.encodedPath)

        server.enqueue(ok(Fixtures.text("galgame_works_list.json")))
        val works = api.tagWorks(
            "1333",
            EntityWorkQuery(
                page = 2,
                limit = 2,
                sort = "view_7d_desc",
                resourcePlatform = "win",
                includeNsfw = true,
                excludeTagTrees = listOf("1333"),
            ),
        )
        assertEquals(2, works.items.size)
        val worksRequest = server.takeRequest()
        assertEquals("/api/v1/tags/1333/works", worksRequest.url.encodedPath)
        assertEquals("win", worksRequest.url.queryParameter("resource_platform"))
        assertEquals("view_7d_desc", worksRequest.url.queryParameter("sort"))
        assertEquals(listOf("1333"), worksRequest.url.queryParameterValues("exclude_tag_trees"))
    }

    @Test
    fun `多标签联合筛选带全部标签与分页参数`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_works_list.json")))
        val result = api.taggedWorks(
            tagIds = listOf("1333", "42"),
            page = 3,
            limit = 24,
            includeNsfw = true,
            includeAllOriginalLanguages = true,
            excludeTagIds = listOf("7"),
            excludeTagTrees = listOf("8"),
        )
        assertEquals(2, result.items.size)
        val request = server.takeRequest()
        assertEquals("/api/v1/tagged-works", request.url.encodedPath)
        assertEquals(listOf("1333", "42"), request.url.queryParameterValues("tag_ids"))
        assertEquals("3", request.url.queryParameter("page"))
        assertEquals("24", request.url.queryParameter("limit"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
        assertEquals("true", request.url.queryParameter("include_all_original_languages"))
        assertEquals(listOf("7"), request.url.queryParameterValues("exclude_tag_ids"))
        assertEquals(listOf("8"), request.url.queryParameterValues("exclude_tag_trees"))
    }

    @Test
    fun `旧收藏夹与旧会社 id 解析`() = runBlocking {
        server.enqueue(ok("""{"object":"collection_alias","alias_id":"12","collection_id":"99"}"""))
        val alias = collections.alias("12")
        assertEquals("99", alias.collectionId)
        assertEquals("/api/v1/collection-aliases/12", server.takeRequest().url.encodedPath)

        server.enqueue(
            ok("""{"object":"wiki_company_redirect","wiki_company_id":"7","company_id":"70"}"""),
        )
        val redirect = api.companyRedirect("7")
        assertEquals("70", redirect.companyId)
        assertEquals("/api/v1/wiki-company-redirects/7", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `标签屏蔽读不到时返回 null 并支持设置与取消`() = runBlocking {
        server.enqueue(
            MockResponse.Builder().code(404)
                .addHeader("Content-Type", "application/problem+json")
                .body("""{"type":"t","title":"Not Found","status":404,"detail":"no","instance":"/x","code":"NOT_FOUND","request_id":"req_x","errors":[]}""")
                .build(),
        )
        assertNull(api.tagBlockOrNull("12"))
        assertEquals("/api/v1/me/blocked-tags/12", server.takeRequest().url.encodedPath)

        server.enqueue(
            ok("""{"object":"tag_block","id":"12","mode":"exact","created_at":"2026-10-17T00:00:00Z"}"""),
        )
        val block = api.setTagBlock("12", mode = "exact")
        assertEquals("exact", block.mode)
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("""{"mode":"exact"}""", put.body!!.utf8())

        server.enqueue(MockResponse.Builder().code(204).build())
        api.removeTagBlock("12")
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/me/blocked-tags/12", delete.url.encodedPath)
    }

    @Test
    fun `会社列表详情关系图与作品列表`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_company_list.json")))
        val page = api.companies(q = "dlsite", companyKind = "publisher", limit = 3)
        assertEquals(3, page.items.size)
        assertEquals("3187", page.items[0].id)
        val listRequest = server.takeRequest()
        assertEquals("dlsite", listRequest.url.queryParameter("q"))
        assertEquals("publisher", listRequest.url.queryParameter("company_kind"))

        server.enqueue(ok(Fixtures.text("galgame_company_detail.json")))
        val company = api.company("3187")
        assertEquals("publisher", company.companyKind)
        assertEquals(1, company.links.size)
        assertEquals(4608, company.catalogWorkCount)
        assertEquals("/api/v1/companies/3187", server.takeRequest().url.encodedPath)

        server.enqueue(ok(Fixtures.text("galgame_company_graph.json")))
        val graph = api.companyGraph("3187")
        assertEquals("3187", graph.companyId)
        assertEquals(1, graph.nodes.size)
        assertEquals("/api/v1/companies/3187/graph", server.takeRequest().url.encodedPath)

        server.enqueue(ok(Fixtures.text("galgame_works_list.json")))
        api.companyWorks("3187", EntityWorkQuery(via = "imprint", resourceLanguage = "zh-cn"))
        val worksRequest = server.takeRequest()
        assertEquals("imprint", worksRequest.url.queryParameter("via"))
        assertEquals("zh-cn", worksRequest.url.queryParameter("resource_language"))
    }

    @Test
    fun `引擎与系列列表详情`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_engine_list.json")))
        val engines = api.engines(q = "renpy", limit = 3)
        assertEquals(610, engines.total)
        assertEquals("58", engines.items[0].id)

        server.enqueue(ok(Fixtures.text("galgame_engine_detail.json")))
        assertEquals("Ren'Py", api.engine("58").displayName)

        server.enqueue(ok(Fixtures.text("galgame_series_list.json")))
        val series = api.seriesList(page = 1, limit = 3)
        assertEquals(618, series.total)
        assertEquals(5, series.items[0].sampleWorks.size)

        server.enqueue(ok(Fixtures.text("galgame_series_detail.json")))
        val detail = api.series("688")
        assertEquals(19, detail.listedWorkCount)
        assertEquals(19, detail.catalogWorkCount)
    }

    @Test
    fun `角色列表详情与出演作品`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_character_list.json")))
        val page = api.characters(
            q = "爱丽丝",
            traitIds = listOf("1", "2"),
            traitMatch = "any",
            genders = listOf("female"),
            sort = "popularity_desc",
        )
        assertEquals(3, page.items.size)
        val request = server.takeRequest()
        assertEquals(listOf("1", "2"), request.url.queryParameterValues("trait_ids"))
        assertEquals("any", request.url.queryParameter("trait_match"))
        assertEquals(listOf("female"), request.url.queryParameterValues("genders"))

        server.enqueue(ok(Fixtures.text("galgame_character_detail.json")))
        val character = api.character("211108", includeNsfw = true)
        assertEquals(2, character.intros.size)
        assertEquals(1, character.links.size)
        assertEquals("/api/v1/characters/211108", server.takeRequest().url.encodedPath)

        server.enqueue(ok(Fixtures.text("galgame_character_appearances.json")))
        val appearances = api.characterAppearances("211108", limit = 2)
        assertEquals(2, appearances.items.size)
        assertTrue(appearances.nextCursor!!.startsWith("cur_"))
        assertEquals(
            "2",
            server.takeRequest().url.queryParameter("limit"),
        )
    }

    @Test
    fun `特质列表与详情`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_trait_list.json")))
        val roots = api.traits(limit = 3)
        assertEquals(9, roots.total)
        assertEquals("Hair", roots.items[0].displayName)
        assertEquals(8, roots.items[0].childCount)

        server.enqueue(ok(Fixtures.text("galgame_trait_detail.json")))
        val hair = api.trait("1")
        assertEquals(77, hair.subtraits.size)
        assertEquals(142495, hair.characterCount)
        assertEquals("毛发", hair.traitGroup.localized["zh-Hans"]?.value)
    }

    @Test
    fun `Staff 详情的名义与参与作品`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_credit_name.json")))
        val staff = api.creditName("95366")
        assertEquals(4, staff.siblings.size)
        assertEquals(3, staff.links.size)

        server.enqueue(ok(Fixtures.text("galgame_credit_credits.json")))
        val credits = api.creditNameCredits("95366", limit = 2)
        assertEquals(2, credits.items.size)
        assertEquals("illustration", credits.items[0].creditRoles[0].roleKey)
        assertTrue(credits.nextCursor != null)
    }

    @Test
    fun `收藏夹详情作品列表修改与删除`() = runBlocking {
        val collectionJson = """{"object":"collection","id":"12","title":"我的收藏夹",
            "description":"笔记","visibility":"private","is_default":true,"item_count":3,
            "owner":{"object":"user","id":"42","name":"甲","avatar":null,"avatar_frame":null},
            "preview_covers":[],"created_at":"2026-10-01T00:00:00Z",
            "updated_at":"2026-10-01T00:00:00Z",
            "viewer":{"is_owner":true,"can_edit":true,"can_delete":true,"has_work":false}}"""
        server.enqueue(ok(collectionJson))
        val detail = collections.collection("12", includeNsfw = true)
        assertEquals("我的收藏夹", detail.title)
        assertTrue(detail.isDefault)
        assertEquals("true", server.takeRequest().url.queryParameter("include_nsfw"))

        server.enqueue(ok(Fixtures.text("galgame_works_list.json")))
        val works = collections.works("12", page = 2, sort = "released_asc")
        assertEquals(2, works.items.size)
        val worksRequest = server.takeRequest()
        assertEquals("released_asc", worksRequest.url.queryParameter("sort"))
        assertEquals("2", worksRequest.url.queryParameter("page"))

        server.enqueue(ok(collectionJson.replace("\"title\":\"我的收藏夹\"", "\"title\":\"改名\"")))
        val updated = collections.update(
            "12",
            CollectionPatchBody(title = "改名", visibility = "public"),
        )
        assertEquals("改名", updated.title)
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        val patchBody = patch.body!!.utf8()
        assertTrue(patchBody.contains("\"title\":\"改名\""))
        assertTrue(patchBody.contains("\"visibility\":\"public\""))
        assertFalse(patchBody.contains("description"))

        server.enqueue(MockResponse.Builder().code(204).build())
        collections.delete("12")
        assertEquals("DELETE", server.takeRequest().method)

        server.enqueue(ok("""{"object":"list","items":[$collectionJson],"total":1,"total_relation":"eq"}"""))
        val mine = collections.myCollections(workId = "4219")
        assertEquals(1, mine.items.size)
        assertEquals("4219", server.takeRequest().url.queryParameter("work_id"))
    }
}
