package com.kungal.app.ui.profile

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.Bookmarks
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ShoppingBag
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Subscriptions
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material.icons.outlined.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.BuildConfig
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.GroupTone
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.SettingsGroup
import com.kungal.app.ui.user.FramedAvatar
import com.kungal.app.ui.util.AndroidActions
import kotlinx.coroutines.launch

private const val SITE_URL = "https://www.kungal.com"

/**
 * 「我的」页：未登录 = 登录入口；已登录 = 账号卡 + 与网站头像菜单一致的九项入口
 * （萌萌点/商店/签到/个人主页/消息/私信/创作者申请/账号切换/退出）。
 */
@Composable
fun ProfileScreen(
    app: KungalApp,
    onOpenLogin: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenMessages: () -> Unit,
    onOpenSubscriptions: () -> Unit,
    onOpenSelfProfile: () -> Unit,
    onOpenCollections: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPoints: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenCreator: () -> Unit,
    onSwitchAccount: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
) {
    val vm: ProfileViewModel = viewModel { ProfileViewModel(app) }
    val state by vm.state.collectAsState()
    val session by app.session.collectAsState()
    val accountRevision by app.accountRevision.collectAsState()
    val checkingIn by vm.checkingIn.collectAsState()
    val message by vm.message.collectAsState()
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var confirmLogout by remember { mutableStateOf(false) }
    var confirmSwitch by remember { mutableStateOf(false) }
    var showCheckIn by remember { mutableStateOf(false) }

    LaunchedEffect(session, accountRevision) { vm.refresh() }

    LaunchedEffect(message) {
        message?.let {
            snackbar.showSnackbar(it)
            vm.clearMessage()
        }
    }

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
    if (confirmSwitch) {
        AlertDialog(
            onDismissRequest = { confirmSwitch = false },
            title = { Text("账号切换") },
            text = { Text("将退出当前账号，然后重新走网页登录并选择另一个账号。继续吗？") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmSwitch = false
                        scope.launch {
                            app.logout()
                            onSwitchAccount()
                        }
                    },
                ) { Text("切换") }
            },
            dismissButton = {
                TextButton(onClick = { confirmSwitch = false }) { Text("取消") }
            },
        )
    }
    if (showCheckIn) {
        val ready = state as? ProfileViewModel.UiState.Ready
        ShowCheckInDialog(
            alreadyChecked = ready?.me?.hasCheckedInToday == true,
            checkingIn = checkingIn,
            onConfirm = {
                showCheckIn = false
                vm.checkIn()
            },
            onDismiss = { showCheckIn = false },
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
        snackbarHost = { KungalSnackbarHost(snackbar) },
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
                    onOpenSubscriptions = onOpenSubscriptions,
                    onOpenSelfProfile = onOpenSelfProfile,
                    onOpenCollections = onOpenCollections,
                    onEditProfile = onEditProfile,
                    onOpenPoints = onOpenPoints,
                    onOpenShop = onOpenShop,
                    onOpenCreator = onOpenCreator,
                    onCheckIn = { showCheckIn = true },
                    onSwitchAccount = { confirmSwitch = true },
                    onOpenFollowers = onOpenFollowers,
                    onOpenFollowing = onOpenFollowing,
                    onLogout = { confirmLogout = true },
                )
            }
        }
    }
}

