package com.kungal.core

import com.kungal.core.api.ActivityApi
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

    @Test
    fun `动态列表按游标与筛选请求`() = runBlocking {
        server.enqueue(ok(Fixtures.text("activities.json")))
        val page = api.activities(
            cursor = "cur_abc",
            limit = 50,
            activityTypes = listOf("topic_creation", "topic_reply_creation"),
            sort = "bumped_desc",
            includeNsfw = true,
        )
        assertEquals(20, page.items.size)
        assertTrue(page.nextCursor?.startsWith("cur_") == true)

        val first = page.items.first()
        assertEquals("23389420", first.id)
        assertEquals("topic_reply_creation", first.activityType)
        assertEquals("/topic/4491", first.path)
        assertEquals("YEEEEE", first.performer?.name)
        assertNotNull(first.reply)
        assertEquals("4491", first.reply?.topicId)
        assertEquals(2, first.reply?.floor)
        assertTrue(first.reply?.content?.children?.isNotEmpty() == true)

        val request = server.takeRequest()
        assertEquals("/api/v1/activities", request.url.encodedPath)
        assertEquals("cur_abc", request.url.queryParameter("cursor"))
        assertEquals("50", request.url.queryParameter("limit"))
        assertEquals(
            listOf("topic_creation", "topic_reply_creation"),
            request.url.queryParameterValues("activity_types"),
        )
        assertEquals("bumped_desc", request.url.queryParameter("sort"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
    }

    @Test
    fun `各类动态详情块解析`() = runBlocking {
        server.enqueue(ok(Fixtures.text("activities.json")))
        val items = api.activities().items

        val comment = items.first { it.activityType == "topic_comment_creation" }
        assertEquals("3486", comment.comment?.commentId)
        assertEquals("4490", comment.comment?.topicId)
        assertEquals("我本是剧情党，又不是拔作党", comment.comment?.topicTitle)
        assertEquals(1, comment.comment?.quoted?.floor)

        val rating = items.first { it.activityType == "galgame_rating_creation" }
        assertEquals("1000462", rating.galgameRating?.ratingId)
        assertEquals("31", rating.work?.id)
        assertEquals("想要传达给你的爱恋", rating.work?.localized?.get("zh-Hans")?.value)
        assertEquals(6, rating.galgameRating?.overall)
        assertEquals("portion", rating.galgameRating?.spoilerLevel)
        assertEquals("doing", rating.galgameRating?.playStatus)

        val quiz = items.first { it.activityType == "galgame_quiz_creation" }
        assertEquals("300", quiz.quiz?.quizId)
        assertEquals(3, quiz.quiz?.difficulty)
        assertTrue(quiz.excerptMarkdown.startsWith("《水葬银货》"))

        val resourceItem = items.first { it.resource?.resourceId == "72045" }
        assertEquals("game", resourceItem.resource?.resourceType)
        assertEquals(listOf("win"), resourceItem.resource?.resourcePlatforms)
        assertEquals(listOf("zh-cn", "ja-jp"), resourceItem.resource?.resourceLanguages)
        assertEquals("800 MB", resourceItem.resource?.size)
        assertEquals("陽射しの中のリアル~complement", resourceItem.work?.displayName)

        val creation = items.first { it.activityType == "galgame_creation" }
        assertNull(creation.performer)
        assertEquals("237600", creation.work?.id)
        assertEquals(listOf("mu soft"), creation.workDigest?.developerNames)
        assertEquals("2006-06-23", creation.workDigest?.release)
        assertNotNull(creation.workStats)

        val edit = items.first { it.activityType == "galgame_edit" }
        assertEquals("4002", edit.work?.id)
        assertEquals(2, edit.workRevision?.revisionNumber)

        val topic = items.first { it.activityType == "topic_creation" }
        assertEquals("4491", topic.topic?.id)
        assertEquals("大学怎么比高中还难受", topic.topic?.title)
        assertNotNull(topic.topicDigest?.excerptMarkdown)
    }
}
