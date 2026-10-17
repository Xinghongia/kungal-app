package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.FollowedWall
import com.kungal.core.model.Notification
import com.kungal.core.model.NotificationPreferences
import com.kungal.core.model.NotificationReadMarker
import com.kungal.core.model.NotificationSummary
import com.kungal.core.model.WallState
import com.kungal.core.net.PageList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 消息中心：通知列表/汇总/已读/删除、通知偏好（静音），以及关注墙
 * （Galgame/评分/资源/题库/工具集/网站评论区）。
 */
class NotificationApi(private val client: KungalClient) {

    @Serializable
    private data class ReadMarkerBody(
        @SerialName("up_to_id") val upToId: String,
        @SerialName("is_muted") val isMuted: Boolean,
    )

    /**
     * 通知游标分页。[isMuted] 选择分区：false 为未静音（默认），true 为已静音。
     * [type] 为 24 种通知类型之一，与分区取交集；游标绑定三者组合，不能换组合复用。
     */
    suspend fun notifications(
        isMuted: Boolean = false,
        cursor: String? = null,
        limit: Int = 30,
        type: String? = null,
    ): PageList<Notification> = client.get(
        path = "/me/notifications",
        query = mapOf(
            "is_muted" to isMuted,
            "cursor" to cursor,
            "limit" to limit,
            "notification_type" to type,
        ),
        deserializer = PageList.serializer(Notification.serializer()),
    )

    suspend fun summary(): NotificationSummary = client.get(
        path = "/me/notifications/summary",
        deserializer = NotificationSummary.serializer(),
    )

    /** 把某个分区中 id ≤ [upToId] 的未读通知标为已读；重放返回 marked_count=0。 */
    suspend fun markRead(upToId: String, isMuted: Boolean = false): NotificationReadMarker {
        val body = KungalJson.encodeToString(
            ReadMarkerBody.serializer(),
            ReadMarkerBody(upToId = upToId, isMuted = isMuted),
        )
        return client.putJson(
            path = "/me/notifications/read-marker",
            body = body,
            deserializer = NotificationReadMarker.serializer(),
        )
    }

    /** 删除一条通知（204）。 */
    suspend fun deleteNotification(id: String) = client.deleteNoBody("/me/notifications/$id")

    suspend fun preferences(): NotificationPreferences = client.get(
        path = "/me/notification-preferences",
        deserializer = NotificationPreferences.serializer(),
    )

    /** 静音一种类型；使用单类型接口，避免整体覆盖时抹掉客户端不认识的新类型。 */
    suspend fun muteType(type: String): NotificationPreferences = client.put(
        path = "/me/notification-preferences/muted-types/$type",
        deserializer = NotificationPreferences.serializer(),
    )

    /** 取消静音一种类型；未静音时同样返回完整集合。 */
    suspend fun unmuteType(type: String): NotificationPreferences = client.delete(
        path = "/me/notification-preferences/muted-types/$type",
        deserializer = NotificationPreferences.serializer(),
    )

    suspend fun followedWalls(cursor: String? = null, limit: Int = 30): PageList<FollowedWall> =
        client.get(
            path = "/me/walls",
            query = mapOf("cursor" to cursor, "limit" to limit),
            deserializer = PageList.serializer(FollowedWall.serializer()),
        )

    suspend fun wallState(subjectType: String, subjectId: String): WallState = client.get(
        path = "/me/walls/$subjectType/$subjectId",
        deserializer = WallState.serializer(),
    )

    suspend fun followWall(subjectType: String, subjectId: String): WallState = client.put(
        path = "/me/walls/$subjectType/$subjectId/follow",
        deserializer = WallState.serializer(),
    )

    suspend fun unfollowWall(subjectType: String, subjectId: String): WallState = client.delete(
        path = "/me/walls/$subjectType/$subjectId/follow",
        deserializer = WallState.serializer(),
    )

    suspend fun markWallRead(subjectType: String, subjectId: String): WallState = client.put(
        path = "/me/walls/$subjectType/$subjectId/read-marker",
        deserializer = WallState.serializer(),
    )
}
