package com.kungal.app

import android.app.Application
import com.kungal.core.KungalClient
import com.kungal.core.api.MeApi
import com.kungal.core.api.SectionApi
import com.kungal.core.api.TopicApi
import com.kungal.core.session.Session
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

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

    val isLoggedIn: Boolean get() = _session.value != null

    override fun onCreate() {
        super.onCreate()
        client = KungalClient()
        topicApi = TopicApi(client)
        sectionApi = SectionApi(client)
        meApi = MeApi(client)
        sessionStore = SessionStore(this)
        settingsStore = SettingsStore(this)
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

    private fun applySession(session: Session?) {
        client.session.restore(session)
        _session.value = session
    }
}
