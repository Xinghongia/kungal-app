package com.kungal.app.ui.messages

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.SettingsGroup

/**
 * 消息一级页：通知、已静音的消息、关注的话题与评论区、关注动态与私信入口。
 * 汇总数据来自 /me/notifications/summary，进入页面时刷新一次。
 */
@Composable
fun MessagesScreen(
    app: KungalApp,
    onOpenLogin: () -> Unit,
    onOpenFollowing: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenMuted: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenWalls: () -> Unit,
    onOpenChat: () -> Unit,
) {
    val session by app.session.collectAsState()
    val vm: MessagesViewModel = viewModel { MessagesViewModel(app) }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "消息",
                subtitle = state.summary?.let { summary ->
                    if (summary.unreadCount > 0) "${summary.unreadCount} 条未读通知" else "暂无未读通知"
                } ?: "通知、关注动态与私信",
            )
        },
    ) { padding ->
        KungalPullToRefresh(
            isRefreshing = state.refreshing,
            onRefresh = vm::refresh,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
            if (session == null) {
                LoggedOutCard(onOpenLogin = onOpenLogin)
            } else {
                when {
                    state.loading && state.summary == null ->
                        LoadingBox(Modifier.padding(vertical = 24.dp))
                    state.error != null && state.summary == null -> ErrorBox(
                        message = state.error!!,
                        onRetry = vm::refresh,
                    )
                    else -> {
                        val summary = state.summary
                        SettingsGroup(title = "通知") {
                            item(
                                headline = "通知",
                                supporting = summary?.latest
                                    ?.let { NotificationCatalog.preview(it) }
                                    ?.takeIf { it.isNotBlank() }
                                    ?: "还没有通知",
                                icon = Icons.Outlined.Notifications,
                                onClick = onOpenNotifications,
                                trailing = { UnreadCount(summary?.unreadCount ?: 0) },
                            )
                            item(
                                headline = "已静音的消息",
                                supporting = "被你静音的类型收在这里，不再点亮红点",
                                icon = Icons.Outlined.NotificationsOff,
                                onClick = onOpenMuted,
                                trailing = { UnreadCount(summary?.mutedUnreadCount ?: 0) },
                            )
                            item(
                                headline = "通知设置",
                                supporting = "选择哪些通知会出现在列表并计入未读",
                                icon = Icons.Outlined.Settings,
                                onClick = onOpenPreferences,
                            )
                        }
                        SettingsGroup(title = "关注") {
                            item(
                                headline = "我订阅的话题",
                                supporting = "有新回复时显示未读数",
                                icon = Icons.Outlined.Timeline,
                                onClick = onOpenSubscriptions,
                            )
                            item(
                                headline = "关注的评论区",
                                supporting = "Galgame / 评分 / 资源 / 题库 / 工具集 / 网站评论区",
                                icon = Icons.Outlined.Forum,
                                onClick = onOpenWalls,
                            )
                            item(
                                headline = "关注动态",
                                supporting = "你关注的人发布、回复、评论与评分",
                                icon = Icons.Outlined.Timeline,
                                onClick = onOpenFollowing,
                            )
                        }
                        SettingsGroup(title = "私信") {
                            item(
                                headline = "私信",
                                supporting = if (summary != null && summary.isDirectMessageMuted) {
                                    "已静音私信通知；未读消息仍可在这里查看"
                                } else {
                                    "与站内用户一对一聊天"
                                },
                                icon = Icons.Outlined.Mail,
                                onClick = onOpenChat,
                                trailing = {
                                    UnreadCount(
                                        if (summary?.isDirectMessageMuted == true) {
                                            0
                                        } else {
                                            summary?.directMessageUnreadCount ?: 0
                                        },
                                    )
                                },
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
private fun LoggedOutCard(onOpenLogin: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Mail,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("登录后查看消息", style = MaterialTheme.typography.titleMedium)
                Text(
                    "通知与私信需要登录，账号与网页一致。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            androidx.compose.material3.TextButton(onClick = onOpenLogin) { Text("登录") }
        }
    }
}

@Composable
private fun UnreadCount(count: Int) {
    if (count <= 0) return
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
    ) {
        Text(
            text = if (count > 99) "99+" else count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
        )
    }
}
