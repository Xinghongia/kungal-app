package com.kungal.app.ui.user

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.FollowEdge
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FollowListViewModel(
    private val app: KungalApp,
    private val userId: String,
    private val followers: Boolean,
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val items: List<FollowEdge> = emptyList(),
        val nextCursor: String? = null,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                if (followers) app.userApi.followers(userId) else app.userApi.following(userId)
            }.onSuccess { page ->
                _state.update { it.copy(loading = false, items = page.items, nextCursor = page.nextCursor) }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = friendlyError(e)) }
            }
        }
    }

    fun loadMore() {
        val cursor = _state.value.nextCursor ?: return
        if (_state.value.loading) return
        _state.update { it.copy(loading = true) }
        viewModelScope.launch {
            runCatching {
                if (followers) app.userApi.followers(userId, cursor) else app.userApi.following(userId, cursor)
            }.onSuccess { page ->
                _state.update {
                    it.copy(loading = false, items = it.items + page.items, nextCursor = page.nextCursor)
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = friendlyError(e)) }
            }
        }
    }
}

/** 关注/粉丝列表：点用户进入其主页。 */
@Composable
fun FollowListScreen(
    app: KungalApp,
    userId: String,
    followers: Boolean,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
) {
    val vm: FollowListViewModel = viewModel(key = "follow-$userId-$followers") {
        FollowListViewModel(app, userId, followers)
    }
    val state by vm.state.collectAsState()
    val title = if (followers) "粉丝" else "关注"

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = title,
                subtitle = "UID $userId",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading && state.items.isEmpty() -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null && state.items.isEmpty() -> ErrorBox(
                state.error!!,
                onRetry = vm::refresh,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            state.items.isEmpty() -> Text(
                "还没有${if (followers) "粉丝" else "关注"}",
                modifier = Modifier.padding(padding).padding(32.dp),
            )
            else -> LazyColumn(Modifier.fillMaxSize().padding(padding)) {
                items(state.items, key = { it.user?.id ?: it.followedAt }) { edge ->
                    val user = edge.user ?: return@items
                    UserRow(
                        name = user.name,
                        id = user.id,
                        avatarUrl = user.avatar?.url,
                        subtitle = "关注于 " + relativeTime(edge.followedAt),
                        onClick = { onOpenUser(user.id) },
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
