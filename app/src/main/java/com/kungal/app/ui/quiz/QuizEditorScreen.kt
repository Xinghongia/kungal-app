package com.kungal.app.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.galgame.WorkCoverImage
import com.kungal.core.model.WorkSummary

/**
 * 出题 / 编辑题目（BP-05 U4）。题面、描述与解析都用 Markdown 源文；
 * 题干支持 ||剧透|| 标记（由正文渲染器打码，点击显示）。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuizEditorScreen(
    app: KungalApp,
    quizId: String?,
    initialWorkId: String?,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
) {
    val vm: QuizEditorViewModel = viewModel(
        key = "quiz-editor-${quizId ?: initialWorkId ?: "new"}",
    ) { QuizEditorViewModel(app, quizId, initialWorkId) }
    val state by vm.state.collectAsState()
    var confirmCancel by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = if (quizId == null) "出题" else "编辑题目",
                subtitle = "发布后可获得萌萌点奖励",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { vm.submit(onSaved) },
                        enabled = !state.submitting && !state.loading,
                    ) {
                        Text(if (state.submitting) "提交中…" else "保存")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.padding(padding))
            state.error != null -> ErrorBox(
                state.error!!,
                onRetry = onBack,
                modifier = Modifier.padding(padding),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                SectionLabel("题型")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuizTypeOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = state.quizType == value,
                            enabled = !state.editing,
                            onClick = { vm.setType(value) },
                            label = { Text(label) },
                        )
                    }
                }
                Text(
                    QuizTypeDescriptions[state.quizType].orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                SectionLabel("题目")
                OutlinedTextField(
                    value = state.prompt,
                    onValueChange = vm::setPrompt,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    placeholder = { Text("例如：《永不枯萎的世界与终结之花》中……") },
                    supportingText = {
                        Text(
                            "${state.prompt.length}/200 · 可用 ||剧透内容|| 标记剧透",
                            style = MaterialTheme.typography.labelSmall,
                        )
                    },
                )

                SectionLabel("题目描述（可选）")
                Text(
                    "支持 Markdown，可上传图片作为线索（例如根据 CG 猜游戏）",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.description,
                    onValueChange = vm::setDescription,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    placeholder = { Text("补充题目背景、线索图片等（可选）") },
                )

                QuizChoicesEditor(state = state, vm = vm)

                SectionLabel("分类")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuizCategoryOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = state.category == value,
                            onClick = { vm.setCategory(value) },
                            label = { Text(label) },
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("难度", Modifier.weight(1f))
                    Chip("${quizDifficultyLabel(state.difficulty)} · ${state.difficulty}")
                }
                Slider(
                    value = state.difficulty.toFloat(),
                    onValueChange = { vm.setDifficulty(it.toInt()) },
                    valueRange = 1f..10f,
                    steps = 8,
                )

                SectionLabel("剧透等级")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuizSpoilerOptions.forEach { (value, label) ->
                        FilterChip(
                            selected = state.spoilerLevel == value,
                            onClick = { vm.setSpoiler(value) },
                            label = { Text(label) },
                        )
                    }
                }

                if (state.showExplanation) {
                    SectionLabel("解析（可选，作答后展示）")
                    OutlinedTextField(
                        value = state.explanation,
                        onValueChange = vm::setExplanation,
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 2,
                        placeholder = { Text("可以补充答案的解析、出处或冷知识") },
                        supportingText = {
                            Text("${state.explanation.length}/2000", style = MaterialTheme.typography.labelSmall)
                        },
                    )
                } else {
                    TextButton(onClick = { vm.showExplanation() }) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("添加解析（可选）")
                    }
                }

                QuizWorkPicker(state = state, vm = vm)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f)) {
                        Text("隐藏关联作品", style = MaterialTheme.typography.bodyMedium)
                        Text(
                            "开启后，作答前不显示关联的作品",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.isWorkHidden,
                        onCheckedChange = vm::setWorkHidden,
                    )
                }

                Button(
                    onClick = { vm.submit(onSaved) },
                    enabled = !state.submitting,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        when {
                            state.submitting -> "提交中…"
                            state.editing -> "保存修改"
                            else -> "发布题目"
                        },
                    )
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        modifier = modifier,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun QuizChoicesEditor(
    state: QuizEditorState,
    vm: QuizEditorViewModel,
) {
    SectionLabel(if (state.quizType == "judge") "正确答案" else "选项")
    when (state.quizType) {
        "judge" -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            JudgeChoice(
                label = "正确",
                selected = state.judgeAnswer,
                modifier = Modifier.weight(1f),
                onClick = { vm.setJudgeAnswer(true) },
            )
            JudgeChoice(
                label = "错误",
                selected = !state.judgeAnswer,
                modifier = Modifier.weight(1f),
                onClick = { vm.setJudgeAnswer(false) },
            )
        }
        else -> {
            Text(
                "点左侧圆圈标记答案（${if (state.quizType == "single") "单选唯一" else "多选可多个"}，2–20 个）",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.choices.forEachIndexed { index, value ->
                val correct = index in state.correctIndexes
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = if (correct) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                    } else {
                        MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = if (correct) {
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        IconButton(onClick = { vm.toggleCorrect(index) }) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = if (correct) "取消正确答案" else "设为正确答案",
                                tint = if (correct) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                },
                            )
                        }
                        OutlinedTextField(
                            value = value,
                            onValueChange = { vm.setChoice(index, it) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("选项 ${index + 1}") },
                        )
                        IconButton(
                            onClick = { vm.removeChoice(index) },
                            enabled = state.choices.size > 2,
                        ) {
                            Icon(Icons.Outlined.Delete, contentDescription = "删除选项")
                        }
                    }
                }
            }
            TextButton(
                onClick = { vm.addChoice() },
                enabled = state.choices.size < 20,
            ) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("添加选项")
            }
        }
    }
}

@Composable
private fun JudgeChoice(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            if (selected) 1.5.dp else 1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
        ),
        modifier = modifier,
    ) {
        Box(Modifier.height(60.dp), contentAlignment = Alignment.Center) {
            Text(
                label,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun QuizWorkPicker(state: QuizEditorState, vm: QuizEditorViewModel) {
    SectionLabel("关联 Galgame（可选，最多 20 部）")
    state.works.forEach { work ->
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            WorkCoverImage(
                url = work.cover?.url,
                isNsfw = work.isNsfw,
                blurNsfw = true,
                contentDescription = work.displayName,
                modifier = Modifier.width(36.dp).height(50.dp).clip(RoundedCornerShape(5.dp)),
            )
            Column(Modifier.weight(1f)) {
                Text(
                    work.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (work.isNsfw) Chip("NSFW")
            }
            IconButton(onClick = { vm.removeWork(work.id) }) {
                Icon(Icons.Outlined.Close, contentDescription = "移除")
            }
        }
    }
    if (state.works.size < 20) {
        OutlinedTextField(
            value = state.workQuery,
            onValueChange = vm::setWorkQuery,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("输入游戏名搜索并关联") },
            trailingIcon = {
                if (state.searchingWorks) {
                    Text(
                        "搜索中…",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            },
        )
        state.workResults.forEach { work ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { vm.addWork(work) }
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                WorkCoverImage(
                    url = work.cover?.url,
                    isNsfw = work.isNsfw,
                    blurNsfw = true,
                    contentDescription = work.displayName,
                    modifier = Modifier.width(32.dp).height(44.dp).clip(RoundedCornerShape(4.dp)),
                )
                Text(
                    work.displayName,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (work.isNsfw) Chip("NSFW")
            }
        }
    }
}
