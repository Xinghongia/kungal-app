package com.kungal.app.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.Lottery
import com.kungal.core.model.LotteryEntry
import com.kungal.core.model.LotteryPrizeImage
import com.kungal.core.model.Poll
import com.kungal.core.model.PollVote
import kotlinx.coroutines.launch

/** 投票卡片：选项、投票/撤票、结果条。 */
@Composable
fun PollCard(
    poll: Poll,
    onVote: (List<String>) -> Unit,
    onClearVote: () -> Unit,
    onShowVotes: suspend () -> List<PollVote>,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
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
                Spacer(Modifier.weight(1f))
                if (viewer?.canEdit == true && onEdit != null) {
                    TextButton(onClick = onEdit) { Text("编辑") }
                }
                if (viewer?.canDelete == true && onDelete != null) {
                    TextButton(onClick = onDelete) { Text("删除") }
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

/** 抽奖卡片：奖品、参与/退出、名单、中奖与兑换码；作者另有开奖/编辑/取消/删除与履约入口。 */
@Composable
fun LotteryCard(
    lottery: Lottery,
    onEnter: () -> Unit,
    onWithdraw: () -> Unit,
    loadEntries: suspend () -> List<LotteryEntry>,
    onRevealCode: suspend () -> String,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onDraw: (() -> Unit)? = null,
    onCancel: (() -> Unit)? = null,
    onSetFulfillment: (suspend (winnerId: String, fulfillment: String) -> Boolean)? = null,
    nsfwDisplay: NsfwDisplayMode = NsfwDisplayMode.HIDE,
) {
    val viewer = lottery.viewer
    val scope = rememberCoroutineScope()
    var showEntries by remember { mutableStateOf(false) }
    var code by remember { mutableStateOf<String?>(null) }
    var revealError by remember { mutableStateOf<String?>(null) }
    var revealing by remember { mutableStateOf(false) }
    var actionBusy by remember(lottery.id) { mutableStateOf(false) }
    var menuWinnerId by remember(lottery.id) { mutableStateOf<String?>(null) }

    fun perform(action: suspend () -> Boolean) {
        if (actionBusy) return
        scope.launch {
            actionBusy = true
            try {
                action()
            } finally {
                actionBusy = false
            }
        }
    }

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
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(lottery.entryModeLabel())
                Chip(lottery.drawModeLabel())
                Chip("${lottery.entryCount} 人参与")
                Chip("${lottery.slotCount} 个名额")
                Chip(
                    when (lottery.state) {
                        "open" -> "进行中"
                        "drawing" -> "开奖中"
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
                    verticalArrangement = Arrangement.spacedBy(6.dp),
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
                    val visibleImages = prize.images.filter { image ->
                        image.image?.url?.isNotBlank() == true &&
                            (nsfwDisplay != NsfwDisplayMode.HIDE ||
                                !(image.isMarkedAdult || image.isGradedExplicit))
                    }
                    if (visibleImages.isNotEmpty()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            visibleImages.take(3).forEach { image ->
                                PrizeThumb(
                                    image = image,
                                    title = prize.title,
                                    nsfwDisplay = nsfwDisplay,
                                )
                            }
                        }
                    }
                    val hiddenCount = prize.images.count { it.image == null }
                    if (hiddenCount > 0) {
                        Text(
                            "$hiddenCount 张成人内容图片已按内容设置隐藏",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Text(
                        prize.deliveryLine(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            if (lottery.winners.isNotEmpty()) {
                Text("中奖名单", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                lottery.prizes.forEach { prize ->
                    val winners = lottery.winners.filter { it.prizeId == prize.id }
                    if (winners.isEmpty()) return@forEach
                    Text(
                        "${prize.title} · ${winners.size}/${prize.slotCount} 名",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    winners.forEach { winner ->
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
                            if (winner.pointAwarded > 0) {
                                Text(
                                    " ${winner.pointAwarded} 萌萌点",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            Text(
                                " ${fulfillmentLabel(winner.fulfillment)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            if (prize.delivery == "offline" &&
                                viewer?.canManageFulfillment == true &&
                                onSetFulfillment != null
                            ) {
                                Box {
                                    TextButton(
                                        enabled = !actionBusy,
                                        onClick = { menuWinnerId = winner.id },
                                    ) { Text("履约") }
                                    DropdownMenu(
                                        expanded = menuWinnerId == winner.id,
                                        onDismissRequest = { menuWinnerId = null },
                                    ) {
                                        FULFILLMENT_OPTIONS.forEach { (value, label) ->
                                            DropdownMenuItem(
                                                text = { Text(label) },
                                                enabled = winner.fulfillment != value && !actionBusy,
                                                onClick = {
                                                    menuWinnerId = null
                                                    perform { onSetFulfillment(winner.id, value) }
                                                },
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val mine = viewer?.winnerId?.let { id -> lottery.winners.firstOrNull { it.id == id } }
            if (mine != null) {
                val prize = lottery.prizes.firstOrNull { it.id == mine.prizeId }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer,
                ) {
                    Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("您中奖了", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            "您获得了「${prize?.title.orEmpty()}」，当前状态：${fulfillmentLabel(mine.fulfillment)}。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        when {
                            prize?.delivery == "point" ->
                                Text(
                                    "${mine.pointAwarded} 萌萌点已自动发放到您的账户。",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            mine.fulfillment == "forfeited" ->
                                Text("该奖品已放弃。", style = MaterialTheme.typography.bodySmall)
                            prize?.delivery == "offline" &&
                                mine.fulfillment != "received" && onSetFulfillment != null -> {
                                Text(
                                    "实物类奖品由发起人联系您；本站不代收收货地址，请勿在公开楼层留下地址。",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        enabled = !actionBusy,
                                        onClick = { perform { onSetFulfillment(mine.id, "received") } },
                                    ) { Text("确认已收到") }
                                    TextButton(
                                        enabled = !actionBusy,
                                        onClick = { perform { onSetFulfillment(mine.id, "forfeited") } },
                                    ) { Text("放弃奖品") }
                                }
                            }
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

            if (viewer?.canDraw == true || viewer?.canEdit == true ||
                viewer?.canCancel == true || viewer?.canDelete == true
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    if (viewer?.canDraw == true && onDraw != null) {
                        TextButton(enabled = !actionBusy, onClick = onDraw) { Text("立即开奖") }
                    }
                    if (viewer?.canEdit == true && onEdit != null) {
                        TextButton(onClick = onEdit) { Text("编辑") }
                    }
                    if (viewer?.canCancel == true && onCancel != null) {
                        TextButton(enabled = !actionBusy, onClick = onCancel) { Text("取消抽奖") }
                    }
                    if (viewer?.canDelete == true && onDelete != null) {
                        TextButton(enabled = !actionBusy, onClick = onDelete) { Text("删除") }
                    }
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

private val FULFILLMENT_OPTIONS = listOf(
    "pending" to "待发放",
    "shipped" to "已发出",
    "received" to "已确认收到",
    "forfeited" to "已放弃",
)

@Composable
private fun PrizeThumb(
    image: LotteryPrizeImage,
    title: String,
    nsfwDisplay: NsfwDisplayMode,
) {
    val adult = image.isMarkedAdult || image.isGradedExplicit
    if (!adult || nsfwDisplay == NsfwDisplayMode.SHOW) {
        KungalImage(
            url = image.image?.url,
            contentDescription = title,
            modifier = Modifier.size(56.dp),
            shape = RoundedCornerShape(8.dp),
        )
        return
    }
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(RoundedCornerShape(8.dp)),
    ) {
        KungalImage(
            url = image.image?.url,
            contentDescription = title,
            modifier = Modifier.fillMaxSize().then(
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                    Modifier.blur(12.dp)
                } else {
                    Modifier
                },
            ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.72f)),
            contentAlignment = Alignment.Center,
        ) {
            Text("R18", style = MaterialTheme.typography.labelSmall)
        }
    }
}

private fun fulfillmentLabel(value: String): String =
    FULFILLMENT_OPTIONS.firstOrNull { it.first == value }?.second ?: value

private fun Lottery.entryModeLabel(): String = when (entryMode) {
    "reply" -> "回帖参与"
    "floor" -> "楼层抽奖"
    else -> "报名参与"
}

private fun Lottery.drawModeLabel(): String = when (drawMode) {
    "manual" -> "手动开奖"
    "threshold" -> "满员开奖"
    else -> "到点开奖"
}

private fun com.kungal.core.model.LotteryPrize.deliveryLine(): String = when (delivery) {
    "code" -> "$slotCount 份 · 系统托管兑换码"
    "point" -> "$slotCount 份 · " + when (pointMode) {
        "split" -> "奖池 ${pointAmount ?: 0} 点均分"
        "random" -> "奖池 ${pointAmount ?: 0} 点拼手气"
        else -> "每人 ${pointAmount ?: 0} 萌萌点"
    }
    else -> "$slotCount 份 · 楼主私聊发放"
}

private fun blockedReasonLabel(reason: String): String = when (reason) {
    "not_open" -> "抽奖已经结束"
    "past_closes_at" -> "抽奖已过截止时间"
    "no_signup" -> "楼层抽奖无需报名，直接回帖即可"
    "own_lottery" -> "不能参加自己发起的抽奖"
    "reply_required" -> "先回复本话题才能参与"
    "moemoepoint_below_minimum" -> "萌萌点未达到本抽奖的门槛"
    "account_too_new" -> "注册时间未达到本抽奖的门槛"
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
