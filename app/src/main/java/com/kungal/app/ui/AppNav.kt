package com.kungal.app.ui

import android.net.Uri
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Forum
import androidx.compose.material.icons.outlined.Mail
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kungal.app.KungalApp
import com.kungal.app.Settings
import com.kungal.app.ui.components.AppMessageHost
import com.kungal.app.ui.hub.ComingSoonScreen
import com.kungal.app.ui.hub.DiscoverSections
import com.kungal.app.ui.hub.GalgameSections
import com.kungal.app.ui.hub.HubEntry
import com.kungal.app.ui.hub.HubScreen
import com.kungal.app.ui.hub.PublishHubScreen
import com.kungal.app.ui.activity.FollowingScreen
import com.kungal.app.ui.chat.ChatConversationScreen
import com.kungal.app.ui.chat.ChatListScreen
import com.kungal.app.ui.chat.ChatSettingsScreen
import com.kungal.app.ui.creator.CreatorScreen
import com.kungal.app.ui.editor.DraftsScreen
import com.kungal.app.ui.editor.TopicEditorScreen
import com.kungal.app.ui.galgame.GalgameListMode
import com.kungal.app.ui.galgame.GalgameListScreen
import com.kungal.app.ui.galgame.WorkDetailScreen
import com.kungal.app.ui.login.LoginScreen
import com.kungal.app.ui.messages.MessagesScreen
import com.kungal.app.ui.messages.NotificationListScreen
import com.kungal.app.ui.messages.NotificationPreferencesScreen
import com.kungal.app.ui.messages.WallsScreen
import com.kungal.app.ui.points.PointsScreen
import com.kungal.app.ui.profile.ProfileScreen
import com.kungal.app.ui.ranking.RankingScreen
import com.kungal.app.ui.search.TopicSearchScreen
import com.kungal.app.ui.settings.CloudPreferencesScreen
import com.kungal.app.ui.settings.SettingsScreen
import com.kungal.app.ui.shop.ShopScreen
import com.kungal.app.ui.subscriptions.SubscriptionsScreen
import com.kungal.app.ui.topic.TopicDetailScreen
import com.kungal.app.ui.topic.TopicListScreen
import com.kungal.app.ui.user.FollowListScreen
import com.kungal.app.ui.user.ProfileSettingsScreen
import com.kungal.app.ui.user.UserProfileScreen
import com.kungal.app.ui.util.AndroidActions

private object Routes {
    // 一级页面（底部导航）
    const val TOPIC = "tab/topic"
    const val GALGAME = "tab/galgame"
    const val DISCOVER = "tab/discover"
    const val MESSAGES = "tab/messages"
    const val PROFILE = "tab/profile"

    // 二级页面（压栈、隐藏底栏）
    const val TOPIC_DETAIL = "topic/{topicId}"
    const val LOGIN = "login"
    const val SETTINGS = "settings"
    const val CLOUD_PREFERENCES = "settings/preferences"
    const val PUBLISH = "publish"
    const val EDITOR = "editor?topicId={topicId}&draftId={draftId}"
    const val DRAFTS = "drafts"
    const val FOLLOWING = "following"
    const val SEARCH = "search"
    const val SUBSCRIPTIONS = "subscriptions"
    const val USER = "user/{userId}"
    const val FOLLOW_LIST = "user/{userId}/follow?followers={followers}"
    const val PROFILE_SETTINGS = "profile/settings"
    const val POINTS = "points"
    const val SHOP = "shop"
    const val CREATOR = "creator"
    const val NOTIFICATIONS = "messages/notifications"
    const val MUTED_NOTIFICATIONS = "messages/notifications/muted"
    const val NOTIFICATION_PREFERENCES = "messages/notifications/preferences"
    const val WALLS = "messages/walls"
    const val CHAT_LIST = "messages/chat"
    const val CHAT = "messages/chat/room?conversationId={conversationId}&userId={userId}"
    const val CHAT_SETTINGS = "messages/chat/settings"
    const val RANKING = "ranking?tab={tab}"
    const val GALGAME_WORKS = "galgame/works"
    const val GALGAME_LIBRARY = "galgame/library"
    const val GALGAME_WORK = "galgame/work/{workId}"
    const val SOON = "soon?title={title}&note={note}&path={path}"

