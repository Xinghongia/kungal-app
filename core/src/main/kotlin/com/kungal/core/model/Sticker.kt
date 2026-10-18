package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 表情包（GET /sticker-packs）：站点 editor 与私信共用的官方表情包。 */
@Serializable
data class StickerPackList(
    @SerialName("object") val obj: String = "list",
    val items: List<StickerPack> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
) {
    val stickers: List<Sticker> get() = items.flatMap { it.stickers }
}

@Serializable
data class StickerPack(
    @SerialName("object") val obj: String = "sticker_pack",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val stickers: List<Sticker> = emptyList(),
)

@Serializable
data class Sticker(
    @SerialName("object") val obj: String = "sticker",
    val id: String = "",
    @SerialName("display_name") val displayName: String = "",
    val image: ImageInfo? = null,
)

/** 正文里插入的是 320 像素变体（与网页一致）。 */
fun stickerContentUrl(image: ImageInfo): String =
    image.url.replace(Regex("""\.webp$"""), "_320.webp")

fun stickerMarkdown(sticker: Sticker): String {
    val image = sticker.image ?: return ""
    return "![${sticker.displayName}](${stickerContentUrl(image)})"
}
