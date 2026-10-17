package com.kungal.app.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.Lottery
import com.kungal.core.model.LotteryEntry
import com.kungal.core.model.Poll
import com.kungal.core.model.PollVote

/** 投票卡片：选项、投票/撤票、结果条。 */
@Composable
fun PollCard(
    poll: Poll,
    onVote: (List<String>) -> Unit,
    onClearVote: () -> Unit,
    onShowVotes: suspend () -> List<PollVote>,
) {
    val viewer = poll.viewer
    val results = poll.results
    var selected by remember(poll.id, viewer?.chosenOptionIds) {
        mutableStateOf(viewer?.chosenOptionIds?.toSet() ?: emptySet())
    }
    var showVotes by remember { mutableStateOf(false) }
    val canVote = viewer?.canVote == true
    val multiple = poll.choiceType == "multiple"

    if (showVotes) {
        VoteLogDialog(load = onShowVotes, onDismiss = { showVotes = false })
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip("投票")
                Spacer(Modifier.width(8.dp))
                Text(
                    poll.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (poll.description.isNotBlank()) {
                Text(
                    poll.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip(if (multiple) "多选" else "单选")
                if (poll.isAnonymous) Chip("匿名")
                poll.closesAt?.let { Chip("截止 ${relativeTime(it)}") }
                results?.let { Chip("${it.voterCount} 人参与") }
            }

            poll.options.forEach { option ->
                val result = results?.options?.firstOrNull { it.optionId == option.id }
                val total = results?.totalVoteCount?.coerceAtLeast(1) ?: 1
                val fraction = (result?.voteCount ?: 0).toFloat() / total
                val chosen = option.id in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .border(
                            width = if (chosen) 1.5.dp else 1.dp,
                            color = if (chosen) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outlineVariant,
                            shape = RoundedCornerShape(10.dp),
                        )
                        .clickable(enabled = canVote) {
                            selected = if (multiple) {
                                when {
                                    option.id in selected -> selected - option.id
                                    selected.size >= poll.maxChoice -> selected
                                    else -> selected + option.id
                                }
                            } else {
                                setOf(option.id)
                            }
                        }
                        .padding(horizontal = 10.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (multiple) {
                        Checkbox(checked = chosen, onCheckedChange = null, enabled = canVote)
                    } else {
                        RadioButton(selected = chosen, onClick = null, enabled = canVote)
                    }
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(option.text, style = MaterialTheme.typography.bodyMedium)
                        if (results != null) {
                            Spacer(Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { fraction },
                                modifier = Modifier.fillMaxWidth().height(5.dp).clip(RoundedCornerShape(3.dp)),
                            )
                            Text(
                                "${result?.voteCount ?: 0} 票 · ${(fraction * 100).toInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (canVote) {
                    Button(
                        onClick = { onVote(selected.toList()) },
                        enabled = selected.size >= poll.minChoice.coerceAtLeast(1),
                    ) {
                        Text(if (viewer?.hasVoted == true) "更新投票" else "投票")
                    }
                }
                if (viewer?.hasVoted == true && viewer.canChangeVote) {
                    TextButton(onClick = onClearVote) { Text("撤销投票") }
                }
                if (results != null && !poll.isAnonymous) {
                    TextButton(onClick = { showVotes = true }) { Text("投票记录") }
                }
            }
            if (results == null && viewer?.hasVoted == true) {
                Text(
                    "已投票；结果在满足可见条件后显示。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            results?.sampleVoters?.takeIf { it.isNotEmpty() }?.let { voters ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    voters.take(5).forEach { user ->
                        AvatarImage(user.avatar?.url, Modifier.size(20.dp), seed = user.name)
                        Spacer(Modifier.width(4.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "等 ${results.voterCount} 人",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun VoteLogDialog(load: suspend () -> List<PollVote>, onDismiss: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var votes by remember { mutableStateOf<List<PollVote>>(emptyList()) }
    LaunchedEffect(Unit) {
        runCatching { load() }
            .onSuccess { votes = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("投票记录") },
        text = {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                error != null -> ErrorBox(error!!)
                votes.isEmpty() -> Text("还没有人投票")
                else -> LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(votes, key = { it.id }) { vote ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AvatarImage(vote.voter.avatar?.url, Modifier.size(24.dp), seed = vote.voter.name)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                vote.voter.name ?: vote.voter.id,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                vote.optionIds.joinToString("、"),
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

/** 抽奖卡片：奖品、参与/退出、名单、中奖与兑换码。 */
@Composable
fun LotteryCard(
    lottery: Lottery,
    onEnter: () -> Unit,
    onWithdraw: () -> Unit,
    loadEntries: suspend () -> List<LotteryEntry>,
    onRevealCode: suspend () -> String,
) {
    val viewer = lottery.viewer
    var showEntries by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf<String?>(null) }
    var revealError by remember { mutableStateOf<String?>(null) }
    var revealing by remember { mutableStateOf(false) }

    if (showEntries) {
        EntryListDialog(load = loadEntries, onDismiss = { showEntries = false })
    }
    if (code != null || revealError != null) {
        AlertDialog(
            onDismissRequest = { code = null; revealError = null },
            title = { Text("兑换码") },
            text = { Text(code ?: revealError.orEmpty()) },
            confirmButton = {
                TextButton(onClick = { code = null; revealError = null }) { Text("关闭") }
            },
        )
    }

    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Chip("抽奖")
                Spacer(Modifier.width(8.dp))
                Text(
                    lottery.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            if (lottery.description.isNotBlank()) {
                Text(
                    lottery.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("${lottery.entryCount} 人参与")
                Chip("${lottery.slotCount} 个名额")
                Chip(
                    when (lottery.state) {
                        "open" -> "进行中"
                        "drawn" -> "已开奖"
                        "cancelled" -> "已取消"
                        else -> lottery.state
                    },
                )
                lottery.closesAt?.let { Chip("截止 ${relativeTime(it)}") }
            }
            lottery.prizes.forEach { prize ->
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(10.dp),
                ) {
                    Text(prize.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    if (prize.description.isNotBlank()) {
                        Text(
                            prize.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Text(
                        "${prize.slotCount} 份 · " + when (prize.delivery) {
                            "code" -> "兑换码"
                            "point" -> "萌萌点"
                            else -> "线下发放"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (lottery.winners.isNotEmpty()) {
                Text("中奖名单", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                lottery.winners.forEach { winner ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        AvatarImage(winner.winner.avatar?.url, Modifier.size(22.dp), seed = winner.winner.name)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            winner.winner.name ?: winner.winner.id,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.weight(1f),
                        )
                        winner.winningFloor?.let {
                            Text(
                                "#$it 楼",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                when {
                    viewer?.hasEntered == true -> {
                        TextButton(onClick = onWithdraw) { Text("退出抽奖") }
                    }
                    viewer?.canEnter == true -> {
                        Button(onClick = onEnter) { Text("参与抽奖") }
                    }
                    else -> {
                        viewer?.enterBlockedReason?.let { reason ->
                            Text(
                                blockedReasonLabel(reason),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                if (viewer?.canViewEntries == true) {
                    TextButton(onClick = { showEntries = true }) { Text("参与名单") }
                }
                if (viewer?.canRevealCode == true) {
                    TextButton(
                        enabled = !revealing,
                        onClick = {
                            revealing = true
                            revealError = null
                            // 由调用方提供 suspend 加载器，结果在弹窗里展示
                        },
                    ) { Text("查看兑换码") }
                }
            }
        }
    }

    if (viewer?.canRevealCode == true) {
        LaunchedEffect(revealing) {
            if (revealing) {
                runCatching { onRevealCode() }
                    .onSuccess { code = it; revealing = false }
                    .onFailure { revealError = friendlyError(it); revealing = false }
            }
        }
    }
}

private fun blockedReasonLabel(reason: String): String = when (reason) {
    "reply_required" -> "先回复本话题才能参与"
    "account_age" -> "账号注册天数不足"
    "moemoepoint" -> "萌萌点不足"
    "already_entered" -> "你已经参与过了"
    "closed" -> "抽奖已结束"
    "drawn" -> "已开奖"
    else -> "当前不能参与（$reason）"
}

@Composable
private fun EntryListDialog(load: suspend () -> List<LotteryEntry>, onDismiss: () -> Unit) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var entries by remember { mutableStateOf<List<LotteryEntry>>(emptyList()) }
    LaunchedEffect(Unit) {
        runCatching { load() }
            .onSuccess { entries = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("参与名单") },
        text = {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
                }
                error != null -> ErrorBox(error!!)
                entries.isEmpty() -> Text("还没有人参与")
                else -> LazyColumn(Modifier.heightIn(max = 420.dp)) {
                    items(entries, key = { it.id }) { entry ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            AvatarImage(entry.entrant.avatar?.url, Modifier.size(24.dp), seed = entry.entrant.name)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                entry.entrant.name ?: entry.entrant.id,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                relativeTime(entry.createdAt),
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
