package com.kungal.app.ui.chat

import com.kungal.core.model.ChatConversation
import com.kungal.core.model.ChatDialog
import com.kungal.core.model.ChatMessage
import com.kungal.core.model.ChatPhoto
import com.kungal.core.model.ChatUser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.temporal.ChronoUnit

class ChatCatalogTest {

    private fun message(
        text: String = "你好",
        senderId: String = "5",
        createdAt: String = Instant.now().toString(),
        media: ChatPhoto? = null,
        kind: String = "message",
    ) = ChatMessage(
        id = "1",
        conversationId = "77",
        seq = 3,
        senderId = senderId,
        kind = kind,
        text = text,
        media = media,
        createdAt = createdAt,
    )

    @Test
    fun `列表预览区分自己与他人及图片消息`() {
        assertEquals("你好", ChatCatalog.preview(message(), meId = "9"))
        assertEquals("你：你好", ChatCatalog.preview(message(), meId = "5"))
        assertEquals(
            "[图片]",
            ChatCatalog.preview(message(text = "", media = ChatPhoto(imageHash = "abc")), meId = "9"),
        )
        assertEquals("[系统消息]", ChatCatalog.preview(message(kind = "service"), meId = "9"))
        assertEquals("还没有消息", ChatCatalog.preview(null, meId = "9"))
    }

    @Test
    fun `编辑窗口为发出后 48 小时`() {
        val now = Instant.now()
        assertTrue(ChatCatalog.canEdit(message(createdAt = now.minus(1, ChronoUnit.HOURS).toString()), "5"))
        assertTrue(ChatCatalog.canEdit(message(createdAt = now.minus(47, ChronoUnit.HOURS).toString()), "5"))
        assertFalse(ChatCatalog.canEdit(message(createdAt = now.minus(49, ChronoUnit.HOURS).toString()), "5"))
        assertFalse(ChatCatalog.canEdit(message(), meId = "9"))
        assertFalse(ChatCatalog.canEdit(message(text = ""), meId = "5"))
    }

    @Test
    fun `图片地址优先使用服务端 url 否则按 hash 拼接`() {
        assertEquals(
            "https://image.example/a.webp",
            ChatCatalog.imageUrl("abcdef", "https://image.example/a.webp"),
        )
        assertEquals(
            "https://image.kungal.iloveren.link/ab/cd/abcdef.webp",
            ChatCatalog.imageUrl("abcdef", null),
        )
        assertEquals(null, ChatCatalog.imageUrl(null, null))
        assertEquals(null, ChatCatalog.imageUrl("abc", null))
    }

    @Test
    fun `会话静音以 muted_until 判断`() {
        val now = Instant.now()
        assertTrue(ChatCatalog.isMuted(now.plus(1, ChronoUnit.HOURS).toString()))
        assertFalse(ChatCatalog.isMuted(now.minus(1, ChronoUnit.HOURS).toString()))
        assertFalse(ChatCatalog.isMuted(null))
        assertFalse(ChatCatalog.isMuted("not-a-time"))
    }

    @Test
    fun `文件夹归属与名称回退`() {
        fun conversation(dialog: ChatDialog) = ChatConversation(
            id = "77",
            peerId = "9",
            me = dialog,
        )
        assertEquals("inbox", conversation(ChatDialog()).folder)
        assertEquals("requests", conversation(ChatDialog(accepted = false)).folder)
        assertEquals("archive", conversation(ChatDialog(archived = true)).folder)

        assertEquals("阿瞳", ChatCatalog.displayName(ChatUser(id = "5", name = "阿瞳")))
        assertEquals("已注销用户", ChatCatalog.displayName(ChatUser(id = "5", name = null)))
        assertEquals("已注销用户", ChatCatalog.displayName(ChatUser(id = "5", name = "x", deleted = true)))
        assertEquals("所有人", ChatCatalog.allowLabel("all"))
        assertEquals("我关注的人", ChatCatalog.allowLabel("following"))
    }
}
