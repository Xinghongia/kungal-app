package com.kungal.app

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kungal.core.session.Session
import kotlinx.coroutines.flow.first

private val Context.sessionDataStore by preferencesDataStore(name = "kungal_session")

/**
 * 登录会话的本地持久化（cookie + 保存时间）。
 * 注意：当前为应用私有目录明文存储；后续可升级为加密存储（进 Parking Lot 前先如实记录）。
 */
class SessionStore(private val context: Context) {

    private val keyCookie = stringPreferencesKey("cookie")
    private val keySavedAt = longPreferencesKey("saved_at")

    suspend fun load(): Session? {
        val prefs = context.sessionDataStore.data.first()
        val cookie = prefs[keyCookie] ?: return null
        return Session(cookie = cookie, savedAt = prefs[keySavedAt] ?: 0L)
    }

    suspend fun save(session: Session) {
        context.sessionDataStore.edit { prefs ->
            prefs[keyCookie] = session.cookie
            prefs[keySavedAt] = session.savedAt
        }
    }

    suspend fun clear() {
        context.sessionDataStore.edit { it.clear() }
    }
}
