package com.kungal.core

import com.kungal.core.api.TodoApi
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

class TodoApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TodoApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = TodoApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `待办列表带总数与状态筛选`() = runBlocking {
        server.enqueue(ok(Fixtures.text("todos.json")))
        val page = api.list(
            cursor = "cur_todos",
            limit = 20,
            includeTotal = true,
            state = "pending",
        )
        assertEquals(20, page.items.size)
        assertEquals(288, page.total)
        assertTrue(page.nextCursor?.startsWith("cur_") == true)

        val first = page.items.first()
        assertEquals("291", first.id)
        assertEquals("pending", first.state)
        assertEquals("forum", first.project)
        assertEquals("鲲", first.author.name)
        assertNull(first.claimer)
        assertNull(first.completedAt)
        assertNull(first.viewer)

        val done = page.items.first { it.state == "done" }
        assertEquals("286", done.id)
        assertEquals("鲲", done.claimer?.name)
        assertEquals("2026-10-12T07:35:59Z", done.completedAt)

        val request = server.takeRequest()
        assertEquals("/api/v1/todos", request.url.encodedPath)
        assertEquals("cur_todos", request.url.queryParameter("cursor"))
        assertEquals("20", request.url.queryParameter("limit"))
        assertEquals("true", request.url.queryParameter("include_total"))
        assertEquals("pending", request.url.queryParameter("state"))
    }

    @Test
    fun `全部状态不发送 state`() = runBlocking {
        server.enqueue(ok(Fixtures.text("todos.json")))
        api.list()
        val request = server.takeRequest()
        assertNull(request.url.queryParameter("state"))
        assertEquals("false", request.url.queryParameter("include_total"))
        assertNotNull(request.url.queryParameter("limit"))
    }
}
