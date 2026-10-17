package com.kungal.app.ui.user

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.kungal.app.ui.components.AvatarImage
import com.kungal.core.model.AvatarFrame

/** 头像 + 商城头像框（挂件为透明 PNG/WebP，按原始比例铺满外框）。 */
@Composable
fun FramedAvatar(
    avatarUrl: String?,
    frame: AvatarFrame?,
    size: Dp,
    modifier: Modifier = Modifier,
    seed: String? = null,
) {
    val frameUrl = frame?.animatedUrl?.takeIf { it.isNotBlank() } ?: frame?.staticUrl?.takeIf { it.isNotBlank() }
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        AvatarImage(url = avatarUrl, modifier = Modifier.size(size * 0.84f), seed = seed)
        if (frameUrl != null) {
            SubcomposeAsyncImage(
                model = frameUrl,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit,
            )
        }
    }
}

/** 圆角筛选片（用户主页各标签的二级筛选）。 */

/** 区块标题 + 「查看全部」；用于用户主页「动态」概览。 */
@Composable
fun SectionHeader(title: String, onMore: (() -> Unit)? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 14.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        if (onMore != null) {
            TextButton(onClick = onMore) { Text("查看全部") }
        }
    }
}

/** 主页头部的统计格：数值 + 标签；关注/粉丝可点。 */
@Composable
fun StatCell(
    value: Int?,
    label: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    val clickModifier = if (onClick != null) Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClick) else Modifier
    Column(
        modifier = modifier.then(clickModifier).padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value?.toString() ?: "—",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (onClick != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** 「关于」标签的统计行（两列）。 */
@Composable
fun StatGridRow(items: List<Pair<String, Int?>>) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        items.forEach { (label, value) ->
            Surface(
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        text = value?.toString() ?: "—",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
        if (items.size == 1) Spacer(Modifier.weight(1f))
    }
}

/** 用户列表行（关注/粉丝）。 */
@Composable
fun UserRow(
    name: String?,
    id: String,
    avatarUrl: String?,
    subtitle: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(url = avatarUrl, modifier = Modifier.size(40.dp), seed = name)
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = name ?: "已注销用户",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = subtitle ?: "UID $id",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/** 简单内容行：标题 + 副标题（话题/回复/评论/作品等列表通用）。 */
@Composable
fun SimpleContentRow(
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 9.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        if (!subtitle.isNullOrBlank()) {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

val PlayStateLabels: Map<String, String> = mapOf(
    "wish" to "想玩",
    "doing" to "在玩",
    "done_one_route" to "通关一条线",
    "done_main" to "通关主线",
    "done_all" to "全通关",
    "on_hold" to "搁置",
    "dropped" to "抛弃",
    "done" to "玩过",
)

fun playStateLabel(state: String?): String = state?.let { PlayStateLabels[it] ?: it } ?: "未标记"

val RatingRecommendLabels: Map<String, String> = mapOf(
    "strong_yes" to "强烈推荐",
    "yes" to "推荐",
    "neutral" to "一般",
    "no" to "不推荐",
    "strong_no" to "强烈不推荐",
)

fun ratingRecommendLabel(value: String): String = RatingRecommendLabels[value] ?: value

val QuizTypeLabels: Map<String, String> = mapOf(
    "single" to "单选",
    "multiple" to "多选",
    "judge" to "判断",
)

val QuizCategoryLabels: Map<String, String> = mapOf(
    "plot" to "剧情",
    "character" to "角色",
    "system" to "系统",
    "music" to "音乐",
    "voice" to "配音",
    "company" to "会社",
    "trivia" to "杂学",
    "other" to "其它",
)

val ToolsetTypeLabels: Map<String, String> = mapOf(
    "emulator" to "模拟器",
    "translator" to "翻译",
    "extractor" to "解包",
    "converter" to "转换",
    "debug" to "调试",
    "launcher" to "启动器",
    "script" to "脚本",
    "docs" to "文档",
    "others" to "其它",
)

val PlatformLabels: Map<String, String> = mapOf(
    "windows" to "Windows",
    "mac" to "macOS",
    "linux" to "Linux",
    "emulator" to "模拟器",
    "others" to "其它",
)

/** 站点页面地址（详情页尚未在 App 内实现的，先跳网页）。 */
fun siteUrl(path: String): String = "https://www.kungal.com$path"
