package com.kungal.app.ui.profile

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
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.BuildConfig
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.SettingsGroup
import com.kungal.app.ui.util.AndroidActions
import kotlinx.coroutines.launch

private const val SITE_URL = "https://www.kungal.com"

/**
 * 「我的」页：未登录 = 登录入口；已登录 = 账号概览（萌萌点/签到/角色）。
 * 设置入口固定在右上角（沿用 saku 的布局约定）。
 */
@Composable
fun ProfileScreen(
    app: KungalApp,
    onOpenLogin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMessages: () -> Unit,
) {
    val vm: ProfileViewModel = viewModel { ProfileViewModel(app) }
    val state by vm.state.collectAsState()
    val session by app.session.collectAsState()
    val scope = rememberCoroutineScope()
    var confirmLogout by remember { mutableStateOf(false) }

    LaunchedEffect(session) { vm.refresh() }

    if (confirmLogout) {
        AlertDialog(
            onDismissRequest = { confirmLogout = false },
            title = { Text("退出登录") },
            text = { Text("退出后需要重新走网页登录，确定退出吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmLogout = false
                        scope.launch { app.logout() }
                    },
                ) { Text("退出") }
            },
            dismissButton = {
                TextButton(onClick = { confirmLogout = false }) { Text("取消") }
            },
        )
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "我的",
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "设置")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (val current = state) {
                ProfileViewModel.UiState.Loading -> LoadingBox()
                ProfileViewModel.UiState.LoggedOut -> LoggedOutContent(onOpenLogin)
                is ProfileViewModel.UiState.Error -> ErrorBox(current.message, onRetry = vm::refresh)
                is ProfileViewModel.UiState.Ready -> LoggedInContent(
                    state = current,
                    onOpenMessages = onOpenMessages,
                    onOpenSettings = onOpenSettings,
                    onLogout = { confirmLogout = true },
                )
            }
        }
    }
}

@Composable
private fun LoggedOutContent(onOpenLogin: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(30.dp),
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("未登录", style = MaterialTheme.typography.titleMedium)
                Text(
                    "登录后可获得与网页相同的访问权限",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    SettingsGroup(title = "账号与功能") {
        item(
            headline = "登录 / 切换账号",
            supporting = "在应用内网页登录（账号由 NextMoe·未萌 统一管理）",
            icon = Icons.Outlined.Login,
            onClick = onOpenLogin,
        )
    }

    AboutGroup()
}

@Composable
private fun LoggedInContent(
    state: ProfileViewModel.UiState.Ready,
    onOpenMessages: () -> Unit,
    onOpenSettings: () -> Unit,
    onLogout: () -> Unit,
) {
    val me = state.me
    val account = state.account

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(url = account.avatar?.url, modifier = Modifier.size(56.dp))
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = account.name ?: "用户 ${account.id}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "UID ${account.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                roleChips(account.roles).forEach { Chip(it) }
                Chip(if (me.hasCheckedInToday) "今日已签到" else "今日未签到")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Star,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "${me.moemoepoint} 萌萌点",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (me.isCreator) {
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "创作者",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
        }
    }

    SettingsGroup(title = "功能") {
        item(
            headline = "消息中心",
            supporting = if (me.hasUnreadMessages) "有新消息" else "通知与私信",
            icon = Icons.Outlined.Mail,
            onClick = onOpenMessages,
        )
        item(
            headline = "设置",
            supporting = "外观、主题、默认排序与关于",
            icon = Icons.Outlined.Settings,
            onClick = onOpenSettings,
        )
        item(
            headline = "账号资料",
            supporting = "头像、昵称与资料（BP-04 交付）",
            icon = Icons.Outlined.Badge,
        )
        item(
            headline = "退出登录",
            icon = Icons.AutoMirrored.Outlined.Logout,
            onClick = onLogout,
        )
    }

    AboutGroup()
}

@Composable
private fun AboutGroup() {
    val context = androidx.compose.ui.platform.LocalContext.current
    SettingsGroup(title = "关于") {
        item(
            headline = "版本",
            icon = Icons.Outlined.Info,
            trailing = {
                Text(BuildConfig.VERSION_NAME, color = MaterialTheme.colorScheme.onSurfaceVariant)
            },
        )
        item(
            headline = "鲲 Galgame 论坛",
            supporting = SITE_URL,
            icon = Icons.Outlined.Language,
            onClick = { AndroidActions.openInBrowser(context, SITE_URL) },
        )
        item(
            headline = "开源地址",
            supporting = "GitHub · Xinghongia/kungal-app",
            icon = Icons.Outlined.Code,
            onClick = { AndroidActions.openInBrowser(context, "https://github.com/Xinghongia/kungal-app") },
        )
        block {
            Text(
                "本应用是 kungal.com 的第三方客户端，非站点官方产品；" +
                    "所有内容与权限均来自站点官方接口，与网页保持一致。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun roleChips(roles: List<String>): List<String> {
    val known = mapOf(
        "user" to "用户",
        "creator" to "创作者",
        "moderator" to "版主",
        "admin" to "管理员",
    )
    val mapped = roles.map { known[it] ?: it }
    // 用户是默认角色，不必展示；最多展示 3 个
    return mapped.filter { it != "用户" }.take(3).ifEmpty { listOf("用户") }
}
