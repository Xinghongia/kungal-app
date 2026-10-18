package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * 帮助文档（GET /docs 返回摘要，GET /docs/{slug} 额外带 [content]）。
 * 正文由服务端转成块/行内节点树；目录直接取其中的 heading 节点。
 */
@Serializable
data class Doc(
    @SerialName("object") val obj: String = "doc",
    val id: String = "",
    val slug: String = "",
    val title: String = "",
    val description: String = "",
    @SerialName("doc_category") val docCategory: String = "",
    val banner: ImageInfo? = null,
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("view_count") val viewCount: Int = 0,
    @SerialName("published_at") val publishedAt: String = "",
    @SerialName("edited_at") val editedAt: String? = null,
    val author: UserRef = UserRef(),
    val content: ContentDocument? = null,
)
