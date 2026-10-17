package com.kungal.core

import com.kungal.core.api.ToolsetApi
import com.kungal.core.api.ToolsetListQuery
import com.kungal.core.model.ToolsetUploadCompletePart
import com.kungal.core.model.ToolsetUploadCreate
import com.kungal.core.session.Session
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.ByteArrayInputStream

class ToolsetApiTest {

    private lateinit var server: MockWebServer
    private lateinit var client: KungalClient
    private lateinit var api: ToolsetApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = KungalClient(baseUrl = baseUrl)
        client.session.restore(Session(cookie = "kungal_session=test-cookie"))
        api = ToolsetApi(client)
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
    fun `工具集列表解析分页与摘要`() = runBlocking {
        server.enqueue(ok(fixture("toolsets_list.json")))
        val page = api.list(ToolsetListQuery(page = 1, limit = 24))
        assertEquals(124, page.total)
        assertEquals(3, page.items.size)
        val first = page.items.first()
        assertEquals("137", first.id)
        assertEquals("extractor", first.toolsetType)
        assertEquals("linux", first.platform)
        assertEquals("alpha", first.releaseChannel)
        assertEquals(1, first.commentCount)
        assertEquals("D-kaiser", first.author.name)
        assertEquals("/api/v1/toolsets", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `工具集列表拼接筛选排序与搜索参数`() = runBlocking {
        server.enqueue(ok("""{"object":"list","items":[],"total":0,"total_relation":"eq"}"""))
        api.list(
            ToolsetListQuery(
                page = 3,
                limit = 24,
                sort = "view_desc",
                toolsetType = "emulator",
                interfaceLanguage = "zh-cn",
                platform = "windows",
                releaseChannel = "stable",
                q = "解包",
            ),
        )
        val url = server.takeRequest().url
        assertEquals("3", url.queryParameter("page"))
        assertEquals("view_desc", url.queryParameter("sort"))
        assertEquals("emulator", url.queryParameter("toolset_type"))
        assertEquals("zh-cn", url.queryParameter("interface_language"))
        assertEquals("windows", url.queryParameter("platform"))
        assertEquals("stable", url.queryParameter("release_channel"))
        assertEquals("解包", url.queryParameter("q"))
    }

    @Test
    fun `工具集详情解析评分分布与资源`() = runBlocking {
        server.enqueue(ok(fixture("toolset_detail_with_resources.json")))
        val toolset = api.toolset("134")
        assertEquals("134", toolset.id)
        assertEquals(2, toolset.toolsetResources.size)
        val first = toolset.toolsetResources.first()
        assertEquals("file", first.type)
        assertEquals(76560285L, first.archive?.fileSize)
        assertNull(first.link)
        assertEquals("FinSAMA", first.poster.name)
        assertEquals(5, toolset.practicalityDistribution.size)
        assertEquals("FinSAMA", toolset.contributors.first().name)
        assertEquals("/api/v1/toolsets/134", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `创建工具集带幂等键与主页别名`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(201)
                .addHeader("Content-Type", "application/json")
                .body(
                    """{"object":"toolset","id":"200","title":"新工具","toolset_type":"script",
                        "interface_language":"zh-cn","platform":"windows","release_channel":"stable",
                        "practicality_distribution":[0,0,0,0,0],"aliases":[],"homepage_urls":[]}""",
                )
                .build(),
        )
        val body = buildJsonObject {
            put("title", JsonPrimitive("新工具"))
            put("toolset_type", JsonPrimitive("script"))
            put("interface_language", JsonPrimitive("zh-cn"))
            put("platform", JsonPrimitive("windows"))
            put("release_channel", JsonPrimitive("stable"))
            put("content_markdown", JsonPrimitive("介绍"))
        }
        val created = api.create(body, idempotencyKey = "ts-key")
        assertEquals("200", created.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/toolsets", request.url.encodedPath)
        assertEquals("ts-key", request.headers["Idempotency-Key"])
        assertTrue(request.body!!.utf8().contains("\"toolset_type\":\"script\""))
    }

    @Test
    fun `收藏与实用性评分`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"toolset_engagement","toolset_id":"134","favorite_count":3,
                    "viewer":{"has_favorited":true}}""",
            ),
        )
        server.enqueue(
            ok(
                """{"object":"toolset_practicality","toolset_id":"134",
                    "practicality_average":4.5,"practicality_count":2,
                    "practicality_distribution":[0,0,0,1,1],
                    "viewer":{"practicality_rating":5}}""",
            ),
        )
        val fav = api.setFavorite("134", true)
        assertEquals(3, fav.favoriteCount)
        assertEquals(true, fav.viewer?.hasFavorited)
        val rating = api.ratePracticality("134", 5)
        assertEquals(4.5, rating.practicalityAverage!!, 0.001)
        assertEquals(5, rating.viewer?.practicalityRating)
        assertEquals("PUT", server.takeRequest().method)
        val rateRequest = server.takeRequest()
        assertEquals("PUT", rateRequest.method)
        assertEquals("/api/v1/toolsets/134/practicality", rateRequest.url.encodedPath)
        assertEquals("""{"rating":5}""", rateRequest.body!!.utf8())
    }

    @Test
    fun `新增与编辑工具资源`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(201)
                .addHeader("Content-Type", "application/json")
                .body(
                    """{"object":"toolset_resource","id":"190","toolset_resource_type":"link",
                        "link":{"size_label":"12 MB"},"note":null,"download_count":0}""",
                )
                .build(),
        )
        server.enqueue(
            ok(
                """{"object":"toolset_resource","id":"190","toolset_resource_type":"link",
                    "link":{"size_label":"13 MB"},"note":"新备注","download_count":0}""",
            ),
        )
        server.enqueue(MockResponse.Builder().code(204).build())
        val created = api.addResource(
            "134",
            buildJsonObject {
                put("toolset_resource_type", JsonPrimitive("link"))
                put("link_url", JsonPrimitive("https://example.com/t.zip"))
                put("size_label", JsonPrimitive("12 MB"))
            },
            idempotencyKey = "tr-key",
        )
        assertEquals("190", created.id)
        val patched = api.patchResource(
            "134",
            "190",
            buildJsonObject {
                put("size_label", JsonPrimitive("13 MB"))
                put("note", JsonPrimitive("新备注"))
            },
        )
        assertEquals("13 MB", patched.link?.sizeLabel)
        api.deleteResource("134", "190")
        val addRequest = server.takeRequest()
        assertEquals("/api/v1/toolsets/134/resources", addRequest.url.encodedPath)
        assertEquals("tr-key", addRequest.headers["Idempotency-Key"])
        assertEquals("PATCH", server.takeRequest().method)
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `领取工具资源下载解析预签名地址`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"toolset_download",
                    "download_url":"https://s3.example.com/a.zip?sign=1",
                    "expires_at":"2026-10-17T12:00:00Z",
                    "extraction_code":"","archive_password":""}""",
            ),
        )
        val download = api.downloadResource("134", "181")
        assertEquals("https://s3.example.com/a.zip?sign=1", download.downloadUrl)
        assertEquals("2026-10-17T12:00:00Z", download.expiresAt)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/toolsets/134/resources/181/downloads", request.url.encodedPath)
    }

    @Test
    fun `上传会话初始化与分片完成`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(201)
                .addHeader("Content-Type", "application/json")
                .body(
                    """{"object":"toolset_upload","id":"u-1","toolset_id":"134",
                        "filename":"a.zip","file_size":104857600,"state":"pending",
                        "is_multipart":true,"upload_url":null,"part_size":5242880,
                        "part_urls":[{"part_number":1,"url":"https://s3/p1"}],
                        "uploaded_parts":[],"expires_at":"2026-10-17T12:00:00Z",
                        "created_at":"2026-10-17T10:00:00Z","completed_at":null}""",
                )
                .build(),
        )
        server.enqueue(
            ok(
                """{"object":"toolset_upload","id":"u-1","toolset_id":"134",
                    "filename":"a.zip","file_size":104857600,"state":"completed",
                    "is_multipart":true,"upload_url":null,"part_size":5242880,
                    "part_urls":[],"uploaded_parts":[],
                    "created_at":"2026-10-17T10:00:00Z","completed_at":"2026-10-17T10:01:00Z"}""",
            ),
        )
        val session = api.createUpload(
            "134",
            ToolsetUploadCreate("a.zip", 104857600, "application/zip"),
            idempotencyKey = "up-key",
        )
        assertTrue(session.isMultipart)
        assertEquals(1, session.partUrls.size)
        val done = api.completeUpload(
            "134",
            "u-1",
            parts = listOf(ToolsetUploadCompletePart(1, "etag-1")),
        )
        assertEquals("completed", done.state)
        val init = server.takeRequest()
        assertEquals("POST", init.method)
        assertEquals("/api/v1/toolsets/134/uploads", init.url.encodedPath)
        assertEquals("up-key", init.headers["Idempotency-Key"])
        val finish = server.takeRequest()
        assertEquals("PATCH", finish.method)
        assertTrue(finish.body!!.utf8().contains("\"etag\":\"etag-1\""))
    }

    @Test
    fun `上传续传解析已传分片与缺失分片并可中止`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"toolset_upload","id":"u-1","toolset_id":"134",
                    "filename":"a.zip","file_size":104857600,"state":"pending",
                    "is_multipart":true,"upload_url":null,"part_size":5242880,
                    "part_urls":[{"part_number":2,"url":"https://s3/p2"},
                                 {"part_number":3,"url":"https://s3/p3"}],
                    "uploaded_parts":[{"part_number":1,"etag":"etag-1","byte_count":5242880}],
                    "created_at":"2026-10-17T10:00:00Z","completed_at":null}""",
            ),
        )
        server.enqueue(MockResponse.Builder().code(204).build())
        val session = api.upload("134", "u-1")
        assertEquals("pending", session.state)
        assertEquals(1, session.uploadedParts.size)
        assertEquals(5242880L, session.uploadedParts.first().byteCount)
        assertEquals(2, session.partUrls.size)
        api.abortUpload("134", "u-1")
        assertEquals("/api/v1/toolsets/134/uploads/u-1", server.takeRequest().url.encodedPath)
        val abort = server.takeRequest()
        assertEquals("DELETE", abort.method)
        assertEquals("/api/v1/toolsets/134/uploads/u-1", abort.url.encodedPath)
    }

    @Test
    fun `预签名地址流式上传不带会话且回传ETag`() = runBlocking {
        server.enqueue(
            MockResponse.Builder()
                .code(200)
                .addHeader("ETag", "\"etag-9\"")
                .build(),
        )
        val progress = mutableListOf<Long>()
        val etag = client.putExternalStream(
            url = server.url("/s3/part-1").toString(),
            contentLength = 6,
            contentType = "application/zip",
            openStream = { ByteArrayInputStream("abcdef".toByteArray()) },
            onProgress = { progress.add(it) },
        )
        assertEquals("\"etag-9\"", etag)
        assertEquals(listOf(6L), progress)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("abcdef", request.body!!.utf8())
        assertEquals("application/zip", request.headers["Content-Type"])
        assertNull(request.headers["Cookie"])
        // OkHttp 自带 okhttp/x.y 的 UA，这里只确认没有冒充 App 标识
        assertTrue(request.headers["User-Agent"] != KungalClient.USER_AGENT)
    }

    @Test
    fun `沙盒分片上传：两片PUT后带ETag完成`() = runBlocking {
        val part1 = server.url("/s3/p1").toString()
        val part2 = server.url("/s3/p2").toString()
        server.enqueue(
            MockResponse.Builder()
                .code(201)
                .addHeader("Content-Type", "application/json")
                .body(
                    """{"object":"toolset_upload","id":"u-2","toolset_id":"134",
                        "filename":"big.7z","file_size":10,"state":"pending",
                        "is_multipart":true,"part_size":5,
                        "part_urls":[{"part_number":1,"url":"$part1"},
                                     {"part_number":2,"url":"$part2"}],
                        "uploaded_parts":[],"created_at":"2026-10-17T10:00:00Z"}""",
                )
                .build(),
        )
        server.enqueue(MockResponse.Builder().code(200).addHeader("ETag", "e1").build())
        server.enqueue(MockResponse.Builder().code(200).addHeader("ETag", "e2").build())
        server.enqueue(
            ok(
                """{"object":"toolset_upload","id":"u-2","toolset_id":"134",
                    "filename":"big.7z","file_size":10,"state":"completed",
                    "is_multipart":true,"part_urls":[],"uploaded_parts":[],
                    "created_at":"2026-10-17T10:00:00Z","completed_at":"2026-10-17T10:01:00Z"}""",
            ),
        )
        val session = api.createUpload(
            "134",
            ToolsetUploadCreate("big.7z", 10, "application/x-7z-compressed"),
        )
        assertEquals(2, session.partUrls.size)
        val bytes = ByteArray(10) { it.toByte() }
        val parts = session.partUrls.sortedBy { it.partNumber }.map { part ->
            val offset = (part.partNumber - 1) * 5
            val etag = client.putExternalStream(
                url = part.url,
                contentLength = 5,
                contentType = "application/x-7z-compressed",
                openStream = { ByteArrayInputStream(bytes, offset, 5) },
            )
            ToolsetUploadCompletePart(part.partNumber, etag!!)
        }
        val done = api.completeUpload("134", "u-2", parts)
        assertEquals("completed", done.state)
        assertEquals("POST", server.takeRequest().method)
        assertEquals("PUT", server.takeRequest().method)
        assertEquals("PUT", server.takeRequest().method)
        val finish = server.takeRequest()
        assertEquals("PATCH", finish.method)
        val body = finish.body!!.utf8()
        assertTrue(body.contains("\"part_number\":1"))
        assertTrue(body.contains("\"part_number\":2"))
        assertTrue(body.contains("\"etag\":\"e1\""))
        assertTrue(body.contains("\"etag\":\"e2\""))
    }
}
