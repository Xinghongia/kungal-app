package com.kungal.core

import com.kungal.core.api.DocApi
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DocApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: DocApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = DocApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `文档列表按排序与分类请求`() = runBlocking {
        server.enqueue(ok(Fixtures.text("docs.json")))
        val page = api.list(sort = "position_asc", docCategory = "notice", limit = 100)
        assertEquals(29, page.items.size)
        assertNull(page.nextCursor)

        val first = page.items.first()
        assertEquals("14", first.id)
        assertEquals("article-copyright", first.slug)
        assertEquals("鲲 Galgame 网站集群版权政策", first.title)
        assertEquals("notice", first.docCategory)
        assertEquals(13191, first.viewCount)
        assertEquals("2025-10-06T00:00:00Z", first.publishedAt)
        assertNotNull(first.banner)
        assertNull(first.content)

        val request = server.takeRequest()
        assertEquals("/api/v1/docs", request.url.encodedPath)
        assertEquals("position_asc", request.url.queryParameter("sort"))
        assertEquals("notice", request.url.queryParameter("doc_category"))
        assertEquals("100", request.url.queryParameter("limit"))
    }

    @Test
    fun `文档详情解析正文与作者`() = runBlocking {
        server.enqueue(ok(Fixtures.text("doc_detail.json")))
        val doc = api.detail("article-copyright")
        assertEquals("14", doc.id)
        assertEquals("article-copyright", doc.slug)
        assertEquals("鲲", doc.author.name)
        assertEquals(13192, doc.viewCount)
        assertNotNull(doc.content)
        assertEquals(10, doc.content?.children?.size)
        assertTrue(doc.content?.children?.isNotEmpty() == true)

        assertEquals("/api/v1/docs/article-copyright", server.takeRequest().url.encodedPath)
    }
}
