package com.kungal.app.ui.galgame

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.AspectScores
import com.kungal.core.model.RatingSummary
import com.kungal.core.model.WorkRef
import java.util.Locale

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
    "route" to "路线",
    "system" to "系统",
    "voice" to "配音",
    "replay_value" to "重玩价值",
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
    "路线" to scores.route,
    "系统" to scores.system,
    "配音" to scores.voice,
    "重玩价值" to scores.replayValue,
).mapNotNull { (label, value) -> value?.let { label to it } }

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
    val url = work?.cover?.url
    if (work?.isNsfw == true && blurCover) {
        NsfwCoverImage(
            url = url,
            modifier = Modifier.width(48.dp).aspectRatio(5f / 7f).clip(RoundedCornerShape(6.dp)),
        )
    } else {
        KungalImage(
            url = url,
            contentDescription = null,
            modifier = Modifier.width(48.dp).aspectRatio(5f / 7f).clip(RoundedCornerShape(6.dp)),
        )
    }
}

fun workTitleOf(work: WorkRef): String =
    work.localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() }
        ?: work.displayName.ifBlank { "#${work.id}" }

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
