package com.kungal.core

import com.kungal.core.chat.ChatRealtimeClient
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ChatRealtimeClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: KungalClient

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
        client = KungalClient(baseUrl = server.url("/api/v1").toString().trimEnd('/'))
    }

    @After
    fun tearDown() {
        server.close()
    }

    private val messageJson = """
        {"object":"message","id":"9002","conversation_id":"77","seq":13,"sender_id":"9",
         "kind":"message","text":"在的","entities":[],"media":null,"media_group_id":null,
         "reply_to":null,"reply_quote":null,"reactions":[],"silent":false,
         "pinned_at":null,"edited_at":null,"created_at":"2026-10-17T06:00:00Z"}
    """.trimIndent()

    @Test
    fun `连接握手收到推送并回应服务端心跳`() = runBlocking {
        val pong = CompletableDeferred<Unit>()
        val connectCommand = CompletableDeferred<String>()

        server.enqueue(
            MockResponse.Builder().webSocketUpgrade(object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: Response) = Unit

                override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                    webSocket.close(1000, null)
                }

                override fun onMessage(webSocket: WebSocket, text: String) {
                    val json = Json.parseToJsonElement(text).jsonObject
                    if (json.containsKey("connect")) {
                        connectCommand.complete(text)
                        webSocket.send(
                            """{"id":1,"connect":{"client":"c1","version":"6.9.6",
                               "expires":true,"ttl":900,"subs":{"user:1":{}},
                               "ping":25,"pong":true}}""",
                        )
                        webSocket.send(
                            """{"push":{"channel":"user:1","pub":{"data":
                               {"type":"update",
                                "update":{"object":"update","update_seq":7,
                                  "kind":"new_message","conversation_id":"77",
                                  "data":{"seq":13},"created_at":"2026-10-17T06:00:00Z"},
                                "message":$messageJson}}}}""",
                        )
                        // 服务端心跳：空帧
                        webSocket.send("{}")
                    } else if (text.trim() == "{}") {
                        pong.complete(Unit)
                    }
                }
            }).build(),
        )

        val realtime = ChatRealtimeClient(client)
        try {
            val received = async(start = CoroutineStart.UNDISPATCHED) { realtime.pushes.first() }

            val result = realtime.connect(
                url = server.url("/connection/websocket").toString(),
                token = "jwt-1",
                timeoutMs = 5_000,
            )
            assertTrue(result.exceptionOrNull()?.toString(), result.isSuccess)

        val command = connectCommand.await()
        assertEquals(
            "jwt-1",
            Json.parseToJsonElement(command).jsonObject["connect"]!!.jsonObject["token"]!!
                .jsonPrimitive.content,
        )

        val push = withTimeout(5_000) { received.await() }
        assertEquals("update", push.type)
        assertEquals("new_message", push.update?.kind)
        assertEquals(7L, push.update?.updateSeq)
        assertEquals("77", push.update?.conversationId)
        assertEquals(13L, push.message?.seq)
        assertEquals("在的", push.message?.text)

        // 服务端心跳（空帧）→ 客户端回空帧，连接保持
        withTimeout(1_000) { pong.await() }
        } finally {
            realtime.close()
        }
    }
}
