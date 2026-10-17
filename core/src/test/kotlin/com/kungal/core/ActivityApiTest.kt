package com.kungal.core

import com.kungal.core.api.ActivityApi
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class ActivityApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: ActivityApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        api = ActivityApi(KungalClient(baseUrl = baseUrl))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `话题动态解析收藏数与话题摘要`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[
                     {"object":"activity","id":"a1","activity_type":"topic_creation",
                      "path":"/topic/3938","occurred_at":"2026-10-17T12:00:00Z",
                      "performer":{"object":"user","id":"1","name":"鲲","avatar":null,
                        "avatar_frame":null},
                      "topic":{"object":"topic","id":"3938","title":"开源话题",
                        "state":"published","category":"galgame","sections":["g-news"],
                        "cover_images":[],"author":{"object":"user","id":"1","name":"鲲",
                          "avatar":null,"avatar_frame":null},
                        "view_count":8808,"like_count":111,"reply_count":42,
                        "comment_count":14,"has_best_answer":false,"mini_apps":[],
                        "is_nsfw":false,"bumped_at":"2026-10-17T12:00:00Z",
                        "created_at":"2024-01-01T00:00:00Z","edited_at":null},
                      "topic_digest":{"favorite_count":134,"excerpt_markdown":"摘要",
                        "edited_at":null}}],
                   "next_cursor":"cur_next"}""",
            ),
        )
        val page = api.activities(
            limit = 100,
            activityTypes = listOf("topic_creation"),
            sort = "bumped_desc",
            topicSections = "all",
            includeNsfw = true,
        )
        assertEquals(1, page.items.size)
        val activity = page.items[0]
        assertEquals("topic_creation", activity.activityType)
        assertEquals("/topic/3938", activity.path)
        assertEquals("3938", activity.topic?.id)
        assertEquals(111, activity.topic?.likeCount)
        assertEquals(134, activity.topicDigest?.favoriteCount)
        assertEquals("cur_next", page.nextCursor)

        val request = server.takeRequest()
        assertEquals("/api/v1/activities", request.url.encodedPath)
        assertEquals("100", request.url.queryParameter("limit"))
        assertEquals(listOf("topic_creation"), request.url.queryParameterValues("activity_types"))
        assertEquals("bumped_desc", request.url.queryParameter("sort"))
        assertEquals("all", request.url.queryParameter("topic_sections"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
        assertNull(request.url.queryParameter("cursor"))
    }
}
