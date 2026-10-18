package com.kungal.app.ui.quiz

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.PageResult
import com.kungal.app.ui.components.PagedListContent
import com.kungal.app.ui.components.PaginationBar
import com.kungal.app.ui.components.pageCount
import com.kungal.app.ui.content.documentPlainText
import com.kungal.app.ui.galgame.FilterOption
import com.kungal.app.ui.galgame.FilterOptionSheet
import com.kungal.app.ui.galgame.FilterPill
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.QuizSummary

private enum class QuizFilterSheet { CATEGORY, TYPE, DIFFICULTY, SORT }

/**
 * 题库列表（BP-05 U3，对应网页 /galgame-quiz）：
 * 全部题库 / 我的答题；分类、题型、难度、排序与升降序；50/页页码分页。
 */
@Composable
fun QuizListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenQuiz: (String) -> Unit,
    onCreate: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    var tab by rememberSaveable { mutableStateOf("all") }
    var category by rememberSaveable { mutableStateOf<String?>(null) }
    var type by rememberSaveable { mutableStateOf<String?>(null) }
    var difficulty by rememberSaveable { mutableStateOf<Int?>(null) }
    var sortField by rememberSaveable { mutableStateOf("bumped_at") }
    var descending by rememberSaveable { mutableStateOf(true) }
    var sheet by remember { mutableStateOf<QuizFilterSheet?>(null) }

    val loggedIn = app.isLoggedIn

    LaunchedEffect(loggedIn) {
        if (!loggedIn) tab = "all"
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 题库",
                subtitle = "单选、多选与判断，一起出题答题",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(
                        onClick = { if (loggedIn) onCreate() else onOpenLogin() },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(2.dp))
                        Text("出题")
                    }
                },
            )
        },
    ) { padding ->
        PagedListContent(
            key = listOf(
                tab, category, type, difficulty, sortField, descending,
                nsfwDisplay.allowsNsfw, loggedIn,
            ),
            load = { page, _ ->
                if (tab == "mine" && loggedIn) {
                    val result = app.quizApi.myAnswered(page = page, limit = QUIZ_PAGE_SIZE)
                    PageResult(
                        items = result.items,
                        ended = page * QUIZ_PAGE_SIZE >= result.total,
                        total = result.total,
                    )
                } else {
                    val result = app.quizApi.list(
                        page = page,
                        limit = QUIZ_PAGE_SIZE,
                        quizType = type,
                        quizCategory = category,
                        difficulty = difficulty,
                        includeNsfw = nsfwDisplay.allowsNsfw,
                        sort = quizSort(sortField, descending),
                    )
                    PageResult(
                        items = result.items,
                        ended = page * QUIZ_PAGE_SIZE >= result.total,
                        total = result.total,
                    )
                }
            },
            emptyText = if (tab == "mine") {
                "你还没有作答过任何题目"
            } else {
                "还没有人出题，快来出第一题吧"
            },
            modifier = Modifier.fillMaxSize().padding(padding),
            pagination = true,
            pageSize = QUIZ_PAGE_SIZE,
            header = {
                Column {
                    if (loggedIn) {
                        FilterChips(
                            options = listOf("all" to "全部题库", "mine" to "我的答题"),
                            selected = tab,
                            onSelect = { tab = it },
                        )
                    }
                    if (tab == "all") {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(horizontal = 12.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilterPill(
                                label = category?.let(::quizCategoryLabel) ?: "全部分类",
                                active = category != null,
                                onClick = { sheet = QuizFilterSheet.CATEGORY },
                            )
                            FilterPill(
                                label = type?.let(::quizTypeLabel) ?: "全部题型",
                                active = type != null,
                                onClick = { sheet = QuizFilterSheet.TYPE },
                            )
                            FilterPill(
                                label = difficulty?.let { "$it · ${quizDifficultyLabel(it)}" } ?: "全部难度",
                                active = difficulty != null,
                                onClick = { sheet = QuizFilterSheet.DIFFICULTY },
                            )
                            FilterPill(
                                label = QuizSortOptions.firstOrNull { it.first == sortField }?.second ?: "排序",
                                active = false,
                                icon = {
                                    Icon(
                                        Icons.AutoMirrored.Outlined.Sort,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                onClick = { sheet = QuizFilterSheet.SORT },
                            )
                            IconButton(onClick = { descending = !descending }) {
                                Icon(
                                    if (descending) Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                                    contentDescription = if (descending) "降序" else "升序",
                                )
                            }
                        }
                    }
                }
            },
            keyOf = { it.id },
        ) { quiz ->
            QuizRow(
                quiz = quiz,
                onClick = { onOpenQuiz(quiz.id) },
            )
        }
    }

    when (sheet) {
        QuizFilterSheet.CATEGORY -> FilterOptionSheet(
            title = "分类",
            options = QuizCategoryOptions.map { FilterOption(it.first, it.second) },
            selected = setOfNotNull(category),
            multi = false,
            onToggle = { category = if (category == it) null else it },
            onClear = { category = null },
            onDismiss = { sheet = null },
        )
        QuizFilterSheet.TYPE -> FilterOptionSheet(
            title = "题型",
            options = QuizTypeOptions.map { FilterOption(it.first, it.second) },
            selected = setOfNotNull(type),
            multi = false,
            onToggle = { type = if (type == it) null else it },
            onClear = { type = null },
            onDismiss = { sheet = null },
        )
        QuizFilterSheet.DIFFICULTY -> FilterOptionSheet(
            title = "难度",
            options = QuizDifficultyOptions.map { FilterOption(it.first, it.second) },
            selected = setOfNotNull(difficulty?.toString()),
            multi = false,
            onToggle = { value ->
                val next = value.toIntOrNull()
                difficulty = if (difficulty == next) null else next
            },
            onClear = { difficulty = null },
            onDismiss = { sheet = null },
        )
        QuizFilterSheet.SORT -> FilterOptionSheet(
            title = "排序",
            options = QuizSortOptions.map { FilterOption(it.first, it.second) },
            selected = setOf(sortField),
            multi = false,
            includeAll = false,
            onToggle = { sortField = it },
            onClear = null,
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

const val QUIZ_PAGE_SIZE = 50

/** 题库条目（列表与作品页共用）。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QuizRow(quiz: QuizSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.width(24.dp), contentAlignment = Alignment.Center) {
            val viewer = quiz.viewer
            when {
                viewer?.isCorrect == true -> Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "答对了",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp),
                )
                viewer?.isCorrect == false -> Icon(
                    Icons.Outlined.Cancel,
                    contentDescription = "答错了",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp),
                )
                viewer?.hasAnswered == true -> Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = "已作答",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
                else -> Icon(
                    Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = "未作答",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                quizPromptText(quiz),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                AvatarImage(
                    url = quiz.author.avatar?.url,
                    seed = quiz.author.name,
                    modifier = Modifier.size(14.dp),
                )
                Text(
                    quiz.author.name.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    relativeTime(quiz.bumpedAt.ifBlank { quiz.createdAt }),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Chip(quizTypeLabel(quiz.quizType))
                Chip(quizCategoryLabel(quiz.quizCategory))
                if (quiz.spoilerLevel != "none") Chip(quizSpoilerLabel(quiz.spoilerLevel))
                Chip("${quiz.viewCount} 浏览")
                Chip("${quiz.answerCount} 作答")
                if (quiz.favoriteCount > 0) Chip("${quiz.favoriteCount} 收藏")
                if (quiz.commentCount > 0) Chip("${quiz.commentCount} 评论")
            }
        }
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Chip("${quizDifficultyLabel(quiz.difficulty)} ${quiz.difficulty}")
            val rate = quizCorrectRate(quiz)
            if (rate != null) {
                Text(
                    "正确率 $rate%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun quizPromptText(quiz: QuizSummary): String =
    documentPlainText(quiz.prompt).ifBlank { "题目 #${quiz.id}" }

/**
 * 作品词条页的「题库」标签：本作相关题目 12/页 + 出题入口。
 * 放在父级 LazyColumn 的 item 里，自身不是滚动容器。
 */
@Composable
fun WorkQuizSection(
    app: KungalApp,
    workId: String,
    nsfwDisplay: NsfwDisplayMode,
    onOpenQuiz: (String) -> Unit,
    onCreate: () -> Unit,
) {
    var page by remember(workId) { mutableStateOf(1) }
    var items by remember(workId) { mutableStateOf<List<QuizSummary>>(emptyList()) }
    var total by remember(workId) { mutableStateOf(0) }
    var loading by remember(workId) { mutableStateOf(true) }
    var error by remember(workId) { mutableStateOf<String?>(null) }
    var reload by remember(workId) { mutableStateOf(0) }

    LaunchedEffect(workId, page, nsfwDisplay.allowsNsfw, reload) {
        loading = true
        error = null
        runCatching {
            app.quizApi.list(
                page = page,
                limit = WORK_PAGE_SIZE,
                workId = workId,
                includeNsfw = nsfwDisplay.allowsNsfw,
            )
        }.onSuccess { result ->
            items = result.items
            total = result.total
            loading = false
        }.onFailure {
            error = friendlyError(it)
            loading = false
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    "本作题库",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    "与本作相关的题目，也欢迎为本作出题",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onCreate) {
                Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(2.dp))
                Text("出题")
            }
        }
        when {
            loading && items.isEmpty() -> LoadingBox()
            error != null && items.isEmpty() -> ErrorBox(error!!, onRetry = { reload += 1 })
            items.isEmpty() -> EmptyBox("本作还没有题目，快来出第一题吧")
            else -> {
                items.forEach { quiz ->
                    QuizRow(quiz = quiz, onClick = { onOpenQuiz(quiz.id) })
                }
                PaginationBar(
                    page = page,
                    totalPages = pageCount(total, WORK_PAGE_SIZE),
                    onPageChange = { if (!loading) page = it },
                    enabled = !loading,
                )
            }
        }
    }
}

private const val WORK_PAGE_SIZE = 12
