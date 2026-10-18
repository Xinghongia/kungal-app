package com.kungal.app.ui.galgame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.AspectScores
import com.kungal.core.model.RatingSummary
import com.kungal.core.model.WorkRef
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

val RatingRecommendLabels = listOf(
    "strong_no" to "强烈不推荐",
    "no" to "不推荐",
    "neutral" to "中立",
    "yes" to "推荐",
    "strong_yes" to "强烈推荐",
)

val RatingSpoilerLabels = listOf(
    "none" to "无剧透",
    "portion" to "部分剧透",
    "serious" to "严重剧透",
)

val RatingPlayStateLabels = listOf(
    "wish" to "想玩",
    "doing" to "在玩",
    "done_one_route" to "完成一条线",
    "done_main" to "完成主线",
    "done_all" to "全通",
    "done" to "完成（未标注）",
    "on_hold" to "搁置",
    "dropped" to "弃坑",
)

val RatingGameTypeLabels = listOf(
    "ba_saku" to "拔作",
    "plot" to "剧情作",
    "moe" to "萌系",
    "daily" to "日常系",
)

val AspectLabels = listOf(
    "art" to "画面",
    "story" to "剧情",
    "music" to "音乐",
    "character" to "角色",
    "route" to "分支",
    "system" to "系统",
    "voice" to "配音",
    "replay_value" to "重玩",
)

fun recommendLabel(key: String): String =
    RatingRecommendLabels.firstOrNull { it.first == key }?.second ?: key

fun ratingSpoilerLabel(key: String): String =
    RatingSpoilerLabels.firstOrNull { it.first == key }?.second ?: key

fun playStateLabel(key: String?): String =
    key?.let { state -> RatingPlayStateLabels.firstOrNull { it.first == state }?.second } ?: "未记录"

fun gameTypeLabel(key: String): String =
    RatingGameTypeLabels.firstOrNull { it.first == key }?.second ?: key

fun aspectItems(scores: AspectScores): List<Pair<String, Int>> = listOf(
    "画面" to scores.art,
    "剧情" to scores.story,
    "音乐" to scores.music,
    "角色" to scores.character,
    "分支" to scores.route,
    "系统" to scores.system,
    "配音" to scores.voice,
    "重玩" to scores.replayValue,
).mapNotNull { (label, value) -> value?.let { label to it } }

/** 雷达图固定八轴，未填写的维度按 0（与网页 aspectDims 的默认值一致）。 */
fun aspectRadarItems(scores: AspectScores): List<Pair<String, Int>> = listOf(
    "画面" to (scores.art ?: 0),
    "剧情" to (scores.story ?: 0),
    "音乐" to (scores.music ?: 0),
    "角色" to (scores.character ?: 0),
    "分支" to (scores.route ?: 0),
    "系统" to (scores.system ?: 0),
    "配音" to (scores.voice ?: 0),
    "重玩" to (scores.replayValue ?: 0),
)

fun scoreText(value: Int): String = String.format(Locale.US, "%d", value)

