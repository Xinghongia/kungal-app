package com.kungal.app.ui.user

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.fromHtml
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.ReportDialog
import com.kungal.app.ui.components.TabChips
import com.kungal.app.ui.util.AndroidActions
import com.kungal.app.ui.util.fullDateTime
import com.kungal.core.model.UserFollowState
import com.kungal.core.model.UserProfile

/** 用户主页标签（顺序与网站一致；本人额外显示游玩时长）。 */
enum class UserTab(val label: String, val selfOnly: Boolean = false) {
    ACTIVITY("动态"),
    LIBRARY("游戏库"),
    COLLECTION("收藏"),
    TOPIC("话题"),
    GALGAME("Galgame"),
    RATING("评分"),
    RESOURCE("资源"),
    QUIZ("题库"),
    TOOLSET("工具集"),
    PLAYTIME("游玩时长", selfOnly = true),
    ABOUT("关于"),
}

/**
 * 任意用户主页：头部资料与统计、关注/取关与通知档位、举报，以及网站同款的内容标签页。
 * 详情类页面（作品/评分/题库/工具集/收藏夹）尚未在 App 内实现，先跳网页。
 */
@Composable
fun UserProfileScreen(
    app: KungalApp,
    userId: String,
    nsfwDisplay: NsfwDisplayMode,
    initialTab: String? = null,
    onBack: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenFollowList: (userId: String, followers: Boolean) -> Unit,
    onOpenChat: (userId: String) -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenToolset: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
    onEditProfile: () -> Unit,
) {
    val vm: UserProfileViewModel = viewModel(key = "user-profile-$userId") {
        UserProfileViewModel(app, userId)
    }
    val state by vm.state.collectAsState()
    val session by app.session.collectAsState()
    val accountRevision by app.accountRevision.collectAsState()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val includeNsfw = nsfwDisplay.allowsNsfw
    var selectedTab by remember(userId, initialTab) {
        mutableStateOf(if (initialTab == "resource-expire") UserTab.RESOURCE else UserTab.ACTIVITY)
    }
    val resourceInitialState = if (initialTab == "resource-expire") "expired" else "valid"
    var overflowOpen by remember { mutableStateOf(false) }
    var reportOpen by remember { mutableStateOf(false) }

    LaunchedEffect(session, accountRevision) { vm.refresh() }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            vm.clearMessage()
        }
    }
    if (reportOpen) {
        LaunchedEffect(Unit) { vm.loadReportReasons() }
        ReportDialog(
            title = "举报用户",
            reasons = state.reportReasons,
            busy = state.reporting,
            onSubmit = { key, note ->
                vm.report(key, note)
                reportOpen = false
            },
            onDismiss = { reportOpen = false },
        )
    }

    val profile = state.profile
    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = profile?.name ?: "用户主页",
                subtitle = "UID $userId",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (vm.isSelf) {
                        IconButton(onClick = onEditProfile) {
                            Icon(Icons.Outlined.Edit, contentDescription = "编辑资料")
                        }
                    } else {
                        Box {
                            IconButton(onClick = { overflowOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(
                                expanded = overflowOpen,
                                onDismissRequest = { overflowOpen = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                DropdownMenuItem(
                                    text = { Text("分享主页") },
                                    onClick = {
                                        overflowOpen = false
                                        AndroidActions.shareText(
                                            context,
                                            "https://www.kungal.com/user/$userId",
                                        )
                                    },
                                )
                                if (app.isLoggedIn) {
                                    DropdownMenuItem(
                                        text = { Text("发私信") },
                                        onClick = {
                                            overflowOpen = false
                                            onOpenChat(userId)
                                        },
                                    )
                                    DropdownMenuItem(
                                        text = { Text("举报用户", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            overflowOpen = false
                                            reportOpen = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbarHost) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading && profile == null -> LoadingBox(Modifier.fillMaxSize())
                state.error != null && profile == null -> ErrorBox(
                    message = state.error!!,
                    onRetry = vm::refresh,
                    modifier = Modifier.fillMaxSize(),
                )
                profile != null -> {
                    val tabs = UserTab.entries.filter { !it.selfOnly || vm.isSelf }
                    // 头部与标签一起放进内容列表，随列表一起上下滚动（不吸顶）
                    val headerContent: @Composable () -> Unit = {
                        ProfileHeader(
                            profile = profile,
                            isSelf = vm.isSelf,
                            loggedIn = app.isLoggedIn,
                            backgroundUrl = state.backgroundUrl,
                            follow = state.follow,
                            busy = state.followBusy,
                            onFollow = {
                                if (app.isLoggedIn) vm.toggleFollow() else onOpenLogin()
                            },
                            onNotify = vm::setNotify,
                            onRequireLogin = onOpenLogin,
                            onEditProfile = onEditProfile,
                            onOpenFollowers = { onOpenFollowList(userId, true) },
                            onOpenFollowing = { onOpenFollowList(userId, false) },
                        )
                        TabChips(
                            labels = tabs.map { it.label },
                            selected = selectedTab.label,
                            onSelect = { label ->
                                tabs.firstOrNull { it.label == label }?.let { selectedTab = it }
                            },
                            modifier = Modifier.padding(bottom = 6.dp),
                        )
                    }
                    Box(Modifier.fillMaxWidth().weight(1f)) {
                        UserTabContent(
                            tab = selectedTab,
                            app = app,
                            userId = userId,
                            profile = profile,
                            isSelf = vm.isSelf,
                            includeNsfw = includeNsfw,
                            blurNsfw = nsfwDisplay.blursMedia,
                            header = headerContent,
                            onSelectTab = { selectedTab = it },
                            onOpenTopic = onOpenTopic,
                            onOpenUser = onOpenUser,
                            onOpenResource = onOpenResource,
                            onOpenToolset = onOpenToolset,
                            onOpenWeb = onOpenWeb,
                            resourceInitialState = resourceInitialState,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile,
    isSelf: Boolean,
    loggedIn: Boolean,
    backgroundUrl: String?,
    follow: UserFollowState?,
    busy: Boolean,
    onFollow: () -> Unit,
    onNotify: (String) -> Unit,
    onRequireLogin: () -> Unit,
    onEditProfile: () -> Unit,
    onOpenFollowers: () -> Unit,
    onOpenFollowing: () -> Unit,
) {
    var notifyMenu by remember { mutableStateOf(false) }
    val counts = profile.counts
    Column(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!backgroundUrl.isNullOrBlank()) {
            KungalImage(
                url = backgroundUrl,
                contentDescription = "主页背景",
                modifier = Modifier.fillMaxWidth().height(120.dp),
                shape = RoundedCornerShape(16.dp),
                contentScale = ContentScale.Crop,
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            FramedAvatar(
                avatarUrl = profile.avatar?.url,
                frame = profile.avatarFrame,
                size = 68.dp,
                seed = profile.name,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = profile.name ?: "已注销用户",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    roleChips(profile.roles).forEach { Chip(it) }
                }
                val bio = profile.bio
                if (!bio.isNullOrBlank()) {
                    Text(
                        text = bio,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "注册于 " + profile.createdAt.take(10).ifBlank { "未知" },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // 8 项统计（与网站头部一致；关注/粉丝可点）
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            StatCell(profile.moemoepoint, "萌萌点", Modifier.width(54.dp))
            StatCell(counts.topicCount, "话题", Modifier.width(54.dp))
            StatCell(counts.publishedGalgameCount, "Galgame", Modifier.width(64.dp))
            StatCell(counts.galgameRatingCount, "评分", Modifier.width(54.dp))
            StatCell(counts.receivedLikeCount, "被赞", Modifier.width(54.dp))
            StatCell(counts.receivedUpvoteCount, "被推", Modifier.width(54.dp))
            StatCell(counts.followingCount, "关注", Modifier.width(54.dp), onClick = onOpenFollowing)
            StatCell(counts.followerCount, "粉丝", Modifier.width(54.dp), onClick = onOpenFollowers)
        }

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (isSelf) {
                OutlinedButton(onClick = onEditProfile, modifier = Modifier.weight(1f)) {
                    Text("编辑资料")
                }
            } else {
                val following = follow?.isFollowing == true
                val followedBy = follow?.isFollowedBy == true
                Button(
                    onClick = { if (loggedIn) onFollow() else onRequireLogin() },
                    enabled = !busy,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        when {
                            !following -> "关注"
                            followedBy -> "互相关注"
                            else -> "已关注"
                        },
                    )
                }
                if (following) {
                    Box {
                        OutlinedButton(onClick = { notifyMenu = true }, enabled = !busy) {
                            Text(
                                if (follow?.notifyLevel == "feed") "仅关注流" else "全部动态",
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                            )
                        }
                        DropdownMenu(
                            expanded = notifyMenu,
                            onDismissRequest = { notifyMenu = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            DropdownMenuItem(
                                text = { Text("接收全部动态") },
                                onClick = {
                                    notifyMenu = false
                                    onNotify("all")
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("只进关注流") },
                                onClick = {
                                    notifyMenu = false
                                    onNotify("feed")
                                },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun roleChips(roles: List<String>): List<String> {
    val known = mapOf(
        "ren" to "莲",
        "admin" to "管理员",
        "moderator" to "版主",
        "creator" to "创作者",
    )
    val mapped = roles.map { known[it] ?: it }
    return mapped.take(3).ifEmpty { listOf("普通用户") }
}

@Composable
private fun UserTabContent(
    tab: UserTab,
    app: KungalApp,
    userId: String,
    profile: UserProfile,
    isSelf: Boolean,
    includeNsfw: Boolean,
    blurNsfw: Boolean,
    header: @Composable () -> Unit,
    onSelectTab: (UserTab) -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenToolset: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
    resourceInitialState: String = "valid",
) {
    when (tab) {
        UserTab.ACTIVITY -> ActivityOverviewTab(
            app = app,
            userId = userId,
            includeNsfw = includeNsfw,
            blurNsfw = blurNsfw,
            header = header,
            onOpenTopic = onOpenTopic,
            onOpenWeb = onOpenWeb,
            onSelectTab = onSelectTab,
        )
        UserTab.LIBRARY -> LibraryTab(app, userId, includeNsfw, blurNsfw, header, onOpenWeb)
        UserTab.COLLECTION -> CollectionTab(app, userId, includeNsfw, header, onOpenTopic, onOpenWeb)
        UserTab.TOPIC -> TopicsTab(app, userId, includeNsfw, header, onOpenTopic)
        UserTab.GALGAME -> WorksTab(app, userId, includeNsfw, blurNsfw, header, onOpenWeb)
        UserTab.RATING -> RatingsTab(app, userId, includeNsfw, blurNsfw, header, onOpenWeb)
        UserTab.RESOURCE -> ResourcesTab(
            app = app,
            userId = userId,
            isSelf = isSelf,
            includeNsfw = includeNsfw,
            blurNsfw = blurNsfw,
            header = header,
            initialState = resourceInitialState,
            onOpenResource = onOpenResource,
        )
        UserTab.QUIZ -> QuizTab(app, userId, isSelf, header, onOpenWeb)
        UserTab.TOOLSET -> ToolsetTab(
            app = app,
            userId = userId,
            header = header,
            onOpenWeb = onOpenWeb,
            onOpenToolset = onOpenToolset,
        )
        UserTab.PLAYTIME -> PlaytimeTab(app, header, onOpenWeb)
        UserTab.ABOUT -> AboutTab(profile, userId, header)
    }
}

/** 关于：网站「关于」页的统计与资料。 */
@Composable
fun AboutTab(profile: UserProfile, userId: String, header: @Composable () -> Unit) {
    val c = profile.counts
    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
    ) {
        header()
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionHeader("内容")
            StatGridRow(listOf("话题" to c.topicCount, "投票" to c.pollCount))
            StatGridRow(listOf("抽奖" to c.lotteryCount, "回复" to c.replyCount))
            StatGridRow(listOf("话题评论" to c.topicCommentCount, "社区评论" to c.communityCommentCount))
            StatGridRow(listOf("Galgame" to c.publishedGalgameCount, "贡献" to c.contributedGalgameCount))
            StatGridRow(listOf("评分" to c.galgameRatingCount, "资源" to c.galgameResourceCount))
            StatGridRow(listOf("工具" to c.toolsetCount, "工具资源" to c.toolsetResourceCount))
            SectionHeader("互动")
            StatGridRow(listOf("被推" to c.receivedUpvoteCount, "被赞" to c.receivedLikeCount))
            StatGridRow(listOf("被踩" to c.receivedDislikeCount))
            SectionHeader("资料")
            StatGridRow(listOf("注册序号" to userId.toIntOrNull(), "今日话题" to c.topicTodayCount))
            StatGridRow(listOf("今日作品" to c.publishedGalgameTodayCount))
            SimpleContentRow(
                title = "注册时间",
                subtitle = fullDateTime(profile.createdAt),
            )
            val bio = profile.bio
            val aboutHtml = profile.aboutHtml
            if (!aboutHtml.isNullOrBlank()) {
                SectionHeader("介绍")
                Text(
                    text = AnnotatedString.fromHtml(aboutHtml),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            } else if (!bio.isNullOrBlank()) {
                SectionHeader("签名")
                Text(
                    text = bio,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
