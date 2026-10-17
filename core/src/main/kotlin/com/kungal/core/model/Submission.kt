package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Galgame 投稿（BP-02 U6）：把资料库没有的作品提交给本站认领。
 * claim 状态：live 已发布 / draft 草稿 / pending 审核中 / declined 已拒绝 / hidden 已下架。
 */
@Serializable
data class WorkSubmissionViewer(
    @SerialName("can_submit") val canSubmit: Boolean = false,
    @SerialName("can_withdraw") val canWithdraw: Boolean = false,
    @SerialName("can_delete") val canDelete: Boolean = false,
    @SerialName("can_review") val canReview: Boolean = false,
)

/** 一次状态变更（含拒绝原因）。 */
@Serializable
data class ClaimEventRef(
    @SerialName("object") val obj: String = "claim_event",
    val id: String = "",
    @SerialName("from_state") val fromState: String? = null,
    @SerialName("to_state") val toState: String = "",
    val note: String? = null,
    val actor: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
)

/** 候选搜索命中：资料库里已有的作品 + 本站认领状态（none = 尚无本站条目）。 */
@Serializable
data class WorkSubmissionCandidate(
    @SerialName("object") val obj: String = "work_submission_candidate",
    @SerialName("work_summary") val workSummary: WorkSummary = WorkSummary(),
    val state: String = "none",
)

/** 投稿摘要（我的提交 / 审核队列）。 */
@Serializable
data class WorkSubmissionSummary(
    @SerialName("object") val obj: String = "work_submission",
    val id: String = "",
    @SerialName("work_id") val workId: String = "",
    @SerialName("display_name") val displayName: String = "",
    val state: String = "draft",
    @SerialName("work_summary") val workSummary: WorkSummary = WorkSummary(),
    @SerialName("last_event") val lastEvent: ClaimEventRef? = null,
    @SerialName("first_acted_at") val firstActedAt: String? = null,
    val viewer: WorkSubmissionViewer? = null,
)

/** 游标列表 + 可选总数（include_total=true 时才有）。 */
@Serializable
data class WorkSubmissionList(
    @SerialName("object") val obj: String = "list",
    val items: List<WorkSubmissionSummary> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
    val total: Long? = null,
) {
    val hasMore: Boolean get() = nextCursor != null
}

/** 投稿详情。 */
@Serializable
data class WorkSubmission(
    @SerialName("object") val obj: String = "work_submission",
    val id: String = "",
    @SerialName("work_id") val workId: String = "",
    @SerialName("display_name") val displayName: String = "",
    val state: String = "draft",
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    @SerialName("content_rating") val contentRating: String = "all_ages",
    val submitter: UserRef? = null,
    @SerialName("last_event") val lastEvent: ClaimEventRef? = null,
    @SerialName("first_acted_at") val firstActedAt: String? = null,
    @SerialName("acted_count") val actedCount: Long = 0,
    val viewer: WorkSubmissionViewer? = null,
)

/** 新建投稿的响应：比详情多一个「封面是否真的挂上」的标记。 */
@Serializable
data class WorkSubmissionCreated(
    @SerialName("object") val obj: String = "work_submission",
    val id: String = "",
    @SerialName("work_id") val workId: String = "",
    @SerialName("display_name") val displayName: String = "",
    val state: String = "pending",
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
    @SerialName("content_rating") val contentRating: String = "all_ages",
    val submitter: UserRef? = null,
    @SerialName("last_event") val lastEvent: ClaimEventRef? = null,
    @SerialName("first_acted_at") val firstActedAt: String? = null,
    @SerialName("acted_count") val actedCount: Long = 0,
    val viewer: WorkSubmissionViewer? = null,
    @SerialName("has_banner_attached") val hasBannerAttached: Boolean = true,
)

// ---------- 请求体 ----------

@Serializable
data class SubmissionTitle(
    val locale: String,
    val title: String,
)

@Serializable
data class SubmissionIntroduction(
    val locale: String,
    val value: String,
)

/**
 * POST /work-submissions。titles / original_language / content_rating / is_nsfw 必填；
 * is_duplicate_confirmed 只在看到 DUPLICATE_SUSPECTS 并由作者确认后重新发送时为 true。
 */
@Serializable
data class WorkSubmissionCreate(
    val titles: List<SubmissionTitle>,
    val aliases: List<String> = emptyList(),
    val introductions: List<SubmissionIntroduction> = emptyList(),
    @SerialName("original_language") val originalLanguage: String,
    @SerialName("content_rating") val contentRating: String,
    @SerialName("is_nsfw") val isNsfw: Boolean,
    @SerialName("display_name") val displayName: String? = null,
    @SerialName("release_date") val releaseDate: String? = null,
    @SerialName("release_date_precision") val releaseDatePrecision: String? = null,
    @SerialName("banner_hash") val bannerHash: String? = null,
    @SerialName("is_duplicate_confirmed") val isDuplicateConfirmed: Boolean = false,
)

@Serializable
data class WorkSubmissionPatch(
    val state: String,
    val note: String? = null,
)

/** 认领状态的中文标签（与网页一致）。 */
fun claimStateLabel(state: String): String = when (state) {
    "live" -> "已发布"
    "draft" -> "草稿"
    "pending" -> "审核中"
    "declined" -> "已拒绝"
    "hidden" -> "已下架"
    "none" -> "未认领"
    else -> state
}
