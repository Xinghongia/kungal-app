package com.kungal.core

import com.kungal.core.api.WebsiteApi
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

class WebsiteApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: WebsiteApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = WebsiteApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `目录列表按游标与筛选请求`() = runBlocking {
        server.enqueue(ok(Fixtures.text("website_list.json")))
        val page = api.list(
            cursor = "cur_abc",
            limit = 100,
            includeTotal = true,
            includeNsfw = false,
            categoryId = "3",
            tagId = "66",
        )
        assertEquals(69, page.items.size)
        assertEquals(69, page.total)
        assertNull(page.nextCursor)
        val first = page.items.first()
        assertEquals("96", first.id)
        assertEquals("www.yukie.moe", first.host)
        assertEquals("normal", first.state)
        assertEquals(90, first.score)
        assertFalse(first.isNsfw)
        assertEquals("resource", first.websiteCategory.slug)
        assertTrue(first.icon?.url?.startsWith("https://image.kungal.iloveren.link/") == true)
        assertEquals(4, page.items.count { it.state == "closed" })

        val request = server.takeRequest()
        assertEquals("/api/v1/websites", request.url.encodedPath)
        assertEquals("cur_abc", request.url.queryParameter("cursor"))
        assertEquals("100", request.url.queryParameter("limit"))
        assertEquals("true", request.url.queryParameter("include_total"))
        assertEquals("false", request.url.queryParameter("include_nsfw"))
        assertEquals("3", request.url.queryParameter("website_category_id"))
        assertEquals("66", request.url.queryParameter("website_tag_id"))
    }

    @Test
    fun `网站详情解析`() = runBlocking {
        server.enqueue(ok(Fixtures.text("website_detail.json")))
        val site = api.detail("www.letmoe.com")
        assertEquals("95", site.id)
        assertEquals("www.letmoe.com", site.host)
        assertEquals("zh-cn", site.language)
        assertTrue(site.urls.isEmpty())
        assertEquals(19, site.websiteTags.size)
        assertEquals("ad0", site.websiteTags.first().slug)
        assertEquals(20, site.websiteTags.first().level)
        assertEquals("1", site.websiteTags.first().websiteTagGroupId)
        assertEquals(8723, site.viewCount)
        assertEquals(0, site.likeCount)
        assertEquals(0, site.favoriteCount)
        assertEquals(0, site.commentCount)
        assertEquals("2026-09-22T15:14:43Z", site.createdAt)
        assertNull(site.viewer)

        assertEquals("/api/v1/websites/www.letmoe.com", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `分类标签与分组`() = runBlocking {
        server.enqueue(ok(Fixtures.text("website_categories.json")))
        val categories = api.categories()
        assertEquals(4, categories.size)
        assertEquals("resource", categories.first().slug)
        assertEquals(56, categories.first().websiteCount)
        assertEquals(10, categories.first().sortOrder)

        server.enqueue(ok(Fixtures.text("website_category.json")))
        val category = api.category("resource")
        assertEquals("3", category.id)
        assertEquals(56, category.websiteCount)

        server.enqueue(ok(Fixtures.text("website_tags.json")))
        val tags = api.tags()
        assertEquals(73, tags.size)
        assertEquals("performance2", tags.first().slug)
        assertEquals(10, tags.first().level)

        server.enqueue(ok(Fixtures.text("website_tag.json")))
        val tag = api.tag("performance2")
        assertEquals("1", tag.id)
        assertEquals("16", tag.websiteTagGroupId)

        server.enqueue(ok(Fixtures.text("website_tag_groups.json")))
        val groups = api.tagGroups()
        assertEquals(18, groups.size)
        assertEquals("ad", groups.first().slug)
        assertFalse(groups.first().isMultiSelect)

        val paths = (1..5).map { server.takeRequest().url.encodedPath }
        assertEquals(
            listOf(
                "/api/v1/website-categories",
                "/api/v1/website-categories/resource",
                "/api/v1/website-tags",
                "/api/v1/website-tags/performance2",
                "/api/v1/website-tag-groups",
            ),
            paths,
        )
    }

    @Test
    fun `点赞与取消点赞`() = runBlocking {
        val body = """
            {"object":"website_engagement","website_id":"95","like_count":8,
             "favorite_count":2,"viewer":{"has_liked":true,"has_favorited":false}}
        """.trimIndent()
        server.enqueue(ok(body))
        val liked = api.setLike("www.letmoe.com", liked = true)
        assertEquals(8, liked.likeCount)
        assertEquals(2, liked.favoriteCount)
        assertTrue(liked.viewer?.hasLiked == true)
        assertFalse(liked.viewer?.hasFavorited == true)

        server.enqueue(ok(body.replace("\"like_count\":8", "\"like_count\":7").replace("true", "false")))
        api.setLike("www.letmoe.com", liked = false)
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/api/v1/websites/www.letmoe.com/like", put.url.encodedPath)
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/websites/www.letmoe.com/like", delete.url.encodedPath)
    }

    @Test
    fun `收藏与取消收藏`() = runBlocking {
        val body = """
            {"object":"website_engagement","website_id":"95","like_count":7,
             "favorite_count":3,"viewer":{"has_liked":false,"has_favorited":true}}
        """.trimIndent()
        server.enqueue(ok(body))
        val fav = api.setFavorite("www.letmoe.com", favorited = true)
        assertEquals(3, fav.favoriteCount)
        assertTrue(fav.viewer?.hasFavorited == true)

        server.enqueue(ok(body))
        api.setFavorite("www.letmoe.com", favorited = false)

        assertEquals("/api/v1/websites/www.letmoe.com/favorite", server.takeRequest().url.encodedPath)
        assertEquals("DELETE", server.takeRequest().method)
    }
}
