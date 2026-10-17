package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.ChatConversation
import com.kungal.core.model.ChatConversationPage
import com.kungal.core.model.ChatDeleted
import com.kungal.core.model.ChatDialog
import com.kungal.core.model.ChatEntity
import com.kungal.core.model.ChatMessage
import com.kungal.core.model.ChatMessageReactions
import com.kungal.core.model.ChatMessagePage
import com.kungal.core.model.ChatPhoto
import com.kungal.core.model.ChatReactionOption
import com.kungal.core.model.ChatReactionOptions
import com.kungal.core.model.ChatReadState
import com.kungal.core.model.ChatRealtimeToken
import com.kungal.core.model.ChatSettings
import com.kungal.core.model.ChatState
import com.kungal.core.model.ChatUpdatesPage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 私信（NextMoe chat 的 REST 部分，站点代理在 /api/v1/chat/ 下）。
 * 实时通道（WebSocket / updates 轮询）属于 U5；这里只覆盖会话、历史、发送与设置。
 */
class ChatApi(private val client: KungalClient) {

    @Serializable
    private data class MediaBody(
        val type: String = "photo",
        @SerialName("image_hash") val imageHash: String,
    )

    @Serializable
    private data class SendBody(
        @SerialName("client_message_id") val clientMessageId: String,
        val text: String,
        val entities: List<ChatEntity> = emptyList(),
        @SerialName("reply_to_seq") val replyToSeq: Long? = null,
        @SerialName("media") val media: MediaBody? = null,
    )

    @Serializable
    private data class EditBody(
        val text: String,
        val entities: List<ChatEntity> = emptyList(),
    )

    @Serializable
    private data class DeleteBody(
        val seqs: List<Long>,
        @SerialName("for_everyone") val forEveryone: Boolean,
    )

    @Serializable
    private data class ReadBody(
        @SerialName("max_seq") val maxSeq: Long,
    )

    @Serializable
    private data class DialogPatch(
        val archived: Boolean? = null,
        @SerialName("marked_unread") val markedUnread: Boolean? = null,
        val muted: Boolean? = null,
        val pinned: Boolean? = null,
    )

    @Serializable
    private data class SettingsPatch(
        @SerialName("allow_incoming") val allowIncoming: String? = null,
        @SerialName("accept_requests") val acceptRequests: Boolean? = null,
        @SerialName("allow_group_invites") val allowGroupInvites: String? = null,
    )

    @Serializable
    private data class ClearHistoryBody(
        val remove: Boolean,
    )

    @Serializable
    private data class ReportBody(
        val reason: String,
        val note: String? = null,
    )

    suspend fun state(): ChatState = client.get(
        path = "/chat/state",
        deserializer = ChatState.serializer(),
    )

    /** folder：inbox（全部）/ requests（消息请求）/ archive（已归档）。 */
    suspend fun conversations(
        folder: String = "inbox",
        cursor: String? = null,
        limit: Int = 30,
    ): ChatConversationPage = client.get(
        path = "/chat/conversations",
        query = mapOf("folder" to folder, "cursor" to cursor, "limit" to limit),
        deserializer = ChatConversationPage.serializer(),
    )

    suspend fun conversation(id: String): ChatConversation = client.get(
        path = "/chat/conversations/$id",
        deserializer = ChatConversation.serializer(),
    )

    /** 历史消息：before_seq 向前翻页；不传参数取最新一页。 */
    suspend fun messages(
        conversationId: String,
        beforeSeq: Long? = null,
        afterSeq: Long? = null,
        aroundSeq: Long? = null,
        limit: Int = 50,
    ): ChatMessagePage = client.get(
        path = "/chat/conversations/$conversationId/messages",
        query = mapOf(
            "limit" to limit,
            "before_seq" to beforeSeq,
            "after_seq" to afterSeq,
            "around_seq" to aroundSeq,
        ),
        deserializer = ChatMessagePage.serializer(),
    )

    /**
     * 发送消息。client_message_id 是客户端生成的幂等标识（重复提交由服务端去重），
     * 失败重试应沿用同一个 id。
     */
    suspend fun send(
        conversationId: String,
        clientMessageId: String,
        text: String,
        entities: List<ChatEntity> = emptyList(),
        replyToSeq: Long? = null,
        photo: ChatPhoto? = null,
    ): ChatMessage {
        val body = KungalJson.encodeToString(
            SendBody.serializer(),
            SendBody(
                clientMessageId = clientMessageId,
                text = text,
                entities = entities,
                replyToSeq = replyToSeq,
                media = photo?.let { MediaBody(imageHash = it.imageHash) },
            ),
        )
        return client.postJson(
            path = "/chat/conversations/$conversationId/messages",
            body = body,
            deserializer = ChatMessage.serializer(),
        )
    }

    /** 编辑自己的消息（站点限制发出后 48 小时内）。 */
    suspend fun edit(messageId: String, text: String): ChatMessage {
        val body = KungalJson.encodeToString(EditBody.serializer(), EditBody(text))
        return client.patchJson(
            path = "/chat/messages/$messageId",
            body = body,
            deserializer = ChatMessage.serializer(),
        )
    }

    /** 删除消息；forEveryone=true 时同时为对方删除（只能用于自己的消息）。 */
    suspend fun deleteMessages(
        conversationId: String,
        seqs: List<Long>,
        forEveryone: Boolean,
    ): ChatDeleted {
        val body = KungalJson.encodeToString(
            DeleteBody.serializer(),
            DeleteBody(seqs = seqs, forEveryone = forEveryone),
        )
        return client.postJson(
            path = "/chat/conversations/$conversationId/messages/delete",
            body = body,
            deserializer = ChatDeleted.serializer(),
        )
    }

