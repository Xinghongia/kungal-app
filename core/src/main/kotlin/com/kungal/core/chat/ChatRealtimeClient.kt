package com.kungal.core.chat

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.ChatPush
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.concurrent.atomic.AtomicInteger

/**
 * Centrifugo 实时通道的最小客户端（JSON 协议，私信用途）。
 *
 * 站点通过 /chat/realtime-token 下发连接地址与 JWT；token 里带服务端订阅的
 * 个人频道，连接后推送直接以 push.pub.data 到达，无需再发 subscribe。
 * 只处理 connect 握手、服务端心跳（空帧回空帧）与 publication；
 * 断线重连、token 刷新与 /chat/updates 补齐由调用方负责。
 */
class ChatRealtimeClient(
    private val client: KungalClient,
    private val json: Json = KungalJson,
) {

    private val _pushes = MutableSharedFlow<ChatPush>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** 服务端推送（新消息、回应、输入中等）。 */
    val pushes: SharedFlow<ChatPush> = _pushes.asSharedFlow()

    private val ids = AtomicInteger(1)

    @Volatile private var socket: WebSocket? = null
    @Volatile private var ready: CompletableDeferred<Result<Unit>>? = null
    @Volatile private var ended: CompletableDeferred<Unit>? = null
    @Volatile private var connectId = 0
    @Volatile private var connectFrame = ""
    @Volatile private var pong = true

    /**
     * 建立连接并等待 connect 回复；返回成功即表示可以接收推送。
     * 已存在连接会先关闭。url/token 来自 [com.kungal.core.api.ChatApi.realtimeToken]。
     */
    suspend fun connect(url: String, token: String, timeoutMs: Long = 15_000): Result<Unit> {
        close()
        if (url.isBlank() || token.isBlank()) {
            return Result.failure(IllegalStateException("实时通道凭证为空"))
        }
        val connectReady = CompletableDeferred<Result<Unit>>()
        ready = connectReady
        ended = CompletableDeferred()
        connectId = ids.getAndIncrement()
        connectFrame = buildJsonObject {
            put("id", connectId)
            putJsonObject("connect") { put("token", token) }
        }.toString()
        socket = client.newWebSocket(url, listener)
        val result = withTimeoutOrNull(timeoutMs) { connectReady.await() }
        if (result == null) {
            close()
            return Result.failure(IllegalStateException("实时连接超时"))
        }
        return result
    }

    /** 连接结束（服务端断开、网络失败或 [close]）时返回。 */
    suspend fun awaitDisconnected() {
        ended?.await()
    }

    fun close() {
        val current = socket
        socket = null
        current?.close(1000, "client close")
        ready?.complete(Result.failure(IllegalStateException("实时连接已关闭")))
        ended?.complete(Unit)
    }

    private val listener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            webSocket.send(connectFrame)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            // 协议帧通常是「一行一个 JSON」；整帧也能解析时优先按整帧处理
            val whole = runCatching { json.parseToJsonElement(text) as? JsonObject }.getOrNull()
            if (whole != null) {
                handleObject(webSocket, whole)
                return
            }
            text.lineSequence().forEach { line ->
                if (line.isNotBlank()) {
                    val obj = runCatching {
                        json.parseToJsonElement(line) as? JsonObject
                    }.getOrNull() ?: return@forEach
                    handleObject(webSocket, obj)
                }
            }
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(1000, null)
            finish(IllegalStateException("实时连接已断开（$code）"))
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            finish(IllegalStateException("实时连接已断开（$code）"))
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            finish(t)
        }
    }

    private fun handleObject(webSocket: WebSocket, obj: JsonObject) {
        val push = obj["push"] as? JsonObject
        if (push != null) {
            handlePush(push)
            return
        }
        val id = (obj["id"] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()
        if (id != null && id == connectId) {
            val error = obj["error"] as? JsonObject
            if (error != null) {
                val code = (error["code"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                val message = (error["message"] as? JsonPrimitive)?.contentOrNull.orEmpty()
                ready?.complete(Result.failure(IllegalStateException("实时连接被拒绝（$code $message）")))
            } else {
                pong = (obj["connect"] as? JsonObject)
                    ?.get("pong")
                    ?.let { (it as? JsonPrimitive)?.booleanOrNull }
                    ?: true
                ready?.complete(Result.success(Unit))
            }
            return
        }
        // 服务端心跳是空帧；connect 结果要求 pong 时回空帧
        if (obj.isEmpty() && pong) {
            webSocket.send("{}")
        }
    }

    private fun handlePush(push: JsonObject) {
        if (push["disconnect"] != null) {
            socket?.close(1000, "server disconnect")
            ended?.complete(Unit)
            return
        }
        val pub = push["pub"] as? JsonObject ?: return
        val data = pub["data"] ?: return
        val chatPush = runCatching {
            json.decodeFromJsonElement(ChatPush.serializer(), data)
        }.getOrNull() ?: return
        _pushes.tryEmit(chatPush)
    }

    private fun finish(error: Throwable) {
        socket = null
        ready?.complete(Result.failure(error))
        ended?.complete(Unit)
    }
}
