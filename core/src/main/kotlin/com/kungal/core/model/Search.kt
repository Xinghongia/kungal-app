package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 用户搜索结果（GET /search/users）。 */
@Serializable
data class UserSearchHit(
    @SerialName("object") val obj: String = "user",
    val id: String = "",
    val name: String? = null,
    val avatar: ImageInfo? = null,
    @SerialName("avatar_frame") val avatarFrame: AvatarFrame? = null,
    val bio: String? = null,
    @SerialName("registered_at") val registeredAt: String? = null,
    @SerialName("reply_count") val replyCount: Int = 0,
    @SerialName("topic_count") val topicCount: Int = 0,
    val roles: List<String> = emptyList(),
)

/** Galgame 留言墙搜索结果（GET /search/wall-comments）。 */
@Serializable
data class WallCommentSearchHit(
    @SerialName("object") val obj: String = "wall_comment",
    val id: String = "",
    @SerialName("subject_type") val subjectType: String = "",
    @SerialName("subject_id") val subjectId: String = "",
    /** 站内路径，例如 /galgame/11287、/toolset/12。 */
    @SerialName("subject_path") val subjectPath: String = "",
    val work: WorkRef? = null,
    val excerpt: String = "",
    val author: UserRef = UserRef(),
    @SerialName("created_at") val createdAt: String = "",
)
