package com.kungal.app.ui.chat

import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.ChatMessage
import com.kungal.core.model.ChatUser
import com.kungal.core.net.KungalApiException
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/** 私信界面用到的文案、时间与图片地址规则（对齐网页 chat 实现）。 */
object ChatCatalog {

    /** 编辑窗口：消息发出 48 小时内。 */
    const val EDIT_WINDOW_MS = 48L * 3600L * 1000L

    val reportReasons: List<Pair<String, String>> = listOf(
        "spam" to "垃圾广告",
        "harassment" to "骚扰辱骂",
        "sexual" to "色情内容",
        "violence" to "暴力内容",
        "illegal" to "违法内容",
        "other" to "其他",
    )

    val allowIncomingOptions: List<Pair<String, String>> = listOf(
        "all" to "所有人",
        "following" to "我关注的人",
        "none" to "没有人",
    )

    fun allowLabel(value: String): String =
        allowIncomingOptions.firstOrNull { it.first == value }?.second ?: value

    fun user(users: Map<String, ChatUser>, id: String?): ChatUser? = id?.let { users[it] }

    fun displayName(user: ChatUser?): String {
        if (user == null) return "已注销用户"
        if (user.deleted) return "已注销用户"
        return user.name?.takeIf { it.isNotBlank() } ?: "已注销用户"
    }

    fun userName(users: Map<String, ChatUser>, id: String): String =
        displayName(users[id])

    /** 列表预览：图片/系统消息给出占位文案。 */
    fun preview(message: ChatMessage?, meId: String?): String {
        if (message == null) return "还没有消息"
        if (message.kind == "service") return "[系统消息]"
        val body = when {
            message.text.isNotBlank() -> message.text
            message.media != null -> "[图片]"
            else -> "[消息]"
        }.replace('\n', ' ')
        return if (meId != null && message.senderId == meId) "你：$body" else body
    }

    /** 聊天图片：优先用服务端下发的 url；否则按内容寻址 CDN 规则拼接 hash。 */
    fun imageUrl(hash: String?, url: String?): String? {
        if (!url.isNullOrBlank()) return url
        if (hash.isNullOrBlank() || hash.length < 6) return null
        return "https://image.kungal.iloveren.link/${hash.substring(0, 2)}/${hash.substring(2, 4)}/$hash.webp"
    }

    /** 消息列表时间：今天显示时分，其余显示月日。 */
    fun messageTime(iso: String): String {
        if (iso.isBlank()) return ""
        return runCatching {
            val time = Instant.parse(iso)
            val local = time.atZone(ZoneId.systemDefault())
            val today = Instant.now().atZone(ZoneId.systemDefault()).toLocalDate()
            if (local.toLocalDate() == today) {
                DateTimeFormatter.ofPattern("HH:mm").format(local)
            } else {
                DateTimeFormatter.ofPattern("MM-dd HH:mm").format(local)
            }
        }.getOrDefault(iso)
    }

    fun canEdit(message: ChatMessage, meId: String?): Boolean {
        if (message.kind != "message" || message.text.isBlank()) return false
        if (meId == null || message.senderId != meId) return false
        return runCatching {
            val created = Instant.parse(message.createdAt)
            Duration.between(created, Instant.now()).toMillis() < EDIT_WINDOW_MS
        }.getOrDefault(false)
    }

    /** 会话静音：muted_until 在未来。 */
    fun isMuted(mutedUntil: String?): Boolean =
        mutedUntil != null && runCatching {
            Instant.parse(mutedUntil).isAfter(Instant.now())
        }.getOrDefault(false)

    /** 私信专用错误文案（网页 `shared/utils/api/chat.ts` 的同一组机器码）。 */
    fun errorText(e: Throwable): String {
        val code = (e as? KungalApiException)?.problem?.code
        return when (code) {
            "CHAT_BLOCKED" -> "你们之间有拉黑关系，无法互发私信"
            "CHAT_NOT_ACCEPTING" -> "对方暂不接受你的私信，或你的账号注册未满 72 小时"
            "CHAT_REQUEST_LIMIT" -> "对方接受私信请求前，你只能发送最多 3 条不含链接和图片的文字消息"
            "CHAT_EDIT_WINDOW_CLOSED" -> "消息发出超过 48 小时后不能再编辑"
            "CHAT_NOT_PERMITTED" -> "你不能对这条消息这样做"
            "SCOPE_REQUIRED" -> "这次登录还没有授权私信，请退出后在网页重新登录"
            else -> friendlyError(e)
        }
    }
}
