package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 内容寻址图片记录（repr.Image）。宽度/高度/缩略图可缺省。 */
@Serializable
data class ImageInfo(
    val url: String = "",
    val hash: String = "",
    val width: Int? = null,
    val height: Int? = null,
    val thumbhash: String? = null,
    val sexual: String? = null,
)

/** 轻量用户引用（repr.UserRef）：作者、反应者、@提及都用它。 */
@Serializable
data class UserRef(
    @SerialName("object") val obj: String = "user",
    val id: String = "",
    val name: String? = null,
    val avatar: ImageInfo? = null,
    @SerialName("avatar_frame") val avatarFrame: AvatarFrame? = null,
)

/** 反应汇总（ReactionSummary）：一种反应 token 的计数与参与人。token 词汇会增长，未知值按中性处理。 */
@Serializable
data class ReactionSummary(
    val reaction: String = "",
    val count: Int = 0,
    val reactors: List<UserRef> = emptyList(),
    val viewer: ReactionViewer? = null,
)

@Serializable
data class ReactionViewer(
    @SerialName("has_reacted") val hasReacted: Boolean = false,
)
