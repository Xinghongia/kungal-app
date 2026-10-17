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
import com.kungal.app.ui.galgame.CharacterDetailScreen
import com.kungal.app.ui.galgame.CharacterListScreen
import com.kungal.app.ui.galgame.CollectionDetailScreen
import com.kungal.app.ui.galgame.CompanyDetailScreen
import com.kungal.app.ui.galgame.CompanyListScreen
import com.kungal.app.ui.galgame.EngineDetailScreen
import com.kungal.app.ui.galgame.EngineListScreen
import com.kungal.app.ui.galgame.GalgameEditScreen
import com.kungal.app.ui.galgame.GalgameHistoryScreen
import com.kungal.app.ui.galgame.GalgameListMode
import com.kungal.app.ui.galgame.GalgameListScreen
import com.kungal.app.ui.galgame.MyCollectionsScreen
import com.kungal.app.ui.galgame.MyProposalsScreen
import com.kungal.app.ui.galgame.ProposalDetailScreen
import com.kungal.app.ui.galgame.RatingDetailScreen
import com.kungal.app.ui.galgame.RatingListScreen
import com.kungal.app.ui.galgame.ReviewQueueScreen
import com.kungal.app.ui.galgame.SeriesDetailScreen
import com.kungal.app.ui.galgame.SeriesListScreen
import com.kungal.app.ui.galgame.StaffDetailScreen
import com.kungal.app.ui.galgame.TagDetailScreen
import com.kungal.app.ui.galgame.TagListScreen
import com.kungal.app.ui.galgame.TraitDetailScreen
import com.kungal.app.ui.galgame.TraitListScreen
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
    const val GALGAME_EDIT = "galgame/edit/{workId}"
    const val GALGAME_HISTORY = "galgame/history/{workId}"
    const val GALGAME_PROPOSALS = "galgame/proposals"
    const val GALGAME_REVIEW = "galgame/review"
    const val GALGAME_PROPOSAL = "galgame/proposal/{proposalId}"
    const val GALGAME_TAGS = "galgame/tags"
    const val GALGAME_TAG = "galgame/tag/{tagId}"
    const val GALGAME_COMPANIES = "galgame/companies"
    const val GALGAME_COMPANY = "galgame/company/{companyId}"
    const val GALGAME_ENGINES = "galgame/engines"
    const val GALGAME_ENGINE = "galgame/engine/{engineId}"
    const val GALGAME_SERIES_LIST = "galgame/series"
    const val GALGAME_SERIES = "galgame/series/{seriesId}"
    const val GALGAME_CHARACTERS = "galgame/characters?traitId={traitId}"
    const val GALGAME_CHARACTER = "galgame/character/{characterId}"
    const val GALGAME_STAFF = "galgame/staff/{staffId}"
    const val GALGAME_TRAITS = "galgame/traits"
    const val GALGAME_TRAIT = "galgame/trait/{traitId}"
    const val COLLECTION = "collection/{collectionId}"
    const val MY_COLLECTIONS = "collections"
    const val GALGAME_RATINGS = "galgame/ratings"
    const val GALGAME_RATING = "galgame/rating/{ratingId}"
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
    fun galgameEdit(id: String) = "galgame/edit/$id"
    fun galgameHistory(id: String) = "galgame/history/$id"
    fun galgameProposal(id: String) = "galgame/proposal/$id"
    fun galgameTag(id: String) = "galgame/tag/$id"
    fun galgameCompany(id: String) = "galgame/company/$id"
    fun galgameEngine(id: String) = "galgame/engine/$id"
    fun galgameSeries(id: String) = "galgame/series/$id"
    fun galgameCharacter(id: String) = "galgame/character/$id"
    fun galgameStaff(id: String) = "galgame/staff/$id"
    fun galgameTrait(id: String) = "galgame/trait/$id"
    fun galgameCharacters(traitId: String? = null) =
        "galgame/characters?traitId=${Uri.encode(traitId.orEmpty())}"
    fun collection(id: String) = "collection/$id"
    fun galgameRating(id: String) = "galgame/rating/$id"
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

    /** 站内路径优先进 App；未实现的路径交浏览器。 */
    fun openInternalPath(path: String) {
        val clean = path.substringBefore('?').substringBefore('#').trimEnd('/')
        val segment = clean.substringAfterLast('/')
        val looksId = segment.isNotEmpty() && segment.all(Char::isDigit)
        when {
            clean == "/galgame" -> nav.navigate(Routes.GALGAME_WORKS)
            clean == "/gallib" -> nav.navigate(Routes.GALGAME_LIBRARY)
            clean == "/galgame/official" -> nav.navigate(Routes.GALGAME_COMPANIES)
            clean == "/galgame/tag" -> nav.navigate(Routes.GALGAME_TAGS)
            clean == "/galgame/engine" -> nav.navigate(Routes.GALGAME_ENGINES)
            clean == "/galgame/series" -> nav.navigate(Routes.GALGAME_SERIES_LIST)
            clean == "/galgame/character" -> nav.navigate(Routes.galgameCharacters())
            clean == "/galgame/trait" -> nav.navigate(Routes.GALGAME_TRAITS)
            clean == "/galgame-rating" -> nav.navigate(Routes.GALGAME_RATINGS)
            clean == "/galgame-edit/mine" -> nav.navigate(Routes.GALGAME_PROPOSALS)
            clean == "/galgame-edit/review" -> nav.navigate(Routes.GALGAME_REVIEW)
            clean.startsWith("/galgame-edit/review/") && looksId ->
                nav.navigate(Routes.galgameProposal(segment))
            clean.startsWith("/galgame/") && clean.endsWith("/edit") &&
                clean.removeSuffix("/edit").substringAfterLast('/').all(Char::isDigit) ->
                nav.navigate(Routes.galgameEdit(clean.removeSuffix("/edit").substringAfterLast('/')))
            clean.startsWith("/galgame/") && clean.endsWith("/history") &&
                clean.removeSuffix("/history").substringAfterLast('/').all(Char::isDigit) ->
                nav.navigate(Routes.galgameHistory(clean.removeSuffix("/history").substringAfterLast('/')))
            clean.startsWith("/galgame-rating/") && looksId -> nav.navigate(Routes.galgameRating(segment))
            clean.startsWith("/galgame/official/") && looksId -> nav.navigate(Routes.galgameCompany(segment))
            clean.startsWith("/galgame/tag/") && looksId -> nav.navigate(Routes.galgameTag(segment))
            clean.startsWith("/galgame/engine/") && looksId -> nav.navigate(Routes.galgameEngine(segment))
            clean.startsWith("/galgame/series/") && looksId -> nav.navigate(Routes.galgameSeries(segment))
            clean.startsWith("/galgame/character/") && looksId -> nav.navigate(Routes.galgameCharacter(segment))
            clean.startsWith("/galgame/staff/") && looksId -> nav.navigate(Routes.galgameStaff(segment))
            clean.startsWith("/galgame/trait/") && looksId -> nav.navigate(Routes.galgameTrait(segment))
            clean.startsWith("/galgame/") && looksId -> nav.navigate(Routes.galgameWork(segment))
            clean.startsWith("/collection/") && looksId -> nav.navigate(Routes.collection(segment))
            else -> AndroidActions.openInBrowser(context, "https://www.kungal.com$path")
        }
    }

    /** 站内链接进 App；外部链接（含站内尚未实现的编辑/历史页）交浏览器。 */
    fun openUrlOrPath(value: String) {
        val path = when {
            value.startsWith("https://www.kungal.com/") -> value.removePrefix("https://www.kungal.com")
            value.startsWith("http://www.kungal.com/") -> value.removePrefix("http://www.kungal.com")
            value.startsWith("/") -> value
            else -> null
        }
        if (path == null) {
            AndroidActions.openInBrowser(context, value)
        } else {
            openInternalPath(path)
        }
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
            entry.webPath == "/galgame/official" -> nav.navigate(Routes.GALGAME_COMPANIES)
            entry.webPath == "/galgame/tag" -> nav.navigate(Routes.GALGAME_TAGS)
            entry.webPath == "/galgame/character" -> nav.navigate(Routes.galgameCharacters())
            entry.webPath == "/galgame/engine" -> nav.navigate(Routes.GALGAME_ENGINES)
            entry.webPath == "/galgame/series" -> nav.navigate(Routes.GALGAME_SERIES_LIST)
            entry.webPath == "/galgame-rating" -> nav.navigate(Routes.GALGAME_RATINGS)
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
                    onOpenCollections = { nav.navigate(Routes.MY_COLLECTIONS) },
                    onOpenProposals = { nav.navigate(Routes.GALGAME_PROPOSALS) },
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
                    onOpenWeb = ::openUrlOrPath,
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
                    onOpenWeb = ::openUrlOrPath,
                    onOpenBrowser = { url -> AndroidActions.openInBrowser(context, url) },
                    onOpenEdit = { nav.navigate(Routes.galgameEdit(entry.arguments?.getString("workId").orEmpty())) },
                    onOpenHistory = { nav.navigate(Routes.galgameHistory(entry.arguments?.getString("workId").orEmpty())) },
                )
            }
            composable(
                route = Routes.GALGAME_EDIT,
                arguments = listOf(navArgument("workId") { type = NavType.StringType }),
            ) { entry ->
                GalgameEditScreen(
                    app = app,
                    workId = entry.arguments?.getString("workId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenHistory = {
                        nav.navigate(Routes.galgameHistory(entry.arguments?.getString("workId").orEmpty()))
                    },
                    onOpenMyProposals = { nav.navigate(Routes.GALGAME_PROPOSALS) },
                    onOpenReviewQueue = { nav.navigate(Routes.GALGAME_REVIEW) },
                    onOpenProposal = { id -> nav.navigate(Routes.galgameProposal(id)) },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                )
            }
            composable(
                route = Routes.GALGAME_HISTORY,
                arguments = listOf(navArgument("workId") { type = NavType.StringType }),
            ) { entry ->
                GalgameHistoryScreen(
                    app = app,
                    workId = entry.arguments?.getString("workId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenEdit = {
                        val workId = entry.arguments?.getString("workId").orEmpty()
                        if (app.isLoggedIn) nav.navigate(Routes.galgameEdit(workId)) else nav.navigate(Routes.LOGIN)
                    },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenProposal = { id -> nav.navigate(Routes.galgameProposal(id)) },
                )
            }
            composable(Routes.GALGAME_PROPOSALS) {
                MyProposalsScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenProposal = { id -> nav.navigate(Routes.galgameProposal(id)) },
                )
            }
            composable(Routes.GALGAME_REVIEW) {
                ReviewQueueScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenProposal = { id -> nav.navigate(Routes.galgameProposal(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_PROPOSAL,
                arguments = listOf(navArgument("proposalId") { type = NavType.StringType }),
            ) { entry ->
                ProposalDetailScreen(
                    app = app,
                    proposalId = entry.arguments?.getString("proposalId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onDone = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                )
            }
            composable(Routes.GALGAME_TAGS) {
                TagListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenTag = { id -> nav.navigate(Routes.galgameTag(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_TAG,
                arguments = listOf(navArgument("tagId") { type = NavType.StringType }),
            ) { entry ->
                TagDetailScreen(
                    app = app,
                    tagId = entry.arguments?.getString("tagId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenTag = { id -> nav.navigate(Routes.galgameTag(id)) },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.GALGAME_COMPANIES) {
                CompanyListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenCompany = { id -> nav.navigate(Routes.galgameCompany(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_COMPANY,
                arguments = listOf(navArgument("companyId") { type = NavType.StringType }),
            ) { entry ->
                CompanyDetailScreen(
                    app = app,
                    companyId = entry.arguments?.getString("companyId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenCompany = { id -> nav.navigate(Routes.galgameCompany(id)) },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenWeb = ::openUrlOrPath,
                )
            }
            composable(Routes.GALGAME_ENGINES) {
                EngineListScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenEngine = { id -> nav.navigate(Routes.galgameEngine(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_ENGINE,
                arguments = listOf(navArgument("engineId") { type = NavType.StringType }),
            ) { entry ->
                EngineDetailScreen(
                    app = app,
                    engineId = entry.arguments?.getString("engineId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                )
            }
            composable(Routes.GALGAME_SERIES_LIST) {
                SeriesListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenSeries = { id -> nav.navigate(Routes.galgameSeries(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_SERIES,
                arguments = listOf(navArgument("seriesId") { type = NavType.StringType }),
            ) { entry ->
                SeriesDetailScreen(
                    app = app,
                    seriesId = entry.arguments?.getString("seriesId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_CHARACTERS,
                arguments = listOf(
                    navArgument("traitId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                CharacterListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    initialTraitId = entry.arguments?.getString("traitId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenCharacter = { id -> nav.navigate(Routes.galgameCharacter(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_CHARACTER,
                arguments = listOf(navArgument("characterId") { type = NavType.StringType }),
            ) { entry ->
                CharacterDetailScreen(
                    app = app,
                    characterId = entry.arguments?.getString("characterId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenTrait = { id -> nav.navigate(Routes.galgameTrait(id)) },
                    onOpenWeb = ::openUrlOrPath,
                )
            }
            composable(
                route = Routes.GALGAME_STAFF,
                arguments = listOf(navArgument("staffId") { type = NavType.StringType }),
            ) { entry ->
                StaffDetailScreen(
                    app = app,
                    staffId = entry.arguments?.getString("staffId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenWeb = ::openUrlOrPath,
                )
            }
            composable(Routes.GALGAME_TRAITS) {
                TraitListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenTrait = { id -> nav.navigate(Routes.galgameTrait(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_TRAIT,
                arguments = listOf(navArgument("traitId") { type = NavType.StringType }),
            ) { entry ->
                TraitDetailScreen(
                    app = app,
                    traitId = entry.arguments?.getString("traitId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenTrait = { id -> nav.navigate(Routes.galgameTrait(id)) },
                    onOpenCharacterList = { id -> nav.navigate(Routes.galgameCharacters(id)) },
                    onOpenCharacter = { id -> nav.navigate(Routes.galgameCharacter(id)) },
                )
            }
            composable(
                route = Routes.COLLECTION,
                arguments = listOf(navArgument("collectionId") { type = NavType.StringType }),
            ) { entry ->
                CollectionDetailScreen(
                    app = app,
                    collectionId = entry.arguments?.getString("collectionId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.MY_COLLECTIONS) {
                MyCollectionsScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenCollection = { id -> nav.navigate(Routes.collection(id)) },
                )
            }
            composable(Routes.GALGAME_RATINGS) {
                RatingListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenRating = { id -> nav.navigate(Routes.galgameRating(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_RATING,
                arguments = listOf(navArgument("ratingId") { type = NavType.StringType }),
            ) { entry ->
                RatingDetailScreen(
                    app = app,
                    ratingId = entry.arguments?.getString("ratingId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
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
