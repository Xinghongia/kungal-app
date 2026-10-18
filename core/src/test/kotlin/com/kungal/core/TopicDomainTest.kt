package com.kungal.core

import com.kungal.core.api.ImageApi
import com.kungal.core.api.TopicApi
import com.kungal.core.model.PollCreate
import com.kungal.core.model.PollOptionCreate
import com.kungal.core.model.TopicCreate
import com.kungal.core.model.TopicDraftCreate
import com.kungal.core.model.TopicPatch
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TopicDomainTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TopicApi
    private lateinit var images: ImageApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        val client = KungalClient(baseUrl = baseUrl, session = session)
        api = TopicApi(client)
        images = ImageApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    private fun okFile(name: String) = ok(Fixtures.text(name))

    @Test
    fun `发帖发送完整请求体与幂等键`() = runBlocking {
        server.enqueue(okFile("topic_detail.json"))
        api.createTopic(
            TopicCreate(
                title = "标题",
                contentMarkdown = "正文",
                category = "technique",
                sections = listOf("t-practical"),
                isNsfw = false,
                accessScope = "public",
            ),
            idempotencyKey = "01J-abc",
        )
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/topics", request.url.encodedPath)
        assertEquals("01J-abc", request.headers["Idempotency-Key"])
        assertEquals(
            """{"title":"标题","content_markdown":"正文","category":"technique","sections":["t-practical"],"is_nsfw":false,"access_scope":"public"}""",
            request.body!!.utf8(),
        )
    }

    @Test
    fun `编辑话题只发送非空字段`() = runBlocking {
        server.enqueue(okFile("topic_detail.json"))
        api.updateTopic("3938", TopicPatch(title = "新标题", isNsfw = true))
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/topics/3938", request.url.encodedPath)
        assertEquals("""{"title":"新标题","is_nsfw":true}""", request.body!!.utf8())
    }

    @Test
    fun `回复列表支持楼层排序`() = runBlocking {
        server.enqueue(okFile("replies_domain.json"))
        val page = api.replies("3938", sort = "floor_desc", fromFloor = 10)
        assertEquals(2, page.items.size)
        val third = page.items[1]
        assertEquals(3, third.floor)
        assertEquals(1, third.comments.size)
        assertEquals("2970", third.comments[0].id)
        val request = server.takeRequest()
        assertEquals("floor_desc", request.url.queryParameter("sort"))
        assertEquals("10", request.url.queryParameter("from_floor"))
    }

    @Test
    fun `楼中楼评论发送 parent_comment_id`() = runBlocking {
        server.enqueue(okFile("comment_domain.json"))
        api.createComment("15717", "回复内容", parentCommentId = "2970", idempotencyKey = "01J-c")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/replies/15717/comments", request.url.encodedPath)
        assertEquals("01J-c", request.headers["Idempotency-Key"])
        assertEquals(
            """{"text":"回复内容","parent_comment_id":"2970"}""",
            request.body!!.utf8(),
        )
    }

    @Test
    fun `评论点赞与取消`() = runBlocking {
        repeat(2) { server.enqueue(okFile("comment_domain.json")) }
        api.likeComment("2970", active = true)
        api.likeComment("2970", active = false)
        assertEquals("PUT", server.takeRequest().method)
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `回复表情反应返回权威快照`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"reply_engagement","reply_id":"15715","like_count":16,"dislike_count":0,
                   "reactions":[{"reaction":"like","count":16,"viewer":{"has_reacted":true}}],
                   "viewer":{"has_liked":true,"has_disliked":false,"can_like":true}}""",
            ),
        )
        val result = api.reactReply("15715", "like", active = true)
        assertEquals(16, result.likeCount)
        assertTrue(result.viewer?.hasLiked == true)
        assertEquals("/api/v1/replies/15715/reactions/like", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `反应历史解析`() = runBlocking {
        server.enqueue(okFile("reactions_domain.json"))
        val page = api.topicReactionHistory("3938")
        assertEquals(3, page.items.size)
        assertEquals("fire", page.items[0].reaction)
        assertEquals("Esolme", page.items[0].reactor.name)
    }

    @Test
    fun `收藏与订阅读接口`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"topic_engagement","topic_id":"3938","like_count":1,"dislike_count":0,
                   "favorite_count":2,"upvote_count":0,"upvoted_at":null,"reactions":[],
                   "viewer":{"has_favorited":true}}""",
            ),
        )
        val result = api.favorite("3938", active = true)
        assertEquals(2, result.favoriteCount)
        assertTrue(result.viewer?.hasFavorited == true)
        server.takeRequest()

        server.enqueue(okFile("subscription_domain.json"))
        val sub = api.subscription("3938")
        assertEquals("normal", sub.notificationLevel)
        server.takeRequest()

        server.enqueue(okFile("states_domain.json"))
        val states = api.states(listOf("3938", "4462"))
        assertEquals(2, states.items.size)
        assertEquals("3938,4462", server.takeRequest().url.queryParameter("topic_ids"))
    }

    @Test
    fun `取消订阅通知与已读楼层`() = runBlocking {
        server.enqueue(okFile("subscription_domain.json"))
        api.setSubscription("3938", "muted")
        assertEquals(
            """{"notification_level":"muted"}""",
            server.takeRequest().body!!.utf8(),
        )

        server.enqueue(okFile("subscription_domain.json"))
        api.markRead("3938", 12)
        val request = server.takeRequest()
        assertEquals("/api/v1/topics/3938/subscription/read-marker", request.url.encodedPath)
        assertEquals("""{"floor":12}""", request.body!!.utf8())
    }

    @Test
    fun `投票创建、投票与撤票`() = runBlocking {
        server.enqueue(okFile("poll_domain.json"))
        api.createPoll(
            "4481",
            PollCreate(
                title = "喜欢吗",
                options = listOf(PollOptionCreate("yes"), PollOptionCreate("no")),
            ),
            idempotencyKey = "01J-p",
        )
        assertEquals("POST", server.takeRequest().method)

        server.enqueue(okFile("poll_domain.json"))
        api.votePoll("42", listOf("198"))
        assertEquals("""{"option_ids":["198"]}""", server.takeRequest().body!!.utf8())

        server.enqueue(okFile("poll_domain.json"))
        api.clearPollVote("42")
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/polls/42/vote", request.url.encodedPath)
    }

    @Test
    fun `抽奖列表、参与、退出与名单`() = runBlocking {
        server.enqueue(okFile("lottery_domain.json"))
        val lotteries = api.lotteries("4462")
        assertEquals(8, lotteries.items[0].entryCount)
        assertEquals("reply_required", lotteries.items[0].viewer?.enterBlockedReason)
        assertEquals("/api/v1/topics/4462/lotteries", server.takeRequest().url.encodedPath)

        server.enqueue(ok("""{"object":"list","items":[{"object":"lottery_entry","id":"1",
            "entrant":{"object":"user","id":"2","name":"鲲"},"created_at":"2026-10-17T03:00:00Z"}],
            "next_cursor":null}"""))
        val entries = api.lotteryEntries("6")
        assertEquals("鲲", entries.items[0].entrant.name)
    }

    @Test
    fun `草稿创建、列表与删除`() = runBlocking {
        server.enqueue(okFile("draft_detail_domain.json"))
        api.createDraft(
            TopicDraftCreate(title = "未完成的帖子", contentMarkdown = "正文"),
            idempotencyKey = "01J-d",
        )
        val create = server.takeRequest()
        assertEquals("/api/v1/me/topic-drafts", create.url.encodedPath)
        assertEquals("01J-d", create.headers["Idempotency-Key"])

        server.enqueue(okFile("drafts_domain.json"))
        val drafts = api.drafts()
        assertEquals(1, drafts.items.size)
        assertEquals("未完成的帖子", drafts.items[0].title)
        server.takeRequest()

        server.enqueue(MockResponse.Builder().code(204).build())
        api.deleteDraft("12")
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/me/topic-drafts/12", delete.url.encodedPath)
    }

    @Test
    fun `图片上传使用 multipart 与幂等键`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"image","url":"https://image.kungal.iloveren.link/aa/bb/x.webp",
                   "hash":"${"a".repeat(64)}","width":10,"height":20,"sexual":null}""",
            ),
        )
        val image = images.upload(
            bytes = byteArrayOf(1, 2, 3),
            fileName = "cover.png",
            contentType = "image/png",
            idempotencyKey = "01J-i",
        )
        assertEquals("a".repeat(64), image.hash)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/images", request.url.encodedPath)
        assertEquals("01J-i", request.headers["Idempotency-Key"])
        val body = request.body!!.utf8()
        assertTrue(body.contains("name=\"purpose\""))
        assertTrue(body.contains("content"))
        assertTrue(body.contains("filename=\"cover.png\""))
    }

    @Test
    fun `关注动态与摘要`() = runBlocking {
        server.enqueue(okFile("following_summary_domain.json"))
        assertEquals(3, api.followingSummary().unseenCount)

        server.enqueue(okFile("following_domain.json"))
        val page = api.followingActivities()
        assertEquals(1, page.items.size)
        assertEquals("publish", page.items[0].verb)
        assertEquals("新话题", page.items[0].items[0].title)
        server.takeRequest() // summary
        server.takeRequest() // activities

        server.enqueue(
            okFile("following_items_domain.json"),
        )
        val items = api.followingGroupItems("9001", cursor = "c1", limit = 20)
        assertEquals(2, items.items.size)
        assertEquals("7101", items.items[0].id)
        val request = server.takeRequest().url
        assertEquals("/api/v1/activity-groups/9001/items", request.encodedPath)
        assertEquals("c1", request.queryParameter("cursor"))
        assertEquals("20", request.queryParameter("limit"))
        assertEquals("false", request.queryParameter("include_nsfw"))
    }

    @Test
    fun `搜索回复与评论`() = runBlocking {
        server.enqueue(okFile("search_replies_domain.json"))
        val replies = api.searchReplies("开源")
        assertEquals(1, replies.items[0].floor)
        assertEquals("3938", replies.items[0].topicId)

        server.enqueue(okFile("search_comments_domain.json"))
        val comments = api.searchComments("开源")
        assertEquals("2970", comments.items[0].id)
        assertEquals("3938", comments.items[0].topicId)
    }
}
