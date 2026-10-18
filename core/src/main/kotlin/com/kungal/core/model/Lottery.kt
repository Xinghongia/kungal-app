package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Lottery(
    @SerialName("object") val obj: String = "lottery",
    val id: String = "",
    @SerialName("topic_id") val topicId: String = "",
    val author: UserRef = UserRef(),
    val title: String = "",
    val description: String = "",
    @SerialName("entry_mode") val entryMode: String = "signup",
    @SerialName("floor_rule") val floorRule: String? = null,
    @SerialName("draw_mode") val drawMode: String = "deadline",
    @SerialName("draw_threshold") val drawThreshold: Int? = null,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("min_account_age_days") val minAccountAgeDays: Int = 0,
    @SerialName("min_moemoepoint") val minMoemoepoint: Int = 0,
    @SerialName("is_entry_list_public") val isEntryListPublic: Boolean = false,
    val state: String = "open",
    @SerialName("seed_hash") val seedHash: String? = null,
    val seed: String? = null,
    @SerialName("entry_count") val entryCount: Int = 0,
    @SerialName("slot_count") val slotCount: Int = 0,
    @SerialName("drawn_at") val drawnAt: String? = null,
    val prizes: List<LotteryPrize> = emptyList(),
    val winners: List<LotteryWinner> = emptyList(),
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("updated_at") val updatedAt: String = "",
    val viewer: LotteryViewer? = null,
)

@Serializable
data class LotteryPrize(
    @SerialName("object") val obj: String = "lottery_prize",
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val images: List<LotteryPrizeImage> = emptyList(),
    val delivery: String = "offline",
    @SerialName("point_mode") val pointMode: String? = null,
    @SerialName("point_amount") val pointAmount: Int? = null,
    @SerialName("point_budget") val pointBudget: Int? = null,
    @SerialName("slot_count") val slotCount: Int = 0,
    @SerialName("code_count") val codeCount: Int? = null,
)

/** 奖品图片：成人内容被门禁时 image 为 null，但 hash 仍在，编辑表单可原样回写。 */
@Serializable
data class LotteryPrizeImage(
    val hash: String = "",
    val image: ImageInfo? = null,
    @SerialName("is_marked_adult") val isMarkedAdult: Boolean = false,
    @SerialName("is_graded_explicit") val isGradedExplicit: Boolean = false,
)

@Serializable
data class LotteryWinner(
    @SerialName("object") val obj: String = "lottery_winner",
    val id: String = "",
    @SerialName("prize_id") val prizeId: String = "",
    val winner: UserRef = UserRef(),
    @SerialName("winning_floor") val winningFloor: Int? = null,
    @SerialName("rank_key") val rankKey: String? = null,
    val fulfillment: String = "pending",
    @SerialName("point_awarded") val pointAwarded: Int = 0,
    @SerialName("won_at") val wonAt: String = "",
    @SerialName("claim_expires_at") val claimExpiresAt: String? = null,
)

@Serializable
data class LotteryViewer(
    @SerialName("has_entered") val hasEntered: Boolean = false,
    @SerialName("can_enter") val canEnter: Boolean = false,
    @SerialName("enter_blocked_reason") val enterBlockedReason: String? = null,
    @SerialName("can_edit") val canEdit: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("can_draw") val canDraw: Boolean = false,
    @SerialName("can_cancel") val canCancel: Boolean = false,
    @SerialName("can_view_entries") val canViewEntries: Boolean = false,
    @SerialName("can_manage_fulfillment") val canManageFulfillment: Boolean = false,
    @SerialName("winner_id") val winnerId: String? = null,
    @SerialName("can_reveal_code") val canRevealCode: Boolean = false,
)

@Serializable
data class LotteryEntry(
    @SerialName("object") val obj: String = "lottery_entry",
    val id: String = "",
    val entrant: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class LotteryCodeReveal(
    @SerialName("object") val obj: String = "lottery_code_reveal",
    @SerialName("lottery_id") val lotteryId: String = "",
    @SerialName("winner_id") val winnerId: String = "",
    val code: String = "",
)

/** POST /topics/{id}/lotteries。 */
@Serializable
data class LotteryCreate(
    val title: String,
    val description: String? = null,
    @SerialName("entry_mode") val entryMode: String,
    @SerialName("floor_rule") val floorRule: String? = null,
    @SerialName("draw_mode") val drawMode: String,
    @SerialName("draw_threshold") val drawThreshold: Int? = null,
    @SerialName("closes_at") val closesAt: String? = null,
    @SerialName("min_account_age_days") val minAccountAgeDays: Int = 0,
    @SerialName("min_moemoepoint") val minMoemoepoint: Int = 0,
    @SerialName("is_entry_list_public") val isEntryListPublic: Boolean = true,
    val prizes: List<LotteryPrizeInput>,
)

/** 奖项输入；新建与「重写奖项」时的 PATCH 共用。 */
@Serializable
data class LotteryPrizeInput(
    val title: String,
    val description: String? = null,
    @SerialName("image_hashes") val imageHashes: List<String> = emptyList(),
    @SerialName("adult_image_hashes") val adultImageHashes: List<String> = emptyList(),
    val delivery: String,
    @SerialName("slot_count") val slotCount: Int,
    val codes: List<String>? = null,
    @SerialName("point_mode") val pointMode: String? = null,
    @SerialName("point_amount") val pointAmount: Int? = null,
)

/** PATCH /lotteries/{id}：state=drawn/cancelled 时只能单独发送 state。 */
@Serializable
data class LotteryPatch(
    val title: String? = null,
    val description: String? = null,
    @SerialName("entry_mode") val entryMode: String? = null,
    @SerialName("floor_rule") val floorRule: String? = null,
    @SerialName("draw_mode") val drawMode: String? = null,
    @SerialName("draw_threshold") val drawThreshold: Int? = null,
    @SerialName("closes_at") val closesAt: JsonElement? = null,
    @SerialName("min_account_age_days") val minAccountAgeDays: Int? = null,
    @SerialName("min_moemoepoint") val minMoemoepoint: Int? = null,
    @SerialName("is_entry_list_public") val isEntryListPublic: Boolean? = null,
    val prizes: List<LotteryPrizeInput>? = null,
    val state: String? = null,
)

@Serializable
data class LotteryWinnerPatch(
    val fulfillment: String,
)
