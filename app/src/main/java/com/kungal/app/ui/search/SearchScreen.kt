package com.kungal.app.ui.search

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.galgame.FilterOption
import com.kungal.app.ui.galgame.FilterOptionSheet
import com.kungal.app.ui.galgame.FilterPill
import com.kungal.app.ui.galgame.GalgameWorkCard
import com.kungal.app.ui.galgame.YearRangeDialog
import com.kungal.app.ui.toolset.ToolsetCard
import com.kungal.app.ui.util.TopicLabels
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.CommentSearchHit
import com.kungal.core.model.GalgameResource
import com.kungal.core.model.LocalizedText
import com.kungal.core.model.ReplySearchHit
import com.kungal.core.model.ToolsetSummary
import com.kungal.core.model.Topic
import com.kungal.core.model.UserSearchHit
import com.kungal.core.model.WallCommentSearchHit
import com.kungal.core.model.WorkSummary
import kotlinx.coroutines.delay

/**
 * 全站搜索：全部 / Galgame / 话题 / 资料库 / 资源 / 用户 / 回复 / 评论 / 游戏评论 / 工具。
 * 与网页同一组接口；「全部」并行预览各分类，tab 上的数量来自同一份预览。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenToolset: (String) -> Unit,
    onOpenEntity: (EntityFamily, String) -> Unit,
    onOpenPath: (String) -> Unit,
) {
    val vm: SearchViewModel = viewModel(key = "search-${nsfwDisplay.wire}") {
        SearchViewModel(app, nsfwDisplay.allowsNsfw)
    }
    val state by vm.state.collectAsState()
    val keyboard = LocalSoftwareKeyboardController.current

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "搜索",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedTextField(
                    value = state.queryInput,
                    onValueChange = vm::setQueryInput,
                    placeholder = { Text("搜索话题、Galgame、用户…", maxLines = 1) },
                    leadingIcon = {
                        Icon(
                            Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                    shape = CircleShape,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        keyboard?.hide()
                        vm.submit()
                    }),
                )
                Button(
                    onClick = {
                        keyboard?.hide()
                        vm.submit()
                    },
                    enabled = state.queryInput.isNotBlank(),
                    shape = CircleShape,
                    modifier = Modifier.height(56.dp),
                ) {
                    Text(if (state.overviewLoading && state.tab == SearchTab.ALL) "搜索中" else "搜索")
                }
            }

            if (state.submittedQuery.isBlank()) {
                SearchHistory(
                    history = state.history,
                    onSelect = vm::useHistory,
                    onRemove = vm::removeHistory,
                    onClear = vm::clearHistory,
                )
            } else {
                SearchTabs(
                    current = state.tab,
                    totals = state.totals,
                    onSelect = vm::selectTab,
                )
                Box(Modifier.fillMaxSize()) {
                    when (state.tab) {
                        SearchTab.ALL -> OverviewContent(
                            state = state,
                            keywords = state.submittedQuery,
                            blurCover = nsfwDisplay.blursMedia,
                            onRetry = vm::retry,
                            onOpenAll = vm::selectTab,
                            onOpenTopic = onOpenTopic,
                            onOpenWork = onOpenWork,
                            onOpenResource = onOpenResource,
                            onOpenUser = onOpenUser,
                            onOpenToolset = onOpenToolset,
                            onOpenEntity = onOpenEntity,
                            onOpenPath = onOpenPath,
                        )
                        SearchTab.WORKS -> WorksTab(
                            app = app,
                            state = state,
                            keywords = state.submittedQuery,
                            blurCover = nsfwDisplay.blursMedia,
                            includeNsfw = nsfwDisplay.allowsNsfw,
                            vm = vm,
                            onOpenWork = onOpenWork,
                        )
                        SearchTab.ENTITIES -> EntitiesTab(
                            state = state,
                            keywords = state.submittedQuery,
                            onFamily = vm::setEntityFamily,
                            onRetry = vm::retry,
                            onLoadMore = vm::loadMore,
                            onOpenEntity = onOpenEntity,
                        )
                        else -> LaneContent(
                            state = state,
                            keywords = state.submittedQuery,
                            onRetry = vm::retry,
                            onLoadMore = vm::loadMore,
                            onOpenTopic = onOpenTopic,
                            onOpenUser = onOpenUser,
                            onOpenResource = onOpenResource,
                            onOpenToolset = onOpenToolset,
                            onOpenPath = onOpenPath,
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SearchHistory(
    history: List<String>,
    onSelect: (String) -> Unit,
    onRemove: (String) -> Unit,
    onClear: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("搜索历史", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.weight(1f))
            if (history.isNotEmpty()) {
                TextButton(onClick = onClear) { Text("清除全部") }
            }
        }
        if (history.isEmpty()) {
            Text(
                "还没有搜索历史",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                history.asReversed().forEach { value ->
                    AssistChip(
                        onClick = { onSelect(value) },
                        label = {
                            Text(value, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        trailingIcon = {
                            IconButton(onClick = { onRemove(value) }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    Icons.Outlined.Close,
                                    contentDescription = "删除搜索历史 $value",
                                    modifier = Modifier.size(14.dp),
                                )
                            }
                        },
                        shape = CircleShape,
                    )
                }
            }
        }
        Text(
            "输入关键词即可搜索整个论坛：Galgame、话题、资料库角色 / 会社 / Staff / 标签、下载资源、用户、回复与评论。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SearchTabs(
    current: SearchTab,
    totals: Map<SearchTab, Int>,
    onSelect: (SearchTab) -> Unit,
) {
    val allCount = totals.values.sum().takeIf { totals.isNotEmpty() }
    ScrollableTabRow(
        selectedTabIndex = current.ordinal,
        edgePadding = 12.dp,
        divider = {},
    ) {
        SearchTab.entries.forEach { tab ->
            val count = if (tab == SearchTab.ALL) allCount else totals[tab]
            Tab(
                selected = tab == current,
                onClick = { onSelect(tab) },
                text = {
                    Text(
                        if (count != null) "${tab.label} $count" else tab.label,
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                    )
                },
            )
        }
    }
}

@Composable
private fun OverviewContent(
    state: SearchViewModel.State,
    keywords: String,
    blurCover: Boolean,
    onRetry: () -> Unit,
    onOpenAll: (SearchTab) -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenToolset: (String) -> Unit,
    onOpenEntity: (EntityFamily, String) -> Unit,
    onOpenPath: (String) -> Unit,
) {
    val overview = state.overview
    when {
        state.overviewLoading && overview == null -> LoadingBox(Modifier.fillMaxSize())
        state.overviewError != null && overview == null -> ErrorBox(
            state.overviewError,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
        )
        overview == null -> EmptyHint()
        else -> {
            val empty = !overview.anySucceeded &&
                overview.works.isEmpty() && overview.topics.isEmpty() &&
                overview.entities.isEmpty() && overview.resources.isEmpty() &&
                overview.users.isEmpty() && overview.replies.isEmpty() &&
                overview.comments.isEmpty() && overview.wallComments.isEmpty() &&
                overview.toolsets.isEmpty()
            if (empty) {
                EmptyHint()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    if (overview.works.isNotEmpty()) {
                        item {
                            SectionHeader("Galgame", overview.totals[SearchTab.WORKS], overview.works.size) {
                                onOpenAll(SearchTab.WORKS)
                            }
                        }
                        itemsIndexed(overview.works.chunked(2)) { _, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { work ->
                                    Box(Modifier.weight(1f)) {
                                        GalgameWorkCard(work, blurCover, onClick = { onOpenWork(work.id) })
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    if (overview.topics.isNotEmpty()) {
                        item {
                            SectionHeader("话题", overview.totals[SearchTab.TOPICS], overview.topics.size) {
                                onOpenAll(SearchTab.TOPICS)
                            }
                        }
                        items(overview.topics, key = { "t-${it.id}" }) { topic ->
                            TopicHit(topic, keywords, Modifier.padding(horizontal = 12.dp)) {
                                onOpenTopic(topic.id)
                            }
                        }
                    }
                    if (overview.entities.isNotEmpty()) {
                        item {
                            SectionHeader(
                                "资料库",
                                overview.totals[SearchTab.ENTITIES],
                                overview.entities.size,
                            ) { onOpenAll(SearchTab.ENTITIES) }
                        }
                        itemsIndexed(overview.entities.chunked(2)) { _, row ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                row.forEach { hit ->
                                    Box(Modifier.weight(1f)) {
                                        EntityHitCard(hit, keywords) { onOpenEntity(hit.family, hit.id) }
                                    }
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                    if (overview.resources.isNotEmpty()) {
                        item {
                            SectionHeader(
                                "Galgame 资源",
                                overview.totals[SearchTab.RESOURCES],
                                overview.resources.size,
                            ) { onOpenAll(SearchTab.RESOURCES) }
                        }
                        items(overview.resources, key = { "r-${it.id}" }) { resource ->
                            ResourceHit(resource, keywords, Modifier.padding(horizontal = 12.dp)) {
                                onOpenResource(resource.id)
                            }
                        }
                    }
                    if (overview.users.isNotEmpty()) {
                        item {
                            SectionHeader("用户", overview.totals[SearchTab.USERS], overview.users.size) {
                                onOpenAll(SearchTab.USERS)
                            }
                        }
                        items(overview.users, key = { "u-${it.id}" }) { user ->
                            UserHit(user, keywords, Modifier.padding(horizontal = 12.dp)) {
                                onOpenUser(user.id)
                            }
                        }
                    }
                    if (overview.replies.isNotEmpty()) {
                        item {
                            SectionHeader("回复", overview.totals[SearchTab.REPLIES], overview.replies.size) {
                                onOpenAll(SearchTab.REPLIES)
                            }
                        }
                        items(overview.replies, key = { "rp-${it.id}" }) { reply ->
                            ReplyHit(reply, keywords, Modifier.padding(horizontal = 12.dp)) {
                                onOpenTopic(reply.topicId)
                            }
                        }
                    }
                    if (overview.comments.isNotEmpty()) {
                        item {
                            SectionHeader("评论", overview.totals[SearchTab.COMMENTS], overview.comments.size) {
                                onOpenAll(SearchTab.COMMENTS)
                            }
                        }
                        items(overview.comments, key = { "c-${it.id}" }) { comment ->
                            CommentHit(comment, keywords, Modifier.padding(horizontal = 12.dp)) {
                                onOpenTopic(comment.topicId)
                            }
                        }
                    }
                    if (overview.wallComments.isNotEmpty()) {
                        item { SectionHeader("游戏评论", null, overview.wallComments.size) { onOpenAll(SearchTab.WALL) } }
                        items(overview.wallComments, key = { "w-${it.id}" }) { hit ->
                            WallHit(hit, keywords, Modifier.padding(horizontal = 12.dp)) {
                                onOpenPath(hit.subjectPath)
                            }
                        }
                    }
                    if (overview.toolsets.isNotEmpty()) {
                        item {
                            SectionHeader("Gal 工具", overview.totals[SearchTab.TOOLSETS], overview.toolsets.size) {
                                onOpenAll(SearchTab.TOOLSETS)
                            }
                        }
                        items(overview.toolsets, key = { "ts-${it.id}" }) { toolset ->
                            Box(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                                ToolsetCard(toolset, onClick = { onOpenToolset(toolset.id) })
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, total: Int?, shown: Int, onMore: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
        if (total != null) {
            Spacer(Modifier.width(6.dp))
            Text(
                total.toString(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.weight(1f))
        if (total == null || total > shown) {
            TextButton(onClick = onMore) { Text("查看全部") }
        }
    }
}

@Composable
private fun LaneContent(
    state: SearchViewModel.State,
    keywords: String,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenToolset: (String) -> Unit,
    onOpenPath: (String) -> Unit,
) {
    when (state.tab) {
        SearchTab.TOPICS -> LaneList(state.topics, state.error, onRetry, onLoadMore) { topic ->
            TopicHit(topic, keywords, Modifier.padding(horizontal = 12.dp)) { onOpenTopic(topic.id) }
        }
        SearchTab.REPLIES -> LaneList(state.replies, state.error, onRetry, onLoadMore) { reply ->
            ReplyHit(reply, keywords, Modifier.padding(horizontal = 12.dp)) { onOpenTopic(reply.topicId) }
        }
        SearchTab.COMMENTS -> LaneList(state.comments, state.error, onRetry, onLoadMore) { comment ->
            CommentHit(comment, keywords, Modifier.padding(horizontal = 12.dp)) { onOpenTopic(comment.topicId) }
        }
        SearchTab.USERS -> LaneList(state.users, state.error, onRetry, onLoadMore) { user ->
            UserHit(user, keywords, Modifier.padding(horizontal = 12.dp)) { onOpenUser(user.id) }
        }
        SearchTab.RESOURCES -> LaneList(state.resources, state.error, onRetry, onLoadMore) { resource ->
            ResourceHit(resource, keywords, Modifier.padding(horizontal = 12.dp)) {
                onOpenResource(resource.id)
            }
        }
        SearchTab.TOOLSETS -> LaneList(state.toolsets, state.error, onRetry, onLoadMore) { toolset ->
            Box(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                ToolsetCard(toolset, onClick = { onOpenToolset(toolset.id) })
            }
        }
        SearchTab.WALL -> LaneList(state.wallComments, state.error, onRetry, onLoadMore) { hit ->
            WallHit(hit, keywords, Modifier.padding(horizontal = 12.dp)) { onOpenPath(hit.subjectPath) }
        }
        else -> Unit
    }
}

@Composable
private fun <T> LaneList(
    lane: Lane<T>,
    error: String?,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    itemContent: @Composable (T) -> Unit,
) {
    when {
        lane.loading && lane.items.isEmpty() -> LoadingBox(Modifier.fillMaxSize())
        error != null && lane.items.isEmpty() -> ErrorBox(
            error,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize(),
        )
        lane.items.isEmpty() && lane.page > 0 -> EmptyHint()
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(vertical = 6.dp),
        ) {
            items(lane.items) { item -> itemContent(item) }
            if (lane.hasMore) {
                item {
                    TextButton(
                        onClick = onLoadMore,
                        enabled = !lane.loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text(if (lane.loading) "加载中…" else "加载更多") }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WorksTab(
    app: KungalApp,
    state: SearchViewModel.State,
    keywords: String,
    blurCover: Boolean,
    includeNsfw: Boolean,
    vm: SearchViewModel,
    onOpenWork: (String) -> Unit,
) {
    var sheet by remember { mutableStateOf<WorkSheet?>(null) }
    var showYears by remember { mutableStateOf(false) }
    val filters = state.workFilters

    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            item {
                FilterPill("排序：${filters.sortLabel}", active = filters.sort != "relevance_desc") {
                    sheet = WorkSheet.SORT
                }
            }
            item {
                FilterPill(filters.companyName ?: "会社", active = filters.companyId != null) {
                    sheet = WorkSheet.COMPANY
                }
            }
            item {
                FilterPill(
                    if (filters.tags.isEmpty()) "标签" else "标签 ${filters.tags.size}",
                    active = filters.tags.isNotEmpty(),
                ) { sheet = WorkSheet.TAG }
            }
            item {
                FilterPill(yearLabel(filters.releasedFrom, filters.releasedTo), active = filters.releasedFrom != null || filters.releasedTo != null) {
                    showYears = true
                }
            }
            if (filters.isActive) {
                item {
                    TextButton(onClick = vm::clearWorkFilters) { Text("重置") }
                }
            }
        }
        when {
            state.works.loading && state.works.items.isEmpty() -> LoadingBox(Modifier.fillMaxSize())
            state.error != null && state.works.items.isEmpty() -> ErrorBox(
                state.error,
                onRetry = vm::retry,
                modifier = Modifier.fillMaxSize(),
            )
            state.works.items.isEmpty() && state.works.page > 0 ->
                if (filters.isActive) {
                    Text(
                        "这些筛选条件下没有 Galgame，试着去掉一两个",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                } else {
                    EmptyHint()
                }
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                itemsIndexed(state.works.items.chunked(2)) { _, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { work ->
                            Box(Modifier.weight(1f)) {
                                GalgameWorkCard(work, blurCover, onClick = { onOpenWork(work.id) })
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (state.works.hasMore) {
                    item {
                        TextButton(
                            onClick = vm::loadMore,
                            enabled = !state.works.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (state.works.loading) "加载中…" else "加载更多") }
                    }
                }
            }
        }
    }

    when (sheet) {
        WorkSheet.SORT -> FilterOptionSheet(
            title = "排序",
            options = workSortOptions,
            selected = setOf(filters.sort),
            multi = false,
            includeAll = false,
            onToggle = { value ->
                val option = workSortOptions.first { it.value == value }
                vm.setWorkSort(value, option.label)
            },
            onClear = null,
            onDismiss = { sheet = null },
        )
        WorkSheet.COMPANY -> EntityPickerSheet(
            title = "会社",
            multi = false,
            selectedIds = setOfNotNull(filters.companyId),
            load = { query ->
                app.entityApi.companies(q = query.ifBlank { null }, page = 1, limit = 50).items.map {
                    EntityPick(it.id, entityName(it.displayName, it.localized, it.id))
                }
            },
            onToggle = { pick ->
                vm.setWorkCompany(pick)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        WorkSheet.TAG -> EntityPickerSheet(
            title = "标签（最多 10 个）",
            multi = true,
            selectedIds = filters.tags.map { it.id }.toSet(),
            load = { query ->
                app.entityApi.tags(
                    q = query.ifBlank { null },
                    page = 1,
                    limit = 50,
                    includeNsfw = includeNsfw,
                ).items.map {
                    EntityPick(it.id, entityName(it.displayName, it.localized, it.id))
                }
            },
            onToggle = { pick ->
                val already = filters.tags.any { it.id == pick.id }
                if (!already && filters.tags.size >= 10) {
                    AppMessages.show("标签最多选 10 个")
                } else {
                    vm.toggleWorkTag(pick)
                }
            },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
    if (showYears) {
        YearRangeDialog(
            from = filters.releasedFrom.orEmpty(),
            to = filters.releasedTo.orEmpty(),
            onApply = { from, to ->
                vm.setWorkYears(from, to)
                showYears = false
            },
            onDismiss = { showYears = false },
        )
    }
}

private enum class WorkSheet { SORT, COMPANY, TAG }

private val workSortOptions = listOf(
    FilterOption("relevance_desc", "相关度"),
    FilterOption("popularity_desc", "热度"),
    FilterOption("released_desc", "最新发售"),
    FilterOption("released_asc", "最早发售"),
    FilterOption("updated_desc", "资料更新"),
)

private fun yearLabel(from: String?, to: String?): String = when {
    from != null && to != null && from == to -> "$from 年"
    from != null && to != null -> "$from - $to"
    from != null -> "$from 年至今"
    to != null -> "$to 年以前"
    else -> "发售年份"
}

@Composable
private fun EntitiesTab(
    state: SearchViewModel.State,
    keywords: String,
    onFamily: (EntityFamily) -> Unit,
    onRetry: () -> Unit,
    onLoadMore: () -> Unit,
    onOpenEntity: (EntityFamily, String) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(EntityFamily.entries) { family ->
                FilterChip(
                    selected = family == state.entityFamily,
                    onClick = { onFamily(family) },
                    label = { Text(family.label) },
                )
            }
        }
        when {
            state.entities.loading && state.entities.items.isEmpty() ->
                LoadingBox(Modifier.fillMaxSize())
            state.error != null && state.entities.items.isEmpty() -> ErrorBox(
                state.error,
                onRetry = onRetry,
                modifier = Modifier.fillMaxSize(),
            )
            state.entities.items.isEmpty() && state.entities.page > 0 -> Text(
                "资料库里没有找到匹配的条目",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(32.dp),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                itemsIndexed(state.entities.items.chunked(2)) { _, row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        row.forEach { hit ->
                            Box(Modifier.weight(1f)) {
                                EntityHitCard(hit, keywords) { onOpenEntity(hit.family, hit.id) }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
                if (state.entities.hasMore) {
                    item {
                        TextButton(
                            onClick = onLoadMore,
                            enabled = !state.entities.loading,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text(if (state.entities.loading) "加载中…" else "加载更多") }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyHint() {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "杂鱼杂鱼杂鱼~什么也没有搜索到",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// ---------- 结果卡片 ----------

@Composable
private fun TopicHit(topic: Topic, keywords: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    HitCard(modifier, onClick) {
        HighlightedText(
            text = topic.title,
            keywords = keywords,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (topic.isNsfw) Chip("NSFW")
            Chip(TopicLabels.category(topic.category))
            topic.sections.take(2).forEach { Chip(TopicLabels.section(it)) }
        }
        AuthorLine(topic.author.avatar?.url, topic.author.name ?: topic.author.id, topic.author.name) {
            Text(
                "${topic.replyCount} 回复 · ${topic.likeCount} 赞",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReplyHit(hit: ReplySearchHit, keywords: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    HitCard(modifier, onClick) {
        HitTopicTitle(hit.topicTitle, keywords)
        HighlightedText(
            text = hit.excerpt,
            keywords = keywords,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
        )
        AuthorLine(hit.author.avatar?.url, hit.author.name ?: hit.author.id, hit.author.name) {
            Text(
                "#${hit.floor} · ${relativeTime(hit.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CommentHit(hit: CommentSearchHit, keywords: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    HitCard(modifier, onClick) {
        HitTopicTitle(hit.topicTitle, keywords)
        HighlightedText(
            text = hit.excerpt,
            keywords = keywords,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
        )
        AuthorLine(hit.author.avatar?.url, hit.author.name ?: hit.author.id, hit.author.name) {
            Text(
                "评论 · ${relativeTime(hit.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun UserHit(hit: UserSearchHit, keywords: String, modifier: Modifier = Modifier, onClick: () -> Unit) {
    HitCard(modifier, onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(hit.avatar?.url, Modifier.size(42.dp), seed = hit.name)
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HighlightedText(
                        text = hit.name ?: "#${hit.id}",
                        keywords = keywords,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    hit.roles.firstOrNull()?.let { role ->
                        Spacer(Modifier.width(6.dp))
                        Chip(roleLabel(role))
                    }
                }
                hit.bio?.takeIf { it.isNotBlank() }?.let { bio ->
                    HighlightedText(
                        text = bio,
                        keywords = keywords,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                "${hit.topicCount} 话题 · ${hit.replyCount} 回复",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            hit.registeredAt?.let {
                Text(
                    "注册于 ${it.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ResourceHit(
    resource: GalgameResource,
    keywords: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    HitCard(modifier, onClick) {
        HighlightedText(
            text = resource.work?.let { workTitleOf(it) } ?: "资源 #${resource.id}",
            keywords = keywords,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        HighlightedText(
            text = resource.title,
            keywords = keywords,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (resource.state == "expired") Chip("已失效")
            resource.providerNames.take(2).forEach { Chip(it) }
            resource.resourcePlatforms.take(2).forEach { Chip(it) }
            if (resource.size.isNotBlank()) Chip(resource.size)
        }
        Text(
            "${resource.viewCount} 浏览 · ${resource.downloadCount} 下载 · ${resource.likeCount} 赞",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun WallHit(
    hit: WallCommentSearchHit,
    keywords: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    HitCard(modifier, onClick) {
        HighlightedText(
            text = hit.work?.let { workTitleOf(it) } ?: hit.subjectPath,
            keywords = keywords,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        HighlightedText(
            text = hit.excerpt,
            keywords = keywords,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 3,
        )
        AuthorLine(hit.author.avatar?.url, hit.author.name ?: hit.author.id, hit.author.name) {
            Text(
                "${subjectTypeLabel(hit.subjectType)} · ${relativeTime(hit.createdAt)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EntityHitCard(hit: EntityHit, keywords: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (!hit.imageUrl.isNullOrBlank()) {
                KungalImage(
                    url = hit.imageUrl,
                    contentDescription = hit.name,
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                )
            } else {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            hit.name.take(1),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                        )
                    }
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                HighlightedText(
                    text = hit.name,
                    keywords = keywords,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Chip(hit.family.label)
                    hit.workCount?.takeIf { it > 0 }?.let { Chip("$it 部作品") }
                }
            }
        }
    }
}

@Composable
private fun HitCard(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Column(
            Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )
    }
}

@Composable
private fun HitTopicTitle(title: String, keywords: String) {
    HighlightedText(
        text = title,
        keywords = keywords,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
        maxLines = 1,
    )
}

@Composable
private fun AuthorLine(
    avatarUrl: String?,
    name: String,
    seed: String?,
    trailing: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        AvatarImage(avatarUrl, Modifier.size(18.dp), seed = seed)
        Spacer(Modifier.width(4.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        trailing()
    }
}

@Composable
private fun HighlightedText(
    text: String,
    keywords: String,
    style: androidx.compose.ui.text.TextStyle,
    fontWeight: FontWeight? = null,
    color: androidx.compose.ui.graphics.Color = MaterialTheme.colorScheme.onSurface,
    maxLines: Int = Int.MAX_VALUE,
) {
    Text(
        text = highlight(text, keywords, MaterialTheme.colorScheme.primary),
        style = style,
        fontWeight = fontWeight,
        color = color,
        maxLines = maxLines,
        overflow = TextOverflow.Ellipsis,
    )
}

/** 按空白拆词、忽略大小写地高亮（与网页行为一致）。 */
private fun highlight(text: String, keywords: String, color: androidx.compose.ui.graphics.Color): AnnotatedString {
    val words = keywords.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
    if (words.isEmpty()) return AnnotatedString(text)
    return buildAnnotatedString {
        append(text)
        for (word in words) {
            var start = text.indexOf(word, ignoreCase = true)
            while (start >= 0) {
                addStyle(
                    SpanStyle(color = color, fontWeight = FontWeight.Bold),
                    start,
                    (start + word.length).coerceAtMost(text.length),
                )
                start = text.indexOf(word, start + word.length.coerceAtLeast(1), ignoreCase = true)
            }
        }
    }
}

