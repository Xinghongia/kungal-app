package com.kungal.app.ui.activity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.PageResult
import com.kungal.app.ui.components.PagedListContent
import com.kungal.app.ui.galgame.FilterOptionSheet
import com.kungal.app.ui.galgame.FilterPill
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.FeedActivity
import kotlinx.coroutines.launch

private const val ACTIVITY_PAGE_SIZE = 50

/**
 * 动态时间线（BP-05 U8，对应网页 /activity）：
 * 全站动态按时间倒序游标加载，卡片按 activity_type 展示详情，点按进原生详情页。
 */
@Composable
fun ActivityTimelineScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenPath: (String) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    var quizStates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    fun toggleQuizFavorite(quizId: String) {
        val current = quizStates[quizId] ?: false
        val next = !current
        quizStates = quizStates + (quizId to next)
        scope.launch {
            runCatching { app.quizApi.setFavorite(quizId, next) }
                .onSuccess { result ->
                    quizStates = quizStates + (quizId to (result.viewer?.hasFavorited ?: next))
                }
                .onFailure { e ->
                    quizStates = quizStates + (quizId to current)
                    AppMessages.show(friendlyError(e))
                }
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "动态时间线",
                subtitle = "全站话题、回复、Galgame 与资源的最新动态",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        PagedListContent(
            key = nsfwDisplay.allowsNsfw,
            load = { _, cursor ->
                val page = app.activityApi.activities(
                    cursor = cursor,
                    limit = ACTIVITY_PAGE_SIZE,
                    includeNsfw = nsfwDisplay.allowsNsfw,
                )
                quizStates = loadQuizStates(app, page.items, quizStates)
                PageResult(
                    items = page.items,
                    nextCursor = page.nextCursor,
                    ended = page.nextCursor == null || page.items.isEmpty(),
                )
            },
            emptyText = "暂无动态",
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 6.dp),
            keyOf = { it.id },
        ) { activity ->
            ActivityCard(
                activity = activity,
                blurCover = nsfwDisplay.blursMedia,
                onOpenPath = onOpenPath,
                onOpenUser = onOpenUser,
                quizFavorited = activity.quiz?.quizId?.let { quizStates[it] },
                onToggleQuizFavorite = if (app.isLoggedIn) ::toggleQuizFavorite else null,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

/** 按当前页批量读取题库收藏态，避免每张卡片各发一次请求。 */
private suspend fun loadQuizStates(
    app: KungalApp,
    items: List<FeedActivity>,
    current: Map<String, Boolean>,
): Map<String, Boolean> {
    if (!app.isLoggedIn) return current
    val ids = items.mapNotNull { it.quiz?.quizId }.distinct().take(100)
    if (ids.isEmpty()) return current
    val result = runCatching { app.quizApi.states(ids) }.getOrNull() ?: return current
    return current + result.items.associate { it.quizId to it.hasFavorited }
}

/**
 * 分类动态（对应网页 /activity/category）：一次只拉一种 activity_type，
 * 默认「新话题」，页面内可换成任意类型。
 */
@Composable
fun ActivityCategoryScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenPath: (String) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    var selectedType by rememberSaveable { mutableStateOf("topic_creation") }
    var sheet by remember { mutableStateOf(false) }
    var quizStates by remember { mutableStateOf<Map<String, Boolean>>(emptyMap()) }
    val scope = rememberCoroutineScope()

    fun toggleQuizFavorite(quizId: String) {
        val current = quizStates[quizId] ?: false
        val next = !current
        quizStates = quizStates + (quizId to next)
        scope.launch {
            runCatching { app.quizApi.setFavorite(quizId, next) }
                .onSuccess { result ->
                    quizStates = quizStates + (quizId to (result.viewer?.hasFavorited ?: next))
                }
                .onFailure { e ->
                    quizStates = quizStates + (quizId to current)
                    AppMessages.show(friendlyError(e))
                }
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "分类动态",
                subtitle = "按类型查看站内动态",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        PagedListContent(
            key = listOf(selectedType, nsfwDisplay.allowsNsfw),
            load = { _, cursor ->
                val page = app.activityApi.activities(
                    cursor = cursor,
                    limit = ACTIVITY_PAGE_SIZE,
                    activityTypes = listOf(selectedType),
                    includeNsfw = nsfwDisplay.allowsNsfw,
                )
                quizStates = loadQuizStates(app, page.items, quizStates)
                PageResult(
                    items = page.items,
                    nextCursor = page.nextCursor,
                    ended = page.nextCursor == null || page.items.isEmpty(),
                )
            },
            emptyText = "${activityTypeLabel(selectedType)}暂无动态",
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 6.dp),
            keyOf = { it.id },
            header = {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    FilterPill(
                        label = activityTypeLabel(selectedType),
                        active = true,
                        onClick = { sheet = true },
                    )
                    Text(
                        "只看这一种动态",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        ) { activity ->
            ActivityCard(
                activity = activity,
                blurCover = nsfwDisplay.blursMedia,
                onOpenPath = onOpenPath,
                onOpenUser = onOpenUser,
                quizFavorited = activity.quiz?.quizId?.let { quizStates[it] },
                onToggleQuizFavorite = if (app.isLoggedIn) ::toggleQuizFavorite else null,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }

    if (sheet) {
        FilterOptionSheet(
            title = "动态类型",
            options = ActivityTypeOptions,
            selected = setOf(selectedType),
            multi = false,
            includeAll = false,
            onToggle = { selectedType = it },
            onClear = null,
            onDismiss = { sheet = false },
        )
    }
}