    fun topic(id: String) = "topic/$id"
    fun user(id: String) = "user/$id"
    fun followList(userId: String, followers: Boolean) = "user/$userId/follow?followers=$followers"
    fun editor(topicId: String? = null, draftId: String? = null) =
        "editor?topicId=${Uri.encode(topicId.orEmpty())}&draftId=${Uri.encode(draftId.orEmpty())}"
    fun chat(conversationId: String? = null, userId: String? = null) =
        "messages/chat/room?conversationId=${Uri.encode(conversationId.orEmpty())}" +
            "&userId=${Uri.encode(userId.orEmpty())}"
    fun ranking(tab: String) = "ranking?tab=$tab"
    fun galgameWork(id: String) = "galgame/work/$id"
    fun soon(entry: HubEntry): String =
        "soon?title=${Uri.encode(entry.label)}&note=${Uri.encode(entry.note)}&path=${Uri.encode(entry.webPath)}"
}

private data class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.TOPIC, "话题", Icons.Outlined.Forum),
    Tab(Routes.GALGAME, "Galgame", Icons.Outlined.SportsEsports),
    Tab(Routes.DISCOVER, "发现", Icons.Outlined.Explore),
    Tab(Routes.MESSAGES, "消息", Icons.Outlined.Mail),
    Tab(Routes.PROFILE, "我的", Icons.Outlined.Person),
)

