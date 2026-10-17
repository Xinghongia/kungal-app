package com.kungal.core

import com.kungal.core.api.MeApi
import com.kungal.core.api.ShopApi
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

class ShopApiTest {

    private lateinit var server: MockWebServer
    private lateinit var shop: ShopApi
    private lateinit var me: MeApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        val client = KungalClient(baseUrl = baseUrl, session = session)
        shop = ShopApi(client)
        me = MeApi(client)
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String, code: Int = 200) = MockResponse.Builder()
        .code(code).addHeader("Content-Type", "application/json").body(body).build()

    private val offerJson = """
        {"object":"list","items":[{"object":"shop_offer","id":"12","price":300,
         "is_site_exclusive":true,"stock_count":20,"remaining_count":3,"sold_count":17,
         "restocked_at":"2026-10-01T00:00:00Z","ends_at":null,
         "purchase_limit":{"period":"lifetime","quantity":1},
         "rewards":[{"duration_days":30,"item":{"object":"shop_item","id":"7",
           "item_type":"avatar_frame","display_name":"星尘头像框","description":"限时头像框",
           "artwork":{"static_url":"https://img/frame.png","animated_url":null}}}]}],
         "next_cursor":"c2"}
    """.trimIndent()

    @Test
    fun `商品列表解析与分页`() = runBlocking {
        server.enqueue(ok(offerJson))
        val page = shop.offers(cursor = "c1", limit = 20)
        assertEquals(1, page.items.size)
        val offer = page.items[0]
        assertEquals(300, offer.price)
        assertTrue(offer.isSiteExclusive)
        assertEquals(3, offer.remainingCount)
        assertEquals("lifetime", offer.purchaseLimit?.period)
        assertEquals(30, offer.rewards[0].durationDays)
        assertEquals("avatar_frame", offer.rewards[0].item.itemType)
        assertEquals("c2", page.nextCursor)
        val request = server.takeRequest()
        assertEquals("/api/v1/shop/offers", request.url.encodedPath)
        assertEquals("c1", request.url.queryParameter("cursor"))
        assertEquals("20", request.url.queryParameter("limit"))
    }

    @Test
    fun `库存解析包含物品装扮订单与到货提醒`() = runBlocking {
        server.enqueue(
            ok(
                """
                {"object":"shop_inventory","moemoepoint":421,
                 "items":[{"object":"shop_owned_item","acquired_at":"2026-10-01T00:00:00Z",
                   "acquired_via":"purchase","expires_at":null,"is_active":true,
                   "item":{"object":"shop_item","id":"7","item_type":"avatar_frame",
                     "display_name":"星尘头像框","description":"","artwork":null}}],
                 "loadout":[{"object":"shop_loadout_slot","scope":"everywhere",
                   "slot":"avatar_frame","worn_item_id":"7"}],
                 "orders":[{"object":"shop_order","id":"99","offer_id":"12","price":300,
                   "state":"completed","redeem_codes":[{"code":"ABC-123","expiry_date":null}],
                   "rewards":[],"refunded_at":null,"created_at":"2026-10-01T00:00:00Z"}],
                 "limit_usage":[{"offer_id":"12","purchased_count":1}],
                 "restock_alert_offer_ids":["15"]}
                """.trimIndent(),
            ),
        )
        val inv = shop.inventory()
        assertEquals(421, inv.moemoepoint)
        assertEquals("星尘头像框", inv.items[0].item.displayName)
        assertEquals("7", inv.loadout[0].wornItemId)
        assertEquals("ABC-123", inv.orders[0].redeemCodes[0].code)
        assertEquals(1, inv.limitUsage[0].purchasedCount)
        assertEquals(listOf("15"), inv.restockAlertOfferIds)
        assertEquals("/api/v1/me/shop", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `下单带幂等键并解析余额`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"shop_purchase","moemoepoint":121,
                   "order":{"object":"shop_order","id":"100","offer_id":"12","price":300,
                     "state":"completed","redeem_codes":[],"rewards":[],
                     "refunded_at":null,"created_at":"2026-10-17T00:00:00Z"}}""",
            ),
        )
        val result = shop.purchase("12", "ik-order")
        assertEquals(121, result.moemoepoint)
        assertEquals("100", result.order.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/me/shop/orders", request.url.encodedPath)
        assertEquals("ik-order", request.headers["Idempotency-Key"])
        assertEquals("""{"offer_id":"12"}""", request.body!!.utf8())
    }

    @Test
    fun `装备与卸下物品`() = runBlocking {
        server.enqueue(
            ok("""{"object":"shop_loadout_slot","scope":"everywhere","slot":"avatar_frame","worn_item_id":"7"}"""),
        )
        val worn = shop.setLoadout("everywhere", "avatar_frame", "7")
        assertEquals("7", worn.wornItemId)
        val put = server.takeRequest()
        assertEquals("PUT", put.method)
        assertEquals("/api/v1/me/shop/loadout/everywhere/avatar_frame", put.url.encodedPath)
        assertEquals("""{"item_id":"7"}""", put.body!!.utf8())

        server.enqueue(
            ok("""{"object":"shop_loadout_slot","scope":"everywhere","slot":"avatar_frame","worn_item_id":null}"""),
        )
        val cleared = shop.clearLoadout("everywhere", "avatar_frame")
        assertNull(cleared.wornItemId)
        assertEquals("DELETE", server.takeRequest().method)
    }

    @Test
    fun `到货提醒订阅与取消`() = runBlocking {
        server.enqueue(ok("""{"object":"shop_restock_alert","offer_id":"12","is_subscribed":true}"""))
        assertTrue(shop.subscribeRestock("12").isSubscribed)
        assertEquals("PUT", server.takeRequest().method)

        server.enqueue(ok("""{"object":"shop_restock_alert","offer_id":"12","is_subscribed":false}"""))
        assertFalse(shop.unsubscribeRestock("12").isSubscribed)
        val request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/shop/offers/12/restock-alert", request.url.encodedPath)
    }

    @Test
    fun `萌萌点流水解析与筛选参数`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[{"object":"moemoepoint_entry","id":"1","delta":-300,
                   "reason":"购买「星尘头像框」","ref":"99","ref_path":"/shop",
                   "source":"this_site","created_at":"2026-10-17T00:00:00Z"}],"next_cursor":"n"}""",
            ),
        )
        val page = me.moemoepointEntries(limit = 20)
        assertEquals(-300, page.items[0].delta)
        assertEquals("购买「星尘头像框」", page.items[0].reason)
        assertEquals("/shop", page.items[0].refPath)
        val request = server.takeRequest()
        assertEquals("/api/v1/me/moemoepoint-entries", request.url.encodedPath)
        assertEquals("20", request.url.queryParameter("limit"))
    }

    @Test
    fun `创作者状态解析`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"creator_status","is_creator":false,
                   "eligibility":{"is_eligible":true,"merged_pr_count":6,
                     "published_galgame_count":2,"long_review_count":5,"moemoepoint":2100,
                     "required_merged_pr_count":5,"required_published_galgame_count":10,
                     "required_long_review_count":5,"required_moemoepoint":2000},
                   "application":{"object":"creator_application","id":"3","state":"declined",
                     "statement":"我想申请","decline_reason":"请补充作品链接",
                     "created_at":"2026-10-01T00:00:00Z","reviewed_at":"2026-10-02T00:00:00Z"}}""",
            ),
        )
        val status = me.creatorStatus()
        assertFalse(status.isCreator)
        assertTrue(status.eligibility.isEligible)
        assertEquals(6, status.eligibility.mergedPrCount)
        assertEquals("declined", status.application?.state)
        assertEquals("请补充作品链接", status.application?.declineReason)
    }

    @Test
    fun `提交创作者申请带幂等键`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"creator_application","id":"4","state":"pending",
                   "statement":"你好","decline_reason":null,
                   "created_at":"2026-10-17T00:00:00Z","reviewed_at":null}""",
                code = 201,
            ),
        )
        val app = me.applyCreator("  你好  ", "ik-creator")
        assertEquals("pending", app.state)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/me/creator-applications", request.url.encodedPath)
        assertEquals("ik-creator", request.headers["Idempotency-Key"])
        assertEquals("""{"statement":"你好"}""", request.body!!.utf8())
    }
}
