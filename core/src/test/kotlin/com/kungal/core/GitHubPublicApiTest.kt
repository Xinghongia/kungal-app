package com.kungal.core

import com.kungal.core.api.GitHubPublicApi
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class GitHubPublicApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: GitHubPublicApi

    private val baseUrl: String get() = server.url("/").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = GitHubPublicApi(webBase = baseUrl)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun html(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "text/html").body(body).build()

    @Test
    fun `贡献页解析总数并按周排布`() = runBlocking {
        server.enqueue(html(Fixtures.text("github_contributions.html")))
        val calendar = api.contributions("Xinghongia")

        assertEquals("/users/Xinghongia/contributions", server.takeRequest().url.encodedPath)
        assertEquals(262, calendar.total)
        assertEquals(21, calendar.days.size)
        assertEquals(3, calendar.weeks.size)
        // 每周日在前：第一周周日是 10-12，周一 10-13。
        assertEquals("2025-10-12", calendar.weeks[0][0]?.date)
        assertEquals(1, calendar.weeks[0][1]?.level)
        assertEquals("2025-10-19", calendar.weeks[1][0]?.date)
        assertEquals(4, calendar.weeks[1][1]?.level)
        assertEquals("2025-11-01", calendar.weeks[2][6]?.date)
    }

    @Test
    fun `仓库页解析 star 数`() = runBlocking {
        server.enqueue(html(Fixtures.text("github_repo.html")))
        assertEquals(248, api.repoStars("Xinghongia", "Kirameku"))
        assertEquals("/Xinghongia/Kirameku", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `仓库不可见时返回空`() = runBlocking {
        server.enqueue(MockResponse.Builder().code(404).body("Not Found").build())
        assertNull(api.repoStars("Xinghongia", "kungal-app"))
    }
}