/**
 * 应用导航：底部五栏（话题 / Galgame / 发现 / 消息 / 我的）对应官方侧边栏的一级入口；
 * 详情、登录、设置、发布、功能占位页压栈显示并隐藏底栏。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNav(app: KungalApp) {
    val nav = rememberNavController()
    val settings by app.settingsStore.flow.collectAsState(initial = Settings())
    val nsfwDisplay by app.contentStance.collectAsState()
    val currentUserId by app.currentUserId.collectAsState()
    val messageBadge by app.messageBadge.collectAsState()
    val context = LocalContext.current
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    /** 站内路径或完整 URL 交给浏览器（占位功能的临时出口）。 */
    fun openWebOrPath(path: String) {
        val url = if (path.startsWith("http://") || path.startsWith("https://")) {
            path
        } else {
            "https://www.kungal.com$path"
        }
        AndroidActions.openInBrowser(context, url)
    }

    fun openEntry(entry: HubEntry) {
        val rankingTab = when (entry.webPath) {
            "/ranking/user" -> "user"
            "/ranking/topic" -> "topic"
            "/ranking/galgame" -> "work"
            else -> null
        }
        when {
            rankingTab != null -> nav.navigate(Routes.ranking(rankingTab))
            entry.webPath == "/galgame" -> nav.navigate(Routes.GALGAME_WORKS)
            entry.webPath == "/gallib" -> nav.navigate(Routes.GALGAME_LIBRARY)
            else -> nav.navigate(Routes.soon(entry))
        }
    }

    /** 通知里的站内路径：话题/用户进 App，其余（商店、Galgame 等）交给浏览器。 */
    fun openNotificationPath(path: String) {
        if (path.isBlank()) return
        val clean = path.substringBefore('?').substringBefore('#').trimEnd('/')
        val topicId = clean.removePrefix("/topic/")
        val userId = clean.removePrefix("/user/")
        val workId = clean.removePrefix("/galgame/")
        when {
            clean.startsWith("/topic/") && topicId.isNotEmpty() && topicId.all { it.isDigit() } ->
                nav.navigate(Routes.topic(topicId))
            clean.startsWith("/user/") && userId.isNotEmpty() && userId.all { it.isDigit() } ->
                nav.navigate(Routes.user(userId))
            clean.startsWith("/galgame/") && workId.isNotEmpty() && workId.all { it.isDigit() } ->
                nav.navigate(Routes.galgameWork(workId))
            else -> AndroidActions.openInBrowser(context, "https://www.kungal.com$path")
        }
    }

    Scaffold(
        snackbarHost = { AppMessageHost() },
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = {
                                if (tab.route == Routes.MESSAGES && messageBadge > 0) {
                                    BadgedBox(
                                        badge = {
                                            Badge {
                                                Text(if (messageBadge > 99) "99+" else "$messageBadge")
                                            }
                                        },
                                    ) {
                                        Icon(tab.icon, contentDescription = tab.label)
                                    }
                                } else {
                                    Icon(tab.icon, contentDescription = tab.label)
                                }
                            },
                            label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                        )
                    }
                }
            }
        },
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { padding ->
        NavHost(
            navController = nav,
            startDestination = Routes.TOPIC,
            modifier = Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding),
        ) {
            composable(Routes.TOPIC) {
                TopicListScreen(
                    app = app,
                    defaultSort = settings.defaultSort,
                    nsfwDisplay = nsfwDisplay,
                    onOpenTopic = { id -> nav.navigate(Routes.topic(id)) },
                    onOpenPublish = {
                        if (app.isLoggedIn) nav.navigate(Routes.PUBLISH) else nav.navigate(Routes.LOGIN)
                    },
                    onOpenSearch = { nav.navigate(Routes.SEARCH) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.GALGAME) {
                HubScreen(
                    title = "Galgame",
                    subtitle = "资料库、资源、题库与发售月历",
                    sections = GalgameSections,
                    onOpen = ::openEntry,
                )
            }
            composable(Routes.DISCOVER) {
                HubScreen(
                    title = "发现",
                    subtitle = "排行、动态、更新日志与帮助",
                    sections = DiscoverSections,
                    onOpen = ::openEntry,
                )
            }
            composable(Routes.MESSAGES) {
                MessagesScreen(
                    app = app,
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenFollowing = { nav.navigate(Routes.FOLLOWING) },
                    onOpenNotifications = { nav.navigate(Routes.NOTIFICATIONS) },
                    onOpenMuted = { nav.navigate(Routes.MUTED_NOTIFICATIONS) },
                    onOpenPreferences = { nav.navigate(Routes.NOTIFICATION_PREFERENCES) },
                    onOpenSubscriptions = { nav.navigate(Routes.SUBSCRIPTIONS) },
                    onOpenWalls = { nav.navigate(Routes.WALLS) },
                    onOpenChat = { nav.navigate(Routes.CHAT_LIST) },
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    app = app,
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenMessages = { nav.switchTab(Routes.MESSAGES) },
                    onOpenSubscriptions = { nav.navigate(Routes.SUBSCRIPTIONS) },
                    onOpenSelfProfile = {
                        currentUserId?.let { nav.navigate(Routes.user(it)) }
                    },
                    onEditProfile = { nav.navigate(Routes.PROFILE_SETTINGS) },
                    onOpenPoints = { nav.navigate(Routes.POINTS) },
                    onOpenShop = { nav.navigate(Routes.SHOP) },
                    onOpenCreator = { nav.navigate(Routes.CREATOR) },
                    onSwitchAccount = { nav.navigate(Routes.LOGIN) },
                    onOpenFollowers = {
                        currentUserId?.let { nav.navigate(Routes.followList(it, true)) }
                    },
                    onOpenFollowing = {
                        currentUserId?.let { nav.navigate(Routes.followList(it, false)) }
                    },
                )
            }

            composable(
                route = Routes.TOPIC_DETAIL,
                arguments = listOf(navArgument("topicId") { type = NavType.StringType }),
            ) { entry ->
                val topicId = entry.arguments?.getString("topicId").orEmpty()
                TopicDetailScreen(
                    app = app,
                    topicId = topicId,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onRequireLogin = { nav.navigate(Routes.LOGIN) },
                    onEditTopic = { id -> nav.navigate(Routes.editor(topicId = id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.LOGIN) {
                LoginScreen(
                    app = app,
                    onDone = { nav.popBackStack() },
                    onCancel = { nav.popBackStack() },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenCloudPreferences = { nav.navigate(Routes.CLOUD_PREFERENCES) },
                )
            }
            composable(Routes.CLOUD_PREFERENCES) {
                CloudPreferencesScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.PUBLISH) {
                PublishHubScreen(
                    onBack = { nav.popBackStack() },
                    onOpen = { entry ->
                        if (entry.label == "发布话题") {
                            if (app.isLoggedIn) nav.navigate(Routes.editor()) else nav.navigate(Routes.LOGIN)
                        } else {
                            openEntry(entry)
                        }
                    },
                )
            }
            composable(
                route = Routes.EDITOR,
                arguments = listOf(
                    navArgument("topicId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("draftId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                TopicEditorScreen(
                    app = app,
                    topicId = entry.arguments?.getString("topicId")?.takeIf { it.isNotBlank() },
                    draftId = entry.arguments?.getString("draftId")?.takeIf { it.isNotBlank() },
                    onBack = { nav.popBackStack() },
                    onOpenDrafts = { nav.navigate(Routes.DRAFTS) },
                    onPublished = { id ->
                        nav.popBackStack()
                        nav.navigate(Routes.topic(id))
                    },
                )
            }
            composable(Routes.DRAFTS) {
                DraftsScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenDraft = { id -> nav.navigate(Routes.editor(draftId = id)) },
                )
            }
            composable(Routes.FOLLOWING) {
                FollowingScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenTopic = { id -> nav.navigate(Routes.topic(id)) },
                    onOpenWeb = { url -> AndroidActions.openInBrowser(context, url) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.SEARCH) {
                TopicSearchScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenTopic = { id -> nav.navigate(Routes.topic(id)) },
                )
            }
            composable(Routes.SUBSCRIPTIONS) {
                SubscriptionsScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenTopic = { id -> nav.navigate(Routes.topic(id)) },
                )
            }
            composable(
                route = Routes.USER,
                arguments = listOf(navArgument("userId") { type = NavType.StringType }),
            ) { entry ->
                val userId = entry.arguments?.getString("userId").orEmpty()
                UserProfileScreen(
                    app = app,
                    userId = userId,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenTopic = { id -> nav.navigate(Routes.topic(id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenFollowList = { id, followers ->
                        nav.navigate(Routes.followList(id, followers))
                    },
                    onOpenChat = { id -> nav.navigate(Routes.chat(userId = id)) },
                    onOpenWeb = { url -> AndroidActions.openInBrowser(context, url) },
                    onEditProfile = { nav.navigate(Routes.PROFILE_SETTINGS) },
                )
            }
            composable(
                route = Routes.FOLLOW_LIST,
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("followers") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                FollowListScreen(
                    app = app,
                    userId = entry.arguments?.getString("userId").orEmpty(),
                    followers = entry.arguments?.getBoolean("followers") ?: false,
                    onBack = { nav.popBackStack() },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.PROFILE_SETTINGS) {
                ProfileSettingsScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenAccountCenter = {
                        AndroidActions.openInBrowser(context, "https://account.nextmoe.com/profile")
                    },
                )
            }
            composable(Routes.POINTS) {
                PointsScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenWeb = { url -> AndroidActions.openInBrowser(context, url) },
                )
            }
            composable(Routes.SHOP) {
                ShopScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.CREATOR) {
                CreatorScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.NOTIFICATIONS) {
                NotificationListScreen(
                    app = app,
                    muted = false,
                    onBack = { nav.popBackStack() },
                    onOpenPreferences = { nav.navigate(Routes.NOTIFICATION_PREFERENCES) },
                    onOpenPath = ::openNotificationPath,
                )
            }
            composable(Routes.MUTED_NOTIFICATIONS) {
                NotificationListScreen(
                    app = app,
                    muted = true,
                    onBack = { nav.popBackStack() },
                    onOpenPreferences = {},
                    onOpenPath = ::openNotificationPath,
                )
            }
            composable(Routes.NOTIFICATION_PREFERENCES) {
                NotificationPreferencesScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.WALLS) {
                WallsScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenWeb = { url -> AndroidActions.openInBrowser(context, url) },
                )
            }
            composable(Routes.CHAT_LIST) {
                ChatListScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenConversation = { id -> nav.navigate(Routes.chat(conversationId = id)) },
                    onOpenSettings = { nav.navigate(Routes.CHAT_SETTINGS) },
                )
            }
            composable(
                route = Routes.CHAT,
                arguments = listOf(
                    navArgument("conversationId") { type = NavType.StringType; defaultValue = "" },
                    navArgument("userId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                ChatConversationScreen(
                    app = app,
                    conversationId = entry.arguments?.getString("conversationId")?.takeIf { it.isNotBlank() },
                    userId = entry.arguments?.getString("userId")?.takeIf { it.isNotBlank() },
                    onBack = { nav.popBackStack() },
                    onOpenSettings = { nav.navigate(Routes.CHAT_SETTINGS) },
                )
            }
            composable(Routes.CHAT_SETTINGS) {
                ChatSettingsScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.GALGAME_WORKS) {
                GalgameListScreen(
                    app = app,
                    mode = GalgameListMode.RESOURCE,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                )
            }
            composable(Routes.GALGAME_LIBRARY) {
                GalgameListScreen(
                    app = app,
                    mode = GalgameListMode.LIBRARY,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_WORK,
                arguments = listOf(navArgument("workId") { type = NavType.StringType }),
            ) { entry ->
                WorkDetailScreen(
                    app = app,
                    workId = entry.arguments?.getString("workId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenWeb = ::openWebOrPath,
                )
            }
            composable(
                route = Routes.RANKING,
                arguments = listOf(
                    navArgument("tab") { type = NavType.StringType; defaultValue = "user" },
                ),
            ) { entry ->
                RankingScreen(
                    app = app,
                    initialTab = entry.arguments?.getString("tab") ?: "user",
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenTopic = { id -> nav.navigate(Routes.topic(id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenWeb = { url -> AndroidActions.openInBrowser(context, url) },
                )
            }
            composable(
                route = Routes.SOON,
                arguments = listOf(
                    navArgument("title") { type = NavType.StringType },
                    navArgument("note") { type = NavType.StringType },
                    navArgument("path") { type = NavType.StringType },
                ),
            ) { entry ->
                ComingSoonScreen(
                    title = entry.arguments?.getString("title").orEmpty(),
                    note = entry.arguments?.getString("note").orEmpty(),
                    webPath = entry.arguments?.getString("path").orEmpty(),
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}

/** 切换底部标签：保留各 tab 自己的返回栈状态，不堆叠重复页面。 */
private fun NavHostController.switchTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) {
            saveState = true
        }
        launchSingleTop = true
        restoreState = true
    }
}
