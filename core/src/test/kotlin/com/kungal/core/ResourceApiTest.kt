package com.kungal.core

import com.kungal.core.api.ResourceApi
import com.kungal.core.api.ResourceListQuery
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ResourceApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ResourceApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ResourceApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    private fun fixture(name: String): String =
        checkNotNull(javaClass.classLoader?.getResourceAsStream("fixtures/$name")) {
            "缺少夹具 $name"
        }.bufferedReader().use { it.readText() }

    @Test
    fun `资源列表解析分页与作品引用`() = runBlocking {
        server.enqueue(ok(fixture("galgame_resource_list.json")))
        val page = api.list(ResourceListQuery(page = 1, limit = 50, includeNsfw = false))
        assertEquals(3, page.items.size)
        assertEquals(10000, page.total)
        assertEquals("gte", page.totalRelation)
        val first = page.items.first()
        assertEquals("71791", first.id)
        assertEquals("1947", first.work?.id)
        assertEquals("死月妖花～四月八日～", first.work?.displayName)
        assertEquals("百度网盘", first.providerNames.first())
        assertEquals("919 MB", first.size)
        assertEquals("game", first.resourceType)
        assertEquals(listOf("win"), first.resourcePlatforms)
        assertEquals(listOf("native-win"), first.resourceRuntimes)
        assertEquals(1, first.downloadCount)
        assertFalse(first.isExpired)
        assertTrue((first.content?.children?.size ?: 0) > 0)
        assertEquals("/api/v1/galgame-resources", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `资源列表拼接搜索与筛选参数`() = runBlocking {
        server.enqueue(ok("""{"object":"list","items":[],"total":0,"total_relation":"eq"}"""))
        api.list(
            ResourceListQuery(
                page = 2,
                limit = 50,
                q = "四月",
                includeNsfw = true,
                state = "expired",
                sort = "created_asc",
            ),
        )
        val url = server.takeRequest().url
        assertEquals("/api/v1/galgame-resources", url.encodedPath)
        assertEquals("2", url.queryParameter("page"))
        assertEquals("50", url.queryParameter("limit"))
        assertEquals("四月", url.queryParameter("q"))
        assertEquals("true", url.queryParameter("include_nsfw"))
        assertEquals("expired", url.queryParameter("state"))
        assertEquals("created_asc", url.queryParameter("sort"))
    }

    @Test
    fun `词条资源列表解析失效与总数`() = runBlocking {
        server.enqueue(ok(fixture("galgame_work_resources.json")))
        val page = api.workResources("1947", page = 2, limit = 100, state = "valid")
        assertEquals(6, page.total)
        assertEquals("eq", page.totalRelation)
        assertEquals(5, page.items.size)
        assertTrue(page.items.all { it.work?.id == "1947" })
        val url = server.takeRequest().url
        assertEquals("/api/v1/works/1947/resources", url.encodedPath)
        assertEquals("2", url.queryParameter("page"))
        assertEquals("100", url.queryParameter("limit"))
        assertEquals("valid", url.queryParameter("state"))
    }

    @Test
    fun `资源详情解析作者与备注文档`() = runBlocking {
        server.enqueue(ok(fixture("galgame_resource_detail.json")))
        val resource = api.resource("71791")
        assertEquals("71791", resource.id)
        assertEquals("SakuGAL", resource.author.name)
        assertEquals("1947", resource.work?.id)
        assertNotNull(resource.work?.cover?.url)
        assertTrue(resource.viewCount >= 5)
        assertTrue(resource.content != null)
        assertEquals("/api/v1/galgame-resources/71791", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `资源迁移查询解析 LetMoe 地址`() = runBlocking {
        server.enqueue(
            ok(
                """
                {"object":"galgame_resource_relocation","resource_id":"123","work_id":"456",
                 "url":"https://www.letmoe.com/game/456"}
                """.trimIndent(),
            ),
        )
        val moved = api.relocation("123")
        assertEquals("123", moved.resourceId)
        assertEquals("456", moved.workId)
        assertEquals("https://www.letmoe.com/game/456", moved.url)
        assertEquals("/api/v1/galgame-resource-relocations/123", server.takeRequest().url.encodedPath)
    }
}
