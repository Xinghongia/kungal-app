package com.kungal.app.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshState
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.kungal.app.R

/**
 * 下拉刷新容器：指示器只画官方 KunUI 的「Loading」萝莉插图，不加底卡/阴影，
 * 画面自带的白底与浅色界面融合。用法与 [PullToRefreshBox] 一致，
 * 内容需为可滚动列表（LazyColumn / 可滚动 Column）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KungalPullToRefresh(
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    val state = rememberPullToRefreshState()
    PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = onRefresh,
        state = state,
        modifier = modifier,
        indicator = {
            KunRefreshIndicator(state = state, isRefreshing = isRefreshing)
        },
        content = content,
    )
}

@Composable
private fun BoxScope.KunRefreshIndicator(
    state: PullToRefreshState,
    isRefreshing: Boolean,
) {
    val fraction = state.distanceFraction
    if (!isRefreshing && fraction <= 0f) return

    val pulse by rememberInfiniteTransition(label = "kunRefresh").animateFloat(
        initialValue = 0.94f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 650),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "kunRefreshPulse",
    )

    Image(
        painter = painterResource(R.drawable.kun_loading),
        contentDescription = "正在刷新",
        modifier = Modifier
            .align(Alignment.TopCenter)
            .width(150.dp)
            .graphicsLayer {
                translationY = fraction * 72.dp.toPx()
                alpha = if (isRefreshing) 1f else fraction.coerceIn(0f, 1f)
                val scale = if (isRefreshing) pulse else 0.9f + 0.1f * fraction.coerceIn(0f, 1f)
                scaleX = scale
                scaleY = scale
            },
        contentScale = ContentScale.Fit,
    )
}
