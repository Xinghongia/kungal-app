package com.kungal.app.ui.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.FollowedWall
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class WallsViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val items: List<FollowedWall> = emptyList(),
        val nextCursor: String? = null,
        val error: String? = null,
        val busyKey: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        refresh()
    }

    fun clearMessage() {
        _message.value = null
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { app.notificationApi.followedWalls(limit = 30) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(loading = false, items = page.items, nextCursor = page.nextCursor)
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e)) }
                }
        }
    }

    fun loadMore() {
        val cursor = _state.value.nextCursor ?: return
        if (_state.value.loading) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching { app.notificationApi.followedWalls(cursor = cursor, limit = 30) }
                .onSuccess { page ->
                    // 服务端可能省略论坛不认识的墙，按 type:id 去重
                    val seen = _state.value.items.map { "${it.subjectType}:${it.subjectId}" }.toSet()
                    _state.update {
                        it.copy(
                            loading = false,
                            items = it.items + page.items.filter { wall ->
                                "${wall.subjectType}:${wall.subjectId}" !in seen
                            },
                            nextCursor = page.nextCursor,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false) }
                    _message.value = friendlyError(e)
                }
        }
    }

    fun unfollow(wall: FollowedWall) {
        val key = "${wall.subjectType}:${wall.subjectId}"
        if (_state.value.busyKey != null) return
        _state.update { it.copy(busyKey = key) }
        viewModelScope.launch {
            runCatching { app.notificationApi.unfollowWall(wall.subjectType, wall.subjectId) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            busyKey = null,
                            items = state.items.filterNot {
                                it.subjectType == wall.subjectType && it.subjectId == wall.subjectId
                            },
                        )
                    }
                    _message.value = "已取消关注"
                }
                .onFailure { e ->
                    _state.update { it.copy(busyKey = null) }
                    _message.value = friendlyError(e)
                }
        }
    }
}

/** 关注的话题与评论区：列出已关注的评论区并支持取消关注。 */
@Composable
fun WallsScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    val vm: WallsViewModel = viewModel { WallsViewModel(app) }
    val state by vm.state.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var pendingUnfollow by remember { mutableStateOf<FollowedWall?>(null) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        vm.clearMessage()
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "关注的评论区",
                subtitle = "这些评论区有新评论时会通知你",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading && state.items.isEmpty() ->
                LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null && state.items.isEmpty() -> ErrorBox(
                message = state.error!!,
                onRetry = vm::refresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            state.items.isEmpty() -> Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "还没有关注任何评论区。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "在网页的 Galgame、评分、资源、题库、工具集或网站页面可以关注对应评论区；" +
                        "关注后会出现在这里。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                items(
                    state.items,
                    key = { "${it.subjectType}:${it.subjectId}" },
                ) { wall ->
                    WallRow(
                        wall = wall,
                        busy = state.busyKey == "${wall.subjectType}:${wall.subjectId}",
                        onOpen = {
                            onOpenWeb("https://www.kungal.com" + NotificationCatalog.wallWebPath(wall))
                        },
                        onUnfollow = { pendingUnfollow = wall },
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

    pendingUnfollow?.let { wall ->
        AlertDialog(
            onDismissRequest = { pendingUnfollow = null },
            title = { Text("取消关注这个评论区？") },
            text = { Text("取消后，这个评论区的新评论不会再通知你。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        vm.unfollow(wall)
                        pendingUnfollow = null
                    },
                ) {
                    Text("取消关注")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingUnfollow = null }) { Text("保留") }
            },
        )
    }
}

@Composable
private fun WallRow(
    wall: FollowedWall,
    busy: Boolean,
    onOpen: () -> Unit,
    onUnfollow: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onOpen),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            modifier = Modifier.padding(start = 14.dp, end = 6.dp, top = 10.dp, bottom = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = NotificationCatalog.wallTitle(wall),
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Chip(NotificationCatalog.wallTypeLabel(wall.subjectType))
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "评论区有新评论时通知",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            TextButton(onClick = onUnfollow, enabled = !busy) {
                Text(if (busy) "取消中…" else "取消关注")
            }
        }
    }
}
