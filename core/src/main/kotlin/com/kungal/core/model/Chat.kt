package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** 私信用户（聊天响应的 users 数组）。已注销账号 name 为空、deleted=true。 */
@Serializable
data class ChatUser(
    @SerialName("object") val obj: String = "user",
    val id: String = "",
    val name: String? = null,
    val avatar: String? = null,
    val deleted: Boolean = false,
)

/** 富文本片段：@提及、链接、代码等。U4 只按纯文本展示，保留字段供后续渲染。 */
@Serializable
data class ChatEntity(
    val type: String = "",
    val offset: Int = 0,
    val length: Int = 0,
    @SerialName("user_id") val userId: String? = null,
    val url: String? = null,
    val language: String? = null,
)

@Serializable
data class ChatPhoto(
    @SerialName("object") val obj: String = "chat_image",
    val type: String = "photo",
    @SerialName("image_hash") val imageHash: String = "",
    val width: Int = 0,
    val height: Int = 0,
    val thumbhash: String? = null,
    val url: String? = null,
)

/** 被回复消息的摘要（服务端随消息下发，无需另查）。 */
@Serializable
data class ChatReplyTo(
    val seq: Long = 0,
    @SerialName("sender_id") val senderId: String = "",
    val text: String = "",
    val entities: List<ChatEntity> = emptyList(),
    @SerialName("media_type") val mediaType: String? = null,
    val deleted: Boolean = false,
)

@Serializable
data class ChatReplyQuote(
    val text: String = "",
    val entities: List<ChatEntity> = emptyList(),
    val offset: Int = 0,
)

@Serializable
data class ChatReaction(
    val reaction: String = "",
    val count: Int = 0,
    val reacted: Boolean = false,
)

/** 一条私信。seq 是会话内从 1 开始、无空洞的位置号。 */
@Serializable
data class ChatMessage(
    @SerialName("object") val obj: String = "message",
    val id: String = "",
    @SerialName("conversation_id") val conversationId: String = "",
    val seq: Long = 0,
    @SerialName("sender_id") val senderId: String = "",
    val kind: String = "message",
    val text: String = "",
    val entities: List<ChatEntity> = emptyList(),
    val media: ChatPhoto? = null,
    @SerialName("media_group_id") val mediaGroupId: String? = null,
    @SerialName("reply_to") val replyTo: ChatReplyTo? = null,
    @SerialName("reply_quote") val replyQuote: ChatReplyQuote? = null,
    val reactions: List<ChatReaction> = emptyList(),
    val silent: Boolean = false,
    @SerialName("pinned_at") val pinnedAt: String? = null,
    @SerialName("edited_at") val editedAt: String? = null,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("client_message_id") val clientMessageId: String? = null,
    /** 部分响应（发送/编辑）随消息带 users，供界面合并用户名与头像。 */
    val users: List<ChatUser> = emptyList(),
)

@Serializable
data class ChatDraft(
    val text: String = "",
    val entities: List<ChatEntity> = emptyList(),
    @SerialName("reply_to_seq") val replyToSeq: Long? = null,
    @SerialName("updated_at") val updatedAt: String = "",
)

/** 当前账号在这个会话里的状态。 */
@Serializable
data class ChatDialog(
    val role: String = "member",
    val accepted: Boolean = true,
    @SerialName("last_read_seq") val lastReadSeq: Long = 0,
    @SerialName("unread_count") val unreadCount: Int = 0,
    @SerialName("marked_unread") val markedUnread: Boolean = false,
    @SerialName("cleared_through_seq") val clearedThroughSeq: Long = 0,
    @SerialName("visible_from_seq") val visibleFromSeq: Long = 0,
    @SerialName("muted_until") val mutedUntil: String? = null,
    val archived: Boolean = false,
    @SerialName("pinned_rank") val pinnedRank: Int? = null,
    val draft: ChatDraft? = null,
)

@Serializable
data class ChatConversation(
    @SerialName("object") val obj: String = "conversation",
    val id: String = "",
    val kind: String = "direct",
    val title: String? = null,
    val about: String? = null,
    @SerialName("photo_image_hash") val photoImageHash: String? = null,
    @SerialName("peer_id") val peerId: String? = null,
    @SerialName("member_count") val memberCount: Int = 1,
    @SerialName("last_seq") val lastSeq: Long = 0,
    @SerialName("peer_read_seq") val peerReadSeq: Long = 0,
    @SerialName("created_at") val createdAt: String = "",
    val me: ChatDialog = ChatDialog(),
    @SerialName("last_message") val lastMessage: ChatMessage? = null,
    @SerialName("pinned_seqs") val pinnedSeqs: List<Long> = emptyList(),
    @SerialName("pinned_messages") val pinnedMessages: List<ChatMessage> = emptyList(),
    val users: List<ChatUser> = emptyList(),
) {
    /** 会话归属：未接受 = 消息请求；已归档 = 归档；其余 = 收件箱。 */
    val folder: String
        get() = when {
            !me.accepted -> "requests"
            me.archived -> "archive"
            else -> "inbox"
        }
}

