package com.kungal.app

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.kungal.app.ui.theme.DefaultPresetThemeId
import com.kungal.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore by preferencesDataStore(name = "settings")

/** 应用偏好：外观与浏览习惯（与登录会话的 "kungal_session" DataStore 分开存储）。 */
data class Settings(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    /** 动态取色（Android 12+ 跟随壁纸），默认关：默认使用极简主题 */
    val dynamicColor: Boolean = false,
    val presetThemeId: String = DefaultPresetThemeId,
    /** 深色模式下使用纯黑背景（OLED 省电 / 观感） */
    val amoledDark: Boolean = false,
    /** 话题列表默认排序；空字符串 = 站点默认（bumped_desc，最新回复） */
    val defaultSort: String = "",
    /** 未登录时的成人向内容显示方式（登录后跟随 NextMoe 账号） */
    val nsfwDisplay: NsfwDisplayMode = NsfwDisplayMode.HIDE,
)

class SettingsStore(private val context: Context) {

    private val themeModeKey = stringPreferencesKey("theme_mode")
    private val dynamicColorKey = booleanPreferencesKey("dynamic_color")
    private val presetThemeIdKey = stringPreferencesKey("preset_theme_id")
    private val amoledDarkKey = booleanPreferencesKey("amoled_dark")
    private val defaultSortKey = stringPreferencesKey("default_sort")
    private val nsfwDisplayKey = stringPreferencesKey("nsfw_display")

    val flow: Flow<Settings> = context.settingsDataStore.data.map { prefs ->
        Settings(
            themeMode = ThemeMode.entries.firstOrNull { it.name == prefs[themeModeKey] }
                ?: ThemeMode.SYSTEM,
            dynamicColor = prefs[dynamicColorKey] ?: false,
            presetThemeId = prefs[presetThemeIdKey] ?: DefaultPresetThemeId,
            amoledDark = prefs[amoledDarkKey] ?: false,
            defaultSort = prefs[defaultSortKey] ?: "",
            nsfwDisplay = NsfwDisplayMode.fromWire(prefs[nsfwDisplayKey]),
        )
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[themeModeKey] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[dynamicColorKey] = enabled }
    }

    suspend fun setPresetThemeId(id: String) {
        context.settingsDataStore.edit { it[presetThemeIdKey] = id }
    }

    suspend fun setAmoledDark(enabled: Boolean) {
        context.settingsDataStore.edit { it[amoledDarkKey] = enabled }
    }

    suspend fun setDefaultSort(key: String) {
        context.settingsDataStore.edit { it[defaultSortKey] = key }
    }

    suspend fun setNsfwDisplay(mode: NsfwDisplayMode) {
        context.settingsDataStore.edit { it[nsfwDisplayKey] = mode.wire }
    }
}
