package com.kungal.core.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.buildClassSerialDescriptor
import kotlinx.serialization.descriptors.element
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonContentPolymorphicSerializer
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * 正文文档树（ContentDocument）：服务端把 Markdown 转成块/行内节点树下发
 * （apps/api/internal/apiv1/content/node.go 是权威定义）。
 *
 * 客户端模型刻意宽松：块与行内节点共用一个 sealed 层级，children 允许任意节点；
 * 未知的 object 类型落到 [UnknownNode] 而不是解析失败——站点会增加节点类型，渲染端跳过即可。
 */
@Serializable
data class ContentDocument(
    @SerialName("object") val obj: String = "document",
    val children: List<ContentNode> = emptyList(),
)

@Serializable(with = ContentNodeSerializer::class)
sealed class ContentNode

@Serializable
@SerialName("paragraph")
data class ParagraphNode(val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("heading")
data class HeadingNode(
    val depth: Int = 2,
    val anchor: String = "",
    val children: List<ContentNode> = emptyList(),
) : ContentNode()

@Serializable
@SerialName("thematic_break")
data object ThematicBreakNode : ContentNode()

@Serializable
@SerialName("blockquote")
data class BlockquoteNode(val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("list")
data class ListNode(
    @SerialName("is_ordered") val isOrdered: Boolean = false,
    val start: Int? = null,
    @SerialName("is_spread") val isSpread: Boolean = false,
    val children: List<ContentNode> = emptyList(),
) : ContentNode()

@Serializable
@SerialName("list_item")
data class ListItemNode(
    @SerialName("is_checked") val isChecked: Boolean? = null,
    val children: List<ContentNode> = emptyList(),
) : ContentNode()

@Serializable
@SerialName("code")
data class CodeNode(val lang: String? = null, val value: String = "") : ContentNode()

@Serializable
@SerialName("math")
data class MathNode(val value: String = "") : ContentNode()

@Serializable
@SerialName("table")
data class TableNode(val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("table_row")
data class TableRowNode(val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("table_cell")
data class TableCellNode(val align: String? = null, val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("spoiler")
data class SpoilerNode(val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("text")
data class TextNode(val value: String = "") : ContentNode()

@Serializable
@SerialName("emphasis")
data class EmphasisNode(val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("strong")
data class StrongNode(val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("strikethrough")
data class StrikethroughNode(val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("inline_code")
data class InlineCodeNode(val value: String = "") : ContentNode()

@Serializable
@SerialName("inline_math")
data class InlineMathNode(val value: String = "") : ContentNode()

@Serializable
@SerialName("break")
data object BreakNode : ContentNode()

@Serializable
@SerialName("link")
data class LinkNode(val url: String = "", val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("image")
data class ImageNode(
    val url: String = "",
    val alt: String = "",
    val image: ImageInfo? = null,
    @SerialName("is_sticker") val isSticker: Boolean = false,
) : ContentNode()

@Serializable
@SerialName("video")
data class VideoNode(val url: String = "") : ContentNode()

@Serializable
@SerialName("inline_spoiler")
data class InlineSpoilerNode(val children: List<ContentNode> = emptyList()) : ContentNode()

@Serializable
@SerialName("mention")
data class MentionNode(@SerialName("mentioned_user") val mentionedUser: UserRef = UserRef()) : ContentNode()

@Serializable
@SerialName("reply_reference")
data class ReplyReferenceNode(
    @SerialName("reply_id") val replyId: String = "",
    val floor: Int = 0,
) : ContentNode()

/** 服务端新增的、本版本不认识的节点；保留原始 JSON，渲染端跳过。 */
data class UnknownNode(val objectName: String, val raw: JsonElement) : ContentNode()

object UnknownNodeSerializer : KSerializer<UnknownNode> {
    override val descriptor: SerialDescriptor =
        buildClassSerialDescriptor("com.kungal.core.model.UnknownNode") {
            element<String>("objectName")
            element<JsonElement>("raw")
        }

    override fun serialize(encoder: Encoder, value: UnknownNode) {
        encoder.encodeSerializableValue(JsonElement.serializer(), value.raw)
    }

    override fun deserialize(decoder: Decoder): UnknownNode {
        val element = decoder.decodeSerializableValue(JsonElement.serializer())
        val name = (element as? JsonObject)
            ?.get("object")
            ?.jsonPrimitive
            ?.contentOrNull
            ?: "unknown"
        return UnknownNode(name, element)
    }
}

object ContentNodeSerializer : JsonContentPolymorphicSerializer<ContentNode>(ContentNode::class) {
    override fun selectDeserializer(element: JsonElement): DeserializationStrategy<ContentNode> {
        return when (element.jsonObject["object"]?.jsonPrimitive?.contentOrNull) {
            "paragraph" -> ParagraphNode.serializer()
            "heading" -> HeadingNode.serializer()
            "thematic_break" -> ThematicBreakNode.serializer()
            "blockquote" -> BlockquoteNode.serializer()
            "list" -> ListNode.serializer()
            "list_item" -> ListItemNode.serializer()
            "code" -> CodeNode.serializer()
            "math" -> MathNode.serializer()
            "table" -> TableNode.serializer()
            "table_row" -> TableRowNode.serializer()
            "table_cell" -> TableCellNode.serializer()
            "spoiler" -> SpoilerNode.serializer()
            "text" -> TextNode.serializer()
            "emphasis" -> EmphasisNode.serializer()
            "strong" -> StrongNode.serializer()
            "strikethrough" -> StrikethroughNode.serializer()
            "inline_code" -> InlineCodeNode.serializer()
            "inline_math" -> InlineMathNode.serializer()
            "break" -> BreakNode.serializer()
            "link" -> LinkNode.serializer()
            "image" -> ImageNode.serializer()
            "video" -> VideoNode.serializer()
            "inline_spoiler" -> InlineSpoilerNode.serializer()
            "mention" -> MentionNode.serializer()
            "reply_reference" -> ReplyReferenceNode.serializer()
            else -> UnknownNodeSerializer
        }
    }
}