@Serializable
data class ChatConversationPage(
    @SerialName("object") val obj: String = "list",
    val items: List<ChatConversation> = emptyList(),
    val users: List<ChatUser> = emptyList(),
    @SerialName("next_cursor") val nextCursor: String? = null,
)

@Serializable
data class ChatMessagePage(
    @SerialName("object") val obj: String = "list",
    val items: List<ChatMessage> = emptyList(),
    val users: List<ChatUser> = emptyList(),
    @SerialName("has_more_before") val hasMoreBefore: Boolean = false,
    @SerialName("has_more_after") val hasMoreAfter: Boolean = false,
)

@Serializable
data class ChatState(
    @SerialName("object") val obj: String = "chat_state",
    @SerialName("last_update_seq") val lastUpdateSeq: Long = 0,
    @SerialName("unread_conversation_count") val unreadConversationCount: Int = 0,
    @SerialName("unread_message_count") val unreadMessageCount: Int = 0,
    @SerialName("request_count") val requestCount: Int = 0,
)

@Serializable
data class ChatSettings(
    @SerialName("object") val obj: String = "chat_settings",
    @SerialName("allow_incoming") val allowIncoming: String = "following",
    @SerialName("accept_requests") val acceptRequests: Boolean = true,
    @SerialName("allow_group_invites") val allowGroupInvites: String = "following",
)

@Serializable
data class ChatReadState(
    @SerialName("object") val obj: String = "read_state",
    @SerialName("conversation_id") val conversationId: String = "",
    @SerialName("last_read_seq") val lastReadSeq: Long = 0,
    @SerialName("unread_count") val unreadCount: Int = 0,
)

@Serializable
data class ChatDeleted(
    @SerialName("object") val obj: String = "deleted_messages",
    val seqs: List<Long> = emptyList(),
    @SerialName("for_everyone") val forEveryone: Boolean = false,
)

/** 可选表情回应（GET /chat/reactions 返回 33 种）。 */
@Serializable
data class ChatReactionOption(
    val key: String = "",
    val emoji: String = "",
    val label: String = "",
)

@Serializable
data class ChatReactionOptions(
    @SerialName("object") val obj: String = "list",
    val items: List<ChatReactionOption> = emptyList(),
)

/** 设置/取消某条消息的回应后的最新汇总。 */
@Serializable
data class ChatMessageReactions(
    @SerialName("object") val obj: String = "message_reactions",
    @SerialName("message_id") val messageId: String = "",
    val reactions: List<ChatReaction> = emptyList(),
)

/** 实时通道连接凭证（Centrifugo）：url + token + 过期时间。 */
@Serializable
data class ChatRealtimeToken(
    @SerialName("object") val obj: String = "realtime_token",
    val token: String = "",
    @SerialName("expires_at") val expiresAt: String = "",
    val url: String = "",
)

/**
 * 一条增量更新。data 的字段随 kind 变化（seq / seqs / max_seq / unread_count /
 * through_seq / removed / pinned / action / user_id），保持 JsonObject 兼容新增字段。
 */
@Serializable
data class ChatUpdate(
    @SerialName("object") val obj: String = "update",
    @SerialName("update_seq") val updateSeq: Long = 0,
    val kind: String = "",
    @SerialName("conversation_id") val conversationId: String = "",
    val data: JsonObject = JsonObject(emptyMap()),
    @SerialName("created_at") val createdAt: String = "",
)

/** 断线补齐：/chat/updates?after=<seq> 返回一段时间内的全部增量。 */
@Serializable
data class ChatUpdatesPage(
    @SerialName("object") val obj: String = "update_list",
    val updates: List<ChatUpdate> = emptyList(),
    val messages: List<ChatMessage> = emptyList(),
    val conversations: List<ChatConversation> = emptyList(),
    val users: List<ChatUser> = emptyList(),
    @SerialName("last_update_seq") val lastUpdateSeq: Long = 0,
    @SerialName("has_more") val hasMore: Boolean = false,
    @SerialName("too_long") val tooLong: Boolean = false,
)

/** WebSocket 推送：type=update（增量 + 可选的完整消息/回应）或 type=typing。 */
@Serializable
data class ChatPush(
    val type: String = "",
    val update: ChatUpdate? = null,
    val message: ChatMessage? = null,
    val reactions: List<ChatReaction>? = null,
    @SerialName("conversation_id") val conversationId: String? = null,
    @SerialName("user_id") val userId: String? = null,
)