// ---------- 实体选择面板 ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityPickerSheet(
    title: String,
    multi: Boolean,
    selectedIds: Set<String>,
    load: suspend (String) -> List<EntityPick>,
    onToggle: (EntityPick) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var options by remember { mutableStateOf<List<EntityPick>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(query) {
        if (query.isNotBlank()) delay(300)
        loading = true
        options = runCatching { load(query) }.getOrDefault(emptyList())
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onDismiss) { Text("完成") }
            }
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                placeholder = { Text("输入名称搜索") },
                singleLine = true,
                leadingIcon = {
                    Icon(Icons.Outlined.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                shape = CircleShape,
            )
            if (loading) {
                LoadingBox(Modifier.fillMaxWidth())
            } else if (options.isEmpty()) {
                Text(
                    "没有找到匹配的条目",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp),
                )
            } else {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 420.dp)) {
                    items(options, key = { it.id }) { option ->
                        val selected = option.id in selectedIds
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onToggle(option)
                                    if (!multi) onDismiss()
                                }
                                .padding(horizontal = 8.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                option.name,
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            if (selected) {
                                Icon(
                                    Icons.Outlined.Check,
                                    contentDescription = "已选中",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                        HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHigh)
                    }
                    item { Spacer(Modifier.height(24.dp)) }
                }
            }
        }
    }
}

private fun workTitleOf(work: com.kungal.core.model.WorkRef): String =
    work.localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() }
        ?: work.displayName.ifBlank { "#${work.id}" }

private fun entityName(displayName: String, localized: Map<String, LocalizedText>, id: String): String =
    localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() }
        ?: displayName.ifBlank { "#$id" }

private fun roleLabel(role: String): String = when (role) {
    "creator" -> "创作者"
    "moderator" -> "版主"
    "admin" -> "管理员"
    "ren" -> "鲲"
    else -> role
}

private fun subjectTypeLabel(type: String): String = when (type) {
    "galgame" -> "Galgame 评论"
    "galgame_rating" -> "评分评论"
    "galgame_resource" -> "资源评论"
    "galgame_quiz" -> "题目讨论"
    "toolset" -> "工具评论"
    "website" -> "网站评论"
    else -> "评论"
}
