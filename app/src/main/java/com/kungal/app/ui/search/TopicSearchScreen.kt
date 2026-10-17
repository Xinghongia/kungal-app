package com.kungal.app.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.util.TopicLabels
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.CommentSearchHit
import com.kungal.core.model.ReplySearchHit
import com.kungal.core.model.Topic

/** 话题域搜索：话题 / 回复 / 评论三类结果。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicSearchScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenTopic: (String) -> Unit,
) {
    val vm: SearchViewModel = viewModel(key = "topic-search-${nsfwDisplay.wire}") {
        SearchViewModel(app, nsfwDisplay.allowsNsfw)
    }
    val state by vm.state.collectAsState()
    var query by remember { mutableStateOf("") }
    val keyboard = LocalSoftwareKeyboardController.current
    val submit = {
        if (query.isNotBlank()) {
            keyboard?.hide()
            vm.search(query)
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "搜索话题",
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
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("搜索话题、回复", maxLines = 1) },
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
                    keyboardActions = KeyboardActions(onSearch = { submit() }),
                )
                Button(
                    onClick = submit,
                    enabled = query.isNotBlank() && !state.loading,
                    shape = CircleShape,
                    modifier = Modifier.height(56.dp),
                ) {
                    Text(if (state.loading) "搜索中" else "搜索")
                }
            }

            TabRow(selectedTabIndex = state.tab) {
                listOf("话题", "回复", "评论").forEachIndexed { index, label ->
                    Tab(
                        selected = state.tab == index,
                        onClick = { vm.setTab(index, query) },
                        text = { Text(label) },
                    )
                }
            }

            when {
                state.error != null -> ErrorBox(
                    state.error!!,
                    onRetry = { vm.search(query) },
                    modifier = Modifier.fillMaxSize(),
                )
                state.loading && state.isEmpty -> LoadingBox(Modifier.fillMaxSize())
                state.isEmpty -> Text(
                    if (state.searched) "没有找到与「${state.searchedQuery}」相关的结果"
                    else "输入关键词开始搜索",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    when (state.tab) {
                        0 -> items(state.topics, key = { "t-${it.id}" }) { topic ->
                            TopicHit(topic, onClick = { onOpenTopic(topic.id) })
                        }
                        1 -> items(state.replies, key = { "r-${it.id}" }) { reply ->
                            ReplyHit(reply, onClick = { onOpenTopic(reply.topicId) })
                        }
                        else -> items(state.comments, key = { "c-${it.id}" }) { comment ->
                            CommentHit(comment, onClick = { onOpenTopic(comment.topicId) })
                        }
                    }
                    if (state.hasMore) {
                        item {
                            TextButton(
                                onClick = { vm.loadMore(query) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !state.loading,
                            ) { Text(if (state.loading) "加载中…" else "加载更多") }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TopicHit(topic: Topic, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                topic.title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (topic.isNsfw) Chip("NSFW")
                Chip(TopicLabels.category(topic.category))
                topic.sections.take(2).forEach { Chip(TopicLabels.section(it)) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(topic.author.avatar?.url, Modifier.size(18.dp), seed = topic.author.name)
                Spacer(Modifier.width(4.dp))
                Text(
                    topic.author.name ?: topic.author.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${topic.replyCount} 回复 · ${topic.likeCount} 赞",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReplyHit(hit: ReplySearchHit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                hit.topicTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                hit.excerpt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(hit.author.avatar?.url, Modifier.size(18.dp), seed = hit.author.name)
                Spacer(Modifier.width(4.dp))
                Text(
                    hit.author.name ?: hit.author.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "#${hit.floor} · ${relativeTime(hit.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CommentHit(hit: CommentSearchHit, onClick: () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 5.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        onClick = onClick,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                hit.topicTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                hit.excerpt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(hit.author.avatar?.url, Modifier.size(18.dp), seed = hit.author.name)
                Spacer(Modifier.width(4.dp))
                Text(
                    hit.author.name ?: hit.author.id,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "评论 · ${relativeTime(hit.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