/** 评分列表 / 用户主页复用的评分卡片。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingRow(
    rating: RatingSummary,
    blurCover: Boolean,
    onClick: () -> Unit,
) {
    val work = rating.work
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            RatingWorkCover(work, blurCover)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        rating.work?.let { workTitleOf(it) } ?: "未知作品",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        scoreText(rating.overall),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AvatarImage(
                        url = rating.author.avatar?.url,
                        seed = rating.author.name,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        rating.author.name ?: rating.author.id,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        relativeTime(rating.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Chip(recommendLabel(rating.recommend))
                    rating.gameTypes.take(2).forEach { Chip(gameTypeLabel(it)) }
                    if (rating.spoilerLevel != "none") Chip(ratingSpoilerLabel(rating.spoilerLevel))
                }
                if (rating.shortSummary.isNotBlank()) {
                    if (rating.spoilerLevel == "none") {
                        Text(
                            rating.shortSummary,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    } else {
                        Text(
                            "该评分可能含有剧透内容，点进查看",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        rating.likeCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Outlined.ChatBubbleOutline,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        rating.commentCount.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RatingWorkCover(work: WorkRef?, blurCover: Boolean) {
    WorkCoverImage(
        url = work?.cover?.url,
        isNsfw = work?.isNsfw == true,
        blurNsfw = blurCover,
        modifier = Modifier.width(48.dp).aspectRatio(5f / 7f).clip(RoundedCornerShape(6.dp)),
    )
}

fun workTitleOf(work: WorkRef): String =
    work.localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() }
        ?: work.displayName.ifBlank { "#${work.id}" }

/** 八维评分雷达图（对应网页评分详情的 Radar）；0 分维度落在中心。 */
@Composable
fun AspectRadarChart(scores: AspectScores, modifier: Modifier = Modifier) {
    val axes = aspectRadarItems(scores)
    val textMeasurer = rememberTextMeasurer()
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f)
    val strokeColor = MaterialTheme.colorScheme.primary
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        fontSize = 10.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Canvas(modifier) {
        val labelPad = 26.dp.toPx()
        val radius = (min(size.width, size.height) / 2f - labelPad).coerceAtLeast(1f)
        val center = Offset(size.width / 2f, size.height / 2f)
        val count = axes.size

        fun vertex(index: Int, r: Float): Offset {
            val angle = -PI / 2 + index * (2 * PI / count)
            return Offset(
                x = center.x + r * cos(angle).toFloat(),
                y = center.y + r * sin(angle).toFloat(),
            )
        }

        fun polygonPath(radiusAt: (Int) -> Float): Path {
            val path = Path()
            for (index in 0 until count) {
                val point = vertex(index, radiusAt(index))
                if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
            }
            path.close()
            return path
        }

        // 10 圈网格（网页 max = 10）
        for (level in 1..10) {
            drawPath(
                polygonPath { radius * level / 10f },
                gridColor.copy(alpha = if (level == 10) 0.7f else 0.35f),
                style = Stroke(width = 0.7.dp.toPx()),
            )
        }
        for (index in 0 until count) {
            drawLine(
                gridColor.copy(alpha = 0.5f),
                center,
                vertex(index, radius),
                strokeWidth = 0.7.dp.toPx(),
            )
        }

        val valuePath = polygonPath { index ->
            radius * (axes[index].second.coerceIn(0, 10) / 10f)
        }
        drawPath(valuePath, fillColor)
        drawPath(valuePath, strokeColor, style = Stroke(width = 1.5.dp.toPx()))

        axes.forEachIndexed { index, (label, value) ->
            if (value > 0) {
                drawCircle(
                    color = strokeColor,
                    radius = 2.dp.toPx(),
                    center = vertex(index, radius * (value.coerceIn(0, 10) / 10f)),
                )
            }
            val labelCenter = vertex(index, radius + labelPad * 0.62f)
            val layout = textMeasurer.measure(AnnotatedString(label), style = labelStyle)
            drawText(
                textLayoutResult = layout,
                topLeft = Offset(
                    (labelCenter.x - layout.size.width / 2f)
                        .coerceIn(0f, (size.width - layout.size.width).coerceAtLeast(0f)),
                    (labelCenter.y - layout.size.height / 2f)
                        .coerceIn(0f, (size.height - layout.size.height).coerceAtLeast(0f)),
                ),
            )
        }
    }
}

/** 八维评分条形展示。 */
@Composable
fun AspectScoreBars(scores: AspectScores) {
    val items = aspectItems(scores)
    if (items.isEmpty()) return
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        items.forEach { (label, value) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(64.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant,
                            RoundedCornerShape(3.dp),
                        )
                        .padding(0.dp),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(value / 10f)
                            .background(
                                MaterialTheme.colorScheme.primary,
                                RoundedCornerShape(3.dp),
                            )
                            .padding(vertical = 4.dp),
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    value.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.width(18.dp),
                )
            }
        }
    }
}