@Composable
private fun ShowCheckInDialog(
    alreadyChecked: Boolean,
    checkingIn: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("每日签到") },
        text = {
            Text(
                if (alreadyChecked) "今天已经签到过了，明天再来吧。"
                else "每日签到随机获得 0–7 萌萌点。确定现在签到吗？",
            )
        },
        confirmButton = {
            if (alreadyChecked) {
                TextButton(onClick = onDismiss) { Text("知道了") }
            } else {
                TextButton(onClick = onConfirm, enabled = !checkingIn) {
                    Text(if (checkingIn) "签到中…" else "签到")
                }
            }
        },
        dismissButton = if (alreadyChecked) null else {
            { TextButton(onClick = onDismiss) { Text("取消") } }
        },
    )
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
    onOpenSubscriptions: () -> Unit,
    onOpenSelfProfile: () -> Unit,
    onOpenCollections: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenPoints: () -> Unit,
    onOpenShop: () -> Unit,
    onOpenCreator: () -> Unit,
    onCheckIn: () -> Unit,
    onSwitchAccount: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
    onLogout: () -> Unit,
) {
    val me = state.me
    val account = state.account
    val profile = state.profile
    val counts = profile?.counts

    // 账号卡与网站主页头部一致：头像、昵称、角色、注册时间、8 项统计
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable(onClick = onOpenSelfProfile),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FramedAvatar(
                    avatarUrl = account.avatar?.url,
                    frame = profile?.avatarFrame,
                    size = 76.dp,
                    seed = account.name,
                )
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = account.name ?: "用户 ${account.id}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RoleBadge(roleChips(account.roles).first())
                        if (me.isCreator) Chip("创作者")
                    }
                    Text(
                        text = "注册于 " + (profile?.createdAt?.take(10) ?: "—") + " · UID ${account.id}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

            Row(Modifier.fillMaxWidth()) {
                ProfileStatCell(
                    value = profile?.moemoepoint ?: me.moemoepoint,
                    label = "萌萌点",
                    accent = true,
                    modifier = Modifier.weight(1f),
                )
                ProfileStatCell(counts?.topicCount, "话题", Modifier.weight(1f))
                ProfileStatCell(counts?.publishedGalgameCount, "Galgame", Modifier.weight(1f))
                ProfileStatCell(counts?.galgameRatingCount, "评分", Modifier.weight(1f))
            }
            Row(Modifier.fillMaxWidth()) {
                ProfileStatCell(counts?.receivedLikeCount, "被赞", Modifier.weight(1f))
                ProfileStatCell(counts?.receivedUpvoteCount, "被推", Modifier.weight(1f))
                ProfileStatCell(
                    value = counts?.followingCount,
                    label = "关注",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFollowing,
                )
                ProfileStatCell(
                    value = counts?.followerCount,
                    label = "粉丝",
                    modifier = Modifier.weight(1f),
                    onClick = onOpenFollowers,
                )
            }
        }
    }

    SettingsGroup(title = "我的") {
        item(
            headline = "萌萌点",
            supporting = "余额与收支流水",
            icon = Icons.Outlined.Star,
            onClick = onOpenPoints,
            trailing = { TrailingValue("${me.moemoepoint}") },
        )
        item(
            headline = "萌萌点商店",
            supporting = "装扮、兑换码与功能解锁",
            icon = Icons.Outlined.ShoppingBag,
            onClick = onOpenShop,
            trailing = { TrailingChevron() },
        )
        item(
            headline = "每日签到",
            supporting = if (me.hasCheckedInToday) "今日已签到" else "签到随机获得 0–7 萌萌点",
            icon = Icons.Outlined.CheckCircle,
            tone = GroupTone.PRIMARY,
            onClick = onCheckIn,
        )
        item(
            headline = "个人主页",
            supporting = "动态、游戏库、收藏与作品",
            icon = Icons.Outlined.Person,
            onClick = onOpenSelfProfile,
            trailing = { TrailingChevron() },
        )
        item(
            headline = "我的收藏夹",
            supporting = "收藏夹管理与收藏的作品",
            icon = Icons.Outlined.Bookmarks,
            onClick = onOpenCollections,
            trailing = { TrailingChevron() },
        )
    }

    SettingsGroup(title = "消息与创作") {
        item(
            headline = "我的消息",
            supporting = if (me.hasUnreadMessages) "有新消息" else "通知与关注动态",
            icon = Icons.Outlined.Mail,
            onClick = onOpenMessages,
            trailing = { TrailingChevron() },
        )
        item(
            headline = "私信",
            supporting = "会话与实时聊天",
            icon = Icons.Outlined.Chat,
            onClick = onOpenMessages,
            trailing = { TrailingChevron() },
        )
        item(
            headline = "创作者申请",
            supporting = if (me.isCreator) "你已经是创作者" else "查看资格进度并申请",
            icon = Icons.Outlined.Verified,
            onClick = onOpenCreator,
            trailing = { TrailingChevron() },
        )
        item(
            headline = "我的订阅",
            supporting = "已订阅话题与新回复未读数",
            icon = Icons.Outlined.Subscriptions,
            onClick = onOpenSubscriptions,
            trailing = { TrailingChevron() },
        )
    }

    SettingsGroup(title = "账号") {
        item(
            headline = "账号切换",
            supporting = "退出当前账号并重新网页登录",
            icon = Icons.Outlined.SwapHoriz,
            onClick = onSwitchAccount,
            trailing = { TrailingChevron() },
        )
        item(
            headline = "编辑资料",
            supporting = "头像、昵称、签名与隐私",
            icon = Icons.Outlined.Badge,
            onClick = onEditProfile,
            trailing = { TrailingChevron() },
        )
        item(
            headline = "设置",
            supporting = "外观、主题、默认排序与关于",
            icon = Icons.Outlined.Settings,
            onClick = onOpenSettings,
            trailing = { TrailingChevron() },
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
private fun RoleBadge(label: String) {
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp),
        )
    }
}

/** 账号卡统计格：数值 + 标签；null 显示「—」，关注/粉丝可点。 */
@Composable
private fun ProfileStatCell(
    value: Int?,
    label: String,
    modifier: Modifier = Modifier,
    accent: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .then(
                if (onClick != null) {
                    Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onClick)
                } else {
                    Modifier
                },
            )
            .padding(vertical = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value?.toString() ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (accent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TrailingChevron() {
    Icon(
        Icons.Outlined.KeyboardArrowRight,
        contentDescription = null,
        modifier = Modifier.size(20.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun TrailingValue(value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Icon(
            Icons.Outlined.KeyboardArrowRight,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
