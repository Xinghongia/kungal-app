package com.kungal.app.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.SubcomposeAsyncImage
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.util.fullDateTime
import com.kungal.core.model.ReactionRecord
import com.kungal.core.model.ReactionSummary

/** 反应 token 的展示元数据（与站点 constants/reaction.ts 对齐；未知 token 用中性兜底）。 */
data class ReactionMeta(val token: String, val emoji: String, val label: String)

val ReactionMetas: List<ReactionMeta> = listOf(
    ReactionMeta("like", "👍", "点赞"),
    ReactionMeta("dislike", "👎", "点踩"),
    ReactionMeta("heart", "❤️", "爱心"),
    ReactionMeta("fire", "🔥", "火"),
    ReactionMeta("party", "🎉", "庆祝"),
    ReactionMeta("love", "🥰", "喜欢"),
    ReactionMeta("clap", "👏", "鼓掌"),
    ReactionMeta("thinking", "🤔", "思考"),
    ReactionMeta("mindblown", "🤯", "震惊"),
    ReactionMeta("scream", "😱", "尖叫"),
    ReactionMeta("cry", "😢", "哭"),
    ReactionMeta("pray", "🙏", "感谢"),
    ReactionMeta("eyes", "👀", "关注"),
    ReactionMeta("hundred", "💯", "满分"),
    ReactionMeta("partyface", "🥳", "派对"),
    ReactionMeta("starstruck", "🤩", "星星眼"),
    ReactionMeta("angry", "😠", "生气"),
    ReactionMeta("anxious", "😰", "紧张"),
    ReactionMeta("banana", "🍌", "香蕉"),
    ReactionMeta("eyebrow", "🤨", "挑眉"),
    ReactionMeta("voltage", "⚡", "闪电"),
    ReactionMeta("hotdog", "🌭", "热狗"),
    ReactionMeta("hot", "🥵", "热"),
    ReactionMeta("sob", "😭", "大哭"),
    ReactionMeta("moai", "🗿", "摩艾"),
    ReactionMeta("newmoon", "🌚", "黑月亮"),
    ReactionMeta("police", "🚓", "警车"),
    ReactionMeta("pouting", "😡", "怒"),
    ReactionMeta("salute", "🫡", "敬礼"),
    ReactionMeta("shrimp", "🦐", "虾"),
    ReactionMeta("halo", "😇", "天使"),
    ReactionMeta("sunglasses", "😎", "酷"),
    ReactionMeta("whale", "🐳", "鲸鱼"),
)

/** 表情面板里除点赞/点踩外的 31 个表情（点赞/点踩单独成行）。 */
private val EmojiReactions = ReactionMetas.filter { it.token != "like" && it.token != "dislike" }

fun reactionEmoji(token: String): String =
    ReactionMetas.firstOrNull { it.token == token }?.emoji ?: "🙂"

fun reactionLabel(token: String): String =
    ReactionMetas.firstOrNull { it.token == token }?.label ?: token

/** 站点表情是动态 WebP，直接走站点 CDN；Coil 已注册动图解码器。 */
fun reactionAssetUrl(token: String): String = "https://www.kungal.com/emoji/$token.webp"

/** 站点同款动态表情图标；加载失败时回退到系统 emoji 文本。 */
@Composable
fun ReactionIcon(token: String, size: Int = 20, modifier: Modifier = Modifier) {
    SubcomposeAsyncImage(
        model = reactionAssetUrl(token),
        contentDescription = reactionLabel(token),
        modifier = modifier.size(size.dp),
        loading = { ReactionEmojiFallback(token, size) },
        error = { ReactionEmojiFallback(token, size) },
    )
}

@Composable
private fun ReactionEmojiFallback(token: String, size: Int) {
    Box(Modifier.size(size.dp), contentAlignment = Alignment.Center) {
        Text(reactionEmoji(token), style = MaterialTheme.typography.bodyMedium)
    }
}

/**
 * 已有点的反应 chip：点击直接发送；再点一次取消（与网站一致）。
 * 已点过的用强调色背景 + 描边 + 对勾区分，未点过的保持中性。
 */
