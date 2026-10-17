package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 萌萌点流水条目（GET /me/moemoepoint-entries）。 */
@Serializable
data class MoemoepointEntry(
    @SerialName("object") val obj: String = "moemoepoint_entry",
    val id: String = "",
    /** 正数为获得，负数为消耗。 */
    val delta: Int = 0,
    val reason: String = "",
    val ref: String = "",
    @SerialName("ref_path") val refPath: String? = null,
    @SerialName("source") val source: String = "this_site",
    @SerialName("created_at") val createdAt: String = "",
)

/** 创作者资格进度（GET /me/creator-status）。 */
@Serializable
data class CreatorEligibility(
    @SerialName("is_eligible") val isEligible: Boolean = false,
    @SerialName("merged_pr_count") val mergedPrCount: Int = 0,
    @SerialName("published_galgame_count") val publishedGalgameCount: Int = 0,
    @SerialName("long_review_count") val longReviewCount: Int = 0,
    val moemoepoint: Int = 0,
    @SerialName("required_merged_pr_count") val requiredMergedPrCount: Int = 5,
    @SerialName("required_published_galgame_count") val requiredPublishedGalgameCount: Int = 10,
    @SerialName("required_long_review_count") val requiredLongReviewCount: Int = 5,
    @SerialName("required_moemoepoint") val requiredMoemoepoint: Int = 2000,
)

/** 创作者申请记录。state: pending / approved / declined。 */
@Serializable
data class CreatorApplication(
    @SerialName("object") val obj: String = "creator_application",
    val id: String = "",
    val state: String = "pending",
    val statement: String = "",
    @SerialName("decline_reason") val declineReason: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("reviewed_at") val reviewedAt: String? = null,
)

/** 创作者状态：是否已是创作者、资格进度、最近一次申请。 */
@Serializable
data class CreatorStatus(
    @SerialName("object") val obj: String = "creator_status",
    @SerialName("is_creator") val isCreator: Boolean = false,
    val eligibility: CreatorEligibility = CreatorEligibility(),
    val application: CreatorApplication? = null,
)

/** 提交创作者申请（POST /me/creator-applications）。 */
@Serializable
data class CreatorApplicationCreate(
    val statement: String = "",
)
