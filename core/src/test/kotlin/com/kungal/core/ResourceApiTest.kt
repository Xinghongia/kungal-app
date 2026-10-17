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

    @Test
    fun `领取下载链接`() = runBlocking {
        server.enqueue(
            ok(
                """
                {"object":"galgame_resource_download",
                 "download_urls":["https://pan.baidu.com/s/abc","magnet:?xt=urn:btih:def"],
                 "extraction_code":"8ejh","archive_password":"aPassword"}
                """.trimIndent(),
            ),
        )
        val download = api.download("71791")
        assertEquals(2, download.downloadUrls.size)
        assertEquals("https://pan.baidu.com/s/abc", download.downloadUrls.first())
        assertEquals("8ejh", download.extractionCode)
        assertEquals("aPassword", download.archivePassword)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/galgame-resources/71791/downloads", request.url.encodedPath)
    }

    @Test
    fun `点赞与取消点赞`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"galgame_resource_engagement","resource_id":"71791",
                    "like_count":5,"viewer":{"has_liked":true}}""",
            ),
        )
        server.enqueue(
            ok(
                """{"object":"galgame_resource_engagement","resource_id":"71791",
                    "like_count":4,"viewer":{"has_liked":false}}""",
            ),
        )
        val liked = api.setLike("71791", true)
        assertEquals(5, liked.likeCount)
        assertEquals(true, liked.viewer?.hasLiked)
        val unliked = api.setLike("71791", false)
        assertEquals(4, unliked.likeCount)
        assertEquals(false, unliked.viewer?.hasLiked)
        val first = server.takeRequest()
        assertEquals("PUT", first.method)
        assertEquals("/api/v1/galgame-resources/71791/like", first.url.encodedPath)
        val second = server.takeRequest()
        assertEquals("DELETE", second.method)
    }

    @Test
    fun `报告失效带幂等键并解析核验结果`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"galgame_resource_expiry_report","resource_id":"71791",
                    "verdict":"dead","state":"expired"}""",
            ),
        )
        val report = api.reportExpiry("71791", idempotencyKey = "key-1")
        assertEquals("dead", report.verdict)
        assertEquals("expired", report.state)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/galgame-resources/71791/expiry-reports", request.url.encodedPath)
        assertEquals("key-1", request.headers["Idempotency-Key"])
    }

    @Test
    fun `编辑源数据解析大小与链接`() = runBlocking {
        server.enqueue(
            ok(
                """
                {"object":"galgame_resource_source","resource_id":"71791",
                 "resource_type":"patch","title":"全年龄补丁","version_label":"stable",
                 "resource_languages":["zh-cn"],"resource_platforms":["win"],
                 "resource_runtimes":["native-win"],"size":"12 MB",
                 "download_urls":["https://example.com/p.zip"],"extraction_code":"",
                 "archive_password":"","content_markdown":"说明"}
                """.trimIndent(),
            ),
        )
        val source = api.source("71791")
        assertEquals("patch", source.resourceType)
        assertEquals("12 MB", source.size)
        assertEquals(listOf("https://example.com/p.zip"), source.downloadUrls)
        assertEquals("说明", source.contentMarkdown)
        assertEquals("/api/v1/galgame-resources/71791/source", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `发布资源带幂等键并保留响应头`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(201)
                .addHeader("Content-Type", "application/json")
                .addHeader("Location", "/api/v1/galgame-resources/90001")
                .addHeader("ETag", "\"v1\"")
                .body(
                    """{"object":"galgame_resource","id":"90001","resource_type":"game",
                        "resource_languages":["zh-cn"],"resource_platforms":["win"],
                        "resource_runtimes":["native-win"],"size":"1 GB","state":"valid"}""",
                )
                .build(),
        )
        val body = kotlinx.serialization.json.buildJsonObject {
            put("resource_type", kotlinx.serialization.json.JsonPrimitive("game"))
            put("size", kotlinx.serialization.json.JsonPrimitive("1 GB"))
        }
        val created = api.create("1947", body, idempotencyKey = "key-2")
        assertEquals("90001", created.body.id)
        assertEquals("/api/v1/galgame-resources/90001", created.location)
        assertEquals("\"v1\"", created.etag)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/works/1947/resources", request.url.encodedPath)
        assertEquals("key-2", request.headers["Idempotency-Key"])
        assertTrue(request.body!!.utf8().contains("\"resource_type\":\"game\""))
    }

    @Test
    fun `编辑与删除资源`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"galgame_resource","id":"71791","state":"valid",
                    "resource_languages":["zh-cn"],"resource_platforms":["win"],
                    "resource_runtimes":["native-win"],"size":"2 GB"}""",
            ),
        )
        server.enqueue(MockResponse.Builder().code(204).build())
        val body = kotlinx.serialization.json.buildJsonObject {
            put("size", kotlinx.serialization.json.JsonPrimitive("2 GB"))
            put("version_label", kotlinx.serialization.json.JsonNull)
        }
        val patched = api.patch("71791", body)
        assertEquals("2 GB", patched.size)
        api.delete("71791")
        val patchRequest = server.takeRequest()
        assertEquals("PATCH", patchRequest.method)
        assertEquals("/api/v1/galgame-resources/71791", patchRequest.url.encodedPath)
        assertTrue(patchRequest.body!!.utf8().contains("\"version_label\":null"))
        val deleteRequest = server.takeRequest()
        assertEquals("DELETE", deleteRequest.method)
        assertEquals("/api/v1/galgame-resources/71791", deleteRequest.url.encodedPath)
    }

    @Test
    fun `补丁资源解析 moyu 字段`() = runBlocking {
        server.enqueue(
            ok(
                """
                {"object":"list","items":[{"object":"moyu_patch","id":"77",
                  "web_url":"https://www.moyu.moe/patch/77",
                  "resources":[{"object":"moyu_patch_resource","id":"88",
                    "name":"汉化补丁","storage":"s3","size":"0.571 MB",
                    "model_name":null,"note_markdown":null,
                    "types":["manual"],"languages":["zh-Hans"],
                    "platforms":["windows"],"download_count":3,
                    "web_url":"https://www.moyu.moe/patch/77/88",
                    "updated_at":"2026-10-01T00:00:00Z",
                    "publisher":{"object":"user","id":"1","name":"某人"}}]}]}
                """.trimIndent(),
            ),
        )
        val patches = api.moyuPatches("1947", cursor = "cur_abc")
        assertEquals(1, patches.items.size)
        assertEquals("77", patches.items.first().id)
        val resource = patches.items.first().resources.first()
        assertEquals("汉化补丁", resource.name)
        assertEquals("s3", resource.storage)
        assertEquals(3, resource.downloadCount)
        assertEquals("某人", resource.publisher.name)
        val url = server.takeRequest().url
        assertEquals("/api/v1/works/1947/moyu-patches", url.encodedPath)
        assertEquals("cur_abc", url.queryParameter("cursor"))
    }
}
