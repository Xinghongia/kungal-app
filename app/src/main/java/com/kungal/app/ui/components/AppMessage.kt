package com.kungal.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 全 App 共用的轻提示：取代系统 Toast。
 * 文案由 [show] 投递，导航根部的 [AppMessageHost] 用当前主题的 Material 3 样式统一显示。
 */
object AppMessages {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    fun show(message: String) {
        _messages.tryEmit(message)
    }
}

/**
 * 主题化轻提示容器。页面内已有 [SnackbarHostState] 时，用这个渲染，
 * 不要直接用 Material 默认 [SnackbarHost]（默认是深色横条，和 App 风格不一致）。
 */
@Composable
fun KungalSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        Snackbar(
            snackbarData = data,
            shape = RoundedCornerShape(14.dp),
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
    }
}

/** 挂在根 Scaffold 的 snackbarHost 槽位；会显示在底部导航栏上方。 */
@Composable
fun AppMessageHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        AppMessages.messages.collect { message ->
            hostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    KungalSnackbarHost(hostState = hostState, modifier = modifier)
}
