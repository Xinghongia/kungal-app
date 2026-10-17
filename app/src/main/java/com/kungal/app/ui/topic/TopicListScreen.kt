package com.kungal.app.ui.topic

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Sort
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.util.TopicLabels
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.Topic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicListScreen(
    app: KungalApp,
    defaultSort: String,
    nsfwDisplay: NsfwDisplayMode,
    onOpenTopic: (String) -> Unit,
    onOpenPublish: () -> Unit,
    onOpenSearch: () -> Unit,
) {
    val includeNsfw = nsfwDisplay.allowsNsfw
    val vm: TopicListViewModel = viewModel(key = "topic-list-$defaultSort") {
        TopicListViewModel(app, defaultSort, includeNsfw)
    }
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var categoryExpanded by remember { mutableStateOf(false) }
    var sectionExpanded by remember { mutableStateOf(false) }
    var sortExpanded by remember { mutableStateOf(false) }

    // 设置里的内容显示方式变化后，服务端结果集也会变，必须重新请求
    LaunchedEffect(includeNsfw) { vm.setIncludeNsfw(includeNsfw) }

    // 触底加载：接近列表末尾时请求下一页
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 3
        }.collect { nearEnd -> if (nearEnd) vm.loadMore() }
    }

    val categoryLabel = TopicLabels.filterOptions
        .firstOrNull { it.first == state.category }?.second ?: "全部话题"
    val sections = TopicCategorySections[state.category].orEmpty()
    val sectionLabel = state.section?.let { key ->
        sections.firstOrNull { it.key == key }?.label ?: TopicLabels.section(key)
    } ?: "全部版块"
    val sortLabel = TopicSortFields.firstOrNull { it.key == state.sortField }?.label ?: "更新时间"

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "话题",
                subtitle = "鲲 Galgame 论坛的全部话题",
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Outlined.Search, contentDescription = "搜索")
                    }
                    // 二级分类（版块）：也放进右上角子菜单，选中分类后才出现
                    if (state.category != null) {
                        androidx.compose.foundation.layout.Box {
                            TextButton(onClick = { sectionExpanded = true }, colors = neutralTextButtonColors()) {
                                Text(sectionLabel, style = MaterialTheme.typography.labelLarge)
                                Icon(Icons.Outlined.ArrowDropDown, contentDescription = "切换版块")
                            }
                            DropdownMenu(
                                expanded = sectionExpanded,
                                onDismissRequest = { sectionExpanded = false },
                                containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            ) {
                                DropdownMenuItem(
                                    text = { Text("全部版块") },
                                    onClick = {
                                        sectionExpanded = false
                                        vm.setSection(null)
                                    },
                                    trailingIcon = {
                                        if (state.section == null) CheckMark()
                                    },
                                )
                                sections.forEach { section ->
                                    DropdownMenuItem(
                                        text = { Text(section.label) },
                                        onClick = {
                                            sectionExpanded = false
                                            vm.setSection(section.key)
                                        },
                                        trailingIcon = {
                                            if (state.section == section.key) CheckMark()
                                        },
                                    )
                                }
                            }
                        }
                    }
                    // 一级分类切换
                    androidx.compose.foundation.layout.Box {
                        TextButton(onClick = { categoryExpanded = true }, colors = neutralTextButtonColors()) {
                            Text(categoryLabel, style = MaterialTheme.typography.labelLarge)
                            Icon(Icons.Outlined.ArrowDropDown, contentDescription = "切换分类")
                        }
                        DropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            TopicLabels.filterOptions.forEach { (value, label) ->
                                DropdownMenuItem(
                                    text = { Text(label) },
                                    onClick = {
                                        categoryExpanded = false
                                        vm.setCategory(value)
                                    },
                                    trailingIcon = {
                                        if (state.category == value) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onOpenPublish,
                icon = { Icon(Icons.Outlined.Edit, contentDescription = null) },
                text = { Text("发布") },
            )
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = state.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                // 排序行：字段下拉 + 升降序切换
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Spacer(Modifier.weight(1f))

                    // 排序：字段下拉 + 升降序切换
                    androidx.compose.foundation.layout.Box {
                        TextButton(onClick = { sortExpanded = true }, colors = neutralTextButtonColors()) {
                            Icon(Icons.Outlined.Sort, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(sortLabel, style = MaterialTheme.typography.labelMedium)
                        }
                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                        ) {
                            TopicSortFields.forEach { field ->
                                DropdownMenuItem(
                                    text = { Text(field.label) },
                                    onClick = {
                                        sortExpanded = false
                                        vm.setSortField(field.key)
                                    },
                                    trailingIcon = {
                                        if (state.sortField == field.key) {
                                            Icon(
                                                Icons.Outlined.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp),
                                            )
                                        }
                                    },
                                )
                            }
                        }
                    }
                    TextButton(
                        onClick = { vm.setSortOrder(if (state.sortOrder == "desc") "asc" else "desc") },
                        colors = neutralTextButtonColors(),
                    ) {
                        Icon(
                            if (state.sortOrder == "desc") Icons.Outlined.ArrowDownward else Icons.Outlined.ArrowUpward,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            if (state.sortOrder == "desc") "降序" else "升序",
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }

                when {
                    state.items.isEmpty() && state.loading -> LoadingBox(Modifier.fillMaxSize())
                    state.items.isEmpty() && state.error != null ->
                        ErrorBox(state.error!!, onRetry = { vm.refresh() }, modifier = Modifier.fillMaxSize())
                    state.items.isEmpty() -> Text(
                        text = "该分类下暂无话题",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 96.dp),
                    ) {
                        items(state.items, key = { it.id }) { topic ->
                            TopicCard(
                                topic = topic,
                                blurCover = nsfwDisplay.blursMedia,
                                onClick = { onOpenTopic(topic.id) },
                            )
                        }
                        item {
                            when {
                                state.loading -> LoadingBox()
                                state.error != null -> ErrorBox(state.error!!, onRetry = { vm.loadMore() })
                                state.endReached -> Text(
                                    text = "没有更多了",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CheckMark() {
    Icon(
        Icons.Outlined.Check,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(18.dp),
    )
}

/** 排序/分类等工具按钮：固定用正文前景色（浅色近黑、深色近白），不跟主题强调色走。 */
@Composable
private fun neutralTextButtonColors() = ButtonDefaults.textButtonColors(
    contentColor = MaterialTheme.colorScheme.onSurface,
)

@Composable
private fun TopicCard(topic: Topic, blurCover: Boolean, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        val cover = topic.coverImages.firstOrNull()
        if (cover != null) {
            if (topic.isNsfw && blurCover) {
                NsfwCoverImage(
                    url = cover.url,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
            } else {
                KungalImage(
                    url = cover.url,
                    contentDescription = null,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
            }
        }
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = topic.title,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (topic.isNsfw) Chip("NSFW")
                Chip(TopicLabels.category(topic.category))
                topic.sections.take(2).forEach { Chip(TopicLabels.section(it)) }
                if (topic.miniApps.contains("lottery")) Chip("抽奖话题")
                if (topic.hasBestAnswer) Chip("已解决")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(url = topic.author.avatar?.url, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = topic.author.name ?: topic.author.id,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = relativeTime(topic.bumpedAt.ifBlank { topic.createdAt }),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.weight(1f))
                CountIcon(Icons.Outlined.Visibility, topic.viewCount)
                CountIcon(Icons.Outlined.ChatBubbleOutline, topic.replyCount)
                CountIcon(Icons.Outlined.FavoriteBorder, topic.likeCount)
            }
        }
    }
}

@Composable
private fun CountIcon(icon: ImageVector, count: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(2.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
    }
}
