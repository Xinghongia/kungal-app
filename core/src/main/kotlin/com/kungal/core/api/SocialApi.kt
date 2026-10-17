package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.ReportCreate
import com.kungal.core.model.ReportReason
import com.kungal.core.model.UserFollowState
import com.kungal.core.net.PageList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 关注与举报（/me/following、/report-reasons、/reports）。 */
class SocialApi(private val client: KungalClient) {

    @Serializable
    private data class FollowNotifyBody(
        @SerialName("notify") val notify: String,
    )

    suspend fun followState(userId: String): UserFollowState = client.get(
        path = "/me/following/$userId",
        deserializer = UserFollowState.serializer(),
    )

    suspend fun follow(userId: String): UserFollowState = client.put(
        path = "/me/following/$userId",
        deserializer = UserFollowState.serializer(),
    )

    suspend fun unfollow(userId: String): UserFollowState = client.delete(
        path = "/me/following/$userId",
        deserializer = UserFollowState.serializer(),
    )

    /** 关注通知档位：all = 全部动态，feed = 只进关注流（与网页「关注」按钮菜单一致）。 */
    suspend fun setFollowNotify(userId: String, notify: String): UserFollowState = client.patchJson(
        path = "/me/following/$userId",
        body = KungalJson.encodeToString(FollowNotifyBody.serializer(), FollowNotifyBody(notify)),
        deserializer = UserFollowState.serializer(),
    )

    suspend fun reportReasons(cursor: String? = null, limit: Int = 50): PageList<ReportReason> =
        client.get(
            path = "/report-reasons",
            query = mapOf("cursor" to cursor, "limit" to limit),
            deserializer = PageList.serializer(ReportReason.serializer()),
        )

    /** 举报内容；服务端返回 204。同一内容重复举报只计一次。 */
    suspend fun report(payload: ReportCreate, idempotencyKey: String) {
        client.postJsonNoContent(
            path = "/reports",
            body = KungalJson.encodeToString(ReportCreate.serializer(), payload),
            idempotencyKey = idempotencyKey,
        )
    }
}
