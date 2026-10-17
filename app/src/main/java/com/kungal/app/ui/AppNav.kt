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
import com.kungal.app.ui.hub.ComingSoonScreen
import com.kungal.app.ui.hub.DiscoverSections
import com.kungal.app.ui.hub.GalgameSections
import com.kungal.app.ui.hub.HubEntry
import com.kungal.app.ui.hub.HubScreen
import com.kungal.app.ui.hub.PublishHubScreen
import com.kungal.app.ui.activity.FollowingScreen
import com.kungal.app.ui.editor.DraftsScreen
import com.kungal.app.ui.editor.TopicEditorScreen
import com.kungal.app.ui.login.LoginScreen
import com.kungal.app.ui.messages.MessagesScreen
import com.kungal.app.ui.profile.ProfileScreen
import com.kungal.app.ui.search.TopicSearchScreen
import com.kungal.app.ui.settings.SettingsScreen
import com.kungal.app.ui.subscriptions.SubscriptionsScreen
import com.kungal.app.ui.topic.TopicDetailScreen
import com.kungal.app.ui.topic.TopicListScreen
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
    const val PUBLISH = "publish"
    const val EDITOR = "editor?topicId={topicId}&draftId={draftId}"
    const val DRAFTS = "drafts"
    const val FOLLOWING = "following"
    const val SEARCH = "search"
    const val SUBSCRIPTIONS = "subscriptions"
    const val SOON = "soon?title={title}&note={note}&path={path}"

    fun topic(id: String) = "topic/$id"
    fun editor(topicId: String? = null, draftId: String? = null) =
        "editor?topicId=${Uri.encode(topicId.orEmpty())}&draftId=${Uri.encode(draftId.orEmpty())}"
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
    val context = LocalContext.current
    val backStackEntry by nav.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val showBottomBar = tabs.any { it.route == currentRoute }

    fun openEntry(entry: HubEntry) {
        nav.navigate(Routes.soon(entry))
    }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = currentRoute == tab.route,
                            onClick = { nav.switchTab(tab.route) },
                            icon = { Icon(tab.icon, contentDescription = tab.label) },
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
                )
            }
            composable(Routes.PROFILE) {
                ProfileScreen(
                    app = app,
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenSettings = { nav.navigate(Routes.SETTINGS) },
                    onOpenMessages = { nav.switchTab(Routes.MESSAGES) },
                    onOpenSubscriptions = { nav.navigate(Routes.SUBSCRIPTIONS) },
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
