package com.kungal.app.ui.galgame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.content.FullScreenImageViewer
import com.kungal.core.model.Work
import java.util.Locale

private enum class WorkTab(val label: String) {
    INTRO("游戏介绍"),
    RESOURCE("本体资源下载"),
    PATCH("补丁资源下载"),
    COMMENT("评论区"),
    QUIZ("题库"),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkDetailScreen(
    app: KungalApp,
    workId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenWeb: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
    onOpenEdit: () -> Unit,
    onOpenHistory: () -> Unit,
) {
    val vm: WorkDetailViewModel = viewModel(key = "work-detail-$workId") {
        WorkDetailViewModel(app, workId, nsfwDisplay.allowsNsfw)
    }
    val state by vm.state.collectAsState()
    var tab by rememberSaveable { mutableStateOf(WorkTab.INTRO) }
    var nsfwRevealed by rememberSaveable { mutableStateOf(false) }
    var showCovers by remember { mutableStateOf(false) }
    var showCollectionPicker by remember { mutableStateOf(false) }
    var showRecordDialog by remember { mutableStateOf(false) }
    var viewerUrl by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(nsfwDisplay.allowsNsfw) { vm.setIncludeNsfw(nsfwDisplay.allowsNsfw) }

    val work = state.work
    val gated = work?.isNsfw == true && nsfwDisplay == NsfwDisplayMode.HIDE && !nsfwRevealed

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 词条",
                subtitle = work?.let { workTitle(it) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (work != null) {
                        IconButton(onClick = { onOpenBrowser("https://www.kungal.com/galgame/${work.id}") }) {
                            Icon(Icons.Outlined.OpenInNew, contentDescription = "在浏览器打开")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading && work == null -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null && work == null -> ErrorBox(
                state.error!!,
                onRetry = { vm.refresh() },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            work == null -> EmptyBox("未找到这个 Galgame", Modifier.fillMaxSize().padding(padding))
            gated -> NsfwGate(
                noun = "Galgame",
                onReveal = { nsfwRevealed = true },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    item(key = "header") {
                        WorkHeader(
                            work = work,
                            blurMedia = nsfwDisplay.blursMedia,
                            likePending = state.likePending,
                            hasFavorited = state.hasFavorited,
                            favoriteCount = state.favoriteCount,
                            onLike = {
                                if (app.isLoggedIn) vm.toggleLike() else onOpenLogin()
                            },
                            onFavorite = {
                                if (app.isLoggedIn) showCollectionPicker = true else onOpenLogin()
                            },
                            onRate = {
                                if (app.isLoggedIn) showRecordDialog = true else onOpenLogin()
                            },
                            onEdit = {
                                if (app.isLoggedIn) onOpenEdit() else onOpenLogin()
                            },
                            onHistory = onOpenHistory,
                            onOpenCovers = { showCovers = true },
                            onOpenCover = { url -> viewerUrl = url },
                            onOpenUrl = onOpenWeb,
                        )
                    }
                    item(key = "tabs") {
                        ScrollableTabRow(
                            selectedTabIndex = tab.ordinal,
                            edgePadding = 12.dp,
                            containerColor = MaterialTheme.colorScheme.surface,
                            divider = {},
                        ) {
                            WorkTab.entries.forEach { item ->
                                Tab(
                                    selected = tab == item,
                                    onClick = { tab = item },
                                    text = { Text(item.label, style = MaterialTheme.typography.labelMedium) },
                                )
                            }
                        }
                    }
                    when (tab) {
                        WorkTab.INTRO -> workIntroItems(
                            work = work,
                            blurMedia = nsfwDisplay.blursMedia,
                            allowNsfw = nsfwDisplay.allowsNsfw,
                            onOpenWork = onOpenWork,
                            onOpenUser = onOpenUser,
                            onOpenWeb = onOpenWeb,
                            onOpenImage = { url -> viewerUrl = url },
                        )
                        WorkTab.RESOURCE -> item(key = "resource-placeholder") {
                            UpcomingPanel(
                                icon = Icons.Outlined.Visibility,
                                title = "本体资源下载",
                                description = "资源列表、下载与失效报告将在 BP-03 资源域接入。当前可先在网页查看。",
                                onOpenWeb = { onOpenBrowser("https://www.kungal.com/galgame/${work.id}?tab=resource") },
                            )
                        }
                        WorkTab.PATCH -> item(key = "patch-placeholder") {
                            UpcomingPanel(
                                icon = Icons.Outlined.Visibility,
                                title = "补丁资源下载",
                                description = "补丁资源（moyu）将在 BP-03 资源域接入。当前可先在网页查看。",
                                onOpenWeb = { onOpenBrowser("https://www.kungal.com/galgame/${work.id}?tab=patch") },
                            )
                        }
                        WorkTab.COMMENT -> item(key = "comment-wall") {
                            WallCommentSection(
                                app = app,
                                subjectType = "galgame",
                                subjectId = work.id,
                                maxLength = 5000,
                                title = "游戏评论",
                                onOpenUser = onOpenUser,
                                onOpenLogin = onOpenLogin,
                            )
                        }
                        WorkTab.QUIZ -> item(key = "quiz-placeholder") {
                            UpcomingPanel(
                                icon = Icons.Outlined.Visibility,
                                title = "题库",
                                description = "该作品的题库将在 BP-05 接入。当前可先在网页查看。",
                                onOpenWeb = { onOpenBrowser("https://www.kungal.com/galgame/${work.id}?tab=quiz") },
                            )
                        }
                    }
                }
            }
        }
    }

    if (showCovers && work != null) {
        CoverGallerySheet(
            work = work,
            coverCounts = state.coverCounts,
            votedCoverId = state.votedCoverId,
            votingCoverId = state.votingCoverId,
            allowNsfw = nsfwDisplay.allowsNsfw,
            blurMedia = nsfwDisplay.blursMedia,
            onVote = { coverId ->
                if (app.isLoggedIn) vm.voteCover(coverId) else onOpenLogin()
            },
            onOpenImage = { url -> viewerUrl = url },
            onDismiss = { showCovers = false },
        )
    }

    if (showCollectionPicker && work != null) {
        CollectionPickerSheet(
            app = app,
            workId = work.id,
            onDismiss = { showCollectionPicker = false },
            onSaved = { favorited -> vm.applyFavoriteState(favorited) },
        )
    }

    if (showRecordDialog && work != null) {
        WorkRecordDialog(
            app = app,
            workId = work.id,
            workName = workTitle(work),
            onDismiss = { showRecordDialog = false },
            onSaved = { vm.refresh() },
        )
    }

    viewerUrl?.let { url ->
        FullScreenImageViewer(url = url, onDismiss = { viewerUrl = null })
    }
}

@Composable
private fun WorkHeader(
    work: Work,
    blurMedia: Boolean,
    likePending: Boolean,
    hasFavorited: Boolean?,
    favoriteCount: Int,
    onLike: () -> Unit,
    onFavorite: () -> Unit,
    onRate: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onOpenCovers: () -> Unit,
    onOpenCover: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val cover = work.cover ?: work.covers.firstOrNull { it.image != null }?.image
    val coverUrl = cover?.url
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .width(126.dp)
                    .aspectRatio(5f / 7f)
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                if (work.isNsfw && blurMedia) {
                    NsfwCoverImage(url = coverUrl, modifier = Modifier.fillMaxSize())
                } else {
                    KungalImage(
                        url = coverUrl,
                        contentDescription = workTitle(work),
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                TextButton(
                    onClick = { coverUrl?.let(onOpenCover) ?: onOpenCovers() },
                    modifier = Modifier.align(Alignment.BottomStart),
                ) {
                    Text("查看封面", style = MaterialTheme.typography.labelSmall)
                }
                if (work.covers.size > 1) {
                    TextButton(
                        onClick = onOpenCovers,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    ) {
                        Text("全部 ${work.covers.size}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = workTitle(work),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (work.isNsfw) {
                        Text(
                            "NSFW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.error)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    }
                }
                if (work.aliases.isNotEmpty()) {
                    Text(
                        text = work.aliases.take(5).joinToString(" · "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(3.dp))
                    val score = work.ratingScore
                    Text(
                        text = if (score != null && work.ratingCount > 0) {
                            String.format(Locale.US, "%.1f", score)
                        } else {
                            "暂无评分"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (work.ratingCount > 0) {
                        Spacer(Modifier.width(4.dp))
                        Text(
                            "${work.ratingCount} 人评分",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    work.resourceTypes.take(2).forEach { Chip(labelOf(GalgameResourceTypeOptions, it)) }
                    work.resourceLanguages.take(2).forEach { Chip(labelOf(GalgameLanguageOptions, it)) }
                    work.resourcePlatforms.take(2).forEach { Chip(labelOf(GalgamePlatformOptions, it)) }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${work.viewCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Outlined.ThumbUp,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${work.likeCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Outlined.FavoriteBorder,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "$favoriteCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            FilledTonalButton(
                onClick = onLike,
                enabled = !likePending,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Icon(
                    Icons.Outlined.ThumbUp,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (work.viewer?.hasLiked == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.width(4.dp))
                Text(if (work.viewer?.hasLiked == true) "已点赞" else "点赞", style = MaterialTheme.typography.labelMedium)
            }
            FilledTonalButton(
                onClick = onFavorite,
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Icon(
                    if (hasFavorited == true) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = if (hasFavorited == true) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Spacer(Modifier.width(4.dp))
                Text(if (hasFavorited == true) "已收藏" else "收藏", style = MaterialTheme.typography.labelMedium)
            }
            TextButton(onClick = onRate, contentPadding = PaddingValues(horizontal = 8.dp)) {
                Icon(Icons.Outlined.Star, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text("评分", style = MaterialTheme.typography.labelMedium)
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 10.dp, end = 10.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            HeaderAction(Icons.Outlined.Edit, "编辑资料", onEdit)
            HeaderAction(Icons.Outlined.History, "修订历史", onHistory)
            HeaderAction(Icons.Outlined.Visibility, "查看封面", onOpenCovers)
            work.dlsite?.let { offer ->
                HeaderAction(Icons.Outlined.OpenInNew, "DLsite 购买") {
                    onOpenUrl(offer.purchaseUrl)
                }
            }
        }
        if (work.isResourceHostedElsewhere) {
            Text(
                text = "该作品的原语言不是日语或中文，本站不托管下载资源；资源在同 id 的 LetMoe 页面。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            )
        }
    }
}

@Composable
private fun HeaderAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    TextButton(onClick = onClick, contentPadding = PaddingValues(horizontal = 8.dp)) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(15.dp))
        Spacer(Modifier.width(3.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun UpcomingPanel(
    icon: ImageVector,
    title: String,
    description: String,
    onOpenWeb: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(
            Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Icon(
                icon,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onOpenWeb) { Text("在浏览器查看") }
        }
    }
}

@Composable
private fun NsfwGate(noun: String, onReveal: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Outlined.VisibilityOff,
            contentDescription = null,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "该 $noun 包含成人向内容",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = "当前内容设置为「隐藏」。你可以继续查看，或到设置中调整成人向内容的显示方式。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = onReveal) { Text("仍然查看") }
    }
}
