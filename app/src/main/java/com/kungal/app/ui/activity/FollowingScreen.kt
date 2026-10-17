package com.kungal.app.ui.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.FollowingActivityGroup
import com.kungal.core.model.FollowingActivityItem

/** 关注动态：关注的人发布/回复/评论/评分等分组列表。 */
@Composable
fun FollowingScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    val vm: FollowingViewModel = viewModel { FollowingViewModel(app) }
    val state by vm.state.collectAsState()

    LaunchedEffect(Unit) { vm.refresh() }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "关注动态",
                subtitle = if (state.unseenCount > 0) "${state.unseenCount} 条未读" else "你关注的人的最新动态",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        KungalPullToRefresh(
            isRefreshing = state.loading && state.groups.isNotEmpty(),
            onRefresh = { vm.refresh() },
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            when {
                state.loading && state.groups.isEmpty() -> LoadingBox(Modifier.fillMaxSize())
                state.error != null && state.groups.isEmpty() -> ErrorBox(
                    state.error!!,
                    onRetry = { vm.refresh() },
                    modifier = Modifier.fillMaxSize(),
                )
                state.groups.isEmpty() -> Text(
                    "还没有动态。关注一些用户后，他们的话题、回复与评论会出现在这里。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.groups, key = { it.id }) { group ->
                    FollowingGroupCard(
                        group = group,
                        onOpenTopic = onOpenTopic,
                        onOpenWeb = onOpenWeb,
                        onOpenUser = onOpenUser,
                    )
                }
                item {
                    if (state.loading) {
                        LoadingBox()
                    } else if (state.nextCursor != null) {
                        androidx.compose.material3.TextButton(
                            onClick = vm::loadMore,
                            modifier = Modifier.fillMaxWidth(),
                        ) { Text("加载更多") }
                    }
                }
            }
        }
    }
}
}

@Composable
private fun FollowingGroupCard(
    group: FollowingActivityGroup,
    onOpenTopic: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onOpenUser(group.actor.id) },
                ) {
                    AvatarImage(group.actor.avatar?.url, Modifier.size(26.dp), seed = group.actor.name)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        group.actor.name ?: group.actor.id,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    verbLabel(group.verb) + group.objectLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                Text(
                    relativeTime(group.latestAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            group.items.forEach { item ->
                FollowingItemRow(item, group, onOpenTopic, onOpenWeb)
            }
            if (group.itemCount > group.items.size) {
                Text(
                    "共 ${group.itemCount} 条",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun FollowingItemRow(
    item: FollowingActivityItem,
    group: FollowingActivityGroup,
    onOpenTopic: (String) -> Unit,
    onOpenWeb: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable {
                val topicId = topicIdFromPath(item.inSitePath)
                when {
                    topicId != null -> onOpenTopic(topicId)
                    item.inSitePath != null -> onOpenWeb("https://www.kungal.com${item.inSitePath}")
                    else -> Unit
                }
            }
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val cover = item.cover
        if (cover != null) {
            KungalImage(
                url = cover.url,
                contentDescription = null,
                modifier = Modifier.size(48.dp).clip(RoundedCornerShape(8.dp)),
            )
            Spacer(Modifier.width(8.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                item.title.ifBlank { item.objectLabel },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.excerpt.isNotBlank()) {
                Text(
                    item.excerpt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

private fun topicIdFromPath(path: String?): String? {
    if (path == null) return null
    val match = Regex("^/topic/(\\d+)").find(path) ?: return null
    return match.groupValues[1]
}

private fun verbLabel(verb: String): String = when (verb) {
    "publish" -> "发布了"
    "reply" -> "回复了"
    "comment" -> "评论了"
    "rate" -> "评分了"
    "like" -> "点赞了"
    "edit" -> "编辑了"
    else -> verb
}
