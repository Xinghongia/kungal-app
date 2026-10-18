package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 友情链接（GET /friend-links）。分类顺序由客户端按 official → galgame → others 展示。 */
@Serializable
data class FriendLink(
    @SerialName("object") val obj: String = "friend_link",
    val id: String = "",
    @SerialName("friend_link_category") val friendLinkCategory: String = "",
    val title: String = "",
    val url: String = "",
    val description: String = "",
    val banner: ImageInfo? = null,
    val state: String = "normal",
)
