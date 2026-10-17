package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 关注动态摘要（未读数）。 */
@Serializable
data class FollowingActivitySummary(
    @SerialName("object") val obj: String = "following_activity_summary",
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
    @SerialName("unseen_count") val unseenCount: Int = 0,
)

/** 关注动态分组（同一人、同一动作、同一天）。 */
@Serializable
data class FollowingActivityGroup(
    @SerialName("object") val obj: String = "following_activity_group",
    val id: String = "",
    val site: String = "",
    val actor: UserRef = UserRef(),
    val verb: String = "",
    @SerialName("object_kind") val objectKind: String = "",
    @SerialName("object_label") val objectLabel: String = "",
    @SerialName("calendar_date") val calendarDate: String = "",
    @SerialName("item_count") val itemCount: Int = 0,
    @SerialName("latest_at") val latestAt: String = "",
    val items: List<FollowingActivityItem> = emptyList(),
)

@Serializable
data class FollowingActivityItem(
    @SerialName("object") val obj: String = "following_activity_item",
    val id: String = "",
    @SerialName("object_kind") val objectKind: String = "",
    @SerialName("object_label") val objectLabel: String = "",
    val title: String = "",
    val excerpt: String = "",
    val cover: ImageInfo? = null,
    @SerialName("in_site_path") val inSitePath: String? = null,
    @SerialName("is_nsfw") val isNsfw: Boolean = false,
)

@Serializable
data class FollowingReadMarker(
    @SerialName("object") val obj: String = "following_activity_read_marker",
    @SerialName("last_seen_at") val lastSeenAt: String? = null,
)

/** 话题搜索结果就是话题摘要，直接复用 [Topic]。 */
@Serializable
data class ReplySearchHit(
    @SerialName("object") val obj: String = "reply",
    val id: String = "",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("topic_title") val topicTitle: String = "",
    val floor: Int = 0,
    val excerpt: String = "",
    val author: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
)

@Serializable
data class CommentSearchHit(
    @SerialName("object") val obj: String = "comment",
    val id: String = "",
    @SerialName("topic_id") val topicId: String = "",
    @SerialName("topic_title") val topicTitle: String = "",
    val excerpt: String = "",
    val author: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
)

/** 图片上传成功后返回的图像记录（POST /images）。purpose=content / message。 */
@Serializable
data class UploadedImage(
    @SerialName("object") val obj: String = "image",
    val url: String = "",
    val hash: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val thumbhash: String? = null,
    val sexual: String? = null,
)
