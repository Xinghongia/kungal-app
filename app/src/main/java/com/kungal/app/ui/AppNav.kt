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
import com.kungal.app.ui.activity.ActivityCategoryScreen
import com.kungal.app.ui.activity.ActivityTimelineScreen
import com.kungal.app.ui.activity.UpdateHistoryScreen
import com.kungal.app.ui.activity.UpdateTodoScreen
import com.kungal.app.ui.activity.FollowingScreen
import com.kungal.app.ui.about.AboutMeScreen
import com.kungal.app.ui.about.DisclaimerScreen
import com.kungal.app.ui.about.PrivacyPolicyScreen
import com.kungal.app.ui.doc.DocDetailScreen
import com.kungal.app.ui.doc.DocListScreen
import com.kungal.app.ui.friendlinks.FriendLinksScreen
import com.kungal.app.ui.sites.SitesScreen
import com.kungal.app.ui.chat.ChatConversationScreen
import com.kungal.app.ui.chat.ChatListScreen
import com.kungal.app.ui.chat.ChatSettingsScreen
import com.kungal.app.ui.creator.CreatorScreen
import com.kungal.app.ui.editor.DraftsScreen
import com.kungal.app.ui.editor.TopicEditorScreen
import com.kungal.app.ui.galgame.CharacterDetailScreen
import com.kungal.app.ui.galgame.CharacterListScreen
import com.kungal.app.ui.galgame.CalendarScreen
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
import com.kungal.app.ui.galgame.MySubmissionsScreen
import com.kungal.app.ui.galgame.ProposalDetailScreen
import com.kungal.app.ui.galgame.RatingDetailScreen
import com.kungal.app.ui.galgame.RatingListScreen
import com.kungal.app.ui.galgame.ResourceDetailScreen
import com.kungal.app.ui.galgame.ResourceListScreen
import com.kungal.app.ui.galgame.ReviewQueueScreen
import com.kungal.app.ui.galgame.SeriesDetailScreen
import com.kungal.app.ui.galgame.SeriesListScreen
import com.kungal.app.ui.galgame.StaffDetailScreen
import com.kungal.app.ui.galgame.SubmissionCreateScreen
import com.kungal.app.ui.toolset.ToolsetDetailScreen
import com.kungal.app.ui.toolset.ToolsetEditorScreen
import com.kungal.app.ui.toolset.ToolsetListScreen
import com.kungal.app.ui.galgame.SubmissionWizardScreen
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
import com.kungal.app.ui.news.NewsDetailScreen
import com.kungal.app.ui.news.NewsEditorScreen
import com.kungal.app.ui.news.NewsListScreen
import com.kungal.app.ui.news.NewsMineScreen
import com.kungal.app.ui.news.NewsMonthScreen
import com.kungal.app.ui.points.PointsScreen
import com.kungal.app.ui.profile.ProfileScreen
import com.kungal.app.ui.quiz.QuizDetailScreen
import com.kungal.app.ui.quiz.QuizEditorScreen
import com.kungal.app.ui.quiz.QuizListScreen
import com.kungal.app.ui.ranking.RankingScreen
import com.kungal.app.ui.search.EntityFamily
import com.kungal.app.ui.search.SearchScreen
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
import com.kungal.app.ui.website.WebsiteCategoryScreen
import com.kungal.app.ui.website.WebsiteDetailScreen
import com.kungal.app.ui.website.WebsiteListScreen
import com.kungal.app.ui.website.WebsiteTagScreen

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
    const val USER = "user/{userId}?tab={tab}"
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
    const val GALGAME_QUIZ = "galgame/quiz"
    const val GALGAME_QUIZ_DETAIL = "galgame/quiz/{quizId}"
    const val GALGAME_QUIZ_CREATE = "galgame/quiz/create?workId={workId}"
    const val GALGAME_QUIZ_EDIT = "galgame/quiz/edit/{quizId}"
    const val GALGAME_CALENDAR = "galgame/calendar?view={view}&month={month}&year={year}&doujin={doujin}"
    const val NEWS = "news"
    const val NEWS_MINE = "news/mine"
    const val NEWS_DETAIL = "news/detail/{newsItemId}"
    const val NEWS_EDIT = "edit/news"
    const val NEWS_EDIT_ID = "edit/news/{submissionId}"
    const val NEWS_MONTH = "news/archive/{year}/{month}?lane={lane}&source={source}"
    const val WEBSITE = "website"
    const val WEBSITE_DETAIL = "website/detail/{host}"
    const val WEBSITE_CATEGORY = "website/category/{slug}"
    const val WEBSITE_TAG = "website/tag/{slug}"
    const val ACTIVITY = "activity"
    const val ACTIVITY_CATEGORY = "activity/category"
    const val UPDATE_HISTORY = "update/history"
    const val UPDATE_TODO = "update/todo"
    const val DOC = "doc"
    const val DOC_DETAIL = "doc/{slug}"
    const val FRIEND_LINKS = "friend-links"
    const val SITES = "sites"
    const val ABOUT_ME = "about/me"
    const val ABOUT_DISCLAIMER = "about/disclaimer"
    const val ABOUT_PRIVACY = "about/privacy"
    const val GALGAME_RESOURCE_LIST = "galgame/resources"
    const val GALGAME_LIBRARY = "galgame/library"
    const val GALGAME_WORK = "galgame/work/{workId}"
    const val GALGAME_RESOURCE = "galgame/resource/{resourceId}"
    const val GALGAME_EDIT = "galgame/edit/{workId}"
    const val GALGAME_HISTORY = "galgame/history/{workId}"
    const val GALGAME_PROPOSALS = "galgame/proposals"
    const val GALGAME_REVIEW = "galgame/review"
    const val GALGAME_PROPOSAL = "galgame/proposal/{proposalId}"
    const val GALGAME_SUBMIT = "galgame/submit"
    const val GALGAME_SUBMIT_CREATE = "galgame/submit/create?q={q}"
    const val GALGAME_SUBMISSIONS = "galgame/submissions"
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
    const val TOOLSET_LIST = "toolset/list"
    const val TOOLSET_DETAIL = "toolset/detail/{toolsetId}"
    const val TOOLSET_CREATE = "toolset/create"
    const val TOOLSET_EDIT = "toolset/edit/{toolsetId}"
    const val SOON = "soon?title={title}&note={note}&path={path}"

    fun topic(id: String) = "topic/$id"
    fun user(id: String) = "user/$id"
    fun user(id: String, tab: String?) =
        if (tab.isNullOrBlank()) "user/$id" else "user/$id?tab=${Uri.encode(tab)}"
    fun followList(userId: String, followers: Boolean) = "user/$userId/follow?followers=$followers"
    fun editor(topicId: String? = null, draftId: String? = null) =
        "editor?topicId=${Uri.encode(topicId.orEmpty())}&draftId=${Uri.encode(draftId.orEmpty())}"
    fun chat(conversationId: String? = null, userId: String? = null) =
        "messages/chat/room?conversationId=${Uri.encode(conversationId.orEmpty())}" +
            "&userId=${Uri.encode(userId.orEmpty())}"
    fun ranking(tab: String) = "ranking?tab=$tab"
    fun galgameCalendar(
        view: String? = null,
        month: String? = null,
        year: String? = null,
        doujin: Boolean = false,
    ): String = "galgame/calendar?view=${Uri.encode(view.orEmpty())}" +
        "&month=${Uri.encode(month.orEmpty())}&year=${Uri.encode(year.orEmpty())}&doujin=$doujin"
    fun galgameWork(id: String) = "galgame/work/$id"
    fun galgameQuiz(id: String) = "galgame/quiz/$id"
    fun galgameQuizCreate(workId: String? = null) =
        "galgame/quiz/create?workId=${Uri.encode(workId.orEmpty())}"
    fun galgameQuizEdit(id: String) = "galgame/quiz/edit/$id"
    fun newsDetail(id: String) = "news/detail/$id"
    fun newsEdit(id: String) = "edit/news/$id"
    fun docDetail(slug: String) = "doc/${Uri.encode(slug)}"
    fun websiteDetail(host: String) = "website/detail/${Uri.encode(host)}"
    fun websiteCategory(slug: String) = "website/category/${Uri.encode(slug)}"
    fun websiteTag(slug: String) = "website/tag/${Uri.encode(slug)}"
    fun newsMonth(year: Int, month: Int, lane: String? = null, source: String? = null): String =
        "news/archive/$year/$month?lane=${Uri.encode(lane.orEmpty())}" +
            "&source=${Uri.encode(source.orEmpty())}"
    fun galgameResource(id: String) = "galgame/resource/$id"
    fun toolset(id: String) = "toolset/detail/$id"
    fun toolsetEdit(id: String) = "toolset/edit/$id"
    fun userResourceExpire(id: String) = "user/$id?tab=resource-expire"
    fun galgameEdit(id: String) = "galgame/edit/$id"
    fun galgameHistory(id: String) = "galgame/history/$id"
    fun galgameProposal(id: String) = "galgame/proposal/$id"
    fun galgameSubmitCreate(query: String) = "galgame/submit/create?q=${Uri.encode(query)}"
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
        val docSlug = parseDocPath(clean)
        when {
            clean == "/galgame" -> nav.navigate(Routes.GALGAME_WORKS)
            clean == "/news" -> nav.navigate(Routes.NEWS)
            clean == "/news/mine" ->
                if (app.isLoggedIn) nav.navigate(Routes.NEWS_MINE) else nav.navigate(Routes.LOGIN)
            clean == "/edit/news" ->
                if (app.isLoggedIn) nav.navigate(Routes.NEWS_EDIT) else nav.navigate(Routes.LOGIN)
            clean.startsWith("/edit/news/") && looksId ->
                if (app.isLoggedIn) nav.navigate(Routes.newsEdit(segment)) else nav.navigate(Routes.LOGIN)
            clean.startsWith("/news/") -> {
                val monthLink = parseNewsMonthPath(path)
                val newsId = clean.removePrefix("/news/")
                when {
                    monthLink != null -> nav.navigate(
                        Routes.newsMonth(monthLink.year, monthLink.month, monthLink.lane, monthLink.source),
                    )
                    newsId.isNotEmpty() && newsId.all(Char::isDigit) ->
                        nav.navigate(Routes.newsDetail(newsId))
                    else -> AndroidActions.openInBrowser(context, "https://www.kungal.com$path")
                }
            }
            clean == "/galgame-quiz" -> nav.navigate(Routes.GALGAME_QUIZ)
            clean.startsWith("/galgame-quiz/") && looksId -> nav.navigate(Routes.galgameQuiz(segment))
            clean == "/edit/galgame/quiz" ->
                if (app.isLoggedIn) nav.navigate(Routes.galgameQuizCreate()) else nav.navigate(Routes.LOGIN)
            clean == "/galgame-calendar" -> {
                val link = parseCalendarPath(path)
                nav.navigate(
                    Routes.galgameCalendar(
                        view = link.view,
                        month = link.month,
                        year = link.year?.toString(),
                        doujin = link.doujin,
                    ),
                )
            }
            clean == "/website" -> nav.navigate(Routes.WEBSITE)
            clean == "/activity" -> nav.navigate(Routes.ACTIVITY)
            clean == "/activity/category" -> nav.navigate(Routes.ACTIVITY_CATEGORY)
            clean == "/update/history" -> nav.navigate(Routes.UPDATE_HISTORY)
            clean == "/update/todo" -> nav.navigate(Routes.UPDATE_TODO)
            clean == "/doc" -> nav.navigate(Routes.DOC)
            clean == "/friend-links" -> nav.navigate(Routes.FRIEND_LINKS)
            clean == "/sites" -> nav.navigate(Routes.SITES)
            docSlug != null -> nav.navigate(Routes.docDetail(docSlug))
            clean.startsWith("/website-category/") && segment.isNotBlank() ->
                nav.navigate(Routes.websiteCategory(segment))
            clean.startsWith("/website-tag/") && segment.isNotBlank() ->
                nav.navigate(Routes.websiteTag(segment))
            clean.startsWith("/website/") && segment.isNotBlank() ->
                nav.navigate(Routes.websiteDetail(segment))
            clean == "/galgame/resource" -> nav.navigate(Routes.GALGAME_RESOURCE_LIST)
            clean == "/toolset" -> nav.navigate(Routes.TOOLSET_LIST)
            clean == "/edit/toolset/create" ->
                if (app.isLoggedIn) nav.navigate(Routes.TOOLSET_CREATE) else nav.navigate(Routes.LOGIN)
            clean.startsWith("/edit/toolset/rewrite") ->
                if (app.isLoggedIn) {
                    val id = clean.substringBefore('?').substringAfterLast('/')
                    if (id.all(Char::isDigit) && id.isNotEmpty()) nav.navigate(Routes.toolsetEdit(id))
                } else {
                    nav.navigate(Routes.LOGIN)
                }
            clean.startsWith("/toolset/") && looksId -> nav.navigate(Routes.toolset(segment))
            clean.startsWith("/galgame/resource/") && looksId -> nav.navigate(Routes.galgameResource(segment))
            clean == "/gallib" -> nav.navigate(Routes.GALGAME_LIBRARY)
            clean == "/search" -> nav.navigate(Routes.SEARCH)
            clean == "/galgame/official" -> nav.navigate(Routes.GALGAME_COMPANIES)
            clean == "/galgame/tag" -> nav.navigate(Routes.GALGAME_TAGS)
            clean == "/galgame/engine" -> nav.navigate(Routes.GALGAME_ENGINES)
            clean == "/galgame/series" -> nav.navigate(Routes.GALGAME_SERIES_LIST)
            clean == "/galgame/character" -> nav.navigate(Routes.galgameCharacters())
            clean == "/galgame/trait" -> nav.navigate(Routes.GALGAME_TRAITS)
            clean == "/galgame-rating" -> nav.navigate(Routes.GALGAME_RATINGS)
            clean == "/galgame-edit/mine" -> nav.navigate(Routes.GALGAME_PROPOSALS)
            clean == "/galgame-edit/review" -> nav.navigate(Routes.GALGAME_REVIEW)
            clean == "/edit/galgame/publish" ->
                if (app.isLoggedIn) nav.navigate(Routes.GALGAME_SUBMIT) else nav.navigate(Routes.LOGIN)
            clean == "/edit/galgame/create" ->
                if (app.isLoggedIn) {
                    nav.navigate(Routes.galgameSubmitCreate(""))
                } else {
                    nav.navigate(Routes.LOGIN)
                }
            clean == "/edit/galgame/mine" ->
                if (app.isLoggedIn) nav.navigate(Routes.GALGAME_SUBMISSIONS) else nav.navigate(Routes.LOGIN)
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
            clean.startsWith("/user/") -> {
                val parsed = parseUserTabPath(clean)
                if (parsed != null) {
                    nav.navigate(Routes.user(parsed.first, parsed.second))
                } else {
                    AndroidActions.openInBrowser(context, "https://www.kungal.com$path")
                }
            }
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
            entry.webPath == "/galgame-calendar" -> nav.navigate(Routes.galgameCalendar())
            entry.webPath == "/news" -> nav.navigate(Routes.NEWS)
            entry.webPath == "/website" -> nav.navigate(Routes.WEBSITE)
            entry.webPath == "/activity" -> nav.navigate(Routes.ACTIVITY)
            entry.webPath == "/activity/category" -> nav.navigate(Routes.ACTIVITY_CATEGORY)
            entry.webPath == "/update/history" -> nav.navigate(Routes.UPDATE_HISTORY)
            entry.webPath == "/update/todo" -> nav.navigate(Routes.UPDATE_TODO)
            entry.webPath == "/doc" -> nav.navigate(Routes.DOC)
            entry.webPath == "/friend-links" -> nav.navigate(Routes.FRIEND_LINKS)
            entry.webPath == "/sites" -> nav.navigate(Routes.SITES)
            entry.webPath == "/edit/news" ->
                if (app.isLoggedIn) nav.navigate(Routes.NEWS_EDIT) else nav.navigate(Routes.LOGIN)
            entry.webPath == "/galgame-quiz" -> nav.navigate(Routes.GALGAME_QUIZ)
            entry.webPath == "/edit/galgame/quiz" ->
                if (app.isLoggedIn) nav.navigate(Routes.galgameQuizCreate()) else nav.navigate(Routes.LOGIN)
            entry.webPath == "/galgame/resource" -> nav.navigate(Routes.GALGAME_RESOURCE_LIST)
            entry.webPath == "/toolset" -> nav.navigate(Routes.TOOLSET_LIST)
            entry.webPath == "/edit/toolset/create" ->
                if (app.isLoggedIn) nav.navigate(Routes.TOOLSET_CREATE) else nav.navigate(Routes.LOGIN)
            entry.webPath == "/gallib" -> nav.navigate(Routes.GALGAME_LIBRARY)
            entry.webPath == "/galgame/official" -> nav.navigate(Routes.GALGAME_COMPANIES)
            entry.webPath == "/galgame/tag" -> nav.navigate(Routes.GALGAME_TAGS)
            entry.webPath == "/galgame/character" -> nav.navigate(Routes.galgameCharacters())
            entry.webPath == "/galgame/engine" -> nav.navigate(Routes.GALGAME_ENGINES)
            entry.webPath == "/galgame/series" -> nav.navigate(Routes.GALGAME_SERIES_LIST)
            entry.webPath == "/galgame-rating" -> nav.navigate(Routes.GALGAME_RATINGS)
            entry.webPath == "/edit/galgame/publish" ->
                if (app.isLoggedIn) nav.navigate(Routes.GALGAME_SUBMIT) else nav.navigate(Routes.LOGIN)
            else -> nav.navigate(Routes.soon(entry))
        }
    }

    /** 通知里的站内路径：话题/用户/资源/工具集进 App，其余（商店等）交给浏览器。 */
    fun openNotificationPath(path: String) {
        if (path.isBlank()) return
        val clean = path.substringBefore('?').substringBefore('#').trimEnd('/')
        val topicId = clean.removePrefix("/topic/")
        val userId = clean.removePrefix("/user/")
        val workId = clean.removePrefix("/galgame/")
        val resourceId = clean.removePrefix("/galgame/resource/")
        val toolsetId = clean.removePrefix("/toolset/")
        val quizId = clean.removePrefix("/galgame-quiz/")
        val newsId = clean.removePrefix("/news/")
        val websiteHost = clean.removePrefix("/website/")
        val docSlug = parseDocPath(clean)
        when {
            clean == "/news" -> nav.navigate(Routes.NEWS)
            clean == "/doc" -> nav.navigate(Routes.DOC)
            clean == "/friend-links" -> nav.navigate(Routes.FRIEND_LINKS)
            clean == "/sites" -> nav.navigate(Routes.SITES)
            docSlug != null -> nav.navigate(Routes.docDetail(docSlug))
            clean.startsWith("/news/") && newsId.isNotEmpty() && newsId.all { it.isDigit() } ->
                nav.navigate(Routes.newsDetail(newsId))
            clean.startsWith("/website/") && websiteHost.isNotBlank() ->
                nav.navigate(Routes.websiteDetail(websiteHost))
            clean.startsWith("/galgame-quiz/") && quizId.isNotEmpty() && quizId.all { it.isDigit() } ->
                nav.navigate(Routes.galgameQuiz(quizId))
            clean.startsWith("/topic/") && topicId.isNotEmpty() && topicId.all { it.isDigit() } ->
                nav.navigate(Routes.topic(topicId))
            clean.startsWith("/user/") -> {
                val parsed = parseUserTabPath(clean)
                if (parsed != null) {
                    nav.navigate(Routes.user(parsed.first, parsed.second))
                } else {
                    AndroidActions.openInBrowser(context, "https://www.kungal.com$path")
                }
            }
            clean.startsWith("/galgame/resource/") && resourceId.isNotEmpty() && resourceId.all { it.isDigit() } ->
                nav.navigate(Routes.galgameResource(resourceId))
            clean.startsWith("/toolset/") && toolsetId.isNotEmpty() && toolsetId.all { it.isDigit() } ->
                nav.navigate(Routes.toolset(toolsetId))
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
                    onOpenMyResources = {
                        currentUserId?.let { nav.navigate(Routes.user(it, "resource")) }
                    },
                    onOpenMyToolsets = {
                        currentUserId?.let { nav.navigate(Routes.user(it, "toolset")) }
                    },
                    onOpenProposals = { nav.navigate(Routes.GALGAME_PROPOSALS) },
                    onOpenSubmissions = { nav.navigate(Routes.GALGAME_SUBMISSIONS) },
                    onOpenNewsSubmissions = { nav.navigate(Routes.NEWS_MINE) },
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
                    onOpenAboutMe = { nav.navigate(Routes.ABOUT_ME) },
                    onOpenDisclaimer = { nav.navigate(Routes.ABOUT_DISCLAIMER) },
                    onOpenPrivacy = { nav.navigate(Routes.ABOUT_PRIVACY) },
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
                SearchScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenTopic = { id -> nav.navigate(Routes.topic(id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenResource = { id -> nav.navigate(Routes.galgameResource(id)) },
                    onOpenToolset = { id -> nav.navigate(Routes.toolset(id)) },
                    onOpenEntity = { family, id ->
                        when (family) {
                            EntityFamily.CHARACTER -> nav.navigate(Routes.galgameCharacter(id))
                            EntityFamily.COMPANY -> nav.navigate(Routes.galgameCompany(id))
                            EntityFamily.STAFF -> nav.navigate(Routes.galgameStaff(id))
                            EntityFamily.TAG -> nav.navigate(Routes.galgameTag(id))
                            EntityFamily.SERIES -> nav.navigate(Routes.galgameSeries(id))
                            EntityFamily.ENGINE -> nav.navigate(Routes.galgameEngine(id))
                            EntityFamily.ALL -> Unit
                        }
                    },
                    onOpenPath = ::openUrlOrPath,
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
                arguments = listOf(
                    navArgument("userId") { type = NavType.StringType },
                    navArgument("tab") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                val userId = entry.arguments?.getString("userId").orEmpty()
                UserProfileScreen(
                    app = app,
                    userId = userId,
                    nsfwDisplay = nsfwDisplay,
                    initialTab = entry.arguments?.getString("tab").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenTopic = { id -> nav.navigate(Routes.topic(id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenFollowList = { id, followers ->
                        nav.navigate(Routes.followList(id, followers))
                    },
                    onOpenChat = { id -> nav.navigate(Routes.chat(userId = id)) },
                    onOpenResource = { id -> nav.navigate(Routes.galgameResource(id)) },
                    onOpenToolset = { id -> nav.navigate(Routes.toolset(id)) },
                    onOpenQuiz = { id -> nav.navigate(Routes.galgameQuiz(id)) },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenRating = { id -> nav.navigate(Routes.galgameRating(id)) },
                    onOpenCollection = { id -> nav.navigate(Routes.collection(id)) },
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
                    onOpenWeb = ::openUrlOrPath,
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
            composable(
                route = Routes.GALGAME_CALENDAR,
                arguments = listOf(
                    navArgument("view") { type = NavType.StringType; defaultValue = "" },
                    navArgument("month") { type = NavType.StringType; defaultValue = "" },
                    navArgument("year") { type = NavType.StringType; defaultValue = "" },
                    navArgument("doujin") { type = NavType.BoolType; defaultValue = false },
                ),
            ) { entry ->
                CalendarScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    initialView = entry.arguments?.getString("view").orEmpty(),
                    initialMonth = entry.arguments?.getString("month")?.takeIf { it.isNotBlank() },
                    initialYear = entry.arguments?.getString("year")?.toIntOrNull(),
                    initialDoujin = entry.arguments?.getBoolean("doujin") ?: false,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                )
            }
            composable(Routes.NEWS) {
                NewsListScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenItem = { id -> nav.navigate(Routes.newsDetail(id)) },
                    onOpenMonth = { year, month, lane, source ->
                        nav.navigate(Routes.newsMonth(year, month, lane, source))
                    },
                    onOpenUrl = { url -> AndroidActions.openInBrowser(context, url) },
                    onCreateSubmission = {
                        if (app.isLoggedIn) nav.navigate(Routes.NEWS_EDIT) else nav.navigate(Routes.LOGIN)
                    },
                    onOpenMySubmissions = {
                        if (app.isLoggedIn) nav.navigate(Routes.NEWS_MINE) else nav.navigate(Routes.LOGIN)
                    },
                )
            }
            composable(Routes.NEWS_MINE) {
                NewsMineScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenNews = { nav.navigate(Routes.NEWS) },
                    onCreate = { nav.navigate(Routes.NEWS_EDIT) },
                    onOpenEditor = { id -> nav.navigate(Routes.newsEdit(id)) },
                    onOpenItem = { id -> nav.navigate(Routes.newsDetail(id)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.NEWS_EDIT) {
                NewsEditorScreen(
                    app = app,
                    submissionId = null,
                    onBack = { nav.popBackStack() },
                    onSaved = {
                        nav.navigate(Routes.NEWS_MINE) {
                            popUpTo(Routes.NEWS_EDIT) { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onOpenMine = { nav.navigate(Routes.NEWS_MINE) },
                )
            }
            composable(
                route = Routes.NEWS_EDIT_ID,
                arguments = listOf(navArgument("submissionId") { type = NavType.StringType }),
            ) { entry ->
                NewsEditorScreen(
                    app = app,
                    submissionId = entry.arguments?.getString("submissionId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                    onOpenMine = { nav.navigate(Routes.NEWS_MINE) },
                )
            }
            composable(
                route = Routes.NEWS_DETAIL,
                arguments = listOf(navArgument("newsItemId") { type = NavType.StringType }),
            ) { entry ->
                NewsDetailScreen(
                    app = app,
                    newsItemId = entry.arguments?.getString("newsItemId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenUrl = { url -> AndroidActions.openInBrowser(context, url) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                )
            }
            composable(
                route = Routes.NEWS_MONTH,
                arguments = listOf(
                    navArgument("year") { type = NavType.IntType },
                    navArgument("month") { type = NavType.IntType },
                    navArgument("lane") { type = NavType.StringType; defaultValue = "" },
                    navArgument("source") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                NewsMonthScreen(
                    app = app,
                    year = entry.arguments?.getInt("year") ?: 0,
                    month = entry.arguments?.getInt("month") ?: 0,
                    lane = entry.arguments?.getString("lane")?.takeIf { it.isNotBlank() },
                    source = entry.arguments?.getString("source")?.takeIf { it.isNotBlank() },
                    onBack = { nav.popBackStack() },
                    onOpenItem = { id -> nav.navigate(Routes.newsDetail(id)) },
                    onOpenMonth = { y, m, l, s -> nav.navigate(Routes.newsMonth(y, m, l, s)) },
                    onOpenUrl = { url -> AndroidActions.openInBrowser(context, url) },
                )
            }
            composable(Routes.WEBSITE) {
                WebsiteListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWebsite = { host -> nav.navigate(Routes.websiteDetail(host)) },
                    onOpenCategory = { slug -> nav.navigate(Routes.websiteCategory(slug)) },
                )
            }
            composable(
                route = Routes.WEBSITE_DETAIL,
                arguments = listOf(navArgument("host") { type = NavType.StringType }),
            ) { entry ->
                WebsiteDetailScreen(
                    app = app,
                    host = entry.arguments?.getString("host").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenCategory = { slug -> nav.navigate(Routes.websiteCategory(slug)) },
                    onOpenTag = { slug -> nav.navigate(Routes.websiteTag(slug)) },
                )
            }
            composable(
                route = Routes.WEBSITE_CATEGORY,
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            ) { entry ->
                WebsiteCategoryScreen(
                    app = app,
                    slug = entry.arguments?.getString("slug").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWebsite = { host -> nav.navigate(Routes.websiteDetail(host)) },
                )
            }
            composable(
                route = Routes.WEBSITE_TAG,
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            ) { entry ->
                WebsiteTagScreen(
                    app = app,
                    slug = entry.arguments?.getString("slug").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWebsite = { host -> nav.navigate(Routes.websiteDetail(host)) },
                )
            }
            composable(Routes.ACTIVITY) {
                ActivityTimelineScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenPath = { path -> openInternalPath(path) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.ACTIVITY_CATEGORY) {
                ActivityCategoryScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenPath = { path -> openInternalPath(path) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.UPDATE_HISTORY) {
                UpdateHistoryScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                )
            }
            composable(Routes.UPDATE_TODO) {
                UpdateTodoScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                )
            }
            composable(Routes.DOC) {
                DocListScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenDoc = { slug -> nav.navigate(Routes.docDetail(slug)) },
                )
            }
            composable(
                route = Routes.DOC_DETAIL,
                arguments = listOf(navArgument("slug") { type = NavType.StringType }),
            ) { entry ->
                DocDetailScreen(
                    app = app,
                    slug = entry.arguments?.getString("slug").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenDoc = { slug -> nav.navigate(Routes.docDetail(slug)) },
                )
            }
            composable(Routes.FRIEND_LINKS) {
                FriendLinksScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenUrl = { url -> AndroidActions.openInBrowser(context, url) },
                    onOpenContact = { nav.navigate(Routes.docDetail("contact")) },
                )
            }
            composable(Routes.SITES) {
                SitesScreen(
                    onBack = { nav.popBackStack() },
                    onOpenUrl = { url -> AndroidActions.openInBrowser(context, url) },
                )
            }
            composable(Routes.ABOUT_ME) {
                AboutMeScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenUrl = { url -> AndroidActions.openInBrowser(context, url) },
                )
            }
            composable(Routes.ABOUT_DISCLAIMER) {
                DisclaimerScreen(onBack = { nav.popBackStack() })
            }
            composable(Routes.ABOUT_PRIVACY) {
                PrivacyPolicyScreen(onBack = { nav.popBackStack() })
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
            composable(Routes.GALGAME_QUIZ) {
                QuizListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenQuiz = { id -> nav.navigate(Routes.galgameQuiz(id)) },
                    onCreate = { nav.navigate(Routes.galgameQuizCreate()) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                )
            }
            composable(
                route = Routes.GALGAME_QUIZ_DETAIL,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType }),
            ) { entry ->
                val quizId = entry.arguments?.getString("quizId").orEmpty()
                QuizDetailScreen(
                    app = app,
                    quizId = quizId,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onEdit = { id -> nav.navigate(Routes.galgameQuizEdit(id)) },
                )
            }
            composable(
                route = Routes.GALGAME_QUIZ_CREATE,
                arguments = listOf(
                    navArgument("workId") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                QuizEditorScreen(
                    app = app,
                    quizId = null,
                    initialWorkId = entry.arguments?.getString("workId")?.takeIf { it.isNotBlank() },
                    onBack = { nav.popBackStack() },
                    onSaved = { id ->
                        nav.popBackStack()
                        nav.navigate(Routes.galgameQuiz(id))
                    },
                )
            }
            composable(
                route = Routes.GALGAME_QUIZ_EDIT,
                arguments = listOf(navArgument("quizId") { type = NavType.StringType }),
            ) { entry ->
                val quizId = entry.arguments?.getString("quizId").orEmpty()
                QuizEditorScreen(
                    app = app,
                    quizId = quizId,
                    initialWorkId = null,
                    onBack = { nav.popBackStack() },
                    onSaved = { nav.popBackStack() },
                )
            }
            composable(Routes.GALGAME_RESOURCE_LIST) {
                ResourceListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenResource = { resource ->
                        nav.navigate(Routes.galgameResource(resource.id))
                    },
                )
            }
            composable(Routes.TOOLSET_LIST) {
                ToolsetListScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenToolset = { id -> nav.navigate(Routes.toolset(id)) },
                    onCreate = { nav.navigate(Routes.TOOLSET_CREATE) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                )
            }
            composable(
                route = Routes.TOOLSET_DETAIL,
                arguments = listOf(navArgument("toolsetId") { type = NavType.StringType }),
            ) { entry ->
                ToolsetDetailScreen(
                    app = app,
                    toolsetId = entry.arguments?.getString("toolsetId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenBrowser = { url -> AndroidActions.openInBrowser(context, url) },
                    onEditToolset = { id -> nav.navigate(Routes.toolsetEdit(id)) },
                    onDeleted = { nav.popBackStack() },
                    onRequireLoginThenUpload = { nav.navigate(Routes.LOGIN) },
                )
            }
            composable(Routes.TOOLSET_CREATE) {
                ToolsetEditorScreen(
                    app = app,
                    toolsetId = null,
                    onBack = { nav.popBackStack() },
                    onSaved = { id ->
                        nav.popBackStack()
                        nav.navigate(Routes.toolset(id))
                    },
                )
            }
            composable(
                route = Routes.TOOLSET_EDIT,
                arguments = listOf(navArgument("toolsetId") { type = NavType.StringType }),
            ) { entry ->
                ToolsetEditorScreen(
                    app = app,
                    toolsetId = entry.arguments?.getString("toolsetId").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onSaved = { id ->
                        nav.popBackStack()
                        nav.navigate(Routes.toolset(id))
                    },
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
                    onOpenResource = { id -> nav.navigate(Routes.galgameResource(id)) },
                    onOpenExpireResources = { id -> nav.navigate(Routes.userResourceExpire(id)) },
                    onOpenEdit = { nav.navigate(Routes.galgameEdit(entry.arguments?.getString("workId").orEmpty())) },
                    onOpenHistory = { nav.navigate(Routes.galgameHistory(entry.arguments?.getString("workId").orEmpty())) },
                    onOpenQuiz = { id -> nav.navigate(Routes.galgameQuiz(id)) },
                    onCreateQuiz = { workId ->
                        if (app.isLoggedIn) {
                            nav.navigate(Routes.galgameQuizCreate(workId))
                        } else {
                            nav.navigate(Routes.LOGIN)
                        }
                    },
                )
            }
            composable(
                route = Routes.GALGAME_RESOURCE,
                arguments = listOf(navArgument("resourceId") { type = NavType.StringType }),
            ) { entry ->
                ResourceDetailScreen(
                    app = app,
                    resourceId = entry.arguments?.getString("resourceId").orEmpty(),
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenResourceList = { nav.navigate(Routes.GALGAME_RESOURCE_LIST) },
                    onOpenResource = { id -> nav.navigate(Routes.galgameResource(id)) },
                    onOpenUser = { id -> nav.navigate(Routes.user(id)) },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenBrowser = { url -> AndroidActions.openInBrowser(context, url) },
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
            composable(Routes.GALGAME_SUBMIT) {
                SubmissionWizardScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
                    onOpenEdit = { id -> nav.navigate(Routes.galgameEdit(id)) },
                    onOpenMySubmissions = { nav.navigate(Routes.GALGAME_SUBMISSIONS) },
                    onCreateNew = { q -> nav.navigate(Routes.galgameSubmitCreate(q)) },
                )
            }
            composable(
                route = Routes.GALGAME_SUBMIT_CREATE,
                arguments = listOf(
                    navArgument("q") { type = NavType.StringType; defaultValue = "" },
                ),
            ) { entry ->
                SubmissionCreateScreen(
                    app = app,
                    initialTitle = entry.arguments?.getString("q").orEmpty(),
                    onBack = { nav.popBackStack() },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onSubmittedLive = { id ->
                        nav.navigate(Routes.galgameWork(id)) {
                            popUpTo(Routes.GALGAME_SUBMIT) { inclusive = false }
                        }
                    },
                    onSubmittedPending = { nav.navigate(Routes.GALGAME_SUBMISSIONS) },
                )
            }
            composable(Routes.GALGAME_SUBMISSIONS) {
                MySubmissionsScreen(
                    app = app,
                    onBack = { nav.popBackStack() },
                    onOpenLogin = { nav.navigate(Routes.LOGIN) },
                    onOpenEdit = { id ->
                        if (app.isLoggedIn) nav.navigate(Routes.galgameEdit(id)) else nav.navigate(Routes.LOGIN)
                    },
                    onCreateNew = { nav.navigate(Routes.GALGAME_SUBMIT) },
                )
            }
            composable(Routes.GALGAME_TAGS) {
                TagListScreen(
                    app = app,
                    nsfwDisplay = nsfwDisplay,
                    onBack = { nav.popBackStack() },
                    onOpenTag = { id -> nav.navigate(Routes.galgameTag(id)) },
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
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
                    onOpenWork = { id -> nav.navigate(Routes.galgameWork(id)) },
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

/**
 * 月历分享链接的参数（网页把视图状态放在 URL 里）：
 * `/galgame-calendar?view=upcoming&doujin=1&month=2026-10&year=2027`。
 */
internal data class CalendarLink(
    val view: String? = null,
    val month: String? = null,
    val year: Int? = null,
    val doujin: Boolean = false,
)

internal fun parseCalendarPath(path: String): CalendarLink {
    val query = path.substringAfter('?', "").substringBefore('#')
    if (query.isBlank()) return CalendarLink()
    val params = query.split('&').mapNotNull { part ->
        val index = part.indexOf('=')
        if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
    }.toMap()
    return CalendarLink(
        view = params["view"]?.takeIf { it in setOf("month", "upcoming", "pending", "tba") },
        month = params["month"]?.takeIf { it.matches(Regex("""\d{4}-\d{2}""")) },
        year = params["year"]?.toIntOrNull()?.takeIf { it > 0 },
        doujin = params["doujin"] == "1" || params["doujin"] == "true",
    )
}

/**
 * 情报月归档链接：`/news/{year}/{month}`，可带 `?lane=news|column&source=ymgal`。
 * 其他 /news 路径（详情、投稿）返回 null。
 */
internal data class NewsMonthLink(
    val year: Int,
    val month: Int,
    val lane: String? = null,
    val source: String? = null,
)

internal fun parseNewsMonthPath(path: String): NewsMonthLink? {    val clean = path.substringBefore('?').substringBefore('#').trimEnd('/')
    val parts = clean.removePrefix("/news/").split('/').filter { it.isNotBlank() }
    if (parts.size != 2) return null
    val year = parts[0].toIntOrNull()?.takeIf { it in 1970..9999 } ?: return null
    val month = parts[1].toIntOrNull()?.takeIf { it in 1..12 } ?: return null
    val params = path.substringAfter('?', "").substringBefore('#')
        .split('&')
        .mapNotNull { part ->
            val index = part.indexOf('=')
            if (index <= 0) null else part.substring(0, index) to part.substring(index + 1)
        }
        .toMap()
    return NewsMonthLink(
        year = year,
        month = month,
        lane = params["lane"]?.takeIf { it == "news" || it == "column" },
        source = params["source"]?.takeIf { it.isNotBlank() },
    )
}

/**
 * 文档详情路径 `/doc/{slug}`：slug 只允许小写字母、数字与连字符（网页路由约定）。
 * `/doc` 本身返回 null，交给文档列表页。
 */
internal fun parseDocPath(path: String): String? {
    val clean = path.substringBefore('?').substringBefore('#').trimEnd('/')
    if (!clean.startsWith("/doc/")) return null
    val slug = clean.removePrefix("/doc/")
    if (slug.isBlank() || slug.contains('/')) return null
    return slug.takeIf { it.matches(Regex("[a-z0-9]+(?:-[a-z0-9]+)*")) }
}

/**
 * 网页用户页深路径 → (userId, tab)：
 * `/user/1` → 动态；`/user/1/toolset` → 工具集；
 * `/user/1/resource/{valid|expire|galgame-resource-like}` → 资源（有效/过期/点赞）。
 */
internal fun parseUserTabPath(path: String): Pair<String, String?>? {    val parts = path.removePrefix("/user/").split('/').filter { it.isNotBlank() }
    val userId = parts.firstOrNull()?.takeIf { it.isNotEmpty() && it.all(Char::isDigit) }
        ?: return null
    val tab = when {
        parts.getOrNull(1) == "toolset" -> "toolset"
        parts.getOrNull(1) == "resource" -> when (parts.getOrNull(2)) {
            "expire" -> "resource-expire"
            "galgame-resource-like" -> "resource-like"
            else -> "resource"
        }
        else -> null
    }
    return userId to tab
}
