package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.ReportCreate
import com.kungal.core.model.ReportReason
import com.kungal.core.model.UserFollowState
import com.kungal.core.net.PageList

/** 关注与举报（/me/following、/report-reasons、/reports）。 */
class SocialApi(private val client: KungalClient) {

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
