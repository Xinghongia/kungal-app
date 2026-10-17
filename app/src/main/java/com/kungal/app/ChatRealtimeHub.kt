package com.kungal.app

import com.kungal.core.chat.ChatRealtimeClient
import com.kungal.core.model.ChatPush
import com.kungal.core.model.ChatUpdatesPage
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant

/**
 * 私信实时通道（App 级）：
 * - 登录后用 /chat/realtime-token 连接 Centrifugo，接收新消息/回应/输入中；
 * - 断线重连，并按 /chat/updates?after 补齐断线期间错过的增量；
 * - token 到期前主动换新连接（Centrifugo token 约 15 分钟）。
 * 界面层订阅 [pushes]（实时）与 [syncPages]（补齐批次），不直接碰 WebSocket。
 */
class ChatRealtimeHub(private val app: KungalApp) {

    private val _pushes = MutableSharedFlow<ChatPush>(
        extraBufferCapacity = 128,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val pushes: SharedFlow<ChatPush> = _pushes.asSharedFlow()

    private val _syncPages = MutableSharedFlow<ChatUpdatesPage>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val syncPages: SharedFlow<ChatUpdatesPage> = _syncPages.asSharedFlow()

    /** 当前是否连着实时通道；界面可据此提示「实时/手动刷新」。 */
    private val _connected = MutableStateFlow(false)
    val connected: StateFlow<Boolean> = _connected.asStateFlow()

    private val client = ChatRealtimeClient(app.client)
    private val syncMutex = Mutex()
    private var job: Job? = null

    @Volatile
    private var lastUpdateSeq = 0L

    fun start() {
        if (job?.isActive == true) return
        job = app.appScope.launch { run() }
    }

    fun stop() {
        job?.cancel()
        job = null
        client.close()
        _connected.value = false
        lastUpdateSeq = 0
    }

    private suspend fun run() {
        var initialized = false
        while (currentCoroutineContext().isActive && app.isLoggedIn) {
            if (!initialized) {
                val state = runCatching { app.chatApi.state() }.getOrNull()
                if (state == null) {
                    delay(30_000)
                    continue
                }
                lastUpdateSeq = state.lastUpdateSeq
                initialized = true
            }

            val token = runCatching { app.chatApi.realtimeToken() }.getOrNull()
            if (token == null) {
                _connected.value = false
                delay(30_000)
                continue
            }
            val result = client.connect(token.url, token.token)
            if (result.isFailure) {
                _connected.value = false
                delay(15_000)
                continue
            }

            _connected.value = true

            coroutineScope {
                var gapJob: Job? = null
                // 先订阅推送再补齐，避免补齐期间到达的新消息落空
                val collector = launch {
                    client.pushes.collect { push ->
                        push.update?.let { update ->
                            if (lastUpdateSeq > 0 && update.updateSeq > lastUpdateSeq + 1) {
                                // 推送序号有空洞：等 500ms 仍未补齐就整段重放
                                gapJob?.cancel()
                                gapJob = launch {
                                    delay(500)
                                    syncMissed()
                                }
                            } else if (update.updateSeq > lastUpdateSeq) {
                                lastUpdateSeq = update.updateSeq
                            }
                        }
                        _pushes.emit(push)
                        if (push.update?.kind == "new_message") app.refreshMessageBadge()
                    }
                }
                syncMissed()
                try {
                    // token 快到期时主动断开换新，避免被服务端踢下线
                    withTimeoutOrNull(holdMillis(token.expiresAt)) { client.awaitDisconnected() }
                } finally {
                    collector.cancel()
                    client.close()
                }
            }
            _connected.value = false
            delay(2_000)
        }
    }

    private suspend fun syncMissed() {
        syncMutex.withLock {
            var seq = lastUpdateSeq
            while (true) {
                val page = runCatching { app.chatApi.updates(after = seq, limit = 100) }.getOrNull() ?: return
                _syncPages.emit(page)
                seq = page.updates.fold(seq) { acc, update -> maxOf(acc, update.updateSeq) }
                seq = maxOf(seq, page.lastUpdateSeq)
                lastUpdateSeq = maxOf(lastUpdateSeq, seq)
                if (page.tooLong || !page.hasMore) return
            }
        }
    }

    private fun holdMillis(expiresAt: String): Long {
        val remaining = runCatching {
            Instant.parse(expiresAt).toEpochMilli() - System.currentTimeMillis()
        }.getOrDefault(10 * 60_000L)
        return (remaining - 60_000L).coerceIn(60_000L, 20 * 60_000L)
    }
}
