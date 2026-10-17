package com.kungal.app.ui.topic

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.Topic

/** 话题分类（与站点 TopicCategory 对齐；null = 全部） */
private val categories = listOf(
    null to "全部",
    "galgame" to "Galgame",
    "technique" to "技术交流",
    "others" to "其它",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicListScreen(
    app: KungalApp,
    defaultSort: String,
    onOpenTopic: (String) -> Unit,
    onOpenPublish: () -> Unit,
) {
    val vm: TopicListViewModel = viewModel(key = "topic-list-$defaultSort") {
        TopicListViewModel(app, defaultSort)
    }
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()

    // 触底加载：接近列表末尾时请求下一页
    LaunchedEffect(listState) {
        snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 3
        }.collect { nearEnd -> if (nearEnd) vm.loadMore() }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "话题",
                subtitle = "鲲 Galgame 论坛的全部话题",
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    categories.forEach { (value, label) ->
                        FilterChip(
                            selected = state.category == value,
                            onClick = { vm.setCategory(value) },
                            label = { Text(label) },
                        )
                    }
                }
                when {
                    state.items.isEmpty() && state.loading -> LoadingBox(Modifier.fillMaxSize())
                    state.items.isEmpty() && state.error != null ->
                        ErrorBox(state.error!!, onRetry = { vm.refresh() }, modifier = Modifier.fillMaxSize())
                    else -> LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 96.dp),
                    ) {
                        items(state.items, key = { it.id }) { topic ->
                            TopicCard(topic = topic, onClick = { onOpenTopic(topic.id) })
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
private fun TopicCard(topic: Topic, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        val cover = topic.coverImages.firstOrNull()
        if (cover != null) {
            KungalImage(
                url = cover.url,
                contentDescription = null,
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )
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
                Chip(topic.category)
                topic.sections.take(2).forEach { Chip(it) }
                if (topic.miniApps.contains("lottery")) Chip("抽奖")
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
