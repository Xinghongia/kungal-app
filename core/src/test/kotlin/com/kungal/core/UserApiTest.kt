package com.kungal.core

import com.kungal.core.api.MeApi
import com.kungal.core.api.SocialApi
import com.kungal.core.api.UserApi
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

class UserApiTest {

    private lateinit var server: MockWebServer
    private lateinit var api: UserApi
    private lateinit var meApi: MeApi
    private lateinit var socialApi: SocialApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        val client = KungalClient(baseUrl = baseUrl, session = session)
        api = UserApi(client)
        meApi = MeApi(client)
        socialApi = SocialApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200).addHeader("Content-Type", "application/json").body(body).build()

    @Test
    fun `用户主页解析统计与头像框`() = runBlocking {
        server.enqueue(
            ok(
                """
                {"object":"user","id":"42","name":"星鸿",
                 "avatar":{"url":"https://img/a.webp","hash":"a"},
                 "avatar_frame":{"static_url":"https://img/f.png","animated_url":null},
                 "bio":"你好","about_html":null,"roles":["creator"],
                 "created_at":"2026-10-17T01:17:07Z","moemoepoint":10,
                 "counts":{"topic_count":3,"poll_count":1,"lottery_count":0,"reply_count":9,
                   "topic_comment_count":4,"community_comment_count":null,
                   "published_galgame_count":2,"contributed_galgame_count":1,
                   "published_galgame_today_count":0,"galgame_rating_count":5,
                   "galgame_resource_count":6,"toolset_count":1,"toolset_resource_count":0,
                   "received_upvote_count":7,"received_like_count":11,"received_dislike_count":0,
                   "topic_today_count":1,"follower_count":20,"following_count":13}}
                """.trimIndent(),
            ),
        )
        val profile = api.profile("42")
        assertEquals("星鸿", profile.name)
        assertEquals("你好", profile.bio)
        assertEquals(listOf("creator"), profile.roles)
        assertEquals("https://img/f.png", profile.avatarFrame?.staticUrl)
        assertEquals(11, profile.counts.receivedLikeCount)
        assertEquals(20, profile.counts.followerCount)
        assertNull(profile.counts.communityCommentCount)
        assertEquals("/api/v1/users/42", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `用户话题列表带 relation 与分页参数`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[{"object":"topic","id":"1","title":"标题","created_at":"2026-10-17T00:00:00Z"}],
                   "total":41,"total_relation":"eq"}""",
            ),
        )
        val page = api.topics("42", relation = "favorited", page = 2, limit = 20)
        assertEquals(1, page.items.size)
        assertEquals(41, page.total)
        assertTrue(page.hasMore(page = 2, pageSize = 20))
        val request = server.takeRequest()
        assertEquals("/api/v1/users/42/topics", request.url.encodedPath)
        assertEquals("favorited", request.url.queryParameter("relation"))
        assertEquals("2", request.url.queryParameter("page"))
        assertEquals("20", request.url.queryParameter("limit"))
    }

    @Test
    fun `游戏库游标分页与状态数组`() = runBlocking {
        server.enqueue(
            ok(
                """
                {"object":"list","next_cursor":"c2",
                 "state_counts":{"wish":1,"doing":2,"done":3,"on_hold":4,"dropped":5},
                 "items":[{"object":"work_record","play_state":"doing","playtime_minutes":90,
                   "state_updated_at":"2026-10-16T00:00:00Z","user_rating":{"id":"9","overall":8},
                   "work_summary":{"object":"work","id":"7","display_name":"作品","is_nsfw":false,
                     "like_count":0,"rating_count":0,"view_count":0,"is_published":true,
                     "localized":{"zh-Hans":{"value":"作品","is_machine":false}}}}]}
                """.trimIndent(),
            ),
        )
        val page = api.workRecords("42", states = listOf("doing", "wish"), cursor = "c1")
        assertEquals(3, page.stateCounts.done)
        assertEquals("c2", page.nextCursor)
        assertEquals(8, page.items[0].userRating?.overall)
        assertEquals("作品", page.items[0].workSummary.displayName)
        val request = server.takeRequest()
        assertEquals(listOf("doing", "wish"), request.url.queryParameterValues("state"))
        assertEquals("c1", request.url.queryParameter("cursor"))
    }

    @Test
    fun `关注与粉丝列表解析`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[{"object":"follow",
                   "follower":{"object":"user","id":"2","name":"甲","avatar_frame":null},
                   "followed_at":"2026-10-01T00:00:00Z"}],"next_cursor":"n"}""",
            ),
        )
        val page = api.followers("42")
        assertEquals("甲", page.items[0].user?.name)
        assertEquals("/api/v1/users/42/followers", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `用户评分列表`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[{"object":"rating","id":"5","overall":9,
                   "recommend":"strong_yes","short_summary":"很好","created_at":"2026-10-01T00:00:00Z",
                   "updated_at":"2026-10-01T00:00:00Z",
                   "work":{"object":"work","id":"7","display_name":"作品","is_nsfw":false}}],
                   "total":1,"total_relation":"eq"}""",
            ),
        )
        val page = api.ratings(authorId = "42")
        assertEquals(9, page.items[0].overall)
        assertEquals("作品", page.items[0].work?.displayName)
        assertEquals("42", server.takeRequest().url.queryParameter("author_id"))
    }

    @Test
    fun `资料修改只发送提供的字段`() = runBlocking {
        server.enqueue(
            ok("""{"object":"user","id":"42","name":"新名字","bio":null,"avatar":null}"""),
        )
        val result = meApi.updateProfile(name = "新名字")
        assertEquals("新名字", result.name)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/me/profile", request.url.encodedPath)
        assertEquals("""{"name":"新名字"}""", request.body!!.utf8())
    }

    @Test
    fun `头像上传是 multipart 且字段名为 file`() = runBlocking {
        server.enqueue(
            ok("""{"url":"https://img/new.webp","hash":"new","width":256,"height":256}"""),
        )
        val image = meApi.uploadAvatar(byteArrayOf(1, 2, 3), "avatar.png", "image/png")
        assertEquals("new", image.hash)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/me/avatar", request.url.encodedPath)
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
        val body = request.body!!.utf8()
        assertTrue(body.contains("name=\"file\""))
        assertTrue(body.contains("filename=\"avatar.png\""))
    }

    @Test
    fun `隐私与活动设置读写方法`() = runBlocking {
        server.enqueue(ok("""{"object":"profile_settings","is_sign_in_required":true}"""))
        assertTrue(meApi.setProfileSignInRequired(true).isSignInRequired)
        assertEquals("PUT", server.takeRequest().method)

        server.enqueue(ok("""{"object":"privacy_settings","is_play_records_public":false,"changed_at":null}"""))
        assertFalse(meApi.setPlayRecordsPublic(false).isPlayRecordsPublic)
        assertEquals("PATCH", server.takeRequest().method)

        server.enqueue(ok("""{"object":"activity_settings","is_hidden":true}"""))
        assertTrue(meApi.setActivityHidden(true).isHidden)
        assertEquals("PUT", server.takeRequest().method)
    }

    @Test
    fun `签到带幂等键`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"check_in","check_in_date":"2026-10-17","moemoepoint":17,
                   "moemoepoint_awarded":7}""",
            ),
        )
        val result = meApi.checkIn("ik-1")
        assertEquals(7, result.moemoepointAwarded)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/me/check-ins", request.url.encodedPath)
        assertEquals("ik-1", request.headers["Idempotency-Key"])
    }

    @Test
    fun `关注通知档位使用 PATCH`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"user_follow_state","id":"2","user_id":"2","is_following":true,
                   "is_followed_by":true,"notify_level":"feed"}""",
            ),
        )
        val state = socialApi.setFollowNotify("2", "feed")
        assertEquals("feed", state.notifyLevel)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/me/following/2", request.url.encodedPath)
        assertEquals("""{"notify":"feed"}""", request.body!!.utf8())
    }

    @Test
    fun `用户搜索与 id 解析二选一`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[{"object":"user","id":"42","name":"星鸿",
                   "avatar":null,"avatar_frame":null}],"missing":[]}""",
            ),
        )
        val found = api.resolveUsers(q = "星", limit = 5)
        assertEquals("星鸿", found.items[0].name)
        val search = server.takeRequest().url
        assertEquals("/api/v1/users", search.encodedPath)
        assertEquals("星", search.queryParameter("q"))
        assertEquals("5", search.queryParameter("limit"))
        assertNull(search.queryParameter("ids"))

        server.enqueue(ok("""{"object":"list","items":[],"missing":["9"]}"""))
        val resolved = api.resolveUsers(ids = listOf("42", "9"))
        assertEquals(listOf("9"), resolved.missing)
        val batch = server.takeRequest().url
        assertEquals(listOf("42", "9"), batch.queryParameterValues("ids"))
        assertNull(batch.queryParameter("q"))
        assertNull(batch.queryParameter("limit"))
    }

    @Test
    fun `用户作品留言墙按关系与游标分页`() = runBlocking {
        server.enqueue(ok(Fixtures.text("galgame_wall_comments.json")))
        val page = api.wallComments("42", relation = "liked", cursor = "c1", limit = 24)
        assertEquals(5, page.items.size)
        assertEquals("galgame", page.items[0].subjectType)
        assertEquals("229", page.items[0].subjectId)
        assertTrue(page.nextCursor!!.startsWith("cur_"))

        val url = server.takeRequest().url
        assertEquals("/api/v1/users/42/wall-comments", url.encodedPath)
        assertEquals("liked", url.queryParameter("relation"))
        assertEquals("galgame", url.queryParameter("subject_type"))
        assertEquals("c1", url.queryParameter("cursor"))
        assertEquals("24", url.queryParameter("limit"))
    }

    @Test
    fun `点赞资源列表不传 state`() = runBlocking {
        server.enqueue(ok("""{"object":"list","items":[],"total":0,"total_relation":"eq"}"""))
        api.resources("42", relation = "liked", state = null)
        val url = server.takeRequest().url
        assertEquals("/api/v1/users/42/galgame-resources", url.encodedPath)
        assertEquals("liked", url.queryParameter("relation"))
        assertNull(url.queryParameter("state"))
    }
}
