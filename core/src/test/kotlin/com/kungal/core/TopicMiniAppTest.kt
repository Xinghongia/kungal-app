package com.kungal.core

import com.kungal.core.api.TopicApi
import com.kungal.core.model.LotteryCreate
import com.kungal.core.model.LotteryPatch
import com.kungal.core.model.LotteryPrizeInput
import com.kungal.core.model.PollPatch
import com.kungal.core.model.PollOptionChanges
import com.kungal.core.model.PollOptionCreate
import com.kungal.core.model.PollOptionUpdate
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonNull
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/** P0：投票 / 抽奖的作者侧写接口（创建、编辑、开奖、履约、删除）。 */
class TopicMiniAppTest {

    private lateinit var server: MockWebServer
    private lateinit var api: TopicApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        api = TopicApi(KungalClient(baseUrl = baseUrl, session = session))
    }

    @After
    fun tearDown() {
        server.close()
    }

    @Test
    fun `编辑投票只发变化字段并可清除截止时间`() = runBlocking {
        server.enqueue(ok(Fixtures.text("poll_domain.json")))
        api.updatePoll(
            "42",
            PollPatch(
                title = "新标题",
                closesAt = JsonNull,
                optionChanges = PollOptionChanges(
                    add = listOf(PollOptionCreate("新选项")),
                    update = listOf(PollOptionUpdate("198", "改后的选项")),
                    remove = listOf("199"),
                ),
            ),
        )
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/polls/42", request.url.encodedPath)
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"title\":\"新标题\""))
        assertTrue(body.contains("\"closes_at\":null"))
        assertTrue(body.contains("\"add\":[{\"text\":\"新选项\"}]"))
        assertTrue(body.contains("\"option_id\":\"198\""))
        assertTrue(body.contains("\"remove\":[\"199\"]"))
        assertFalse(body.contains("\"description\""))

        server.enqueue(MockResponse.Builder().code(204).build())
        api.deletePoll("42")
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/polls/42", delete.url.encodedPath)
    }

    @Test
    fun `创建抽奖带幂等键与完整奖品`() = runBlocking {
        server.enqueue(ok(lotteryJson()))
        val lottery = api.createLottery(
            topicId = "4462",
            payload = LotteryCreate(
                title = "楼层福利",
                description = "回帖即可",
                entryMode = "floor",
                floorRule = "8,18",
                drawMode = "manual",
                closesAt = "2026-12-31T15:59:59Z",
                minAccountAgeDays = 3,
                minMoemoepoint = 20,
                isEntryListPublic = false,
                prizes = listOf(
                    LotteryPrizeInput(
                        title = "激活码",
                        description = "Steam key",
                        imageHashes = listOf("a".repeat(64)),
                        adultImageHashes = emptyList(),
                        delivery = "code",
                        slotCount = 2,
                        codes = listOf("CODE-1", "CODE-2"),
                    ),
                ),
            ),
            idempotencyKey = "01J-lottery",
        )
        assertEquals("6", lottery.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/topics/4462/lotteries", request.url.encodedPath)
        assertEquals("01J-lottery", request.headers["Idempotency-Key"])
        val body = request.body!!.utf8()
        assertTrue(body.contains("\"entry_mode\":\"floor\""))
        assertTrue(body.contains("\"floor_rule\":\"8,18\""))
        assertTrue(body.contains("\"draw_mode\":\"manual\""))
        assertTrue(body.contains("\"closes_at\":\"2026-12-31T15:59:59Z\""))
        assertTrue(body.contains("\"slot_count\":2"))
        assertTrue(body.contains("\"codes\":[\"CODE-1\",\"CODE-2\"]"))
        assertTrue(body.contains("\"image_hashes\":[\"${"a".repeat(64)}\"]"))
        assertTrue(body.contains("\"is_entry_list_public\":false"))
    }

    @Test
    fun `开奖取消与编辑抽奖`() = runBlocking {
        server.enqueue(ok(lotteryJson()))
        api.updateLottery("6", LotteryPatch(state = "drawn"))
        val draw = server.takeRequest()
        assertEquals("PATCH", draw.method)
        assertEquals("/api/v1/lotteries/6", draw.url.encodedPath)
        assertEquals("""{"state":"drawn"}""", draw.body!!.utf8())

        server.enqueue(ok(lotteryJson()))
        api.updateLottery("6", LotteryPatch(state = "cancelled"))
        assertEquals("""{"state":"cancelled"}""", server.takeRequest().body!!.utf8())

        server.enqueue(ok(lotteryJson()))
        api.updateLottery(
            "6",
            LotteryPatch(
                title = "改后的标题",
                closesAt = JsonNull,
                prizes = listOf(
                    LotteryPrizeInput(title = "周边", delivery = "offline", slotCount = 1),
                ),
            ),
        )
        val edit = server.takeRequest().body!!.utf8()
        assertTrue(edit.contains("\"title\":\"改后的标题\""))
        assertTrue(edit.contains("\"closes_at\":null"))
        assertTrue(edit.contains("\"prizes\":[{"))
        assertTrue(edit.contains("\"delivery\":\"offline\""))

        server.enqueue(MockResponse.Builder().code(204).build())
        api.deleteLottery("6")
        val delete = server.takeRequest()
        assertEquals("DELETE", delete.method)
        assertEquals("/api/v1/lotteries/6", delete.url.encodedPath)
    }

    @Test
    fun `读取单个中奖记录并更新履约状态`() = runBlocking {
        server.enqueue(ok(winnerJson("pending")))
        val winner = api.lotteryWinner("6", "9")
        assertEquals("pending", winner.fulfillment)
        assertEquals("鲲", winner.winner.name)
        assertEquals("/api/v1/lotteries/6/winners/9", server.takeRequest().url.encodedPath)

        server.enqueue(ok(winnerJson("shipped")))
        val updated = api.updateLotteryWinner("6", "9", "shipped")
        assertEquals("shipped", updated.fulfillment)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/lotteries/6/winners/9", request.url.encodedPath)
        assertEquals("""{"fulfillment":"shipped"}""", request.body!!.utf8())
    }

    @Test
    fun `抽奖奖品图片保留 hash 与成人标记`() = runBlocking {
        val hash = "b".repeat(64)
        server.enqueue(
            ok(
                """
                {"object":"lottery","id":"6","topic_id":"4462","title":"福利",
                 "entry_mode":"signup","draw_mode":"manual","state":"open",
                 "prizes":[{"object":"lottery_prize","id":"7","title":"周边",
                   "description":"","images":[{"hash":"$hash",
                     "image":{"url":"https://image.example/$hash.webp","hash":"$hash"},
                     "is_marked_adult":true,"is_graded_explicit":false}],
                   "delivery":"offline","point_mode":null,"point_amount":null,
                   "point_budget":null,"slot_count":1,"code_count":null}],
                 "winners":[]}
                """.trimIndent(),
            ),
        )
        val prize = api.lottery("6").prizes.single()
        val image = prize.images.single()
        assertEquals(hash, image.hash)
        assertTrue(image.isMarkedAdult)
        assertEquals("https://image.example/$hash.webp", image.image?.url)
    }

    private fun ok(body: String) = MockResponse.Builder()
        .code(200)
        .addHeader("Content-Type", "application/json")
        .body(body)
        .build()

    private fun lotteryJson() = """
        {"object":"lottery","id":"6","topic_id":"4462","title":"福利",
         "entry_mode":"floor","draw_mode":"manual","state":"open",
         "entry_count":0,"slot_count":0,"prizes":[],"winners":[],
         "viewer":{"has_entered":false,"can_enter":false,"can_edit":true,
           "can_delete":true,"can_draw":true,"can_cancel":true,
           "can_view_entries":true,"can_manage_fulfillment":false,
           "can_reveal_code":false}}
    """.trimIndent()

    private fun winnerJson(fulfillment: String) = """
        {"object":"lottery_winner","id":"9","prize_id":"7",
         "winner":{"object":"user","id":"2","name":"鲲"},
         "winning_floor":null,"rank_key":null,"fulfillment":"$fulfillment",
         "point_awarded":0,"won_at":"2026-10-18T00:00:00Z","claim_expires_at":null}
    """.trimIndent()
}
