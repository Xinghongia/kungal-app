package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.fullDateTime
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.RatingDetail
import com.kungal.core.model.RatingViewer
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RatingDetailScreen(
    app: KungalApp,
    ratingId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var rating by remember(ratingId) { mutableStateOf<RatingDetail?>(null) }
    var loading by remember(ratingId) { mutableStateOf(true) }
    var error by remember(ratingId) { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    var revealed by remember(ratingId) { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var liking by remember { mutableStateOf(false) }

    LaunchedEffect(ratingId, reloadToken) {
        loading = true
        error = null
        runCatching { app.ratingApi.get(ratingId) }
            .onSuccess { rating = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 评分",
                subtitle = rating?.workSummary?.let { workTitle(it) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading && rating == null -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            error != null && rating == null -> ErrorBox(
                error!!,
                onRetry = { reloadToken++ },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            rating == null -> EmptyBox("未找到这条评分", Modifier.fillMaxSize().padding(padding))
            else -> {
                val value = rating!!
                LazyColumn(modifier = Modifier.fillMaxSize().padding(padding).imePadding()) {
                    item(key = "rating-info") {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                            ),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                // 作品
                                value.workSummary?.let { work ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(8.dp))
                                            .clickable { onOpenWork(work.id) }
                                            .padding(vertical = 2.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        WorkCoverImage(
                                            url = work.cover?.url,
                                            isNsfw = work.isNsfw,
                                            blurNsfw = nsfwDisplay.blursMedia,
                                            contentDescription = workTitle(work),
                                            modifier = Modifier
                                                .width(52.dp)
                                                .aspectRatio(5f / 7f)
                                                .clip(RoundedCornerShape(5.dp)),
                                        )
                                        Column {
                                            Text(
                                                workTitle(work),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                            Text(
                                                "查看作品资料",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                    }
                                }

                                // 作者
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.clickable { onOpenUser(value.author.id) },
                                ) {
                                    AvatarImage(
                                        url = value.author.avatar?.url,
                                        seed = value.author.name,
                                        modifier = Modifier.size(30.dp),
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column {
                                        Text(
                                            value.author.name ?: value.author.id,
                                            style = MaterialTheme.typography.bodyMedium,
                                        )
                                        Text(
                                            relativeTime(value.createdAt) +
                                                if (value.updatedAt != value.createdAt) " · 已编辑" else "",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Outlined.Star,
                                        contentDescription = null,
                                        modifier = Modifier.size(28.dp),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(
                                        value.overall.toString(),
                                        style = MaterialTheme.typography.displaySmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary,
                                    )
                                    Spacer(Modifier.width(10.dp))
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Text(
                                            recommendLabel(value.recommend),
                                            style = MaterialTheme.typography.titleSmall,
                                        )
                                        FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Chip(playStateLabel(value.playStatus))
                                            if (value.spoilerLevel != "none") {
                                                Chip(ratingSpoilerLabel(value.spoilerLevel))
                                            }
                                        }
                                        if (value.gameTypes.isNotEmpty()) {
                                            FlowRow(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                value.gameTypes.forEach { Chip(gameTypeLabel(it)) }
                                            }
                                        }
                                    }
                                }

                                if (value.shortSummary.isNotBlank()) {
                                    if (value.spoilerLevel == "none" || revealed) {
                                        Text(value.shortSummary, style = MaterialTheme.typography.bodyMedium)
                                        if (value.spoilerLevel != "none") {
                                            TextButton(onClick = { revealed = false }) { Text("收起剧透内容") }
                                        }
                                    } else {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Text(
                                                "该评分可能含有剧透内容，点进查看",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                            TextButton(onClick = { revealed = true }) { Text("仍然查看") }
                                        }
                                    }
                                }

                                if (aspectItems(value.aspectScores).isNotEmpty()) {
                                    Text(
                                        "八维评分",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    AspectRadarChart(
                                        scores = value.aspectScores,
                                        modifier = Modifier
                                            .align(Alignment.CenterHorizontally)
                                            .size(250.dp),
                                    )
                                    AspectScoreBars(value.aspectScores)
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    val liked = value.viewer?.hasLiked == true
                                    FilledTonalButton(
                                        onClick = {
                                            if (!app.isLoggedIn) {
                                                onOpenLogin()
                                            } else if (!liking) {
                                                val target = !liked
                                                liking = true
                                                // 先切换本地状态，点击立即变色，不等接口
                                                rating = value.copy(
                                                    likeCount = (value.likeCount +
                                                        if (target) 1 else -1).coerceAtLeast(0),
                                                    viewer = (value.viewer ?: RatingViewer())
                                                        .copy(hasLiked = target),
                                                )
                                                scope.launch {
                                                    runCatching { app.ratingApi.setLike(ratingId, target) }
                                                        .onSuccess { result ->
                                                            rating = value.copy(
                                                                likeCount = result.likeCount,
                                                                // viewer 缺失时也要落成对象，否则图标不会切换
                                                                viewer = (value.viewer
                                                                    ?: RatingViewer())
                                                                    .copy(hasLiked = result.viewer.hasLiked),
                                                            )
                                                            liking = false
                                                        }
                                                        .onFailure {
                                                            // 失败回滚到点击前的样子
                                                            rating = value
                                                            liking = false
                                                            AppMessages.show(friendlyError(it))
                                                        }
                                                }
                                            }
                                        },
                                        enabled = !liking,
                                        colors = if (liked) {
                                            androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                                                containerColor = MaterialTheme.colorScheme.primary,
                                                contentColor = MaterialTheme.colorScheme.onPrimary,
                                            )
                                        } else {
                                            androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
                                        },
                                    ) {
                                        Icon(
                                            if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(if (liked) "已点赞" else "点赞")
                                        Spacer(Modifier.width(4.dp))
                                        Text(value.likeCount.toString())
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Icon(
                                        Icons.Outlined.ChatBubbleOutline,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        value.commentCount.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(12.dp))
                                    Icon(
                                        Icons.Outlined.Visibility,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                    Spacer(Modifier.width(3.dp))
                                    Text(
                                        value.viewCount.toString(),
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                if (value.likers.isNotEmpty()) {
                                    Text(
                                        "最近点赞：" + value.likers.take(6).joinToString(" / ") {
                                            it.name ?: it.id
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (value.viewer?.canEdit == true) {
                                        TextButton(onClick = { editing = true }) { Text("编辑评分") }
                                    }
                                    if (value.viewer?.canDelete == true) {
                                        TextButton(onClick = { deleting = true }) {
                                            Text("删除评分", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    item(key = "wall") {
                        WallCommentSection(
                            app = app,
                            subjectType = "galgame_rating",
                            subjectId = ratingId,
                            maxLength = 1314,
                            title = "评分评论",
                            onOpenUser = onOpenUser,
                            onOpenLogin = onOpenLogin,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                }
            }
        }
    }

    if (editing && rating?.workSummary != null) {
        val work = rating!!.workSummary!!
        WorkRecordDialog(
            app = app,
            workId = work.id,
            workName = workTitle(work),
            onDismiss = { editing = false },
            onSaved = {
                editing = false
                reloadToken++
            },
        )
    }

    if (deleting) {
        AlertDialog(
            onDismissRequest = { deleting = false },
            title = { Text("删除评分") },
            text = { Text("评分会在所有 NextMoe 站点一起删除，确定吗？") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = false
                    scope.launch {
                        runCatching { app.ratingApi.delete(ratingId) }
                            .onSuccess { onBack() }
                            .onFailure { AppMessages.show(friendlyError(it)) }
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = false }) { Text("取消") } },
        )
    }
}
