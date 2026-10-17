package com.kungal.app.ui.components

import android.os.Build
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.kungal.app.R

/**
 * 通用图片：加载失败/为空时兜底显示占位底图（弱网加载失败很常见，界面不能什么都不画）。
 */
@Composable
fun KungalImage(
    url: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    fallbackIcon: Boolean = false,
    shape: androidx.compose.ui.graphics.Shape = RoundedCornerShape(0.dp),
    contentScale: ContentScale = ContentScale.Crop,
) {
    val placeholder: @Composable () -> Unit = {
        Box(
            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (fallbackIcon) Icons.Outlined.Person else Icons.Outlined.BrokenImage,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp),
            )
        }
    }
    if (url.isNullOrBlank()) {
        Box(modifier = modifier.clip(shape)) { placeholder() }
        return
    }
    SubcomposeAsyncImage(
        model = url,
        contentDescription = contentDescription,
        modifier = modifier.clip(shape),
        contentScale = contentScale,
        loading = { placeholder() },
        error = { placeholder() },
    )
}

@Composable
fun AvatarImage(url: String?, modifier: Modifier = Modifier, seed: String? = null) {
    val pool by AvatarPool.urls.collectAsState()
    KungalImage(
        // 没有上传头像的账号跟网页一致：按用户名稳定挑一张默认头像，而不是灰色占位图。
        url = url?.takeIf { it.isNotBlank() } ?: AvatarPool.pick(seed.orEmpty(), pool),
        contentDescription = null,
        modifier = modifier.clip(CircleShape),
        fallbackIcon = true,
    )
}

/**
 * 成人向封面：「模糊」模式下打码，点击在本卡片内揭示（与网站一致）。
 * Android 12 以下没有 Modifier.blur，用近乎不透明的遮罩兜底。
 */
@Composable
fun NsfwCoverImage(url: String?, modifier: Modifier = Modifier) {
    var revealed by remember(url) { mutableStateOf(false) }
    val canBlur = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    Box(modifier) {
        KungalImage(
            url = url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .then(if (!revealed && canBlur) Modifier.blur(18.dp) else Modifier),
        )
        if (!revealed) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = if (canBlur) 0.55f else 0.96f),
                    )
                    .clickable { revealed = true },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                Icon(
                    Icons.Outlined.Visibility,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "成人向封面已模糊",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "点击查看",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                )
            }
        }
    }
}

/**
 * 加载占位：官方 KunUI 的「Loading」萝莉插图（白底与浅色界面融合），
 * 不再使用转圈指示器。
 */
@Composable
fun LoadingBox(modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxWidth().padding(vertical = 16.dp), contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(R.drawable.kun_loading),
            contentDescription = "加载中",
            modifier = Modifier.width(132.dp),
            contentScale = ContentScale.Fit,
        )
    }
}

/**
 * 空状态：网站同款的笨蛋萝莉插图 + 说明文字。
 * 插图来自官方 KunUI（@kungal/ui-vue，AGPL-3.0），仅作界面装饰。
 */
@Composable
fun EmptyBox(
    text: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.kun_empty),
            contentDescription = null,
            modifier = Modifier.width(210.dp),
            contentScale = ContentScale.Fit,
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
fun ErrorBox(
    message: String,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) { Text("重试") }
        }
    }
}

@Composable
fun Chip(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

/** 透明融合顶栏（沿用 saku 设计）：内容滚动时容器保持透明，标题加粗。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KungalTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    navigationIcon: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        },
        navigationIcon = navigationIcon,
        actions = actions,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = Color.Transparent,
        ),
    )
}
