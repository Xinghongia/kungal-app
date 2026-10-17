package com.kungal.app.ui.galgame

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.AspectScores
import com.kungal.core.model.RecordRatingBody
import com.kungal.core.model.WorkRecord
import com.kungal.core.model.WorkRecordPutBody
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val RatingPlayStateOptions = RatingPlayStateLabels.filter { it.first != "done" }

private val AspectKeys = listOf(
    "art" to "画面",
    "story" to "剧情",
    "music" to "音乐",
    "character" to "角色",
    "route" to "路线",
    "system" to "系统",
    "voice" to "配音",
    "replay_value" to "重玩价值",
)

private val GameTypeDescriptions = mapOf(
    "ba_saku" to "以色情为主要游戏内容",
    "plot" to "作品是否是剧情作由你来定义",
    "moe" to "画风萌萌的 Galgame 即为萌作",
    "daily" to "作品风格偏日常恋爱系",
)

/**
 * 评分 / 游玩状态 / 游玩时长统一表单（PUT /works/{id}/record）。
 * 与网页一致：记录整体替换；想玩状态不能评分；保存会删除未发送的评分/时长。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkRecordDialog(
    app: KungalApp,
    workId: String,
    workName: String,
    onDismiss: () -> Unit,
    onSaved: (WorkRecord) -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var loaded by remember { mutableStateOf<WorkRecord?>(null) }
    var reloadToken by remember { mutableStateOf(0) }

    var playState by remember { mutableStateOf("") }
    var hoursText by remember { mutableStateOf("") }
    var overall by remember { mutableStateOf(0) }
    var recommend by remember { mutableStateOf("neutral") }
    var spoiler by remember { mutableStateOf("none") }
    var gameTypes by remember { mutableStateOf(setOf<String>()) }
    var shortSummary by remember { mutableStateOf("") }
    var advanced by remember { mutableStateOf(false) }
    var aspects by remember { mutableStateOf(listOf(0, 0, 0, 0, 0, 0, 0, 0)) }
    var saving by remember { mutableStateOf(false) }
    var confirmLoss by remember { mutableStateOf<(() -> Unit)?>(null) }

    fun fill(record: WorkRecord) {
        loaded = record
        playState = record.playState.orEmpty()
        hoursText = if (record.minutes > 0) {
            val hours = record.minutes / 60.0
            if (hours == hours.roundToInt().toDouble()) {
                hours.roundToInt().toString()
            } else {
                String.format(java.util.Locale.US, "%.1f", hours)
            }
        } else {
            ""
        }
        val rating = record.ratingSummary
        overall = rating?.overall ?: 0
        recommend = rating?.recommend ?: "neutral"
        spoiler = rating?.spoilerLevel ?: "none"
        gameTypes = rating?.gameTypes?.toSet() ?: emptySet()
        shortSummary = rating?.shortSummary.orEmpty()
        val scores = rating?.aspectScores
        val values = listOf(
            scores?.art, scores?.story, scores?.music, scores?.character,
            scores?.route, scores?.system, scores?.voice, scores?.replayValue,
        ).map { it ?: 0 }
        aspects = values
        advanced = values.any { it > 0 }
    }

    LaunchedEffect(workId, reloadToken) {
        loading = true
        error = null
        runCatching { app.galgameApi.workRecord(workId) }
            .onSuccess { fill(it); loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }

    val wish = playState == "wish"
    val hadRating = loaded?.ratingSummary != null
    val minutes = ((hoursText.toDoubleOrNull() ?: 0.0) * 60).roundToInt()
    val tooShort = minutes in 1..9

    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 680.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text("评分与游玩记录", style = MaterialTheme.typography.titleMedium)
            Text(
                workName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
            )
            Text(
                "评分、游玩状态与时长在所有 NextMoe 站点共用，在这里写的，在其它站点也是同一份。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            when {
                loading -> LoadingBox()
                error != null -> ErrorBox(error!!, onRetry = { reloadToken++ })
                else -> {
                    if (!hadRating) {
                        Text(
                            "在本站发布评分可获得萌萌点：短评超过 20 字为 5，超过 100 字为 10。",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (wish) {
                        Text(
                            "想玩的作品还不能评分" + if (hadRating) "；保存后会删除你的评分" else "",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    Text("游玩状态", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RatingPlayStateOptions.forEach { (value, label) ->
                            FilterChip(
                                selected = playState == value,
                                onClick = { playState = if (playState == value) "" else value },
                                label = { Text(label) },
                            )
                        }
                    }

                    Text("总体评分（1-10）", style = MaterialTheme.typography.labelMedium)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Slider(
                            value = overall.toFloat(),
                            onValueChange = { overall = it.roundToInt() },
                            valueRange = 0f..10f,
                            steps = 9,
                            enabled = !wish,
                            modifier = Modifier.weight(1f),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (overall > 0) "$overall" else "未打分",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (overall > 0) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = false,
                                onClick = {
                                    overall = 0
                                    gameTypes = emptySet()
                                    shortSummary = ""
                                    spoiler = "none"
                                    recommend = "neutral"
                                    aspects = List(8) { 0 }
                                    advanced = false
                                },
                                label = { Text(if (hadRating) "删除评分" else "清除评分") },
                            )
                            if (advanced) {
                                TextButton(onClick = { advanced = false; aspects = List(8) { 0 } }) {
                                    Text("收起高级评分")
                                }
                            } else {
                                TextButton(onClick = {
                                    aspects = List(8) { if (overall > 0) overall else 0 }
                                    advanced = true
                                }) { Text("展开高级评分") }
                            }
                        }
                    }

                    Text("本游戏的类型（必选，可多选）", style = MaterialTheme.typography.labelMedium)
                    RatingGameTypeLabels.forEach { (value, label) ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            FilterChip(
                                selected = value in gameTypes,
                                onClick = {
                                    gameTypes = if (value in gameTypes) gameTypes - value else gameTypes + value
                                },
                                enabled = !wish,
                                label = { Text(label) },
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                GameTypeDescriptions[value].orEmpty(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Text("推荐程度", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        RatingRecommendLabels.forEach { (value, label) ->
                            FilterChip(
                                selected = recommend == value,
                                onClick = { recommend = value },
                                enabled = !wish,
                                label = { Text(label) },
                            )
                        }
                    }

                    Text("剧透程度", style = MaterialTheme.typography.labelMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        RatingSpoilerLabels.forEach { (value, label) ->
                            FilterChip(
                                selected = spoiler == value,
                                onClick = { spoiler = value },
                                enabled = !wish,
                                label = { Text(label) },
                            )
                        }
                    }

                    OutlinedTextField(
                        value = shortSummary,
                        onValueChange = { if (it.length <= 2000) shortSummary = it },
                        label = { Text("短评（可选，最多 2000 字）") },
                        minLines = 3,
                        enabled = !wish,
                        supportingText = { Text("${shortSummary.length}/2000") },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    if (advanced) {
                        Text("高级评分（0 表示不打分）", style = MaterialTheme.typography.labelMedium)
                        AspectKeys.forEachIndexed { index, (_, label) ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(64.dp),
                                )
                                Slider(
                                    value = aspects[index].toFloat(),
                                    onValueChange = { value ->
                                        aspects = aspects.toMutableList().also { it[index] = value.roundToInt() }
                                    },
                                    valueRange = 0f..10f,
                                    steps = 9,
                                    enabled = !wish,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    if (aspects[index] > 0) aspects[index].toString() else "—",
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(18.dp),
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = hoursText,
                        onValueChange = {
                            if (it.count { c -> c == '.' } <= 1 && it.all { c -> c.isDigit() || c == '.' }) {
                                hoursText = it.take(6)
                            }
                        },
                        label = { Text("游玩时长（小时，可选）") },
                        enabled = !wish,
                        supportingText = {
                            if (tooShort) {
                                Text(
                                    "不足 10 分钟的时长不会计入统计，请填 0 或更长",
                                    color = MaterialTheme.colorScheme.error,
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.End,
                    ) {
                        TextButton(onClick = onDismiss, enabled = !saving) { Text("取消") }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (saving || loading) return@Button
                                if (!wish && tooShort) {
                                    AppMessages.show("不足 10 分钟的时长不计入统计，请填 0 或更长")
                                    return@Button
                                }
                                if (!wish && overall > 0 && gameTypes.isEmpty()) {
                                    AppMessages.show("请选择至少一个游戏类型")
                                    return@Button
                                }
                                fun doSave() {
                                    saving = true
                                    scope.launch {
                                        val aspectScores = if (advanced) {
                                            AspectScores(
                                                art = aspects[0].takeIf { it > 0 },
                                                story = aspects[1].takeIf { it > 0 },
                                                music = aspects[2].takeIf { it > 0 },
                                                character = aspects[3].takeIf { it > 0 },
                                                route = aspects[4].takeIf { it > 0 },
                                                system = aspects[5].takeIf { it > 0 },
                                                voice = aspects[6].takeIf { it > 0 },
                                                replayValue = aspects[7].takeIf { it > 0 },
                                            )
                                        } else {
                                            AspectScores()
                                        }
                                        val body = WorkRecordPutBody(
                                            minutes = if (wish) null else minutes.takeIf { it > 0 },
                                            playState = playState.ifBlank { null },
                                            ratingInput = if (!wish && overall > 0) {
                                                RecordRatingBody(
                                                    recommend = recommend,
                                                    overall = overall,
                                                    gameTypes = gameTypes.toList(),
                                                    spoilerLevel = spoiler,
                                                    shortSummary = shortSummary,
                                                    aspectScores = aspectScores,
                                                )
                                            } else {
                                                null
                                            },
                                        )
                                        runCatching { app.galgameApi.putWorkRecord(workId, body) }
                                            .onSuccess { saved ->
                                                saving = false
                                                AppMessages.show("已保存")
                                                onSaved(saved)
                                                onDismiss()
                                            }
                                            .onFailure {
                                                saving = false
                                                AppMessages.show(friendlyError(it))
                                            }
                                    }
                                }
                                val losingRating = hadRating && (wish || overall == 0)
                                val losingTime = (loaded?.minutes ?: 0) > 0 && minutes == 0
                                when {
                                    losingRating -> confirmLoss = {
                                        confirmLoss = null
                                        doSave()
                                    }
                                    losingTime -> confirmLoss = {
                                        confirmLoss = null
                                        doSave()
                                    }
                                    else -> doSave()
                                }
                            },
                            enabled = !saving,
                        ) { Text(if (saving) "保存中…" else "保存") }
                    }
                }
            }
            Spacer(Modifier.width(8.dp))
        }
    }

    confirmLoss?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmLoss = null },
            title = { Text("确认保存") },
            text = {
                val losingRating = hadRating && (wish || overall == 0)
                val losingTime = (loaded?.minutes ?: 0) > 0 && minutes == 0
                Text(
                    when {
                        losingRating && losingTime ->
                            "保存后会删除你的评分（所有 NextMoe 站点同步），并撤回你在本站上报的游玩时长。"
                        losingRating ->
                            "保存后会删除你的评分（所有 NextMoe 站点同步）。游玩状态与时长保留。"
                        else ->
                            "保存后会撤回你在本站上报的游玩时长；其它应用上报的不受影响。"
                    },
                )
            },
            confirmButton = { TextButton(onClick = action) { Text("确认") } },
            dismissButton = { TextButton(onClick = { confirmLoss = null }) { Text("取消") } },
        )
    }
}
