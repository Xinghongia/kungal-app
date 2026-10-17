package com.kungal.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.kungal.app.ui.AppNav
import com.kungal.app.ui.theme.KungalTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val app = application as KungalApp
        // 恢复上次登录的会话；失败不影响匿名浏览
        lifecycleScope.launch {
            runCatching { app.restoreSession() }
        }
        setContent {
            val settings by app.settingsStore.flow.collectAsState(initial = Settings())
            KungalTheme(
                themeMode = settings.themeMode,
                dynamicColor = settings.dynamicColor,
                presetThemeId = settings.presetThemeId,
                amoledDark = settings.amoledDark,
            ) {
                AppNav(app)
            }
        }
    }
}
