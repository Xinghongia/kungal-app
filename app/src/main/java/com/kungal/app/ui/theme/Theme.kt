package com.kungal.app.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** 深浅模式偏好（跟随系统 / 固定浅色 / 固定深色） */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

private val AmoledBlack = Color(0xFF000000)

/**
 * 应用主题（与 saku 同一套外观体系）：
 * - 配色来源：动态取色（Android 12+，默认开）→ 预设主题（[PresetThemes]）
 * - MaterialExpressiveTheme + 弹性动效（依赖 material3 1.5.0 系）
 * - 状态栏/导航栏图标颜色跟随应用深浅（固定深色时也清晰）
 */
@Composable
fun KungalTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    dynamicColor: Boolean = true,
    presetThemeId: String = DefaultPresetThemeId,
    amoledDark: Boolean = false,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    val context = LocalContext.current
    val baseScheme: ColorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

        else -> findPresetTheme(presetThemeId).scheme(darkTheme)
    }

    val colorScheme = if (darkTheme && amoledDark) {
        baseScheme.copy(background = AmoledBlack, surface = AmoledBlack)
    } else {
        baseScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(view, darkTheme) {
            val window = (view.context as? android.app.Activity)?.window
            val controller = window?.let { WindowCompat.getInsetsController(it, view) }
            val prevStatus = controller?.isAppearanceLightStatusBars
            val prevNav = controller?.isAppearanceLightNavigationBars
            controller?.apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
            onDispose {
                controller?.apply {
                    prevStatus?.let { isAppearanceLightStatusBars = it }
                    prevNav?.let { isAppearanceLightNavigationBars = it }
                }
            }
        }
    }

    MaterialExpressiveTheme(
        colorScheme = colorScheme,
        motionScheme = MotionScheme.expressive(),
        content = content,
    )
}
