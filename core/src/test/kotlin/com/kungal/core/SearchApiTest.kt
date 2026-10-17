package com.kungal.core

import com.kungal.core.api.SearchApi
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

class SearchApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: SearchApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        api = SearchApi(KungalClient(baseUrl = baseUrl, session = session))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `用户搜索解析资料并带分页参数`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[
                     {"object":"user","id":"10698","name":"galgame","avatar":null,
                      "avatar_frame":null,"bio":"","roles":[],
                      "registered_at":"2024-07-08T13:51:41Z","topic_count":0,"reply_count":0},
                     {"object":"user","id":"8882","name":"GalGameBoy",
                      "avatar":{"url":"https://img/u.webp","hash":"h","width":null,
                        "height":null,"thumbhash":null,"sexual":null},
                      "avatar_frame":null,"bio":"签名","roles":["creator"],
                      "registered_at":null,"topic_count":3,"reply_count":9}],
                   "total":50,"total_relation":"gte"}""",
            ),
        )
        val page = api.users(q = "galgame", page = 2, limit = 24)
        assertEquals(2, page.items.size)
        assertEquals(50, page.total)
        assertEquals("gte", page.totalRelation)
        assertEquals("10698", page.items[0].id)
        assertEquals(0, page.items[0].replyCount)
        assertEquals("https://img/u.webp", page.items[1].avatar?.url)
        assertEquals("签名", page.items[1].bio)
        assertEquals(listOf("creator"), page.items[1].roles)
        assertNull(page.items[1].registeredAt)

        val request = server.takeRequest()
        assertEquals("/api/v1/search/users", request.url.encodedPath)
        assertEquals("galgame", request.url.queryParameter("q"))
        assertEquals("2", request.url.queryParameter("page"))
        assertEquals("24", request.url.queryParameter("limit"))
    }

    @Test
    fun `留言墙搜索使用游标并解析主题路径`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[
                     {"object":"wall_comment","id":"19607","subject_type":"galgame",
                      "subject_id":"11287","subject_path":"/galgame/11287",
                      "work":{"object":"work","id":"11287","display_name":"人妻 雪乃",
                        "latin":null,"localized":{},
                        "cover":{"url":"https://img/c.webp","hash":"c","width":256,
                          "height":368,"thumbhash":null,"sexual":"explicit"},
                        "is_nsfw":true},
                      "excerpt":"解压之后打开文件夹","author":{"object":"user","id":"70738",
                        "name":"web_Ljust","avatar":null,"avatar_frame":null},
                      "created_at":"2026-10-17T01:30:21Z"}],
                   "next_cursor":"cur_abc"}""",
            ),
        )
        val page = api.wallComments(q = "解压", cursor = "cur_prev", limit = 24)
        assertEquals(1, page.items.size)
        val hit = page.items[0]
        assertEquals("galgame", hit.subjectType)
        assertEquals("/galgame/11287", hit.subjectPath)
        assertEquals("11287", hit.work?.id)
        assertTrue(hit.work?.isNsfw == true)
        assertEquals("web_Ljust", hit.author.name)
        assertEquals("cur_abc", page.nextCursor)

        val request = server.takeRequest()
        assertEquals("/api/v1/search/wall-comments", request.url.encodedPath)
        assertEquals("解压", request.url.queryParameter("q"))
        assertEquals("cur_prev", request.url.queryParameter("cursor"))
    }

    @Test
    fun `作品搜索带筛选排序与屏蔽标签`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[
                     {"object":"work","id":"57302","display_name":"想成为Galgame领域大神",
                      "latin":null,"localized":{},
                      "cover":{"url":"https://img/w.webp","hash":"w","width":600,
                        "height":900,"thumbhash":null,"sexual":"safe"},
                      "is_nsfw":false,"banner":null,"release_date":"2026-01-16",
                      "release_date_precision":"day",
                      "maker":{"object":"company","id":"34310",
                        "display_name":"Noctella","latin":null,"localized":{}},
                      "view_count":1743,"like_count":3,"rating_score":7.5,
                      "rating_count":2,"resource_platforms":["win","and"],
                      "resource_languages":["zh-cn"],
                      "resource_updated_at":"2026-07-20T02:59:55Z","is_published":true}],
                   "total":1,"total_relation":"eq"}""",
            ),
        )
        val page = api.works(
            q = "大神",
            page = 1,
            limit = 24,
            sort = "released_desc",
            companyId = "34310",
            tagIds = listOf("1", "2"),
            releasedFrom = "2026-01",
            releasedTo = "2026-12",
            includeNsfw = true,
            excludeTagIds = listOf("99"),
            excludeTagTrees = listOf("88"),
        )
        assertEquals(1, page.items.size)
        assertEquals("57302", page.items[0].id)
        assertEquals("Noctella", page.items[0].maker?.displayName)
        assertEquals(7.5, page.items[0].ratingScore ?: 0.0, 0.001)
        assertFalse(page.items[0].isNsfw)

        val request = server.takeRequest()
        assertEquals("/api/v1/search/works", request.url.encodedPath)
        assertEquals("大神", request.url.queryParameter("q"))
        assertEquals("released_desc", request.url.queryParameter("sort"))
        assertEquals("34310", request.url.queryParameter("company_id"))
        assertEquals(listOf("1", "2"), request.url.queryParameterValues("tag_ids"))
        assertEquals("2026-01", request.url.queryParameter("released_from"))
        assertEquals("2026-12", request.url.queryParameter("released_to"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
        assertEquals(listOf("99"), request.url.queryParameterValues("exclude_tag_ids"))
        assertEquals(listOf("88"), request.url.queryParameterValues("exclude_tag_trees"))
    }
}
