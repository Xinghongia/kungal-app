package com.kungal.core

import com.kungal.core.api.GalgameApi
import com.kungal.core.api.RatingApi
import com.kungal.core.api.WallCommentApi
import com.kungal.core.model.AspectScores
import com.kungal.core.model.PlaytimePutBody
import com.kungal.core.model.RatingCreateBody
import com.kungal.core.model.RatingPatchBody
import com.kungal.core.model.RecordRatingBody
import com.kungal.core.model.WorkRecordPutBody
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

class GalgameRatingApiTest {

    private lateinit var server: MockWebServer
    private lateinit var ratings: RatingApi
    private lateinit var walls: WallCommentApi
    private lateinit var galgame: GalgameApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        val client = KungalClient(baseUrl = baseUrl, session = session)
        ratings = RatingApi(client)
        walls = WallCommentApi(client)
        galgame = GalgameApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String, code: Int = 200) = MockResponse.Builder()
        .code(code).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `评分列表解析真实样本并传筛选参数`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_ratings_list.json")))
        val page = ratings.list(
            page = 2,
            limit = 3,
            sort = "overall_desc",
            workId = "229",
            spoilerLevel = "none",
            playStatus = "done_all",
            gameType = "moe",
            includeNsfw = true,
        )
        assertEquals(5043, page.total)
        assertEquals(3, page.items.size)
        assertEquals(8, page.items[0].overall)
        assertEquals("yes", page.items[0].recommend)
        assertEquals(listOf("ba_saku", "moe"), page.items[0].gameTypes)
        assertEquals("229", page.items[0].work?.id)
        assertNull(page.items[0].viewer)

        val request = server.takeRequest()
        assertEquals("/api/v1/ratings", request.url.encodedPath)
        assertEquals("overall_desc", request.url.queryParameter("sort"))
        assertEquals("229", request.url.queryParameter("work_id"))
        assertEquals("none", request.url.queryParameter("spoiler_level"))
        assertEquals("done_all", request.url.queryParameter("play_status"))
        assertEquals("moe", request.url.queryParameter("game_type"))
        assertEquals("true", request.url.queryParameter("include_nsfw"))
    }

    @Test
    fun `评分详情解析八维评分与作品摘要`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_rating_detail.json")))
        val rating = ratings.get("1000448")
        assertEquals(8, rating.overall)
        assertEquals(8, rating.aspectScores.art)
        assertEquals(8, rating.aspectScores.replayValue)
        assertEquals("229", rating.workSummary?.id)
        assertEquals(0, rating.likers.size)
        assertNull(rating.viewer)
    }

    @Test
    fun `评分创建修改删除与点赞`() = runBlocking {
        val detailJson = Fixtures.text("galgame_rating_detail.json")
        server.enqueue(ok(detailJson, code = 201))
        val created = ratings.create(
            RatingCreateBody(
                workId = "229",
                recommend = "yes",
                overall = 8,
                gameTypes = listOf("moe"),
                playStatus = "done_all",
                spoilerLevel = "none",
                shortSummary = "很好",
                aspectScores = AspectScores(art = 8, story = 8, music = 8, character = 8, route = 8, system = 8, voice = 8, replayValue = 8),
            ),
            idempotencyKey = "ik-rating",
        )
        assertEquals("1000448", created.id)
        val createRequest = server.takeRequest()
        assertEquals("POST", createRequest.method)
        assertEquals("ik-rating", createRequest.headers["Idempotency-Key"])
        assertTrue(createRequest.body!!.utf8().contains("\"work_id\":\"229\""))

        server.enqueue(ok(detailJson))
        ratings.update("1000448", RatingPatchBody(overall = 9, shortSummary = "改"))
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertTrue(patch.body!!.utf8().contains("\"overall\":9"))
        assertFalse(patch.body!!.utf8().contains("recommend"))

        server.enqueue(
            ok("""{"object":"rating_engagement","rating_id":"1000448","like_count":5,
               "viewer":{"has_liked":true}}"""),
        )
        val liked = ratings.setLike("1000448", true)
        assertEquals(5, liked.likeCount)
        assertTrue(liked.viewer.hasLiked)
        assertEquals("PUT", server.takeRequest().method)

        server.enqueue(MockResponse.Builder().code(204).build())
        ratings.delete("1000448")
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/ratings/1000448", delete.url.encodedPath)
    }

    @Test
    fun `游玩记录读写与时长接口`() = runBlocking {
        server.enqueue(
            ok("""{"object":"work_record","work_id":"229","play_state":"done_all",
               "minutes":1200,"rating_summary":{"object":"rating","id":"1000448","overall":8,
               "recommend":"yes","game_types":["moe"],"play_status":"done_all",
               "spoiler_level":"none","short_summary":"好","created_at":"2026-01-01T00:00:00Z",
               "updated_at":"2026-01-01T00:00:00Z"}}"""),
        )
        val record = galgame.workRecord("229")
        assertEquals("done_all", record.playState)
        assertEquals(1200, record.minutes)
        assertEquals(8, record.ratingSummary?.overall)
        assertEquals("/api/v1/works/229/record", server.takeRequest().url.encodedPath)

        server.enqueue(
            ok("""{"object":"work_record","work_id":"229","play_state":"doing",
               "minutes":600,"rating_summary":null}"""),
        )
        val saved = galgame.putWorkRecord(
            "229",
            WorkRecordPutBody(
                minutes = 600,
                playState = "doing",
                ratingInput = RecordRatingBody(
                    recommend = "yes",
                    overall = 9,
                    gameTypes = listOf("plot"),
                    spoilerLevel = "portion",
                    shortSummary = "很强",
                ),
            ),
        )
        assertEquals("doing", saved.playState)
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        val body = put.body!!.utf8()
        assertTrue(body.contains("\"play_state\":\"doing\""))
        assertTrue(body.contains("\"rating_input\""))
        assertTrue(body.contains("\"spoiler_level\":\"portion\""))

        server.enqueue(ok("""{"minutes":300,"play_state":"doing"}"""))
        val playtime = galgame.putPlaytime("229", PlaytimePutBody(minutes = 300, playState = "doing"))
        assertEquals(300, playtime.minutes)
        assertEquals("/api/v1/works/229/playtime", server.takeRequest().url.encodedPath)

        server.enqueue(ok("""{"minutes":0,"play_state":null}"""))
        val cleared = galgame.deletePlaytime("229")
        assertEquals(0, cleared.minutes)
        assertNull(cleared.playState)
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `评论墙列表解析内容文档与游标`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_wall_comments.json")))
        val page = walls.list(subjectType = "galgame", subjectId = "229", limit = 5)
        assertEquals(5, page.items.size)
        assertTrue(page.nextCursor != null)
        assertEquals("galgame", page.items[0].subjectType)
        assertEquals("229", page.items[0].subjectId)
        assertTrue(page.items[0].content.children.isNotEmpty())
        assertEquals(10023, page.items[0].author.id.toInt())

        val request = server.takeRequest()
        assertEquals("/api/v1/wall-comments", request.url.encodedPath)
        assertEquals("galgame", request.url.queryParameter("subject_type"))
        assertEquals("229", request.url.queryParameter("subject_id"))
    }

    @Test
    fun `评论发布编辑点赞举报与删除`() = runBlocking {
        val commentJson = """{"object":"wall_comment","id":"900","subject_type":"galgame",
            "subject_id":"229","parent_comment_id":null,"root_comment_id":null,
            "author":{"object":"user","id":"42","name":"甲","avatar":null,"avatar_frame":null},
            "addressee":null,"state":"visible",
            "content":{"object":"document","children":[]},"like_count":0,
            "created_at":"2026-10-17T00:00:00Z","edited_at":null,
            "is_edited_by_moderator":false,
            "viewer":{"has_liked":false,"can_edit":true,"can_delete":true,"can_like":true,"can_flag":false}}"""
        server.enqueue(ok(commentJson, code = 201))
        val comment = walls.create(
            subjectType = "galgame",
            subjectId = "229",
            contentMarkdown = "第一条评论",
            idempotencyKey = "ik-comment",
        )
        assertEquals("900", comment.id)
        assertTrue(comment.viewer?.canEdit == true)
        val create = server.takeRequest()
        assertEquals("ik-comment", create.headers["Idempotency-Key"])
        val createBody = create.body!!.utf8()
        assertTrue(createBody.contains("\"content_markdown\":\"第一条评论\""))
        assertFalse(createBody.contains("\"parent_comment_id\""))

        server.enqueue(ok(commentJson))
        walls.update("900", "改过的评论")
        val patch = server.takeRequest()
        assertEquals("PATCH", patch.method)
        assertTrue(patch.body!!.utf8().contains("改过的评论"))

        server.enqueue(ok(commentJson))
        walls.setLike("900", true)
        assertEquals("PUT", server.takeRequest().method)

        server.enqueue(MockResponse.Builder().code(204).build())
        walls.flag("900", reason = "spam", note = "广告")
        val flag = server.takeRequest()
        assertEquals("POST", flag.method)
        assertEquals("/api/v1/wall-comments/900/flags", flag.url.encodedPath)
        assertTrue(flag.body!!.utf8().contains("\"flag_reason\":\"spam\""))

        server.enqueue(
            ok("""{"object":"wall_comment_source","wall_comment_id":"900",
               "content_markdown":"改过的评论"}"""),
        )
        assertEquals("改过的评论", walls.source("900").contentMarkdown)
        assertEquals("/api/v1/wall-comments/900/source", server.takeRequest().url.encodedPath)

        server.enqueue(MockResponse.Builder().code(204).build())
        walls.delete("900")
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/wall-comments/900", delete.url.encodedPath)
    }
}