    suspend fun markRead(conversationId: String, maxSeq: Long): ChatReadState {
        val body = KungalJson.encodeToString(ReadBody.serializer(), ReadBody(maxSeq))
        return client.postJson(
            path = "/chat/conversations/$conversationId/read",
            body = body,
            deserializer = ChatReadState.serializer(),
        )
    }

    /** 接受消息请求（无响应体）。 */
    suspend fun accept(conversationId: String) =
        client.postNoBody("/chat/conversations/$conversationId/accept")

    suspend fun setDialog(
        conversationId: String,
        archived: Boolean? = null,
        markedUnread: Boolean? = null,
        muted: Boolean? = null,
        pinned: Boolean? = null,
    ): ChatDialog {
        val body = KungalJson.encodeToString(
            DialogPatch.serializer(),
            DialogPatch(archived = archived, markedUnread = markedUnread, muted = muted, pinned = pinned),
        )
        return client.patchJson(
            path = "/chat/conversations/$conversationId/me",
            body = body,
            deserializer = ChatDialog.serializer(),
        )
    }

    /** 清空聊天记录（只对自己生效）；remove=true 用于拒绝并移除消息请求。 */
    suspend fun clearHistory(conversationId: String, remove: Boolean = false) {
        val body = KungalJson.encodeToString(ClearHistoryBody.serializer(), ClearHistoryBody(remove))
        client.postJsonNoContent(
            path = "/chat/conversations/$conversationId/clear-history",
            body = body,
        )
    }

    suspend fun report(messageId: String, reason: String, note: String? = null) {
        val body = KungalJson.encodeToString(
            ReportBody.serializer(),
            ReportBody(reason = reason, note = note?.takeIf { it.isNotBlank() }),
        )
        client.postJsonNoContent(path = "/chat/messages/$messageId/report", body = body)
    }

    suspend fun settings(): ChatSettings = client.get(
        path = "/chat/settings",
        deserializer = ChatSettings.serializer(),
    )

    suspend fun updateSettings(
        allowIncoming: String? = null,
        acceptRequests: Boolean? = null,
        allowGroupInvites: String? = null,
    ): ChatSettings {
        val body = KungalJson.encodeToString(
            SettingsPatch.serializer(),
            SettingsPatch(
                allowIncoming = allowIncoming,
                acceptRequests = acceptRequests,
                allowGroupInvites = allowGroupInvites,
            ),
        )
        return client.patchJson(
            path = "/chat/settings",
            body = body,
            deserializer = ChatSettings.serializer(),
        )
    }

    /** 打开（必要时创建）与某个用户的私信会话。 */
    suspend fun openDirect(userId: String): ChatConversation = client.put(
        path = "/chat/direct/$userId",
        deserializer = ChatConversation.serializer(),
    )

    /** 上传聊天图片（multipart，字段名 file），返回图片 hash 供发送消息引用。 */
    suspend fun uploadPhoto(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
    ): ChatPhoto = client.postMultipart(
        path = "/chat/images",
        fileBytes = bytes,
        fileName = fileName,
        contentType = contentType,
        deserializer = ChatPhoto.serializer(),
    )

    // ---- U5：实时通道、回应与置顶 ----

    /** 获取 Centrifugo 连接凭证（token 约 15 分钟有效，过期需重新获取）。 */
    suspend fun realtimeToken(): ChatRealtimeToken = client.postNoBodyJson(
        path = "/chat/realtime-token",
        deserializer = ChatRealtimeToken.serializer(),
    )

    /** 断线补齐：after 为已处理到的 last_update_seq。 */
    suspend fun updates(after: Long, limit: Int = 100): ChatUpdatesPage = client.get(
        path = "/chat/updates",
        query = mapOf("after" to after, "limit" to limit),
        deserializer = ChatUpdatesPage.serializer(),
    )

    /** 输入中提示（服务端按会话 5 秒节流；无响应体）。 */
    suspend fun typing(conversationId: String) =
        client.postNoBody("/chat/conversations/$conversationId/typing")

    /** 可选表情回应列表（33 种）。 */
    suspend fun reactions(): List<ChatReactionOption> = client.get(
        path = "/chat/reactions",
        deserializer = ChatReactionOptions.serializer(),
    ).items

    /** 设置或取消（reaction=null）自己对该消息的回应，返回最新汇总。 */
    suspend fun react(messageId: String, reaction: String?): ChatMessageReactions {
        // 取消回应必须显式发 {"reaction":null}：KungalJson 的 explicitNulls=false
        // 会把 null 字段整个省掉，服务端就理解成「没改」。
        val body = buildJsonObject {
            put("reaction", reaction?.let(::JsonPrimitive) ?: JsonNull)
        }.toString()
        return client.putJson(
            path = "/chat/messages/$messageId/reaction",
            body = body,
            deserializer = ChatMessageReactions.serializer(),
        )
    }

    /** 置顶消息（无响应体）。 */
    suspend fun pin(conversationId: String, seq: Long) =
        client.putNoBody("/chat/conversations/$conversationId/pins/$seq")

    /** 取消置顶（无响应体）。 */
    suspend fun unpin(conversationId: String, seq: Long) =
        client.deleteNoBody("/chat/conversations/$conversationId/pins/$seq")
}
