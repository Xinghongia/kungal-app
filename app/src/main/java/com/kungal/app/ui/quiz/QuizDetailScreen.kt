package com.kungal.app.ui.quiz

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.content.documentPlainText
import com.kungal.app.ui.galgame.WallCommentSection
import com.kungal.app.ui.galgame.WorkCoverImage
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.QuizAnswer
import com.kungal.core.model.QuizDetail
import com.kungal.core.model.QuizSolution
import com.kungal.core.model.WorkSummary
import com.kungal.core.net.KungalApiException

/**
 * 题目详情与作答（BP-05 U3，对应网页 /galgame-quiz/{id}）：
 * 单选/多选/判断作答、结果与解析、收藏、质量评分、作答记录、作品信息与评论墙；
 * 作者/有权限者可进入编辑与删除。
 */
@Composable
fun QuizDetailScreen(
    app: KungalApp,
    quizId: String,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
    onEdit: (String) -> Unit,
) {
    val vm: QuizDetailViewModel = viewModel(key = "quiz-$quizId") {
        QuizDetailViewModel(app, quizId)
    }
    val state by vm.state.collectAsState()
    var menuOpen by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val quiz = state.quiz

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 题目",
                subtitle = quiz?.let { quizTypeLabel(it.quizType) } ?: "题库",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (quiz != null && (quiz.viewer?.canEdit == true || quiz.viewer?.canDelete == true)) {
                        Box {
                            IconButton(onClick = { menuOpen = true }) {
                                Icon(Icons.Outlined.MoreVert, contentDescription = "更多")
                            }
                            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                                if (quiz.viewer?.canEdit == true) {
                                    DropdownMenuItem(
                                        text = { Text("编辑") },
                                        onClick = {
                                            menuOpen = false
                                            onEdit(quizId)
                                        },
                                    )
                                }
                                if (quiz.viewer?.canDelete == true) {
                                    DropdownMenuItem(
                                        text = { Text("删除", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            menuOpen = false
                                            confirmDelete = true
                                        },
                                    )
                                }
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            quiz == null && state.loading -> LoadingBox(Modifier.padding(padding))
            quiz == null && state.error != null -> ErrorBox(
                state.error!!,
                onRetry = vm::load,
                modifier = Modifier.padding(padding),
            )
            quiz == null -> EmptyBox("题目不存在或已被删除", Modifier.padding(padding))
            else -> QuizBody(
                app = app,
                quiz = quiz,
                state = state,
                onOpenWork = onOpenWork,
                onOpenUser = onOpenUser,
                onOpenLogin = onOpenLogin,
                vm = vm,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("确认删除") },
            text = { Text("删除后本题及所有作答记录将被移除，出题获得的萌萌点会被扣除。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmDelete = false
                        vm.delete(onBack)
                    },
                ) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun QuizBody(
    app: KungalApp,
    quiz: QuizDetail,
    state: QuizDetailState,
    vm: QuizDetailViewModel,
    onOpenWork: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val hasAnswered = quiz.viewer?.hasAnswered == true
    val solution = quiz.solution
    val showInput = !hasAnswered && solution == null
    val showResult = solution != null
    var detailExpanded by remember(quiz.id) { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "chips") {
            QuizChips(quiz = quiz, onOpenWork = onOpenWork)
        }
        item(key = "prompt") {
            ContentDocumentView(document = quiz.prompt)
        }
        if (quiz.content?.children?.isNotEmpty() == true) {
            item(key = "description") {
                ContentDocumentView(document = quiz.content)
            }
        }
        item(key = "meta") {
            QuizMetaRow(
                quiz = quiz,
                favoritePending = state.favoritePending,
                onOpenUser = onOpenUser,
                onToggleFavorite = {
                    if (app.isLoggedIn) vm.toggleFavorite() else onOpenLogin()
                },
            )
        }
        item(key = "divider-1") { HorizontalDivider() }

        if (showInput) {
            item(key = "answer-input") {
                QuizAnswerInputSection(
                    quiz = quiz,
                    input = state.input,
                    submitting = state.submitting,
                    onSingle = vm::selectSingle,
                    onToggleMultiple = vm::toggleMultiple,
                    onJudge = vm::selectJudge,
                    onSubmit = {
                        if (app.isLoggedIn) vm.submit() else onOpenLogin()
                    },
                )
            }
        } else if (showResult) {
            item(key = "answer-result") {
                QuizResultSection(
                    quiz = quiz,
                    solution = solution,
                    ratingPending = state.ratingPending,
                    onSubmitRating = { rating ->
                        if (app.isLoggedIn) vm.rateQuality(rating) else onOpenLogin()
                    },
                )
            }
        }

        item(key = "divider-2") { HorizontalDivider() }
        item(key = "detail") {
            QuizDetailPanel(
                quiz = quiz,
                state = state,
                expanded = detailExpanded,
                onToggle = { detailExpanded = !detailExpanded },
                onOpenWork = onOpenWork,
                onOpenUser = onOpenUser,
                onLoadMore = vm::loadMoreRecords,
            )
        }

        item(key = "divider-3") { HorizontalDivider() }
        item(key = "comments") {
            val commentLocked = !hasAnswered && quiz.viewer?.canEdit != true &&
                quiz.viewer?.canDelete != true &&
                (quiz.isWorkHidden || quiz.spoilerLevel != "none")
            if (commentLocked) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(Icons.Outlined.Lock, contentDescription = null)
                        Text("作答后即可查看并参与讨论", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            } else {
                WallCommentSection(
                    app = app,
                    subjectType = "galgame_quiz",
                    subjectId = quiz.id,
                    maxLength = 5000,
                    title = "题目讨论",
                    onOpenUser = onOpenUser,
                    onOpenLogin = onOpenLogin,
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuizChips(quiz: QuizDetail, onOpenWork: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Chip(quizTypeLabel(quiz.quizType))
            Chip("${quizDifficultyLabel(quiz.difficulty)} · ${quiz.difficulty}")
            Chip(quizCategoryLabel(quiz.quizCategory))
            if (quiz.spoilerLevel != "none") Chip(quizSpoilerLabel(quiz.spoilerLevel))
        }
        if (quiz.works.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                quiz.works.forEach { work ->
                    Text(
                        work.displayName,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenWork(work.id) }
                            .padding(vertical = 2.dp, horizontal = 2.dp),
                    )
                }
            }
        } else if (quiz.isWorkHidden) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Icon(
                    Icons.Outlined.Lock,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "关联作品作答后揭晓",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun QuizMetaRow(
    quiz: QuizDetail,
    favoritePending: Boolean,
    onOpenUser: (String) -> Unit,
    onToggleFavorite: () -> Unit,
) {
    val rate = quizCorrectRate(quiz.answerCount, quiz.correctCount)
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            AvatarImage(
                url = quiz.author.avatar?.url,
                seed = quiz.author.name,
                modifier = Modifier.size(22.dp),
            )
            Text(
                quiz.author.name.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onOpenUser(quiz.author.id) },
            )
            QuizFavoriteButton(
                favorited = quiz.viewer?.hasFavorited == true,
                count = quiz.favoriteCount,
                pending = favoritePending,
                onClick = onToggleFavorite,
            )
        }
        Text(
            buildString {
                append(relativeTime(quiz.createdAt))
                append(" · ${quiz.viewCount} 浏览 · ${quiz.answerCount} 人作答")
                if (rate != null) append(" · 正确率 $rate%")
                if (quiz.qualityCount > 0 && quiz.qualityAverage != null) {
                    append(" · 质量 ${quiz.qualityAverage}（${quiz.qualityCount} 人）")
                }
            },
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun QuizFavoriteButton(
    favorited: Boolean,
    count: Int,
    pending: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .clickable(enabled = !pending, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Icon(
            if (favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
            contentDescription = if (favorited) "取消收藏" else "收藏",
            modifier = Modifier.size(16.dp),
            tint = if (favorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = if (favorited) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (favorited) Chip("已收藏")
    }
}

@Composable
private fun QuizAnswerInputSection(
    quiz: QuizDetail,
    input: QuizAnswerInput,
    submitting: Boolean,
    onSingle: (Int) -> Unit,
    onToggleMultiple: (Int) -> Unit,
    onJudge: (Boolean) -> Unit,
    onSubmit: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (quiz.quizType) {
            "single", "multiple" -> quiz.choices.forEachIndexed { index, text ->
                val selected = if (quiz.quizType == "single") {
                    input.singleChoice == index
                } else {
                    index in input.multipleChoices
                }
                AnswerOptionRow(
                    letter = ('A' + index).toString(),
                    text = text,
                    selected = selected,
                    multi = quiz.quizType == "multiple",
                    onClick = {
                        if (quiz.quizType == "single") onSingle(index) else onToggleMultiple(index)
                    },
                )
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                JudgeButton(
                    label = "正确",
                    selected = input.judgeAnswer == true,
                    modifier = Modifier.weight(1f),
                    onClick = { onJudge(true) },
                )
                JudgeButton(
                    label = "错误",
                    selected = input.judgeAnswer == false,
                    modifier = Modifier.weight(1f),
                    onClick = { onJudge(false) },
                )
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            Button(onClick = onSubmit, enabled = !submitting) {
                Text(if (submitting) "提交中…" else "提交答案")
            }
        }
    }
}

@Composable
private fun AnswerOptionRow(
    letter: String,
    text: String,
    selected: Boolean,
    multi: Boolean,
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
        border = if (selected) {
            BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        } else {
            BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        },
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Icon(
                if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "$letter.",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(20.dp),
            )
            Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            if (multi) {
                Text(
                    if (selected) "已选" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun JudgeButton(
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
        Box(Modifier.height(64.dp), contentAlignment = Alignment.Center) {
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
private fun QuizResultSection(
    quiz: QuizDetail,
    solution: QuizSolution,
    ratingPending: Boolean,
    onSubmitRating: (Int) -> Unit,
) {
    val answer = quiz.viewer?.answer
    val correct = answer?.isCorrect
    val chosen = answer?.submission?.choiceIndexes.orEmpty()
    val correctIndexes = solution.correctChoiceIndexes.toSet()

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = when (correct) {
                true -> MaterialTheme.colorScheme.primaryContainer
                false -> MaterialTheme.colorScheme.errorContainer
                null -> MaterialTheme.colorScheme.surfaceContainerHigh
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(
                Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Icon(
                    if (correct == true) Icons.Outlined.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = null,
                )
                Text(
                    when (correct) {
                        true -> "回答正确"
                        false -> "回答错误"
                        else -> "本题答案"
                    },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }

        if (quiz.quizType == "judge") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Chip("正确答案: ${if (solution.isStatementTrue == true) "正确" else "错误"}")
                val mine = answer?.submission?.isStatementTrue
                if (mine != null) {
                    Chip("你的回答: ${if (mine) "正确" else "错误"}")
                }
            }
        } else {
            quiz.choices.forEachIndexed { index, text ->
                val isCorrect = index in correctIndexes
                val isChosen = index in chosen
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = when {
                        isCorrect -> MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        isChosen -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
                        else -> MaterialTheme.colorScheme.surfaceContainerLow
                    },
                    border = BorderStroke(
                        1.dp,
                        when {
                            isCorrect -> MaterialTheme.colorScheme.primary
                            isChosen -> MaterialTheme.colorScheme.error
                            else -> MaterialTheme.colorScheme.outlineVariant
                        },
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "${('A' + index)}.",
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (isChosen) Chip("你的选择")
                        if (isCorrect) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = "正确答案",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }

        if (solution.explanation?.children?.isNotEmpty() == true) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "解析",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    ContentDocumentView(document = solution.explanation)
                }
            }
        }

        if (quiz.viewer?.hasAnswered == true) {
            var rating by remember(quiz.id, quiz.viewer?.qualityRating) {
                mutableStateOf(quiz.viewer?.qualityRating ?: 0)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "给这道题的质量打分（1-10）",
                        style = MaterialTheme.typography.labelMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (quiz.qualityCount > 0 && quiz.qualityAverage != null) {
                        Text(
                            "平均 ${quiz.qualityAverage} · ${quiz.qualityCount} 人",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    (1..10).forEach { value ->
                        Icon(
                            if (value <= rating) Icons.Filled.Star else Icons.Outlined.StarBorder,
                            contentDescription = "$value 分",
                            modifier = Modifier
                                .size(26.dp)
                                .clickable { rating = value },
                            tint = if (value <= rating) {
                                MaterialTheme.colorScheme.tertiary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            },
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        "$rating 分",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    Button(
                        onClick = { onSubmitRating(rating) },
                        enabled = rating >= 1 && !ratingPending,
                    ) {
                        Text(
                            when {
                                ratingPending -> "提交中"
                                quiz.viewer?.qualityRating != null -> "更新评分"
                                else -> "提交评分"
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuizDetailPanel(
    quiz: QuizDetail,
    state: QuizDetailState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onLoadMore: () -> Unit,
) {
    val wrong = quiz.answerCount - quiz.correctCount
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!expanded) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onToggle) { Text("查看详情") }
            }
            return
        }
        if (quiz.works.isNotEmpty()) {
            Text(
                "关联作品",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            quiz.works.forEach { work ->
                WorkRow(work = work, onClick = { onOpenWork(work.id) })
            }
        }
        Text(
            "作答统计",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (quiz.answerCount > 0) {
            Text(
                "正确 ${quiz.correctCount} · 错误 $wrong · 共 ${quiz.answerCount} 人作答",
                style = MaterialTheme.typography.bodySmall,
            )
        } else {
            Text(
                "暂无作答",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(
            "作答记录",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            state.recordsLoading && state.records.isEmpty() -> LoadingBox()
            state.recordsError != null && state.records.isEmpty() -> ErrorBox(
                state.recordsError,
                onRetry = onLoadMore,
            )
            state.records.isEmpty() -> Text(
                "还没有人作答",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> {
                state.records.forEach { record ->
                    QuizAnswerRecord(record = record, quizType = quiz.quizType, onOpenUser = onOpenUser)
                }
                if (state.recordsCursor != null) {
                    TextButton(
                        onClick = onLoadMore,
                        enabled = !state.recordsLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (state.recordsLoading) "加载中…" else "加载更多")
                    }
                }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = onToggle) { Text("收起") }
        }
    }
}

@Composable
private fun WorkRow(work: WorkSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        WorkCoverImage(
            url = work.cover?.url,
            isNsfw = work.isNsfw,
            blurNsfw = true,
            contentDescription = work.displayName,
            modifier = Modifier.width(44.dp).height(62.dp).clip(RoundedCornerShape(6.dp)),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                work.displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (work.isNsfw) Chip("NSFW")
        }
    }
}

@Composable
private fun QuizAnswerRecord(
    record: QuizAnswer,
    quizType: String,
    onOpenUser: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AvatarImage(
            url = record.answerer.avatar?.url,
            seed = record.answerer.name,
            modifier = Modifier.size(22.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(
                record.answerer.name.orEmpty(),
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.clickable { onOpenUser(record.answerer.id) },
            )
            val submission = formatQuizSubmission(quizType, record.submission)
            if (submission.isNotBlank()) {
                Text(
                    submission,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Text(
            relativeTime(record.answeredAt),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (record.isCorrect) {
            true -> Chip("正确")
            false -> Chip("错误")
            null -> Unit
        }
    }
}
