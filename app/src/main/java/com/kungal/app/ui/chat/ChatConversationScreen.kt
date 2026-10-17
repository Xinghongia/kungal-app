package com.kungal.app.ui.chat

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MarkEmailUnread
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.core.model.ChatConversation
import com.kungal.core.model.ChatMessage
import com.kungal.core.model.ChatPush
import com.kungal.core.model.ChatReactionOption
import com.kungal.core.model.ChatUpdatesPage
import com.kungal.core.model.ChatUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import java.util.UUID

class ChatViewModel(
    private val app: KungalApp,
    private val conversationId: String?,
    private val userId: String?,
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val conversation: ChatConversation? = null,
        val users: Map<String, ChatUser> = emptyMap(),
        val messages: List<ChatMessage> = emptyList(),
        val hasMoreBefore: Boolean = false,
        val loadingOlder: Boolean = false,
        val sending: Boolean = false,
        val uploading: Boolean = false,
        val meId: String? = null,
        val replyTo: ChatMessage? = null,
        val sentTicks: Int = 0,
        val message: String? = null,
        val actionBusy: Boolean = false,
        /** 对方已读到的会话内位置（用于「已读」标记）。 */
        val peerReadSeq: Long = 0,
        /** 对方正在输入的用户 id（6 秒内有效）。 */
        val typingUserId: String? = null,
        /** 可选表情回应；进入页面后拉取一次。 */
        val reactions: List<ChatReactionOption> = emptyList(),
        /** 请求滚动到的消息 seq（置顶跳转）。 */
        val scrollTarget: Long? = null,
        val hasMoreAfter: Boolean = false,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var typingJob: Job? = null
    private var lastTypingSentAt = 0L

    init {
        load()
        viewModelScope.launch {
            runCatching { app.chatApi.reactions() }
                .onSuccess { options -> _state.update { it.copy(reactions = options) } }
        }
        viewModelScope.launch {
            app.chatRealtime.pushes.collect { push -> onPush(push) }
        }
        viewModelScope.launch {
            app.chatRealtime.syncPages.collect { page -> onSyncPage(page) }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun setReplyTo(message: ChatMessage?) = _state.update { it.copy(replyTo = message) }

    fun load() {
        if (!app.isLoggedIn) {
            _state.value = State(loading = false, error = "登录后使用私信")
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                val meId = app.currentUserId.value ?: runCatching {
                    app.meApi.account().also { app.applyAccount(it) }
                }.getOrNull()?.id
                val conversation = if (!conversationId.isNullOrBlank()) {
                    app.chatApi.conversation(conversationId)
                } else {
                    app.chatApi.openDirect(requireNotNull(userId) { "缺少会话与用户参数" })
                }
                val page = app.chatApi.messages(conversation.id, limit = 50)
                Triple(conversation, page, meId)
            }.onSuccess { (conversation, page, meId) ->
                val users = (conversation.users + page.users).associateBy { it.id }
                _state.update {
                    it.copy(
                        loading = false,
                        conversation = conversation,
                        users = users,
                        messages = page.items,
                        hasMoreBefore = page.hasMoreBefore,
                        hasMoreAfter = page.hasMoreAfter,
                        peerReadSeq = conversation.peerReadSeq,
                        meId = meId,
                    )
                }
                markRead(conversation, page.items)
                app.refreshMessageBadge()
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = ChatCatalog.errorText(e)) }
            }
        }
    }

    /** 重新拉取最新一页（没有实时通道时用于手动刷新）。 */
    fun refresh() {
        val conversation = _state.value.conversation ?: return load()
        viewModelScope.launch {
            runCatching {
                val fresh = app.chatApi.conversation(conversation.id)
                val page = app.chatApi.messages(conversation.id, limit = 50)
                fresh to page
            }.onSuccess { (fresh, page) ->
                _state.update {
                    it.copy(
                        conversation = fresh,
                        users = it.users + fresh.users.associateBy { user -> user.id } +
                            page.users.associateBy { user -> user.id },
                        messages = page.items,
                        hasMoreBefore = page.hasMoreBefore,
                        hasMoreAfter = page.hasMoreAfter,
                        peerReadSeq = fresh.peerReadSeq,
                        error = null,
                    )
                }
                markRead(fresh, page.items)
            }.onFailure { e ->
                _state.update { it.copy(message = ChatCatalog.errorText(e)) }
            }
        }
    }

    private suspend fun markRead(conversation: ChatConversation, messages: List<ChatMessage>) {
        val maxSeq = messages.maxOfOrNull { it.seq } ?: return
        if (conversation.me.unreadCount <= 0 && !conversation.me.markedUnread) return
        runCatching { app.chatApi.markRead(conversation.id, maxSeq) }
            .onSuccess { read ->
                _state.update { current ->
                    val c = current.conversation ?: return@update current
                    current.copy(
                        conversation = c.copy(
                            me = c.me.copy(
                                lastReadSeq = maxOf(c.me.lastReadSeq, read.lastReadSeq),
                                unreadCount = read.unreadCount,
                                markedUnread = false,
                            ),
                        ),
                    )
                }
                app.refreshMessageBadge()
            }
    }

    fun loadOlder() {
        val current = _state.value
        val conversation = current.conversation ?: return
        val first = current.messages.firstOrNull { it.seq > 0 } ?: return
        if (current.loadingOlder || !current.hasMoreBefore) return
        _state.update { it.copy(loadingOlder = true) }
        viewModelScope.launch {
            runCatching {
                app.chatApi.messages(conversation.id, beforeSeq = first.seq, limit = 50)
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        loadingOlder = false,
                        messages = page.items + it.messages,
                        users = it.users + page.users.associateBy { user -> user.id },
                        hasMoreBefore = page.hasMoreBefore,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loadingOlder = false, message = ChatCatalog.errorText(e)) }
            }
        }
    }

    fun send(text: String) {
        val conversation = _state.value.conversation ?: return
        val body = text.trim()
        if (body.isEmpty() || _state.value.sending) return
        val replySeq = _state.value.replyTo?.seq
        _state.update { it.copy(sending = true, message = null) }
        viewModelScope.launch {
            runCatching {
                app.chatApi.send(
                    conversationId = conversation.id,
                    clientMessageId = UUID.randomUUID().toString(),
                    text = body,
                    replyToSeq = replySeq,
                )
            }.onSuccess { message ->
                appendMessage(message)
                _state.update { it.copy(sending = false, replyTo = null, sentTicks = it.sentTicks + 1) }
                app.refreshMessageBadge()
            }.onFailure { e ->
                _state.update { it.copy(sending = false, message = ChatCatalog.errorText(e)) }
            }
        }
    }

    fun sendPhoto(bytes: ByteArray, fileName: String, contentType: String) {
        val conversation = _state.value.conversation ?: return
        if (_state.value.uploading || _state.value.sending) return
        val replySeq = _state.value.replyTo?.seq
        _state.update { it.copy(uploading = true, message = null) }
        viewModelScope.launch {
            runCatching {
                val photo = app.chatApi.uploadPhoto(bytes, fileName, contentType)
                app.chatApi.send(
                    conversationId = conversation.id,
                    clientMessageId = UUID.randomUUID().toString(),
                    text = "",
                    replyToSeq = replySeq,
                    photo = photo,
                )
            }.onSuccess { message ->
                appendMessage(message)
                _state.update {
                    it.copy(uploading = false, replyTo = null, sentTicks = it.sentTicks + 1)
                }
                app.refreshMessageBadge()
            }.onFailure { e ->
                _state.update { it.copy(uploading = false, message = ChatCatalog.errorText(e)) }
            }
        }
    }

    private fun appendMessage(message: ChatMessage) {
        _state.update {
            it.copy(
                messages = it.messages + message,
                users = it.users + message.users.associateBy { user -> user.id },
            )
        }
    }

    // ---- 实时推送与断线补齐 ----

    private fun onPush(push: ChatPush) {
        val conversation = _state.value.conversation ?: return
        when (push.type) {
            "typing" -> {
                if (push.conversationId == conversation.id && push.userId != _state.value.meId) {
                    _state.update { it.copy(typingUserId = push.userId) }
                    typingJob?.cancel()
                    typingJob = viewModelScope.launch {
                        delay(6_000)
                        _state.update { it.copy(typingUserId = null) }
                    }
                }
            }
            "update" -> {
                val update = push.update ?: return
                if (update.conversationId != conversation.id) return
                when (update.kind) {
                    "new_message" -> {
                        val message = push.message ?: return
                        if (message.senderId != _state.value.meId) {
                            _state.update { it.copy(typingUserId = null) }
                        }
                        upsertMessage(message)
                        _state.update { current ->
                            val c = current.conversation ?: return@update current
                            val newest = c.lastMessage
                            current.copy(
                                conversation = c.copy(
                                    lastSeq = maxOf(c.lastSeq, message.seq),
                                    lastMessage = if (newest == null || message.seq >= newest.seq) {
                                        message
                                    } else {
                                        newest
                                    },
                                    me = if (message.senderId == current.meId) {
                                        c.me.copy(
                                            lastReadSeq = maxOf(c.me.lastReadSeq, message.seq),
                                            unreadCount = 0,
                                            markedUnread = false,
                                        )
                                    } else {
                                        c.me
                                    },
                                ),
                            )
                        }
                        if (message.senderId != _state.value.meId) postRead(message.seq)
                        app.refreshMessageBadge()
                    }
                    "edit_message" -> push.message?.let { upsertMessage(it) }
                    "delete_messages", "hide_messages" -> {
                        val seqs = update.data.seqSet()
                        if (seqs.isNotEmpty()) {
                            _state.update { current ->
                                current.copy(messages = current.messages.filterNot { it.seq in seqs })
                            }
                        }
                        if (update.kind == "hide_messages") refetchConversation()
                    }
                    "read_inbox" -> {
                        val maxSeq = update.data.long("max_seq")
                        val unread = update.data.int("unread_count")
                        _state.update { current ->
                            val c = current.conversation ?: return@update current
                            current.copy(
                                conversation = c.copy(
                                    me = c.me.copy(
                                        lastReadSeq = maxOf(c.me.lastReadSeq, maxSeq),
                                        unreadCount = unread,
                                    ),
                                ),
                            )
                        }
                    }
                    "read_outbox" -> {
                        val maxSeq = update.data.long("max_seq")
                        if (maxSeq > 0) {
                            _state.update { it.copy(peerReadSeq = maxOf(it.peerReadSeq, maxSeq)) }
                        }
                    }
                    "message_reactions" -> {
                        val seq = update.data.long("seq")
                        val reactions = push.reactions ?: push.message?.reactions
                        if (seq > 0 && reactions != null) {
                            _state.update { current ->
                                current.copy(
                                    messages = current.messages.map {
                                        if (it.seq == seq) it.copy(reactions = reactions) else it
                                    },
                                )
                            }
                        } else if (seq > 0) {
                            refetchMessage(seq)
                        }
                    }
                    "pinned_messages", "dialog", "conversation", "member" -> refetchConversation()
                    "clear_history" -> {
                        val through = update.data.long("through_seq")
                        _state.update { current ->
                            current.copy(
                                messages = current.messages.filter { it.seq <= 0 || it.seq > through },
                            )
                        }
                        refetchConversation()
                    }
                }
            }
        }
    }

    private fun onSyncPage(page: ChatUpdatesPage) {
        val conversation = _state.value.conversation ?: return
        if (page.users.isNotEmpty()) {
            _state.update { it.copy(users = it.users + page.users.associateBy { user -> user.id }) }
        }
        page.messages
            .filter { it.conversationId == conversation.id }
            .forEach { upsertMessage(it) }
        page.conversations.firstOrNull { it.id == conversation.id }?.let { fresh ->
            _state.update {
                it.copy(
                    conversation = fresh,
                    peerReadSeq = fresh.peerReadSeq,
                    users = it.users + fresh.users.associateBy { user -> user.id },
                )
            }
        }
        if (page.tooLong) refresh()
    }

    private fun upsertMessage(message: ChatMessage) {
        _state.update { current ->
            val merged = if (message.seq > 0 && current.messages.any { it.seq == message.seq }) {
                current.messages.map { if (it.seq == message.seq) message else it }
            } else {
                (current.messages + message).sortedBy { it.seq }
            }
            current.copy(
                messages = merged,
                users = current.users + message.users.associateBy { user -> user.id },
            )
        }
    }

    private fun postRead(maxSeq: Long) {
        val conversation = _state.value.conversation ?: return
        viewModelScope.launch {
            runCatching { app.chatApi.markRead(conversation.id, maxSeq) }
                .onSuccess { read ->
                    _state.update { current ->
                        val c = current.conversation ?: return@update current
                        current.copy(
                            conversation = c.copy(
                                me = c.me.copy(
                                    lastReadSeq = maxOf(c.me.lastReadSeq, read.lastReadSeq),
                                    unreadCount = read.unreadCount,
                                    markedUnread = false,
                                ),
                            ),
                        )
                    }
                    app.refreshMessageBadge()
                }
        }
    }

    private fun refetchConversation() {
        val id = _state.value.conversation?.id ?: return
        viewModelScope.launch {
            runCatching { app.chatApi.conversation(id) }
                .onSuccess { fresh ->
                    _state.update {
                        it.copy(
                            conversation = fresh,
                            peerReadSeq = fresh.peerReadSeq,
                            users = it.users + fresh.users.associateBy { user -> user.id },
                        )
                    }
                }
        }
    }

    private fun refetchMessage(seq: Long) {
        val conversation = _state.value.conversation ?: return
        viewModelScope.launch {
            runCatching { app.chatApi.messages(conversation.id, aroundSeq = seq, limit = 1) }
                .onSuccess { page ->
                    val fresh = page.items.firstOrNull { it.seq == seq } ?: return@onSuccess
                    upsertMessage(fresh)
                }
        }
    }

    /** 输入文字时告诉服务端「正在输入」；5 秒最多一次（与网页一致）。 */
    fun notifyTyping() {
        val conversation = _state.value.conversation ?: return
        val now = System.currentTimeMillis()
        if (now - lastTypingSentAt < 5_000) return
        lastTypingSentAt = now
        viewModelScope.launch { runCatching { app.chatApi.typing(conversation.id) } }
    }

    /** 设置/取消表情回应；再点同一种即取消。 */
    fun react(message: ChatMessage, key: String) {
        if (_state.value.actionBusy) return
        val mine = message.reactions.any { it.reacted && it.reaction == key }
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.react(message.id, if (mine) null else key) }
                .onSuccess { result ->
                    _state.update { current ->
                        current.copy(
                            actionBusy = false,
                            messages = current.messages.map {
                                if (it.seq == message.seq) it.copy(reactions = result.reactions) else it
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }

    fun setPinned(message: ChatMessage, pinned: Boolean) {
        val conversation = _state.value.conversation ?: return
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching {
                if (pinned) {
                    app.chatApi.pin(conversation.id, message.seq)
                } else {
                    app.chatApi.unpin(conversation.id, message.seq)
                }
            }.onSuccess {
                _state.update { it.copy(actionBusy = false) }
                refetchConversation()
            }.onFailure { e ->
                _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
            }
        }
    }

    /** 跳到某条消息：已加载就滚动，没加载就按 around_seq 取一页替换窗口。 */
    fun jumpTo(seq: Long) {
        if (seq <= 0) return
        if (_state.value.messages.any { it.seq == seq }) {
            _state.update { it.copy(scrollTarget = seq) }
            return
        }
        val conversation = _state.value.conversation ?: return
        viewModelScope.launch {
            runCatching { app.chatApi.messages(conversation.id, aroundSeq = seq, limit = 50) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            messages = page.items,
                            users = it.users + page.users.associateBy { user -> user.id },
                            hasMoreBefore = page.hasMoreBefore,
                            hasMoreAfter = page.hasMoreAfter,
                            scrollTarget = if (page.items.any { item -> item.seq == seq }) seq else null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(message = ChatCatalog.errorText(e)) }
                }
        }
    }

    fun clearScrollTarget() = _state.update { it.copy(scrollTarget = null) }

    fun edit(message: ChatMessage, text: String) {
        val body = text.trim()
        if (body.isEmpty() || body == message.text) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.edit(message.id, body) }
                .onSuccess { edited ->
                    _state.update { current ->
                        current.copy(
                            actionBusy = false,
                            messages = current.messages.map { if (it.seq == edited.seq) edited else it },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }

    fun delete(message: ChatMessage, forEveryone: Boolean) {
        val conversation = _state.value.conversation ?: return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching {
                app.chatApi.deleteMessages(conversation.id, listOf(message.seq), forEveryone)
            }.onSuccess { deleted ->
                val gone = deleted.seqs.toSet()
                _state.update { current ->
                    current.copy(
                        actionBusy = false,
                        messages = current.messages.filterNot { it.seq in gone },
                    )
                }
                app.refreshMessageBadge()
            }.onFailure { e ->
                _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
            }
        }
    }

    fun report(message: ChatMessage, reason: String, note: String) {
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.report(message.id, reason, note.trim()) }
                .onSuccess {
                    _state.update { it.copy(actionBusy = false, message = "已提交举报，感谢反馈") }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }

    fun acceptRequest() {
        val conversation = _state.value.conversation ?: return
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.accept(conversation.id) }
                .onSuccess {
                    _state.update { current ->
                        val c = current.conversation ?: return@update current
                        current.copy(
                            actionBusy = false,
                            conversation = c.copy(me = c.me.copy(accepted = true)),
                            message = "已接受消息请求",
                        )
                    }
                    app.refreshMessageBadge()
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }

    /** 拒绝消息请求：清空并移除该会话，成功后由界面返回列表。 */
    fun declineRequest(onDone: () -> Unit) {
        val conversation = _state.value.conversation ?: return
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.clearHistory(conversation.id, remove = true) }
                .onSuccess {
                    _state.update { it.copy(actionBusy = false) }
                    app.refreshMessageBadge()
                    onDone()
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }

    fun setArchived(archived: Boolean) {
        val conversation = _state.value.conversation ?: return
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.setDialog(conversation.id, archived = archived) }
                .onSuccess { dialog ->
                    _state.update { current ->
                        val c = current.conversation ?: return@update current
                        current.copy(
                            actionBusy = false,
                            conversation = c.copy(me = dialog),
                            message = if (archived) "已归档" else "已移出归档",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }

    /** 静音/取消静音会话（不接收提醒，消息照常接收）。 */
    fun setMuted(muted: Boolean) {
        val conversation = _state.value.conversation ?: return
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.setDialog(conversation.id, muted = muted) }
                .onSuccess { dialog ->
                    _state.update { current ->
                        val c = current.conversation ?: return@update current
                        current.copy(
                            actionBusy = false,
                            conversation = c.copy(me = dialog.copy(draft = c.me.draft)),
                            message = if (muted) "已静音" else "已取消静音",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }

    /** 置顶/取消置顶整个会话（与消息置顶不同）。 */
    fun setPinnedConversation(pinned: Boolean) {
        val conversation = _state.value.conversation ?: return
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.setDialog(conversation.id, pinned = pinned) }
                .onSuccess { dialog ->
                    _state.update { current ->
                        val c = current.conversation ?: return@update current
                        current.copy(
                            actionBusy = false,
                            conversation = c.copy(me = dialog.copy(draft = c.me.draft)),
                            message = if (pinned) "已置顶对话" else "已取消置顶对话",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }

    fun markUnread() {        val conversation = _state.value.conversation ?: return
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.setDialog(conversation.id, markedUnread = true) }
                .onSuccess { dialog ->
                    _state.update { current ->
                        val c = current.conversation ?: return@update current
                        current.copy(
                            actionBusy = false,
                            conversation = c.copy(me = dialog),
                            message = "已标为未读",
                        )
                    }
                    app.refreshMessageBadge()
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }

    fun clearHistory() {
        val conversation = _state.value.conversation ?: return
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.chatApi.clearHistory(conversation.id, remove = false) }
                .onSuccess {
                    _state.update {
                        it.copy(
                            actionBusy = false,
                            messages = emptyList(),
                            message = "聊天记录已清空",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = ChatCatalog.errorText(e)) }
                }
        }
    }
}

/** 私信会话：历史消息、发送文本/图片、引用回复、编辑、删除、举报、消息请求。 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ChatConversationScreen(
    app: KungalApp,
    conversationId: String?,
    userId: String?,
    onBack: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: ChatViewModel = viewModel(key = "chat-${conversationId ?: userId}") {
        ChatViewModel(app, conversationId, userId)
    }
    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val realtimeConnected by app.chatRealtime.connected.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current

    var draft by remember { mutableStateOf("") }
    var menuOpen by remember { mutableStateOf(false) }
    var actionMessage by remember { mutableStateOf<ChatMessage?>(null) }
    var editing by remember { mutableStateOf<ChatMessage?>(null) }
    var deleting by remember { mutableStateOf<ChatMessage?>(null) }
    var reporting by remember { mutableStateOf<ChatMessage?>(null) }
    var reactionTarget by remember { mutableStateOf<ChatMessage?>(null) }
    var pinnedOpen by remember { mutableStateOf(false) }
    var lastSentTicks by remember { mutableStateOf(0) }
    var confirmClear by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        val text = state.message ?: return@LaunchedEffect
        snackbarHost.showSnackbar(text)
        vm.clearMessage()
    }
    // 首次加载/发送后滚到底；阅读历史或置顶跳转时不强行拽动
    LaunchedEffect(state.messages.lastOrNull()?.seq, state.sentTicks) {
        if (state.messages.isNotEmpty() && state.scrollTarget == null) {
            val count = snapshotFlow { listState.layoutInfo.totalItemsCount }.first { it > 0 }
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            val nearBottom = info.totalItemsCount - lastVisible <= 3
            val sent = state.sentTicks != lastSentTicks
            lastSentTicks = state.sentTicks
            if (sent || nearBottom) listState.scrollToItem(count - 1)
        }
    }
    // 置顶跳转
    LaunchedEffect(state.scrollTarget, state.messages.size) {
        val target = state.scrollTarget ?: return@LaunchedEffect
        val index = state.messages.indexOfFirst { it.seq == target }
        if (index >= 0) {
            val offset = if (state.hasMoreBefore) 1 else 0
            listState.animateScrollToItem(index + offset)
            vm.clearScrollTarget()
        }
    }

    val pickPhoto = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }.getOrNull()
            }
            if (bytes == null) {
                snackbarHost.showSnackbar("无法读取这张图片")
                return@launch
            }
            if (bytes.size > 10 * 1024 * 1024) {
                snackbarHost.showSnackbar("图片不能超过 10 MiB")
                return@launch
            }
            val mime = context.contentResolver.getType(uri) ?: "image/png"
            val ext = when (mime) {
                "image/jpeg" -> "jpg"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                else -> "png"
            }
            vm.sendPhoto(bytes, "chat.$ext", mime)
        }
    }

    val conversation = state.conversation
    val peer = conversation?.peerId?.let { state.users[it] }
    val peerName = conversation?.title?.takeIf { it.isNotBlank() } ?: ChatCatalog.displayName(peer)
    val pinned = conversation?.pinnedMessages.orEmpty()
    val muted = ChatCatalog.isMuted(conversation?.me?.mutedUntil)
    val lastOwnSeq = state.messages.lastOrNull { it.seq > 0 && it.senderId == state.meId }?.seq

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = peerName,
                subtitle = when {
                    conversation == null -> "私信"
                    state.typingUserId != null -> "正在输入…"
                    !realtimeConnected -> "实时通道未连接 · 可手动刷新"
                    !conversation.me.accepted -> "消息请求"
                    state.uploading -> "正在发送图片…"
                    else -> "私信"
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = vm::refresh) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                    Box {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Outlined.MoreVert, contentDescription = "会话操作")
                        }
                        DropdownMenu(
                            expanded = menuOpen,
                            onDismissRequest = { menuOpen = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            if (conversation != null && conversation.me.accepted) {
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (conversation.me.pinnedRank != null) "取消置顶对话" else "置顶对话",
                                        )
                                    },
                                    leadingIcon = { Icon(Icons.Outlined.PushPin, contentDescription = null) },
                                    onClick = {
                                        menuOpen = false
                                        vm.setPinnedConversation(conversation.me.pinnedRank == null)
                                    },
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(if (muted) "取消静音" else "静音") },
                                leadingIcon = {
                                    Icon(
                                        if (muted) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                                        contentDescription = null,
                                    )
                                },
                                onClick = {
                                    menuOpen = false
                                    vm.setMuted(!muted)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("标为未读") },
                                leadingIcon = { Icon(Icons.Outlined.MarkEmailUnread, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    vm.markUnread()
                                },
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        if (conversation?.me?.archived == true) "移出归档" else "归档",
                                    )
                                },
                                leadingIcon = { Icon(Icons.Outlined.Archive, contentDescription = null) },
                                onClick = {
                                    menuOpen = false
                                    vm.setArchived(conversation?.me?.archived != true)
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("清空聊天记录") },
                                onClick = {
                                    menuOpen = false
                                    confirmClear = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("私信设置") },
                                onClick = {
                                    menuOpen = false
                                    onOpenSettings()
                                },
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            when {
                state.loading -> LoadingBox(Modifier.fillMaxSize())
                state.error != null && conversation == null -> ErrorBox(
                    message = state.error!!,
                    onRetry = vm::load,
                    modifier = Modifier.fillMaxSize(),
                )
                conversation != null -> {
                    if (!conversation.me.accepted) {
                        RequestBar(
                            busy = state.actionBusy,
                            onAccept = vm::acceptRequest,
                            onDecline = { vm.declineRequest(onBack) },
                        )
                    }
                    if (pinned.isNotEmpty()) {
                        PinnedBar(
                            message = pinned.first(),
                            total = pinned.size,
                            users = state.users,
                            onOpen = { pinnedOpen = true },
                            onJump = { vm.jumpTo(pinned.first().seq) },
                        )
                    }
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        if (state.hasMoreBefore) {
                            item(key = "older") {
                                TextButton(
                                    onClick = vm::loadOlder,
                                    enabled = !state.loadingOlder,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(if (state.loadingOlder) "加载中…" else "加载更早的消息")
                                }
                            }
                        }
                        if (state.messages.isEmpty()) {
                            item(key = "empty") {
                                EmptyBox("还没有消息，打个招呼吧")
                            }
                        }
                        items(state.messages, key = { it.id.ifBlank { "seq-${it.seq}" } }) { message ->
                            val own = state.meId != null && message.senderId == state.meId
                            MessageBubble(
                                message = message,
                                sender = state.users[message.senderId],
                                meId = state.meId,
                                users = state.users,
                                readLabel = if (
                                    own && message.seq == lastOwnSeq &&
                                    state.peerReadSeq >= message.seq
                                ) {
                                    "已读"
                                } else {
                                    null
                                },
                                reactionOptions = state.reactions,
                                onLongPress = { actionMessage = message },
                                onReact = { key -> vm.react(message, key) },
                            )
                        }
                        if (state.hasMoreAfter) {
                            item(key = "newer") {
                                TextButton(onClick = vm::refresh, modifier = Modifier.fillMaxWidth()) {
                                    Text("回到最新消息")
                                }
                            }
                        }
                    }
                    state.replyTo?.let { target ->
                        ReplyBar(
                            name = ChatCatalog.userName(state.users, target.senderId),
                            text = ChatCatalog.preview(target, null),
                            onClose = { vm.setReplyTo(null) },
                        )
                    }
                    state.typingUserId?.let { typingId ->
                        Text(
                            text = ChatCatalog.userName(state.users, typingId) + " 正在输入…",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 2.dp),
                        )
                    }
                    Composer(
                        draft = draft,
                        onDraftChange = {
                            draft = it.take(4096)
                            if (it.isNotBlank()) vm.notifyTyping()
                        },
                        sending = state.sending,
                        uploading = state.uploading,
                        onSend = { vm.send(draft) },
                        onAttach = { pickPhoto.launch("image/*") },
                    )
                }
            }
        }
    }

    actionMessage?.let { message ->
        val own = state.meId != null && message.senderId == state.meId
        ModalBottomSheet(onDismissRequest = { actionMessage = null }) {
            Column(Modifier.fillMaxWidth().padding(bottom = 20.dp)) {
                val quick = state.reactions.take(8)
                if (quick.isNotEmpty() && message.seq > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        quick.forEach { option ->
                            val selected = message.reactions.any {
                                it.reacted && it.reaction == option.key
                            }
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceContainerHigh
                                },
                                modifier = Modifier.clickable {
                                    vm.react(message, option.key)
                                    actionMessage = null
                                },
                            ) {
                                Text(
                                    option.emoji,
                                    fontSize = 20.sp,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                )
                            }
                        }
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerHigh,
                            modifier = Modifier.clickable {
                                reactionTarget = message
                                actionMessage = null
                            },
                        ) {
                            Icon(
                                Icons.Outlined.AddReaction,
                                contentDescription = "更多回应",
                                modifier = Modifier.padding(8.dp).size(20.dp),
                            )
                        }
                    }
                    HorizontalDivider(Modifier.padding(bottom = 4.dp))
                }
                Text(
                    "消息操作",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
                )
                ChatAction("回复", onClick = {
                    vm.setReplyTo(message)
                    actionMessage = null
                })
                if (message.text.isNotBlank()) {
                    ChatAction("复制文字", onClick = {
                        clipboard.setText(AnnotatedString(message.text))
                        actionMessage = null
                    })
                }
                if (ChatCatalog.canEdit(message, state.meId)) {
                    ChatAction("编辑", onClick = {
                        editing = message
                        actionMessage = null
                    })
                }
                if (conversation?.me?.accepted == true && message.seq > 0) {
                    ChatAction(
                        if (message.pinnedAt != null) "取消置顶" else "置顶消息",
                        onClick = {
                            vm.setPinned(message, message.pinnedAt == null)
                            actionMessage = null
                        },
                    )
                }
                ChatAction("删除", danger = true, onClick = {
                    deleting = message
                    actionMessage = null
                })
                if (!own) {
                    ChatAction("举报", danger = true, onClick = {
                        reporting = message
                        actionMessage = null
                    })
                }
            }
        }
    }

    reactionTarget?.let { message ->
        ModalBottomSheet(onDismissRequest = { reactionTarget = null }) {
            ReactionPicker(
                options = state.reactions,
                message = message,
                onPick = { key ->
                    vm.react(message, key)
                    reactionTarget = null
                },
            )
        }
    }

    if (pinnedOpen) {
        ModalBottomSheet(onDismissRequest = { pinnedOpen = false }) {
            PinnedSheet(
                messages = pinned,
                users = state.users,
                unpinnable = conversation?.me?.accepted == true,
                onJump = { seq ->
                    pinnedOpen = false
                    vm.jumpTo(seq)
                },
                onUnpin = { seq ->
                    pinned.firstOrNull { it.seq == seq }?.let { vm.setPinned(it, false) }
                },
            )
        }
    }

    editing?.let { message ->
        EditDialog(
            original = message.text,
            busy = state.actionBusy,
            onDismiss = { editing = null },
            onConfirm = { text ->
                vm.edit(message, text)
                editing = null
            },
        )
    }

    deleting?.let { message ->
        val own = state.meId != null && message.senderId == state.meId
        DeleteDialog(
            own = own,
            busy = state.actionBusy,
            onDismiss = { deleting = null },
            onConfirm = { forEveryone ->
                vm.delete(message, forEveryone)
                deleting = null
            },
        )
    }

    reporting?.let { message ->
        ReportDialog(
            busy = state.actionBusy,
            onDismiss = { reporting = null },
            onSubmit = { reason, note ->
                vm.report(message, reason, note)
                reporting = null
            },
        )
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空聊天记录？") },
            text = { Text("只会从你这里清空，对方仍能看到这些消息。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmClear = false
                    vm.clearHistory()
                }) { Text("清空") }
            },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun RequestBar(
    busy: Boolean,
    onAccept: () -> Unit,
    onDecline: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "对方发来了消息请求，接受后即可正常聊天。",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = onAccept, enabled = !busy) { Text("接受") }
            TextButton(onClick = onDecline, enabled = !busy) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun Composer(
    draft: String,
    onDraftChange: (String) -> Unit,
    sending: Boolean,
    uploading: Boolean,
    onSend: () -> Unit,
    onAttach: () -> Unit,
) {
    Surface(
        tonalElevation = 2.dp,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            IconButton(onClick = onAttach, enabled = !sending && !uploading) {
                if (uploading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.Outlined.AttachFile, contentDescription = "发送图片")
                }
            }
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("输入消息…") },
                maxLines = 4,
                shape = RoundedCornerShape(18.dp),
            )
            Spacer(Modifier.width(6.dp))
            FilledIconButton(
                onClick = onSend,
                enabled = draft.isNotBlank() && !sending && !uploading,
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                }
            }
        }
    }
}

@Composable
private fun ReplyBar(name: String, text: String, onClose: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerHigh, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(start = 14.dp, top = 6.dp, bottom = 6.dp, end = 2.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text("回复 $name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                Text(
                    text,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "取消回复")
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun MessageBubble(
    message: ChatMessage,
    sender: ChatUser?,
    meId: String?,
    users: Map<String, ChatUser>,
    readLabel: String?,
    reactionOptions: List<ChatReactionOption>,
    onLongPress: () -> Unit,
    onReact: (String) -> Unit,
) {
    val own = meId != null && message.senderId == meId
    if (message.kind == "service") {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text(
                "系统消息",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 4.dp),
            )
        }
        return
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (own) Arrangement.End else Arrangement.Start,
        verticalAlignment = Alignment.Top,
    ) {
        if (!own) {
            AvatarImage(url = sender?.avatar, modifier = Modifier.size(34.dp), seed = sender?.name)
            Spacer(Modifier.width(8.dp))
        }
        Column(
            modifier = Modifier.widthIn(max = 300.dp),
            horizontalAlignment = if (own) Alignment.End else Alignment.Start,
        ) {
            if (!own) {
                Text(
                    ChatCatalog.displayName(sender),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.padding(start = 2.dp, bottom = 2.dp),
                )
            }
            Surface(
                shape = RoundedCornerShape(
                    topStart = 14.dp,
                    topEnd = 14.dp,
                    bottomStart = if (own) 14.dp else 4.dp,
                    bottomEnd = if (own) 4.dp else 14.dp,
                ),
                color = if (own) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                modifier = Modifier.combinedClickable(
                    onClick = {},
                    onLongClick = onLongPress,
                ),
            ) {
                Column(Modifier.padding(horizontal = 10.dp, vertical = 7.dp)) {
                    message.replyTo?.let { reply ->
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        ) {
                            Column(Modifier.padding(horizontal = 8.dp, vertical = 4.dp)) {
                                Text(
                                    ChatCatalog.userName(users, reply.senderId),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    text = when {
                                        reply.deleted -> "消息已删除"
                                        reply.text.isNotBlank() -> reply.text
                                        reply.mediaType == "photo" -> "[图片]"
                                        else -> "[消息]"
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                    message.media?.let { media ->
                        val url = ChatCatalog.imageUrl(media.imageHash, media.url)
                        val ratio = if (media.width > 0 && media.height > 0) {
                            media.width.toFloat() / media.height
                        } else {
                            4f / 3f
                        }
                        val height = (220f / ratio).coerceIn(90f, 320f)
                        KungalImage(
                            url = url,
                            contentDescription = "图片消息",
                            modifier = Modifier
                                .width(220.dp)
                                .height(height.dp)
                                .clip(RoundedCornerShape(8.dp)),
                            contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                        )
                        if (message.text.isNotBlank()) Spacer(Modifier.height(4.dp))
                    }
                    if (message.text.isNotBlank()) {
                        Text(message.text, style = MaterialTheme.typography.bodyMedium)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (message.editedAt != null) {
                            Text(
                                "已编辑 · ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (readLabel != null) {
                            Text(
                                "$readLabel · ",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                        Text(
                            ChatCatalog.messageTime(message.createdAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            if (message.reactions.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    message.reactions.forEach { reaction ->
                        val selected = reaction.reacted
                        val emoji = reactionOptions
                            .firstOrNull { it.key == reaction.reaction }
                            ?.emoji
                            ?: reaction.reaction
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (selected) {
                                MaterialTheme.colorScheme.primaryContainer
                            } else {
                                MaterialTheme.colorScheme.surfaceContainerHigh
                            },
                            modifier = Modifier.clickable { onReact(reaction.reaction) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(emoji, fontSize = 12.sp)
                                Spacer(Modifier.width(3.dp))
                                Text(
                                    "${reaction.count}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PinnedBar(
    message: ChatMessage,
    total: Int,
    users: Map<String, ChatUser>,
    onOpen: () -> Unit,
    onJump: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.PushPin,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    if (total > 1) "置顶消息（$total）" else "置顶消息",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    ChatCatalog.userName(users, message.senderId) + "：" +
                        ChatCatalog.preview(message, null),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            TextButton(onClick = onJump) { Text("查看") }
        }
    }
}

@Composable
private fun PinnedSheet(
    messages: List<ChatMessage>,
    users: Map<String, ChatUser>,
    unpinnable: Boolean,
    onJump: (Long) -> Unit,
    onUnpin: (Long) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            "置顶消息",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
        if (messages.isEmpty()) {
            Text(
                "还没有置顶消息",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
        }
        messages.forEach { message ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onJump(message.seq) }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        ChatCatalog.userName(users, message.senderId),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        ChatCatalog.preview(message, null),
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        ChatCatalog.messageTime(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (unpinnable) {
                    IconButton(onClick = { onUnpin(message.seq) }) {
                        Icon(
                            Icons.Outlined.PushPin,
                            contentDescription = "取消置顶",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            HorizontalDivider()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReactionPicker(
    options: List<ChatReactionOption>,
    message: ChatMessage,
    onPick: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
        Text(
            "表情回应",
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            options.forEach { option ->
                val selected = message.reactions.any {
                    it.reacted && it.reaction == option.key
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerHigh
                    },
                    modifier = Modifier.clickable { onPick(option.key) },
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Text(option.emoji, fontSize = 22.sp)
                        Text(option.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}

@Composable
private fun ChatAction(
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    TextButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            label,
            color = if (danger) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun EditDialog(
    original: String,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by remember(original) { mutableStateOf(original) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("编辑消息") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it.take(4096) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 5,
                )
                Text(
                    "发出超过 48 小时的消息不能编辑",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 6.dp),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }, enabled = !busy && text.isNotBlank()) {
                Text("保存")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DeleteDialog(
    own: Boolean,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit,
) {
    var forEveryone by remember { mutableStateOf(true) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除消息") },
        text = {
            Column {
                Text(if (own) "确定删除这条消息吗？" else "这条消息只会从你这里删除。")
                if (own) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = forEveryone,
                            onCheckedChange = { forEveryone = it },
                        )
                        Text("同时为对方删除", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(own && forEveryone) }, enabled = !busy) {
                Text("删除", color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ReportDialog(
    busy: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (reason: String, note: String) -> Unit,
) {
    var reason by remember { mutableStateOf("spam") }
    var note by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("举报消息") },
        text = {
            Column {
                Text(
                    "举报会把这条消息和它之前的最多 10 条消息交给审核人员查看。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                ChatCatalog.reportReasons.forEach { (key, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = reason == key, onClick = { reason = key })
                        Text(label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp))
                OutlinedTextField(
                    value = note,
                    onValueChange = { note = it.take(500) },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("补充说明（可选）") },
                    maxLines = 3,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSubmit(reason, note) }, enabled = !busy) {
                Text("提交举报")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun JsonObject.long(key: String): Long =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toLongOrNull() ?: 0L

private fun JsonObject.int(key: String): Int =
    (this[key] as? JsonPrimitive)?.contentOrNull?.toIntOrNull() ?: 0

private fun JsonObject.seqSet(): Set<Long> =
    (this["seqs"] as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull?.toLongOrNull() }
        ?.toSet()
        .orEmpty()
