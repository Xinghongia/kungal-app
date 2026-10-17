package com.kungal.app.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * 全 App 共用的轻提示：取代系统 Toast。
 * 文案由 [show] 投递，导航根部的 [AppMessageHost] 用当前主题的 Material 3 样式统一显示。
 * 底部弹窗（ModalBottomSheet 是独立窗口）里要显示提示时，在弹窗内容顶部放 [SheetMessageHost]，
 * 它会接管提示的显示；根部的提示会自动让位，避免被弹窗遮挡后用户看不到。
 */
object AppMessages {
    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 8)
    val messages: SharedFlow<String> = _messages

    /** 当前是否有弹窗内的提示容器；为 true 时根部容器不重复显示。 */
    @Volatile
    var sheetHostActive: Boolean = false

    fun show(message: String) {
        _messages.tryEmit(message)
    }
}

/**
 * 主题化轻提示容器，左右滑动可立即关闭。
 * 页面内已有 [SnackbarHostState] 时，用这个渲染，
 * 不要直接用 Material 默认 [SnackbarHost]（默认是深色横条，和 App 风格不一致）。
 */
@Composable
fun KungalSnackbarHost(
    hostState: SnackbarHostState,
    modifier: Modifier = Modifier,
) {
    SnackbarHost(hostState = hostState, modifier = modifier) { data ->
        val dismissState = rememberSwipeToDismissBoxState(
            confirmValueChange = { value ->
                if (value != SwipeToDismissBoxValue.Settled) {
                    data.dismiss()
                    true
                } else {
                    false
                }
            },
        )
        SwipeToDismissBox(
            state = dismissState,
            enableDismissFromStartToEnd = true,
            enableDismissFromEndToStart = true,
            backgroundContent = {},
            modifier = Modifier.padding(horizontal = 16.dp),
        ) {
            Snackbar(
                snackbarData = data,
                shape = RoundedCornerShape(14.dp),
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                contentColor = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

/** 挂在根 Scaffold 的 snackbarHost 槽位；弹窗打开时让位给弹窗内的 [SheetMessageHost]。 */
@Composable
fun AppMessageHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        AppMessages.messages.collect { message ->
            if (AppMessages.sheetHostActive) return@collect
            hostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    KungalSnackbarHost(hostState = hostState, modifier = modifier)
}

/**
 * 底部弹窗内容顶部的提示容器：让「已复制」这类反馈显示在弹窗可见范围内，
 * 不再被弹窗挡住。放在弹窗内容 Column 的最上面（或 Box 的 TopCenter）。
 */
@Composable
fun SheetMessageHost(modifier: Modifier = Modifier) {
    val hostState = remember { SnackbarHostState() }
    DisposableEffect(Unit) {
        AppMessages.sheetHostActive = true
        onDispose { AppMessages.sheetHostActive = false }
    }
    LaunchedEffect(Unit) {
        AppMessages.messages.collect { message ->
            hostState.showSnackbar(message, duration = SnackbarDuration.Short)
        }
    }
    KungalSnackbarHost(hostState = hostState, modifier = modifier)
}
