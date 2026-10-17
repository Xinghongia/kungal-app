package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 商品/奖励的展示图（头像框、主页背景等装扮）。 */
@Serializable
data class ShopItemArtwork(
    @SerialName("static_url") val staticUrl: String = "",
    @SerialName("animated_url") val animatedUrl: String? = null,
)

/** 商店物品：装扮、兑换码、功能介绍等。 */
@Serializable
data class ShopItem(
    @SerialName("object") val obj: String = "shop_item",
    val id: String = "",
    @SerialName("item_type") val itemType: String = "avatar_frame",
    @SerialName("display_name") val displayName: String = "",
    val description: String = "",
    val artwork: ShopItemArtwork? = null,
)

/** 购买奖励：物品 + 可选有效天数。 */
@Serializable
data class ShopReward(
    @SerialName("duration_days") val durationDays: Int? = null,
    val item: ShopItem = ShopItem(),
)

/** 限购规则：lifetime（永久一次）/ month（每月一次）。 */
@Serializable
data class ShopPurchaseLimit(
    val period: String = "lifetime",
    val quantity: Int = 1,
)

/** 商店在售商品。 */
@Serializable
data class ShopOffer(
    @SerialName("object") val obj: String = "shop_offer",
    val id: String = "",
    val price: Int = 0,
    @SerialName("is_site_exclusive") val isSiteExclusive: Boolean = false,
    val rewards: List<ShopReward> = emptyList(),
    @SerialName("purchase_limit") val purchaseLimit: ShopPurchaseLimit? = null,
    @SerialName("stock_count") val stockCount: Int? = null,
    @SerialName("remaining_count") val remainingCount: Int? = null,
    @SerialName("sold_count") val soldCount: Int = 0,
    @SerialName("restocked_at") val restockedAt: String? = null,
    @SerialName("ends_at") val endsAt: String? = null,
) {
    val soldOut: Boolean get() = remainingCount != null && remainingCount <= 0
}

/** 已拥有的物品。 */
@Serializable
data class ShopOwnedItem(
    @SerialName("object") val obj: String = "shop_owned_item",
    @SerialName("acquired_at") val acquiredAt: String = "",
    @SerialName("acquired_via") val acquiredVia: String = "purchase",
    @SerialName("expires_at") val expiresAt: String? = null,
    @SerialName("is_active") val isActive: Boolean = true,
    val item: ShopItem = ShopItem(),
)

/** 当前装备槽（scope: everywhere / this_site；slot: avatar_frame / profile_background）。 */
@Serializable
data class ShopLoadoutSlot(
    @SerialName("object") val obj: String = "shop_loadout_slot",
    val scope: String = "everywhere",
    val slot: String = "avatar_frame",
    @SerialName("worn_item_id") val wornItemId: String? = null,
)

/** 兑换码。 */
@Serializable
data class ShopRedeemCode(
    val code: String = "",
    @SerialName("expiry_date") val expiryDate: String? = null,
)

/** 订单。 */
@Serializable
data class ShopOrder(
    @SerialName("object") val obj: String = "shop_order",
    val id: String = "",
    @SerialName("offer_id") val offerId: String = "",
    val price: Int = 0,
    val state: String = "completed",
    @SerialName("redeem_codes") val redeemCodes: List<ShopRedeemCode> = emptyList(),
    val rewards: List<ShopReward> = emptyList(),
    @SerialName("refunded_at") val refundedAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
)

/** 限购使用情况。 */
@Serializable
data class ShopLimitUsage(
    @SerialName("offer_id") val offerId: String = "",
    @SerialName("purchased_count") val purchasedCount: Int = 0,
)

/** 我的商店：余额、物品、装扮槽、订单、到货提醒。 */
@Serializable
data class ShopInventory(
    @SerialName("object") val obj: String = "shop_inventory",
    val moemoepoint: Int = 0,
    val items: List<ShopOwnedItem> = emptyList(),
    val loadout: List<ShopLoadoutSlot> = emptyList(),
    val orders: List<ShopOrder> = emptyList(),
    @SerialName("limit_usage") val limitUsage: List<ShopLimitUsage> = emptyList(),
    @SerialName("restock_alert_offer_ids") val restockAlertOfferIds: List<String> = emptyList(),
)

/** 下单结果：扣点后的余额 + 订单（含兑换码）。 */
@Serializable
data class ShopPurchase(
    @SerialName("object") val obj: String = "shop_purchase",
    val moemoepoint: Int = 0,
    val order: ShopOrder = ShopOrder(),
)

/** 到货提醒状态。 */
@Serializable
data class ShopRestockAlert(
    @SerialName("object") val obj: String = "shop_restock_alert",
    @SerialName("offer_id") val offerId: String = "",
    @SerialName("is_subscribed") val isSubscribed: Boolean = false,
)
