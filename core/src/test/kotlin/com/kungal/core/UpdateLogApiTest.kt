package com.kungal.core

import com.kungal.core.api.UpdateLogApi
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class UpdateLogApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: UpdateLogApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = UpdateLogApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `更新日志按游标分页并解析`() = runBlocking {
        server.enqueue(ok(Fixtures.text("update_logs.json")))
        val page = api.list(cursor = "cur_logs", limit = 20)
        assertEquals(20, page.items.size)
        assertTrue(page.nextCursor?.startsWith("cur_") == true)

        val first = page.items.first()
        assertEquals("1234", first.id)
        assertEquals("chore", first.changeType)
        assertEquals("5.4.9", first.releaseVersion)
        assertEquals("将项目从 kun-galgame-nuxt4 重命名为 kun-galgame-forum", first.text)
        assertEquals("2026-06-04T12:20:50Z", first.createdAt)
        assertEquals("2026-06-04T12:20:50Z", first.updatedAt)
        assertNull(first.viewer)

        val request = server.takeRequest()
        assertEquals("/api/v1/update-logs", request.url.encodedPath)
        assertEquals("cur_logs", request.url.queryParameter("cursor"))
        assertEquals("20", request.url.queryParameter("limit"))
    }

    @Test
    fun `没有游标时不发送 cursor`() = runBlocking {
        server.enqueue(ok(Fixtures.text("update_logs.json")))
        api.list(limit = 50)
        val request = server.takeRequest()
        assertNull(request.url.queryParameter("cursor"))
        assertEquals("50", request.url.queryParameter("limit"))
    }
}
