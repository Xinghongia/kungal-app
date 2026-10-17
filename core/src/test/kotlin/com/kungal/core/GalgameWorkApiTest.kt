package com.kungal.core

import com.kungal.core.api.CollectionApi
import com.kungal.core.api.GalgameApi
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

class GalgameWorkApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: GalgameApi
    private lateinit var collections: CollectionApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        val client = KungalClient(baseUrl = baseUrl, session = session)
        api = GalgameApi(client)
        collections = CollectionApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String, code: Int = 200) = MockResponse.Builder()
        .code(code).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `作品详情解析真实样本的全部读面区块`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_work_detail.json")))
        val work = api.work("4219", includeNsfw = false)

        assertEquals("4219", work.id)
        assertEquals("ja-jp", work.originalLanguage)
        assertEquals("r18", work.contentRating)
        assertFalse(work.isNsfw)
        assertEquals(8, work.aliases.size)
        assertEquals(3, work.intros.size)
        assertEquals(12, work.covers.size)
        assertEquals(10, work.screenshots.size)
        assertEquals(3, work.companies.size)
        assertEquals(1, work.engines.size)
        assertEquals(3, work.relatedWorks.size)
        assertEquals(102, work.tags.size)
        assertEquals(27, work.credits.size)
        assertEquals(27, work.roster.size)
        assertEquals("main", work.roster[0].characterKind)
        assertEquals(listOf("publisher"), work.companies[0].attributionRoles)
        assertEquals("voice-actor", work.credits[0].roleKey)
        assertEquals(19, work.favoriteCount)
        assertNull(work.viewer)
        assertEquals(2, work.contributors.size)
        assertEquals(3, work.playtimes.size)
        assertEquals(4, work.externalRatings.size)
        assertEquals(11, work.links.size)
        assertEquals(31, work.externalRefs.size)
        assertTrue(work.covers.any { it.coverSlot == "main" })

        val request = server.takeRequest()
        assertEquals("/api/v1/works/4219", request.url.encodedPath)
        assertEquals("false", request.url.queryParameter("include_nsfw"))
    }

    @Test
    fun `点赞与取消点赞使用 PUT DELETE`() = runBlocking {
        server.enqueue(
            ok("""{"object":"work_engagement","work_id":"4219","like_count":6,
               "viewer":{"has_liked":true}}"""),
        )
        val liked = api.setWorkLike("4219", liked = true)
        assertEquals(6, liked.likeCount)
        assertTrue(liked.viewer?.hasLiked == true)
        assertEquals("PUT", server.takeRequest().method)

        server.enqueue(
            ok("""{"object":"work_engagement","work_id":"4219","like_count":5,
               "viewer":{"has_liked":false}}"""),
        )
        val unliked = api.setWorkLike("4219", liked = false)
        assertEquals(5, unliked.likeCount)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/works/4219/like", request.url.encodedPath)
    }

    @Test
    fun `封面投票与批量读取本人选票`() = runBlocking {
        server.enqueue(
            ok("""{"object":"work_cover_engagement","work_id":"4219","cover_id":"30076",
               "vote_count":3,"viewer":{"has_voted":true}}"""),
        )
        val vote = api.setCoverVote("4219", "30076", voted = true)
        assertEquals(3, vote.voteCount)
        assertTrue(vote.viewer?.hasVoted == true)
        val voteRequest = server.takeRequest()
        assertEquals("PUT", voteRequest.method)
        assertEquals("/api/v1/works/4219/covers/30076/vote", voteRequest.url.encodedPath)

        server.enqueue(
            ok("""{"object":"list","items":[
               {"object":"my_cover_vote","work_id":"4219","voted_cover_id":"30076"},
               {"object":"my_cover_vote","work_id":"4220","voted_cover_id":null}],
               "missing":["9999"]}"""),
        )
        val mine = api.myCoverVotes(listOf("4219", "4220", "9999"))
        assertEquals("30076", mine.items[0].votedCoverId)
        assertNull(mine.items[1].votedCoverId)
        assertEquals(listOf("9999"), mine.missing)
        val request = server.takeRequest()
        assertEquals(listOf("4219", "4220", "9999"), request.url.queryParameterValues("work_ids"))
    }

    @Test
    fun `收藏夹选择项与加入移出作品`() = runBlocking {
        server.enqueue(
            ok("""{"object":"list","items":[{"object":"collection","id":"12","title":"默认收藏夹",
               "visibility":"private","is_default":true,"item_count":3,
               "updated_at":"2026-10-01T00:00:00Z",
               "viewer":{"is_owner":true,"can_edit":true,"can_delete":true,"has_work":true}}],
               "total":1,"total_relation":"eq"}"""),
        )
        val page = collections.choicesForWork("4219")
        assertEquals(1, page.items.size)
        assertTrue(page.items[0].viewer?.hasWork == true)
        assertEquals("/api/v1/me/works/4219/collections", server.takeRequest().url.encodedPath)

        server.enqueue(
            ok("""{"object":"collection_work_engagement","collection_id":"12",
               "work_id":"4219","viewer":{"has_work":false}}"""),
        )
        val removed = collections.setWork("12", "4219", member = false)
        assertFalse(removed.viewer?.hasWork == true)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/collections/12/works/4219", request.url.encodedPath)
    }

    @Test
    fun `新建收藏夹带幂等键与必填可见性`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"collection","id":"13","title":"新收藏夹","description":"",
                   "visibility":"public","is_default":false,"item_count":0,
                   "owner":{"object":"user","id":"42","name":"甲"},
                   "preview_covers":[],"created_at":"2026-10-17T00:00:00Z",
                   "updated_at":"2026-10-17T00:00:00Z","viewer":null}""",
                code = 201,
            ),
        )
        val created = collections.create(
            title = "新收藏夹",
            visibility = "public",
            idempotencyKey = "ik-collection",
        )
        assertEquals("13", created.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/collections", request.url.encodedPath)
        assertEquals("ik-collection", request.headers["Idempotency-Key"])
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"title\":\"新收藏夹\""))
        assertTrue(body.contains("\"visibility\":\"public\""))
    }
}
