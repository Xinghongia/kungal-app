package com.kungal.app

import android.app.Application
import com.kungal.core.KungalClient
import com.kungal.core.api.MeApi
import com.kungal.core.api.SectionApi
import com.kungal.core.api.TopicApi
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

    lateinit var sessionStore: SessionStore
        private set

    lateinit var settingsStore: SettingsStore
        private set

    /** 登录态（供界面观察）；写操作前置检查用。 */
    private val _session = MutableStateFlow<Session?>(null)
    val session = _session.asStateFlow()

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** 登录账号的内容显示方式；null = 尚未取到（按隐藏处理）。 */
    private val _accountStance = MutableStateFlow<NsfwDisplayMode?>(null)
    val accountStance: StateFlow<NsfwDisplayMode?> = _accountStance.asStateFlow()

    /**
     * 应用当前生效的内容显示方式：登录时跟账号走，未登录时用本机偏好。
     * 话题列表据此决定是否请求 include_nsfw 以及封面是否打码。
     */
    lateinit var contentStance: StateFlow<NsfwDisplayMode>
        private set

    val isLoggedIn: Boolean get() = _session.value != null

    override fun onCreate() {
        super.onCreate()
        client = KungalClient()
        topicApi = TopicApi(client)
        sectionApi = SectionApi(client)
        meApi = MeApi(client)
        sessionStore = SessionStore(this)
        settingsStore = SettingsStore(this)
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
        appScope.launch { refreshContentStance() }
    }
}
