package com.kungal.app.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.AlertDialog
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationListViewModel(
    private val app: KungalApp,
    private val muted: Boolean,
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val loadingMore: Boolean = false,
        val items: List<Notification> = emptyList(),
        val nextCursor: String? = null,
        val error: String? = null,
        val filters: List<Pair<String, String>> = listOf("" to "全部"),
        val filter: String = "",
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private var mutedTypes: Set<String> = emptySet()
    private var prefsLoaded = false

    init {
        refresh()
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refresh() {
        if (!app.isLoggedIn) {
            _state.value = State(loading = false, error = "登录后查看通知")
            return
        }
        _state.update { it.copy(loading = true, loadingMore = false, error = null) }
        viewModelScope.launch {
            loadPreferences()
            val available = NotificationCatalog.notificationTypes.filter {
                if (muted) it in mutedTypes else it !in mutedTypes
            }
            val filters = listOf("" to "全部") +
                available.map { it to NotificationCatalog.typeLabel(it) }
            val filter = _state.value.filter.takeIf { current -> filters.any { it.first == current } }.orEmpty()
            _state.update { it.copy(filters = filters, filter = filter) }

            runCatching {
                app.notificationApi.notifications(
                    isMuted = muted,
                    limit = 30,
                    type = filter.ifBlank { null },
                )
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        loading = false,
                        items = page.items,
                        nextCursor = page.nextCursor,
                        filter = filter,
                    )
                }
                markLoadedRead(page.items)
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = friendlyError(e)) }
            }
        }
    }

    private suspend fun loadPreferences() {
        if (prefsLoaded) return
        runCatching { app.notificationApi.preferences() }
            .onSuccess {
                prefsLoaded = true
                mutedTypes = it.mutedTypes.toSet()
            }
    }

    fun selectFilter(type: String) {
        if (type == _state.value.filter) return
        _state.update { it.copy(filter = type, items = emptyList(), nextCursor = null) }
        refresh()
    }

    fun loadMore() {
        val cursor = _state.value.nextCursor ?: return
        if (_state.value.loading || _state.value.loadingMore) return
        _state.update { it.copy(loadingMore = true) }
        viewModelScope.launch {
            runCatching {
                app.notificationApi.notifications(
                    isMuted = muted,
                    cursor = cursor,
                    limit = 30,
                    type = _state.value.filter.ifBlank { null },
                )
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        loadingMore = false,
                        items = it.items + page.items,
                        nextCursor = page.nextCursor,
                    )
                }
                markLoadedRead(page.items)
            }.onFailure { e ->
                _state.update { it.copy(loadingMore = false) }
                _message.value = friendlyError(e)
            }
        }
    }

    fun delete(id: String) {
        viewModelScope.launch {
            runCatching { app.notificationApi.deleteNotification(id) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(items = state.items.filterNot { it.id == id })
                    }
                    _message.value = "已删除"
                    app.refreshMessageBadge()
                }
                .onFailure { e -> _message.value = friendlyError(e) }
        }
    }

    /** 进页/翻页后把已加载的未静音通知标为已读（与网页一致）；筛选视图不越界标记其他类型。 */
    private suspend fun markLoadedRead(items: List<Notification>) {
        if (muted) return
        if (_state.value.filter.isNotBlank()) return
        if (items.none { !it.isRead }) return
        val upTo = items.maxOfOrNull { it.id.toLongOrNull() ?: 0L } ?: return
        if (upTo <= 0L) return
        runCatching { app.notificationApi.markRead(upTo.toString(), isMuted = false) }
            .onSuccess {
                _state.update { state ->
                    state.copy(
                        items = state.items.map { item ->
                            val id = item.id.toLongOrNull() ?: 0L
                            if (id in 1..upTo) item.copy(isRead = true) else item
                        },
                    )
                }
                app.refreshMessageBadge()
            }
            .onFailure { e -> _message.value = friendlyError(e) }
    }
}

/** 通知列表；[muted] 为 true 时展示已静音分区的通知。 */
@Composable
fun NotificationListScreen(
    app: KungalApp,
    muted: Boolean,
    onBack: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenPath: (String) -> Unit,
) {
    val vm: NotificationListViewModel = viewModel { NotificationListViewModel(app, muted) }
    val state by vm.state.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<Notification?>(null) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        vm.clearMessage()
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = if (muted) "已静音的消息" else "通知",
                subtitle = if (muted) "被静音的类型不再点亮红点" else "进入列表后自动标记已读",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (!muted) {
                        IconButton(onClick = onOpenPreferences) {
                            Icon(Icons.Outlined.Settings, contentDescription = "通知设置")
                        }
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbar) },
    ) { padding ->
        KungalPullToRefresh(
            isRefreshing = state.loading && state.items.isNotEmpty(),
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                FilterChips(
                    options = state.filters,
                    selected = state.filter,
                    onSelect = vm::selectFilter,
                )
                when {
                    state.loading && state.items.isEmpty() ->
                        LoadingBox(Modifier.padding(vertical = 24.dp))
                    state.error != null && state.items.isEmpty() -> ErrorBox(
                        message = state.error!!,
                        onRetry = vm::refresh,
                    )
                    state.items.isEmpty() -> Text(
                        text = if (muted) "没有已静音的消息" else "没有通知",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                    else -> LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        items(state.items, key = { it.id }) { notification ->
                            NotificationRow(
                                notification = notification,
                                onOpen = { onOpenPath(notification.path) },
                                onDelete = { pendingDelete = notification },
                            )
                        }
                        item {
                            if (state.loadingMore) {
                                LoadingBox()
                            } else if (state.nextCursor != null) {
                                TextButton(
                                    onClick = vm::loadMore,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text("加载更多")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { notification ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("删除这条通知？") },
            text = { Text("删除后不可恢复，网页端的这条通知也会同步消失。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.delete(notification.id)
                        pendingDelete = null
                    },
                ) {
                    Text("删除")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDelete = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun NotificationRow(
    notification: Notification,
    onOpen: () -> Unit,
    onDelete: () -> Unit,
) {
    val isSystem = notification.notificationType == "system_notice"
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        color = if (notification.isRead) {
            MaterialTheme.colorScheme.surfaceContainerLow
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        },
    ) {
        Row(Modifier.padding(start = 12.dp, end = 4.dp, top = 10.dp, bottom = 10.dp)) {
            if (isSystem) {
                Box(
                    modifier = Modifier.size(40.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                Icons.Outlined.Notifications,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            } else {
                AvatarImage(
                    url = notification.actor.avatar?.url,
                    modifier = Modifier.size(40.dp),
                    seed = notification.actor.name,
                )
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = NotificationCatalog.messageText(notification),
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(top = 2.dp),
                    )
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除通知",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                }
                Text(
                    text = NotificationCatalog.preview(notification),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = relativeTime(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Chip(NotificationCatalog.typeLabel(notification.notificationType))
                }
            }
        }
    }
}
