package com.kungal.app.ui.topic

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.AddReaction
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.sp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.util.relativeTime
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

fun reactionEmoji(token: String): String =
    ReactionMetas.firstOrNull { it.token == token }?.emoji ?: "🙂"

fun reactionLabel(token: String): String =
    ReactionMetas.firstOrNull { it.token == token }?.label ?: token

/** 话题/回复共用的反应条：已有反应计数 + 表情选择 + 历史。 */
@Composable
fun ReactionBar(
    reactions: List<ReactionSummary>,
    busy: Boolean,
    onToggle: (String) -> Unit,
    onShowHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var pickerOpen by remember { mutableStateOf(false) }
    val activeTokens = reactions.filter { it.viewer?.hasReacted == true }.map { it.reaction }.toSet()

    if (pickerOpen) {
        ReactionPickerDialog(
            activeTokens = activeTokens,
            enabled = !busy,
            onToggle = onToggle,
            onDismiss = { pickerOpen = false },
        )
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        reactions.filter { it.count > 0 }.take(6).forEach { summary ->
            val active = summary.viewer?.hasReacted == true
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .background(
                        if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                        else MaterialTheme.colorScheme.surfaceVariant,
                    )
                    .clickable(enabled = !busy) { onToggle(summary.reaction) }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(reactionEmoji(summary.reaction), fontSize = 14.sp)
                Spacer(Modifier.width(4.dp))
                Text(
                    summary.count.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(onClick = { pickerOpen = true }, enabled = !busy) {
            Icon(
                Icons.Outlined.AddReaction,
                contentDescription = "添加表情反应",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onShowHistory) {
            Icon(
                Icons.Outlined.History,
                contentDescription = "反应历史",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ReactionPickerDialog(
    activeTokens: Set<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("选择表情反应") },
        text = {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                ReactionMetas.forEach { meta ->
                    val active = meta.token in activeTokens
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                            )
                            .clickable(enabled = enabled) { onToggle(meta.token) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(meta.emoji, fontSize = 22.sp)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("完成") }
        },
    )
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
                                    relativeTime(record.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(reactionEmoji(record.reaction), fontSize = 18.sp)
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

/** 回复卡片底部的快捷反应（点赞/点踩）。 */
@Composable
fun QuickReactionRow(
    reactions: List<ReactionSummary>,
    busy: Boolean,
    onToggle: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("like", "dislike").forEach { token ->
            val summary = reactions.firstOrNull { it.reaction == token }
            val count = summary?.count ?: 0
            val active = summary?.viewer?.hasReacted == true
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .clickable(enabled = !busy) { onToggle(token) }
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(reactionEmoji(token), fontSize = 13.sp)
                Spacer(Modifier.width(3.dp))
                Text(
                    count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (active) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
