package com.kungal.app.ui.topic

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.Reply
import com.kungal.core.model.Topic

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopicDetailScreen(
    app: KungalApp,
    topicId: String,
    onBack: () -> Unit,
    onRequireLogin: () -> Unit,
) {
    val vm: TopicDetailViewModel = viewModel(key = "topic-$topicId") { TopicDetailViewModel(app, topicId) }
    val state by vm.state.collectAsState()
    val session by app.session.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val listState = rememberLazyListState()
    var replyText by remember { mutableStateOf("") }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    // 回复触底加载
    LaunchedEffect(listState) {
        androidx.compose.runtime.snapshotFlow {
            val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 2
        }.collect { nearEnd -> if (nearEnd) vm.loadReplies() }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("话题", maxLines = 1) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            ReplyComposer(
                value = replyText,
                onValueChange = { replyText = it },
                sending = state.replySending,
                onSubmit = {
                    if (session == null) {
                        onRequireLogin()
                    } else {
                        vm.sendReply(replyText)
                        replyText = ""
                    }
                },
            )
        },
    ) { padding ->
        val topic = state.topic
        when {
            state.loading && topic == null -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            topic == null -> ErrorBox(
                state.error ?: "加载失败",
                onRetry = { vm.load() },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                item { TopicHeader(topic) }
                item {
                    ContentDocumentView(
                        document = topic.content,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
                item {
                    ReactionsRow(
                        topic = topic,
                        busy = state.likeBusy,
                        onLike = {
                            if (session == null) onRequireLogin() else vm.toggleLike()
                        },
                    )
                }
                item {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "回复（${topic.replyCount}）",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    )
                }
                items(state.replies, key = { it.id }) { reply -> ReplyCard(reply) }
                item {
                    when {
                        state.loadingReplies -> LoadingBox()
                        state.repliesEndReached && state.replies.isNotEmpty() -> Text(
                            text = "没有更多回复了",
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

@Composable
private fun TopicHeader(topic: Topic) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            text = topic.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (topic.isNsfw) Chip("NSFW")
            Chip(com.kungal.app.ui.util.TopicLabels.category(topic.category))
            topic.sections.forEach { Chip(com.kungal.app.ui.util.TopicLabels.section(it)) }
            if (topic.miniApps.contains("lottery")) Chip("抽奖话题")
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            AvatarImage(url = topic.author.avatar?.url, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = topic.author.name ?: topic.author.id,
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = buildString {
                        append(relativeTime(topic.createdAt))
                        if (topic.editedAt != null) append("（已编辑）")
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                Icons.Outlined.Visibility,
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(modifier = Modifier.width(3.dp))
            Text(
                text = topic.viewCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReactionsRow(topic: Topic, busy: Boolean, onLike: () -> Unit) {
    val liked = topic.viewer?.hasLiked == true ||
        topic.reactions.firstOrNull { it.reaction == "like" }?.viewer?.hasReacted == true
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(
                    if (liked) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                    else MaterialTheme.colorScheme.surfaceVariant,
                )
                .clickable(enabled = !busy) { onLike() }
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            if (busy) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Icon(
                    imageVector = if (liked) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = "点赞",
                    modifier = Modifier.size(18.dp),
                    tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = topic.likeCount.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ReplyCard(reply: Reply) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(url = reply.author.avatar?.url, modifier = Modifier.size(26.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = reply.author.name ?: reply.author.id,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.width(6.dp))
                if (reply.isBestAnswer) Chip("最佳答案")
                if (reply.isPinned) Chip("置顶")
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "#${reply.floor}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = relativeTime(reply.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            ContentDocumentView(document = reply.content)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(modifier = Modifier.width(3.dp))
                Text(
                    text = reply.likeCount.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ReplyComposer(
    value: String,
    onValueChange: (String) -> Unit,
    sending: Boolean,
    onSubmit: () -> Unit,
) {
    Surface(color = MaterialTheme.colorScheme.surfaceContainerLow) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .imePadding()
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            OutlinedTextField(
                value = value,
                onValueChange = onValueChange,
                placeholder = { Text("回复（支持 Markdown）") },
                modifier = Modifier.weight(1f),
                maxLines = 4,
                shape = RoundedCornerShape(12.dp),
            )
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onSubmit,
                enabled = value.isNotBlank() && !sending,
            ) {
                if (sending) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = "发送")
                }
            }
        }
    }
}
