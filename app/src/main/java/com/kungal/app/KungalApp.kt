package com.kungal.app

import android.app.Application
import android.os.Build
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.gif.AnimatedImageDecoder
import coil3.gif.GifDecoder
import com.kungal.app.ui.components.AvatarPool
import com.kungal.core.KungalClient
import com.kungal.core.api.AccountAuthApi
import com.kungal.core.api.ActivityApi
import com.kungal.core.api.CalendarApi
import com.kungal.core.api.ChatApi
import com.kungal.core.api.CollectionApi
import com.kungal.core.api.EditApi
import com.kungal.core.api.GalgameApi
import com.kungal.core.api.GalgameEntityApi
import com.kungal.core.api.ImageApi
import com.kungal.core.api.MeApi
import com.kungal.core.api.NewsApi
import com.kungal.core.api.NewsSubmissionApi
import com.kungal.core.api.NotificationApi
import com.kungal.core.api.QuizApi
import com.kungal.core.api.RatingApi
import com.kungal.core.api.RankingApi
import com.kungal.core.api.ResourceApi
import com.kungal.core.api.SearchApi
import com.kungal.core.api.SectionApi
import com.kungal.core.api.ShopApi
import com.kungal.core.api.SocialApi
import com.kungal.core.api.SubmissionApi
import com.kungal.core.api.ToolsetApi
import com.kungal.core.api.TopicApi
import com.kungal.core.api.UserApi
import com.kungal.core.api.WallCommentApi
import com.kungal.core.model.Account
import com.kungal.core.session.Session
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class KungalApp : Application() {

    lateinit var client: KungalClient
        private set

    lateinit var topicApi: TopicApi
        private set

    lateinit var sectionApi: SectionApi
        private set

    lateinit var meApi: MeApi
        private set

    lateinit var authApi: AccountAuthApi
        private set

    lateinit var imageApi: ImageApi
        private set

    lateinit var socialApi: SocialApi
        private set

    lateinit var userApi: UserApi
        private set

    lateinit var shopApi: ShopApi
        private set

    lateinit var notificationApi: NotificationApi
        private set

    lateinit var chatApi: ChatApi
        private set

    lateinit var chatRealtime: ChatRealtimeHub
        private set

    lateinit var rankingApi: RankingApi
        private set

    lateinit var galgameApi: GalgameApi
        private set

    lateinit var collectionApi: CollectionApi
        private set

    lateinit var entityApi: GalgameEntityApi
        private set

    lateinit var ratingApi: RatingApi
        private set

    lateinit var quizApi: QuizApi
        private set

    lateinit var wallCommentApi: WallCommentApi
        private set

    lateinit var editApi: EditApi
        private set

    lateinit var submissionApi: SubmissionApi
        private set

    lateinit var resourceApi: ResourceApi
        private set

    lateinit var toolsetApi: ToolsetApi
        private set

    lateinit var searchApi: SearchApi
        private set

    lateinit var activityApi: ActivityApi
        private set

    lateinit var calendarApi: CalendarApi
        private set

    lateinit var newsApi: NewsApi
        private set

    lateinit var newsSubmissionApi: NewsSubmissionApi
        private set

    lateinit var sessionStore: SessionStore
        private set

    lateinit var settingsStore: SettingsStore
        private set

    lateinit var searchHistoryStore: SearchHistoryStore
        private set

    /** 登录态（供界面观察）；写操作前置检查用。 */
    private val _session = MutableStateFlow<Session?>(null)
    val session = _session.asStateFlow()

    internal val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 登录账号的内容显示方式；null = 尚未取到（按隐藏处理）。 */
    private val _accountStance = MutableStateFlow<NsfwDisplayMode?>(null)
    val accountStance: StateFlow<NsfwDisplayMode?> = _accountStance.asStateFlow()

    /** 当前登录用户 id（用于区分「自己的内容」与关注/举报入口）。 */
    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId.asStateFlow()

    /** 消息 tab 未读角标：未静音通知 + 未静音时的未读私信。 */
    private val _messageBadge = MutableStateFlow(0)
    val messageBadge: StateFlow<Int> = _messageBadge.asStateFlow()

    /** 重新拉取未读汇总；通知已读/删除、进入消息页后调用。 */
    fun refreshMessageBadge() {
        if (!isLoggedIn) {
            _messageBadge.value = 0
            return
        }
        appScope.launch {
            runCatching { notificationApi.summary() }
                .onSuccess { _messageBadge.value = it.badgeCount }
        }
    }

    /** 账号资料（昵称/头像/签名）变更计数：资料保存后让「我的」等页面重新拉取。 */
    private val _accountRevision = MutableStateFlow(0)
    val accountRevision: StateFlow<Int> = _accountRevision.asStateFlow()

    fun notifyAccountChanged() {
        _accountRevision.value += 1
    }

    /**
     * 应用当前生效的内容显示方式：登录时跟账号走，未登录时用本机偏好。
     * 话题列表据此决定是否请求 include_nsfw 以及封面是否打码。
     */
    lateinit var contentStance: StateFlow<NsfwDisplayMode>
        private set

    val isLoggedIn: Boolean get() = _session.value != null

    override fun onCreate() {
        super.onCreate()
        // 站点表情是动态 WebP（/emoji/*.webp），需要注册动图解码器才会动起来
        SingletonImageLoader.setSafe { context: PlatformContext ->
            ImageLoader.Builder(context)
                .components {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        add(AnimatedImageDecoder.Factory())
                    } else {
                        add(GifDecoder.Factory())
                    }
                }
                .build()
        }
        client = KungalClient()
        topicApi = TopicApi(client)
        sectionApi = SectionApi(client)
        meApi = MeApi(client)
        authApi = AccountAuthApi()
        imageApi = ImageApi(client)
        socialApi = SocialApi(client)
        userApi = UserApi(client)
        shopApi = ShopApi(client)
        notificationApi = NotificationApi(client)
        chatApi = ChatApi(client)
        rankingApi = RankingApi(client)
        galgameApi = GalgameApi(client)
        collectionApi = CollectionApi(client)
        entityApi = GalgameEntityApi(client)
        ratingApi = RatingApi(client)
        quizApi = QuizApi(client)
        wallCommentApi = WallCommentApi(client)
        editApi = EditApi(client)
        submissionApi = SubmissionApi(client)
        resourceApi = ResourceApi(client)
        toolsetApi = ToolsetApi(client)
        searchApi = SearchApi(client)
        activityApi = ActivityApi(client)
        calendarApi = CalendarApi(client)
        newsApi = NewsApi(client)
        newsSubmissionApi = NewsSubmissionApi(client)
        chatRealtime = ChatRealtimeHub(this)
        sessionStore = SessionStore(this)
        settingsStore = SettingsStore(this)
        searchHistoryStore = SearchHistoryStore(this)
        // 默认头像池：先内置快照可用，后台尝试刷新到站点最新版本
        appScope.launch { runCatching { AvatarPool.refresh() } }
        contentStance = combine(session, _accountStance, settingsStore.flow) { session, account, settings ->
            if (session != null) account ?: NsfwDisplayMode.HIDE else settings.nsfwDisplay
        }.stateIn(appScope, SharingStarted.Eagerly, NsfwDisplayMode.HIDE)
    }

    suspend fun restoreSession() {
        val saved = runCatching { sessionStore.load() }.getOrNull()
        applySession(saved)
    }

    suspend fun onLoggedIn(cookie: String) {
        val session = Session(cookie = cookie, savedAt = System.currentTimeMillis())
        sessionStore.save(session)
        applySession(session)
    }

    suspend fun logout() {
        sessionStore.clear()
        applySession(null)
    }

    /** 从 /me/account 刷新账号的内容显示方式；失败时按隐藏处理。 */
    suspend fun refreshContentStance() {
        if (!isLoggedIn) {
            _accountStance.value = null
            return
        }
        runCatching { meApi.account() }
            .onSuccess { applyAccount(it) }
            .onFailure { _accountStance.value = null }
    }

    /** 各页面拿到 /me/account 后同步内容姿态，避免重复请求。 */
    fun applyAccount(account: Account) {
        _accountStance.value = NsfwDisplayMode.fromAccount(account.contentStance)
        _currentUserId.value = account.id.ifBlank { null }
    }

    /**
     * 修改成人向内容显示方式：登录时写账号（所有站点生效），未登录时只存本机。
     * 失败会抛异常，由界面提示；账号值只在服务端确认后更新。
     */
    suspend fun setNsfwDisplay(mode: NsfwDisplayMode) {
        if (isLoggedIn) {
            val result = meApi.setNsfwDisplay(mode.wire)
            _accountStance.value = NsfwDisplayMode.fromWire(result.nsfwDisplay)
        } else {
            settingsStore.setNsfwDisplay(mode)
        }
    }

    private fun applySession(session: Session?) {
        client.session.restore(session)
        _session.value = session
        _accountStance.value = null
        _currentUserId.value = null
        _messageBadge.value = 0
        if (session != null) chatRealtime.start() else chatRealtime.stop()
        appScope.launch {
            refreshContentStance()
            refreshMessageBadge()
        }
    }
}
