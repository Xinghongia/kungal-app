package com.kungal.app.ui.chat

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.ChatConversation
import com.kungal.core.model.ChatUser
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatListViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val folder: String = "inbox",
        val loading: Boolean = true,
        val refreshing: Boolean = false,
        val items: List<ChatConversation> = emptyList(),
        val users: Map<String, ChatUser> = emptyMap(),
        val nextCursor: String? = null,
        val error: String? = null,
        val requestCount: Int = 0,
        val unreadMessageCount: Int = 0,
        /** 正在输入该会话的对端用户 id（6 秒内有效）。 */
        val typingIds: Set<String> = emptySet(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var refreshJob: Job? = null
    private var typingJob: Job? = null
    private val typing = mutableSetOf<String>()

    init {
        viewModelScope.launch {
            app.chatRealtime.pushes.collect { push ->
                if (push.type == "typing") {
                    val id = push.conversationId ?: return@collect
                    if (push.userId != null && push.userId != app.currentUserId.value) {
                        typing.add(id)
                        _state.update { it.copy(typingIds = typing.toSet()) }
                        typingJob?.cancel()
                        typingJob = viewModelScope.launch {
                            delay(6_000)
                            typing.clear()
                            _state.update { it.copy(typingIds = emptySet()) }
                        }
                    }
                } else if (push.update != null) {
                    scheduleRealtimeRefresh()
                }
            }
        }
        viewModelScope.launch {
            app.chatRealtime.syncPages.collect { scheduleRealtimeRefresh() }
        }
    }

    private fun scheduleRealtimeRefresh() {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            delay(400)
            loadFolder(_state.value.folder, silent = true)
            runCatching { app.chatApi.state() }
                .onSuccess { chatState ->
                    _state.update {
                        it.copy(
                            requestCount = chatState.requestCount,
                            unreadMessageCount = chatState.unreadMessageCount,
                        )
                    }
                    app.refreshMessageBadge()
                }
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refresh() {
        _state.update { it.copy(refreshing = true) }
        loadFolder(_state.value.folder)
        viewModelScope.launch {
            runCatching { app.chatApi.state() }
                .onSuccess { chatState ->
                    _state.update {
                        it.copy(
                            requestCount = chatState.requestCount,
                            unreadMessageCount = chatState.unreadMessageCount,
                        )
                    }
                    app.refreshMessageBadge()
                }
        }
    }

    fun selectFolder(folder: String) {
        if (folder == _state.value.folder) return
        loadFolder(folder)
    }

    private fun loadFolder(folder: String, silent: Boolean = false) {
        _state.update {
            if (silent) {
                it.copy(folder = folder, error = null)
            } else {
                // 下拉刷新保留当前列表，只显示顶部插图；切换文件夹才清空并显示加载占位
                val keepList = it.refreshing && folder == it.folder && it.items.isNotEmpty()
                it.copy(
                    folder = folder,
                    loading = !keepList,
                    error = null,
                    items = if (keepList) it.items else emptyList(),
                    nextCursor = if (keepList) it.nextCursor else null,
                )
            }
        }
        viewModelScope.launch {
            runCatching { app.chatApi.conversations(folder = folder, limit = 30) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            items = page.items,
                            users = it.users + page.users.associateBy { user -> user.id },
                            nextCursor = page.nextCursor,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            loading = false,
                            refreshing = false,
                            // 后台实时刷新失败时保留旧列表，不打断阅读
                            error = if (silent && it.items.isNotEmpty()) null else ChatCatalog.errorText(e),
                        )
                    }
                }
        }
    }

    fun loadMore() {
        val cursor = _state.value.nextCursor ?: return
        if (_state.value.loading) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching {
                app.chatApi.conversations(folder = _state.value.folder, cursor = cursor, limit = 30)
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        loading = false,
                        items = it.items + page.items,
                        users = it.users + page.users.associateBy { user -> user.id },
                        nextCursor = page.nextCursor,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false) }
                _message.value = ChatCatalog.errorText(e)
            }
        }
    }
}

/** 私信会话列表：全部 / 消息请求 / 已归档。 */
@Composable
fun ChatListScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val vm: ChatListViewModel = viewModel { ChatListViewModel(app) }
    val state by vm.state.collectAsState()
    val message by vm.message.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val meId by app.currentUserId.collectAsState()
    val realtimeConnected by app.chatRealtime.connected.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }
    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHost.showSnackbar(text)
        vm.clearMessage()
    }

    val filters = listOf(
        "inbox" to "全部",
        "requests" to if (state.requestCount > 0) "消息请求 ${state.requestCount}" else "消息请求",
        "archive" to "已归档",
    )
    val emptyText = when (state.folder) {
        "requests" -> "没有待处理的消息请求"
        "archive" -> "没有归档的对话"
        else -> "还没有私信，去用户主页打个招呼吧"
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "私信",
                subtitle = when {
                    state.unreadMessageCount > 0 -> "${state.unreadMessageCount} 条未读消息"
                    !realtimeConnected -> "实时通道未连接，可手动刷新"
                    else -> "与站内用户一对一聊天"
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
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "私信设置")
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbarHost) },
    ) { padding ->
        KungalPullToRefresh(
            isRefreshing = state.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                FilterChips(
                    options = filters,
                    selected = state.folder,
                    onSelect = vm::selectFolder,
                )
                when {
                    state.loading && state.items.isEmpty() -> LoadingBox(Modifier.padding(vertical = 24.dp))
                    state.error != null && state.items.isEmpty() -> ErrorBox(
                        message = state.error!!,
                        onRetry = vm::refresh,
                    )
                    state.items.isEmpty() -> EmptyBox(emptyText)
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(state.items, key = { it.id }) { conversation ->
                            ConversationRow(
                                conversation = conversation,
                                peer = conversation.peerId?.let { state.users[it] },
                                meId = meId,
                                typing = conversation.id in state.typingIds,
                                onClick = { onOpenConversation(conversation.id) },
                            )
                        }
                        item {
                            if (state.loading) {
                                LoadingBox()
                            } else if (state.nextCursor != null) {
                                TextButton(onClick = vm::loadMore, modifier = Modifier.fillMaxWidth()) {
                                    Text("加载更多")
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConversationRow(
    conversation: ChatConversation,
    peer: ChatUser?,
    meId: String?,
    typing: Boolean,
    onClick: () -> Unit,
) {
    val title = conversation.title?.takeIf { it.isNotBlank() }
        ?: ChatCatalog.displayName(peer)
    val last = conversation.lastMessage
    val time = last?.createdAt ?: conversation.createdAt
    val unread = conversation.me.unreadCount
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AvatarImage(url = peer?.avatar, modifier = Modifier.size(46.dp), seed = peer?.name)
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = relativeTime(time),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = if (typing) "正在输入…" else ChatCatalog.preview(last, meId),
                        style = MaterialTheme.typography.bodySmall,
                        color = if (typing) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (unread > 0) {
                        Spacer(Modifier.width(8.dp))
                        Badge {
                            Text(if (unread > 99) "99+" else "$unread")
                        }
                    } else if (conversation.me.markedUnread) {
                        Spacer(Modifier.width(8.dp))
                        Badge { Text("·") }
                    }
                }
            }
        }
    }
}
