package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.ShopInventory
import com.kungal.core.model.ShopLoadoutSlot
import com.kungal.core.model.ShopOffer
import com.kungal.core.model.ShopPurchase
import com.kungal.core.model.ShopRestockAlert
import com.kungal.core.net.PageList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 萌萌点商店（/shop、/me/shop）。下单必须带 Idempotency-Key；
 * 余额不足/限购/售罄由服务端判定，客户端只做展示与二次确认。
 */
class ShopApi(private val client: KungalClient) {

    @Serializable
    private data class OrderBody(
        @SerialName("offer_id") val offerId: String,
    )

    @Serializable
    private data class LoadoutBody(
        @SerialName("item_id") val itemId: String,
    )

    suspend fun offers(cursor: String? = null, limit: Int = 50): PageList<ShopOffer> = client.get(
        path = "/shop/offers",
        query = mapOf("cursor" to cursor, "limit" to limit),
        deserializer = PageList.serializer(ShopOffer.serializer()),
    )

    suspend fun offer(offerId: String): ShopOffer = client.get(
        path = "/shop/offers/$offerId",
        deserializer = ShopOffer.serializer(),
    )

    suspend fun inventory(): ShopInventory = client.get(
        path = "/me/shop",
        deserializer = ShopInventory.serializer(),
    )

    /** 下单；返回扣点后的余额与订单（兑换码在订单里）。 */
    suspend fun purchase(offerId: String, idempotencyKey: String): ShopPurchase {
        val body = KungalJson.encodeToString(OrderBody.serializer(), OrderBody(offerId))
        return client.postJson(
            path = "/me/shop/orders",
            body = body,
            idempotencyKey = idempotencyKey,
            deserializer = ShopPurchase.serializer(),
        )
    }

    /** 装备物品（scope: everywhere / this_site；slot: avatar_frame / profile_background）。 */
    suspend fun setLoadout(scope: String, slot: String, itemId: String): ShopLoadoutSlot {
        val body = KungalJson.encodeToString(LoadoutBody.serializer(), LoadoutBody(itemId))
        return client.putJson(
            path = "/me/shop/loadout/$scope/$slot",
            body = body,
            deserializer = ShopLoadoutSlot.serializer(),
        )
    }

    suspend fun clearLoadout(scope: String, slot: String): ShopLoadoutSlot = client.delete(
        path = "/me/shop/loadout/$scope/$slot",
        deserializer = ShopLoadoutSlot.serializer(),
    )

    suspend fun subscribeRestock(offerId: String): ShopRestockAlert = client.put(
        path = "/shop/offers/$offerId/restock-alert",
        deserializer = ShopRestockAlert.serializer(),
    )

    suspend fun unsubscribeRestock(offerId: String): ShopRestockAlert = client.delete(
        path = "/shop/offers/$offerId/restock-alert",
        deserializer = ShopRestockAlert.serializer(),
    )
}
