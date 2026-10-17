package com.kungal.core

import com.kungal.core.api.RankingApi
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
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

class RankingApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: RankingApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        api = RankingApi(KungalClient(baseUrl = baseUrl, session = session))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `用户排行榜解析排名指标与签名`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[
                     {"object":"user_ranking_entry","rank":1,"metric_value":47778,
                      "member":{"object":"user","id":"16529","name":"阿瞳",
                        "avatar":{"url":"https://img/a.webp","hash":"h","width":null,
                          "height":null,"thumbhash":null,"sexual":null},
                        "avatar_frame":null},
                      "bio":"签名"},
                     {"object":"user_ranking_entry","rank":2,"metric_value":43330.5,
                      "member":{"object":"user","id":"2","name":"小龙","avatar":null,"avatar_frame":null},
                      "bio":null}]}""",
            ),
        )
        val page = api.users(sort = "moemoepoint_desc", limit = 50)
        assertEquals(2, page.items.size)
        val first = page.items[0]
        assertEquals(1, first.rank)
        assertEquals(47778.0, first.metricValue, 0.001)
        assertEquals("16529", first.member.id)
        assertEquals("阿瞳", first.member.name)
        assertEquals("https://img/a.webp", first.member.avatar?.url)
        assertEquals("签名", first.bio)
        assertNull(page.items[1].bio)
        assertEquals(43330.5, page.items[1].metricValue, 0.001)

        val request = server.takeRequest()
        assertEquals("/api/v1/rankings/users", request.url.encodedPath)
        assertEquals("moemoepoint_desc", request.url.queryParameter("sort"))
        assertEquals("50", request.url.queryParameter("limit"))
    }

    @Test
    fun `话题排行榜带 NSFW 参数并解析话题摘要`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[
                     {"object":"topic_ranking_entry","rank":1,"metric_value":386871,
                      "topic":{"object":"topic","id":"1098","title":"gal合集",
                        "state":"published","category":"galgame","sections":["g-other"],
                        "cover_images":[],"author":{"object":"user","id":"1929",
                          "name":"思而不了","avatar":null,"avatar_frame":null},
                        "view_count":386871,"like_count":302,"reply_count":217,
                        "comment_count":6,"has_best_answer":false,"mini_apps":[],
                        "is_nsfw":false,"bumped_at":"2026-07-15T07:16:53Z",
                        "created_at":"2024-04-25T08:42:31Z","edited_at":null}},
                     {"object":"topic_ranking_entry","rank":2,"metric_value":10,
                      "topic":null}]}""",
            ),
        )
        val page = api.topics(sort = "likes_desc", includeNsfw = true)
        assertEquals(2, page.items.size)
        assertEquals("1098", page.items[0].topic?.id)
        assertEquals("gal合集", page.items[0].topic?.title)
        assertEquals(386871, page.items[0].topic?.viewCount)
        assertNull(page.items[1].topic)

        val request = server.takeRequest()
        assertEquals("/api/v1/rankings/topics", request.url.encodedPath)
        assertEquals("likes_desc", request.url.queryParameter("sort"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
    }

    @Test
    fun `作品排行榜解析作品与创建者`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[
                     {"object":"work_ranking_entry","rank":1,"metric_value":8.84,
                      "work":{"object":"work","id":"628","display_name":"WHITE ALBUM2",
                        "latin":null,
                        "localized":{"zh-Hans":{"value":"白色相簿2","is_machine":false}},
                        "cover":{"url":"https://img/w.webp","hash":"w","width":600,
                          "height":900,"thumbhash":null,"sexual":"suggestive"},
                        "is_nsfw":false},
                      "creator":{"object":"user","id":"6618","name":"panda233",
                        "avatar":null,"avatar_frame":null}},
                     {"object":"work_ranking_entry","rank":2,"metric_value":7.5,
                      "work":null,"creator":null}]}""",
            ),
        )
        val page = api.works(
            sort = "rating_desc",
            includeNsfw = false,
            includeResourceless = true,
            includeAllOriginalLanguages = true,
        )
        assertEquals(2, page.items.size)
        val first = page.items[0]
        assertEquals("628", first.work?.id)
        assertEquals("WHITE ALBUM2", first.work?.displayName)
        assertEquals("白色相簿2", first.work?.localized?.get("zh-Hans")?.value)
        assertEquals("https://img/w.webp", first.work?.cover?.url)
        assertEquals("panda233", first.creator?.name)
        assertFalse(first.work?.isNsfw ?: true)
        assertNull(page.items[1].work)

        val request = server.takeRequest()
        assertEquals("/api/v1/rankings/works", request.url.encodedPath)
        assertEquals("rating_desc", request.url.queryParameter("sort"))
        assertEquals("true", request.url.queryParameter("include_resourceless"))
        assertEquals("true", request.url.queryParameter("include_all_original_languages"))
        assertTrue(request.url.queryParameter("include_nsfw") == "false")
    }
}
