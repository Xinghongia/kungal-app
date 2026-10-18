package com.kungal.app.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.PageResult
import com.kungal.app.ui.components.PagedListContent
import com.kungal.app.ui.galgame.FilterOption
import com.kungal.app.ui.galgame.FilterOptionSheet
import com.kungal.app.ui.galgame.FilterPill
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.Todo
import com.kungal.core.model.UpdateLog

private const val UPDATE_LOG_PAGE_SIZE = 50
private const val TODO_FETCH_PAGE_SIZE = 100
private const val TODO_MAX_PAGES = 20

/** 更新历史（BP-05 U8，对应网页 /update/history）：游标分页，只读。 */
@Composable
fun UpdateHistoryScreen(app: KungalApp, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "更新日志",
                subtitle = "站点的新特性、修复与优化记录",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        PagedListContent(
            key = Unit,
            load = { _, cursor ->
                val page = app.updateLogApi.list(cursor = cursor, limit = UPDATE_LOG_PAGE_SIZE)
                PageResult(
                    items = page.items,
                    nextCursor = page.nextCursor,
                    ended = page.nextCursor == null || page.items.isEmpty(),
                )
            },
            emptyText = "暂无更新日志",
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(vertical = 6.dp),
            keyOf = { it.id },
        ) { log ->
            UpdateLogCard(log, Modifier.padding(horizontal = 12.dp, vertical = 5.dp))
        }
    }
}

@Composable
private fun UpdateLogCard(log: UpdateLog, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    updateLogChangeTypeLabel(log.changeType),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = updateLogChangeTypeColor(log.changeType),
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(updateLogChangeTypeColor(log.changeType).copy(alpha = 0.12f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Text(
                    "${log.createdAt.take(10)} · Version ${log.releaseVersion}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                log.text,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .padding(10.dp),
            )
        }
    }
}

@Composable
private fun updateLogChangeTypeColor(type: String): Color = when (type) {
    "feat" -> MaterialTheme.colorScheme.primary
    "fix", "sec" -> MaterialTheme.colorScheme.error
    "perf" -> MaterialTheme.colorScheme.tertiary
    "refactor", "docs", "test" -> MaterialTheme.colorScheme.secondary
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun todoStateColor(state: String): Color = when (state) {
    "in_progress" -> MaterialTheme.colorScheme.primary
    "done" -> MaterialTheme.colorScheme.tertiary
    "discarded" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

/** 待办没有 project 查询参数：一次拉全（100/页，最多 20 页）后在本地按项目筛选。 */
private suspend fun loadAllTodos(app: KungalApp, state: String?): Pair<List<Todo>, Int?> {
    val all = mutableListOf<Todo>()
    var cursor: String? = null
    var page = 0
    var total: Int? = null
    do {
        val result = app.todoApi.list(
            cursor = cursor,
            limit = TODO_FETCH_PAGE_SIZE,
            includeTotal = page == 0,
            state = state,
        )
        if (page == 0) total = result.total
        all += result.items
        cursor = result.nextCursor
        page++
    } while (cursor != null && page < TODO_MAX_PAGES)
    return all to total
}

private val TodoStateOptions = listOf(
    "" to "全部",
    "pending" to "待处理",
    "in_progress" to "进行中",
    "done" to "已完成",
    "discarded" to "已废弃",
)

private val TodoProjectOptions = listOf(
    FilterOption("forum", "论坛"),
    FilterOption("patch", "补丁站"),
)

/**
 * 待办列表（BP-05 U8，对应网页 /update/todo）：
 * 状态筛选走服务端，项目筛选在本地；只读展示，站务写操作不在 App 开放。
 */
@Composable
fun UpdateTodoScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
) {
    var state by rememberSaveable { mutableStateOf("") }
    var project by rememberSaveable { mutableStateOf("") }
    var projectSheet by remember { mutableStateOf(false) }

    var items by remember { mutableStateOf<List<Todo>>(emptyList()) }
    var total by remember { mutableStateOf<Int?>(null) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }

    LaunchedEffect(state, reload) {
        if (!refreshing) loading = true
        error = null
        runCatching { loadAllTodos(app, state.ifBlank { null }) }
            .onSuccess { (list, count) ->
                items = list
                total = count
            }
            .onFailure { error = friendlyError(it) }
        loading = false
        refreshing = false
    }

    val visible = if (project.isBlank()) items else items.filter { it.project == project }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "待办列表",
                subtitle = "站点将会实现与正在更改的功能",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        if (loading) {
            LoadingBox(Modifier.fillMaxSize().padding(padding))
        } else if (error != null && items.isEmpty()) {
            ErrorBox(
                error!!,
                onRetry = { reload++ },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
        } else {
            KungalPullToRefresh(
                isRefreshing = refreshing,
                onRefresh = {
                    if (!refreshing) {
                        refreshing = true
                        reload++
                    }
                },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp, top = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item(key = "filters") {
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            FilterChips(
                                options = TodoStateOptions,
                                selected = state,
                                onSelect = { state = it },
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                FilterPill(
                                    label = if (project.isBlank()) "全部项目" else todoProjectLabel(project),
                                    active = project.isNotBlank(),
                                    onClick = { projectSheet = true },
                                )
                                Text(
                                    buildString {
                                        append("共 ${total ?: visible.size} 项")
                                        if (project.isNotBlank()) append("，当前项目 ${visible.size} 项")
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                    if (error != null) {
                        item(key = "refresh-error") {
                            ErrorBox(error!!, onRetry = { reload++ })
                        }
                    }
                    if (visible.isEmpty()) {
                        item(key = "empty") {
                            EmptyBox("当前筛选条件下没有待办")
                        }
                    } else {
                        items(visible.size, key = { visible[it].id }) { index ->
                            TodoCard(
                                todo = visible[index],
                                onOpenUser = onOpenUser,
                                modifier = Modifier.padding(horizontal = 12.dp),
                            )
                        }
                        item(key = "footer") {
                            Text(
                                if (items.size >= (total ?: 0)) {
                                    "已加载全部 ${items.size} 项"
                                } else {
                                    "已加载 ${items.size} 项"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp),
                            )
                        }
                    }
                }
            }
        }
    }

    if (projectSheet) {
        FilterOptionSheet(
            title = "项目",
            options = TodoProjectOptions,
            selected = if (project.isBlank()) emptySet() else setOf(project),
            multi = false,
            includeAll = true,
            onToggle = { project = it },
            onClear = { project = "" },
            onDismiss = { projectSheet = false },
        )
    }
}

@Composable
private fun TodoCard(todo: Todo, onOpenUser: (String) -> Unit, modifier: Modifier = Modifier) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    todoProjectLabel(todo.project),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                )
                Text(
                    "创建于 ${todo.createdAt.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Box(Modifier.weight(1f))
                if (todo.state == "in_progress") {
                    todo.claimer?.name?.takeIf { it.isNotBlank() }?.let { name ->
                        Text(
                            "已被 $name 认领",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                AvatarImage(
                    url = todo.author.avatar?.url,
                    seed = todo.author.name,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable { onOpenUser(todo.author.id) },
                )
                Text(
                    todo.author.name?.takeIf { it.isNotBlank() } ?: "未知用户",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                activityBodyText(todo.text),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                todo.completedAt?.takeIf { it.isNotBlank() }?.let { completed ->
                    Text(
                        "完成于 ${completed.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Box(Modifier.weight(1f))
                Text(
                    todoStateLabel(todo.state),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = todoStateColor(todo.state),
                )
            }
        }
    }
}
