package com.kungal.core

import com.kungal.core.api.ChatApi
import com.kungal.core.model.ChatPhoto
import com.kungal.core.session.Session
import com.kungal.core.session.SessionHolder
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatApiTest {

    private lateinit var server: MockWebServer
    private lateinit var chat: ChatApi

    private val baseUrl: String get() = server.url("/api/v1").toString().trimEnd('/')

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        val session = SessionHolder().apply { restore(Session("kungal_session=t")) }
        chat = ChatApi(KungalClient(baseUrl = baseUrl, session = session))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private fun ok(body: String, code: Int = 200) = MockResponse.Builder()
        .code(code).addHeader("Content-Type", "application/json").body(body).build()

    private fun noContent() = MockResponse.Builder().code(204).build()

    private val messageJson = """
        {"object":"message","id":"9001","conversation_id":"77","seq":12,"sender_id":"5",
         "kind":"message","text":"你好呀","entities":[],"media":null,"media_group_id":null,
         "reply_to":{"seq":11,"sender_id":"9","text":"在吗","entities":[],"media_type":null,"deleted":false},
         "reply_quote":null,"reactions":[],"silent":false,"pinned_at":null,"edited_at":null,
         "created_at":"2026-10-17T05:06:07Z","client_message_id":"cm-1",
         "users":[{"object":"user","id":"5","name":"阿瞳","avatar":"https://img/a.webp"}]}
    """.trimIndent()

    private val conversationJson = """
        {"object":"conversation","id":"77","kind":"direct","title":null,"about":null,
         "photo_image_hash":null,"peer_id":"9","member_count":2,"last_seq":12,"peer_read_seq":10,
         "created_at":"2026-10-01T00:00:00Z",
         "me":{"role":"member","accepted":true,"last_read_seq":12,"unread_count":0,
           "marked_unread":false,"cleared_through_seq":0,"visible_from_seq":1,
           "muted_until":null,"archived":false,"pinned_rank":null,"draft":null},
         "last_message":{"object":"message","id":"9001","conversation_id":"77","seq":12,
           "sender_id":"5","kind":"message","text":"你好呀","entities":[],"media":null,
           "media_group_id":null,"reply_to":null,"reply_quote":null,"reactions":[],
           "silent":false,"pinned_at":null,"edited_at":null,
           "created_at":"2026-10-17T05:06:07Z"}}
    """.trimIndent()

    @Test
    fun `状态与私信设置解析`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"chat_state","last_update_seq":40,
                   "unread_conversation_count":2,"unread_message_count":5,"request_count":1}""",
            ),
        )
        val state = chat.state()
        assertEquals(40, state.lastUpdateSeq)
        assertEquals(2, state.unreadConversationCount)
        assertEquals(5, state.unreadMessageCount)
        assertEquals(1, state.requestCount)
        assertEquals("/api/v1/chat/state", server.takeRequest().url.encodedPath)

        server.enqueue(
            ok(
                """{"object":"chat_settings","allow_incoming":"following",
                   "accept_requests":true,"allow_group_invites":"none"}""",
            ),
        )
        val settings = chat.settings()
        assertEquals("following", settings.allowIncoming)
        assertTrue(settings.acceptRequests)
        assertEquals("none", settings.allowGroupInvites)
        assertEquals("/api/v1/chat/settings", server.takeRequest().url.encodedPath)
    }

    @Test
    fun `会话列表按文件夹查询并解析双方用户`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[$conversationJson],
                   "users":[{"object":"user","id":"9","name":"小龙","avatar":null}],
                   "next_cursor":"cur_c"}""",
            ),
        )
        val page = chat.conversations(folder = "requests", limit = 30)
        assertEquals(1, page.items.size)
        val conversation = page.items[0]
        assertEquals("77", conversation.id)
        assertEquals("9", conversation.peerId)
        assertEquals(12, conversation.lastSeq)
        assertEquals("你好呀", conversation.lastMessage?.text)
        assertEquals("inbox", conversation.folder)
        assertEquals("小龙", page.users[0].name)
        assertEquals("cur_c", page.nextCursor)

        val request = server.takeRequest()
        assertEquals("/api/v1/chat/conversations", request.url.encodedPath)
        assertEquals("requests", request.url.queryParameter("folder"))
        assertEquals("30", request.url.queryParameter("limit"))
    }

    @Test
    fun `未接受或归档的会话归属对应文件夹`() {
        val base = conversationJson
        assertEquals(
            "inbox",
            Json.decodeFromString(
                com.kungal.core.model.ChatConversation.serializer(),
                base,
            ).folder,
        )
        assertEquals(
            "requests",
            Json.decodeFromString(
                com.kungal.core.model.ChatConversation.serializer(),
                base.replace("\"accepted\":true", "\"accepted\":false"),
            ).folder,
        )
        assertEquals(
            "archive",
            Json.decodeFromString(
                com.kungal.core.model.ChatConversation.serializer(),
                base.replace("\"archived\":false", "\"archived\":true"),
            ).folder,
        )
    }

    @Test
    fun `历史消息向前翻页并解析回复与用户`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[$messageJson],
                   "users":[{"object":"user","id":"9","name":"小龙","avatar":""}],
                   "has_more_before":true,"has_more_after":false}""",
            ),
        )
        val page = chat.messages("77", beforeSeq = 12, limit = 50)
        assertEquals(1, page.items.size)
        val message = page.items[0]
        assertEquals(12, message.seq)
        assertEquals("你好呀", message.text)
        assertEquals(11L, message.replyTo?.seq)
        assertEquals("在吗", message.replyTo?.text)
        assertEquals("cm-1", message.clientMessageId)
        assertEquals("阿瞳", message.users[0].name)
        assertTrue(page.hasMoreBefore)
        assertFalse(page.hasMoreAfter)

        val request = server.takeRequest()
        assertEquals("/api/v1/chat/conversations/77/messages", request.url.encodedPath)
        assertEquals("12", request.url.queryParameter("before_seq"))
        assertEquals("50", request.url.queryParameter("limit"))
    }

    @Test
    fun `发送消息带客户端幂等键引用与图片`() = runBlocking {
        server.enqueue(ok(messageJson))
        val message = chat.send(
            conversationId = "77",
            clientMessageId = "cm-1",
            text = "你好呀",
            replyToSeq = 11,
            photo = ChatPhoto(imageHash = "abc123"),
        )
        assertEquals("9001", message.id)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/chat/conversations/77/messages", request.url.encodedPath)
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("cm-1", body["client_message_id"]!!.jsonPrimitive.content)
        assertEquals("你好呀", body["text"]!!.jsonPrimitive.content)
        assertEquals("11", body["reply_to_seq"]!!.jsonPrimitive.content)
        assertEquals("photo", body["media"]!!.jsonObject["type"]!!.jsonPrimitive.content)
        assertEquals("abc123", body["media"]!!.jsonObject["image_hash"]!!.jsonPrimitive.content)
    }

    @Test
    fun `编辑消息发送纯文本`() = runBlocking {
        server.enqueue(ok(messageJson))
        val edited = chat.edit("9001", "改一下")
        assertEquals("9001", edited.id)
        val request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/chat/messages/9001", request.url.encodedPath)
        val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("改一下", body["text"]!!.jsonPrimitive.content)
        assertTrue(body["entities"]!!.toString() == "[]")
    }

    @Test
    fun `删除消息与已读标记的请求形状`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"deleted_messages","seqs":[12],"for_everyone":true}""",
            ),
        )
        val deleted = chat.deleteMessages("77", seqs = listOf(12), forEveryone = true)
        assertEquals(listOf(12L), deleted.seqs)
        assertTrue(deleted.forEveryone)
        var request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/chat/conversations/77/messages/delete", request.url.encodedPath)
        var body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("[12]", body["seqs"]!!.toString())
        assertEquals("true", body["for_everyone"]!!.jsonPrimitive.content)

        server.enqueue(
            ok(
                """{"object":"read_state","conversation_id":"77",
                   "last_read_seq":12,"unread_count":0}""",
            ),
        )
        val read = chat.markRead("77", 12)
        assertEquals(12, read.lastReadSeq)
        request = server.takeRequest()
        assertEquals("/api/v1/chat/conversations/77/read", request.url.encodedPath)
        body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("12", body["max_seq"]!!.jsonPrimitive.content)
    }

    @Test
    fun `接受请求清空举报与设置保存`() = runBlocking {
        server.enqueue(noContent())
        chat.accept("77")
        var request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/chat/conversations/77/accept", request.url.encodedPath)

        server.enqueue(noContent())
        chat.clearHistory("77", remove = true)
        request = server.takeRequest()
        assertEquals("/api/v1/chat/conversations/77/clear-history", request.url.encodedPath)
        var body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("true", body["remove"]!!.jsonPrimitive.content)

        server.enqueue(noContent())
        chat.report("9001", reason = "spam", note = "广告")
        request = server.takeRequest()
        assertEquals("/api/v1/chat/messages/9001/report", request.url.encodedPath)
        body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("spam", body["reason"]!!.jsonPrimitive.content)
        assertEquals("广告", body["note"]!!.jsonPrimitive.content)

        server.enqueue(
            ok(
                """{"object":"chat_settings","allow_incoming":"all",
                   "accept_requests":false,"allow_group_invites":"following"}""",
            ),
        )
        val updated = chat.updateSettings(allowIncoming = "all", acceptRequests = false)
        assertEquals("all", updated.allowIncoming)
        assertFalse(updated.acceptRequests)
        request = server.takeRequest()
        assertEquals("PATCH", request.method)
        assertEquals("/api/v1/chat/settings", request.url.encodedPath)
        body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("all", body["allow_incoming"]!!.jsonPrimitive.content)
        assertEquals("false", body["accept_requests"]!!.jsonPrimitive.content)
        assertFalse(body.containsKey("allow_group_invites"))
    }

    @Test
    fun `打开私信会话与上传图片`() = runBlocking {
        server.enqueue(ok(conversationJson))
        val conversation = chat.openDirect("9")
        assertEquals("77", conversation.id)
        var request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/chat/direct/9", request.url.encodedPath)

        server.enqueue(
            ok(
                """{"object":"chat_image","type":"photo","image_hash":"h1",
                   "width":800,"height":600,"thumbhash":null,
                   "url":"https://image.example/h1.webp"}""",
            ),
        )
        val photo = chat.uploadPhoto(byteArrayOf(1, 2, 3), "a.png", "image/png")
        assertEquals("h1", photo.imageHash)
        assertEquals("https://image.example/h1.webp", photo.url)
        request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/chat/images", request.url.encodedPath)
        assertTrue(request.headers["Content-Type"]!!.startsWith("multipart/form-data"))
    }

    @Test
    fun `实时凭证与断线补齐`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"realtime_token","token":"jwt-1",
                   "expires_at":"2026-10-17T06:22:04Z",
                   "url":"wss://api.nextmoe.dev/connection/websocket"}""",
            ),
        )
        val token = chat.realtimeToken()
        assertEquals("jwt-1", token.token)
        assertEquals("wss://api.nextmoe.dev/connection/websocket", token.url)
        var request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/chat/realtime-token", request.url.encodedPath)

        server.enqueue(
            ok(
                """{"object":"update_list","updates":[
                     {"object":"update","update_seq":41,"kind":"new_message",
                      "conversation_id":"77","data":{"seq":13},
                      "created_at":"2026-10-17T06:00:00Z"}],
                   "messages":[$messageJson],
                   "conversations":[$conversationJson],
                   "users":[{"object":"user","id":"9","name":"小龙"}],
                   "last_update_seq":41,"has_more":true,"too_long":false}""",
            ),
        )
        val page = chat.updates(after = 40)
        assertEquals(1, page.updates.size)
        assertEquals("new_message", page.updates[0].kind)
        assertEquals(41, page.updates[0].updateSeq)
        assertEquals("13", page.updates[0].data["seq"]!!.jsonPrimitive.content)
        assertEquals(1, page.messages.size)
        assertEquals(12, page.messages[0].seq)
        assertEquals(1, page.conversations.size)
        assertEquals("小龙", page.users[0].name)
        assertEquals(41, page.lastUpdateSeq)
        assertTrue(page.hasMore)
        assertFalse(page.tooLong)
        request = server.takeRequest()
        assertEquals("/api/v1/chat/updates", request.url.encodedPath)
        assertEquals("40", request.url.queryParameter("after"))
        assertEquals("100", request.url.queryParameter("limit"))
    }

    @Test
    fun `表情回应列表与设置取消`() = runBlocking {
        server.enqueue(
            ok(
                """{"object":"list","items":[
                     {"key":"like","emoji":"👍","label":"赞"},
                     {"key":"heart","emoji":"❤️","label":"爱心"}]}""",
            ),
        )
        val options = chat.reactions()
        assertEquals(2, options.size)
        assertEquals("like", options[0].key)
        assertEquals("爱心", options[1].label)
        var request = server.takeRequest()
        assertEquals("/api/v1/chat/reactions", request.url.encodedPath)

        server.enqueue(
            ok(
                """{"object":"message_reactions","message_id":"9001",
                   "reactions":[{"reaction":"like","count":2,"reacted":true}]}""",
            ),
        )
        val result = chat.react("9001", "like")
        assertEquals(1, result.reactions.size)
        assertTrue(result.reactions[0].reacted)
        request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/chat/messages/9001/reaction", request.url.encodedPath)
        var body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertEquals("like", body["reaction"]!!.jsonPrimitive.content)

        server.enqueue(ok("""{"object":"message_reactions","message_id":"9001","reactions":[]}"""))
        chat.react("9001", null)
        request = server.takeRequest()
        body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
        assertTrue(body["reaction"] is kotlinx.serialization.json.JsonNull)
    }

    @Test
    fun `输入中与置顶取消置顶`() = runBlocking {
        server.enqueue(noContent())
        chat.typing("77")
        var request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/v1/chat/conversations/77/typing", request.url.encodedPath)

        server.enqueue(noContent())
        chat.pin("77", 12)
        request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals("/api/v1/chat/conversations/77/pins/12", request.url.encodedPath)

        server.enqueue(noContent())
        chat.unpin("77", 12)
        request = server.takeRequest()
        assertEquals("DELETE", request.method)
        assertEquals("/api/v1/chat/conversations/77/pins/12", request.url.encodedPath)
    }
}
