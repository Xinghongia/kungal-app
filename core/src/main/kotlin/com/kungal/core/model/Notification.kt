package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 一条通知。notification_type 是服务端封闭词汇（24 种），界面按中文映射；
 * actor 可能是 id=0、name=null 的系统通知。excerpt_markdown 是快照文本，仅作展示。
 */
@Serializable
data class Notification(
    @SerialName("object") val obj: String = "notification",
    val id: String = "",
    @SerialName("notification_type") val notificationType: String = "",
    val actor: UserRef = UserRef(),
    @SerialName("actor_count") val actorCount: Int = 1,
    @SerialName("item_count") val itemCount: Int = 1,
    val path: String = "",
    @SerialName("excerpt_markdown") val excerptMarkdown: String = "",
    val origin: String = "local",
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String = "",
)

/** 未读汇总（GET /me/notifications/summary）。muted 与未静音两个分区互不重叠。 */
@Serializable
data class NotificationSummary(
    @SerialName("object") val obj: String = "notification_summary",
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("muted_unread_count") val mutedUnreadCount: Int = 0,
    @SerialName("direct_message_unread_count") val directMessageUnreadCount: Int = 0,
    @SerialName("is_direct_message_muted") val isDirectMessageMuted: Boolean = false,
    val latest: Notification? = null,
) {
    /** 消息 tab 角标：未静音通知 + 未静音时的未读私信。 */
    val badgeCount: Int
        get() = unreadCount + if (isDirectMessageMuted) 0 else directMessageUnreadCount
}

/** 通知偏好：已静音的类型（含私信 chat）。 */
@Serializable
data class NotificationPreferences(
    @SerialName("object") val obj: String = "notification_preferences",
    @SerialName("muted_types") val mutedTypes: List<String> = emptyList(),
)

/** PUT /me/notifications/read-marker 的响应。 */
@Serializable
data class NotificationReadMarker(
    @SerialName("object") val obj: String = "notification_read_marker",
    @SerialName("marked_count") val markedCount: Int = 0,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

/** 网站（关注墙的一类主体）。 */
@Serializable
data class WebsiteSummary(
    @SerialName("object") val obj: String = "website",
    val id: String = "",
    val host: String = "",
    val title: String = "",
    val icon: ImageInfo? = null,
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
)

/**
 * 已关注的评论区（GET /me/walls）。work 仅 galgame 墙有值，website 仅网站墙有值；
 * 对应目录记录不存在时也可能为 null。
 */
@Serializable
data class FollowedWall(
    @SerialName("object") val obj: String = "followed_wall",
    @SerialName("subject_type") val subjectType: String = "",
    @SerialName("subject_id") val subjectId: String = "",
    val work: WorkRef? = null,
    val website: WebsiteSummary? = null,
)

/** 关注墙的状态（GET/PUT/DELETE /me/walls/{type}/{id}/follow）。 */
@Serializable
data class WallState(
    @SerialName("object") val obj: String = "wall_state",
    val id: String = "",
    @SerialName("subject_type") val subjectType: String = "",
    @SerialName("subject_id") val subjectId: String = "",
    @SerialName("is_following") val isFollowing: Boolean = false,
)
