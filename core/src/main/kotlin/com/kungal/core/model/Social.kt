package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 我对某个用户的关注状态（GET/PUT/DELETE /me/following/{user_id}）。 */
@Serializable
data class UserFollowState(
    @SerialName("object") val obj: String = "user_follow_state",
    val id: String = "",
    @SerialName("user_id") val userId: String = "",
    @SerialName("is_following") val isFollowing: Boolean = false,
    @SerialName("is_followed_by") val isFollowedBy: Boolean = false,
    @SerialName("notify_level") val notifyLevel: String? = null,
)

/** 举报原因（GET /report-reasons）。 */
@Serializable
data class ReportReason(
    @SerialName("object") val obj: String = "report_reason",
    val key: String = "",
    @SerialName("display_name") val displayName: String = "",
)

/** 举报请求体（POST /reports，204 无响应体）。 */
@Serializable
data class ReportCreate(
    @SerialName("subject_kind") val subjectKind: String,
    @SerialName("subject_id") val subjectId: String,
    @SerialName("reason_key") val reasonKey: String,
    val note: String? = null,
    val snapshot: String? = null,
    @SerialName("subject_url") val subjectUrl: String? = null,
)