@Composable
fun ReactionChips(
    reactions: List<ReactionSummary>,
    busy: Boolean,
    onToggle: (String) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        reactions.filter { it.count > 0 }.take(6).forEach { summary ->
            val active = summary.viewer?.hasReacted == true
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .then(
                        if (active) {
                            Modifier.border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                shape = RoundedCornerShape(20.dp),
                            )
                        } else {
                            Modifier
                        },
                    )
                    .clickable(enabled = enabled && !busy) { onToggle(summary.reaction) }
                    .padding(horizontal = 9.dp, vertical = 5.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ReactionIcon(summary.reaction, size = 20)
                Spacer(Modifier.width(4.dp))
                Text(
                    summary.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (active) {
                    Spacer(Modifier.width(2.dp))
                    Icon(
                        Icons.Outlined.Check,
                        contentDescription = "已回应",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

/**
 * 网站同款反应弹层：点赞/点踩带说明 + 动态表情网格 + 底部反应总数与历史入口。
 * 点击任一项立即发送；已选中的再点一次会取消。已选项有强调色和对勾。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionPickerButton(
    reactions: List<ReactionSummary>,
    busy: Boolean,
    subjectLabel: String,
    onToggle: (String) -> Unit,
    onLoadHistory: suspend () -> List<ReactionRecord>,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var historyOpen by remember { mutableStateOf(false) }
    val activeTokens = reactions.filter { it.viewer?.hasReacted == true }.map { it.reaction }.toSet()
    val total = reactions.sumOf { it.count }

    if (historyOpen) {
        ReactionHistoryDialog(load = onLoadHistory, onDismiss = { historyOpen = false })
    }

    Box(modifier) {
        IconButton(onClick = { open = true }, enabled = enabled) {
            Icon(
                Icons.Outlined.SentimentSatisfied,
                contentDescription = "表态",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            modifier = Modifier.width(304.dp),
        ) {
            MenuReactionRow(
                token = "like",
                label = "点赞",
                note = "点赞会使被点赞的用户萌萌点 +1",
                active = "like" in activeTokens,
                onClick = {
                    open = false
                    onToggle("like")
                },
            )
            MenuReactionRow(
                token = "dislike",
                label = "点踩",
                note = "被点踩多的用户将会被智乃屠杀",
                active = "dislike" in activeTokens,
                onClick = {
                    open = false
                    onToggle("dislike")
                },
            )
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
                maxItemsInEachRow = 6,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
            ) {
                EmojiReactions.forEach { meta ->
                    val active = meta.token in activeTokens
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                                else MaterialTheme.colorScheme.surfaceContainer,
                            )
                            .then(
                                if (active) {
                                    Modifier.border(
                                        1.dp,
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                        RoundedCornerShape(8.dp),
                                    )
                                } else {
                                    Modifier
                                },
                            )
                            .clickable {
                                open = false
                                onToggle(meta.token)
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        ReactionIcon(meta.token, size = 26)
                        if (active) {
                            Icon(
                                Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier
                                    .align(Alignment.TopEnd)
                                    .padding(2.dp)
                                    .size(11.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            HorizontalDivider(Modifier.padding(vertical = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "$total 人回应了$subjectLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = {
                        open = false
                        historyOpen = true
                    },
                ) { Text("查看历史") }
            }
        }
    }
}

@Composable
private fun MenuReactionRow(
    token: String,
    label: String,
    note: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else MaterialTheme.colorScheme.surfaceContainer,
            )
            .then(
                if (active) {
                    Modifier.border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                        RoundedCornerShape(8.dp),
                    )
                } else {
                    Modifier
                },
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ReactionIcon(token, size = 26)
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                note,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (active) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "已回应",
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

/** 反应历史：谁在什么时候点了什么。 */
@Composable
fun ReactionHistoryDialog(
    load: suspend () -> List<ReactionRecord>,
    onDismiss: () -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<ReactionRecord>>(emptyList()) }

    LaunchedEffect(Unit) {
        runCatching { load() }
            .onSuccess { items = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("反应历史") },
        text = {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                error != null -> ErrorBox(error!!)
                items.isEmpty() -> Text(
                    "还没有人做出反应",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                else -> LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(items, key = { it.id }) { record ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AvatarImage(record.reactor.avatar?.url, Modifier.size(26.dp))
                            Spacer(Modifier.width(8.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    record.reactor.name ?: record.reactor.id,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                                Text(
                                    fullDateTime(record.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            ReactionIcon(record.reaction, size = 20)
                            Spacer(Modifier.width(4.dp))
                            Text(
                                reactionLabel(record.reaction),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}
