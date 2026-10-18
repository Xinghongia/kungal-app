package com.kungal.app.ui.toolset

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.galgame.WallCommentSection
import com.kungal.app.ui.util.AndroidActions
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.Toolset
import com.kungal.core.model.ToolsetDownload
import com.kungal.core.model.ToolsetResource

/**
 * 工具集详情（BP-03 U4，对应网页 /toolset/{id}）：
 * 介绍、实用性分布与评分、收藏、订阅评论、工具资源下载/编辑/删除、评论墙、管理入口。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsetDetailScreen(
    app: KungalApp,
    toolsetId: String,
    onBack: () -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenBrowser: (String) -> Unit,
    onEditToolset: (String) -> Unit,
    onDeleted: () -> Unit,
    onRequireLoginThenUpload: () -> Unit,
) {
    val vm: ToolsetDetailViewModel = viewModel(key = "toolset-detail-$toolsetId") {
        ToolsetDetailViewModel(app, toolsetId)
    }
    val state by vm.state.collectAsState()
    val toolset = state.toolset
    var confirmDeleteToolset by remember { mutableStateOf(false) }
    var deletingResource by remember { mutableStateOf<ToolsetResource?>(null) }
    var editingResource by remember { mutableStateOf<ToolsetResource?>(null) }
    var addingResource by remember { mutableStateOf(false) }
    var expandedNotes by remember { mutableStateOf<Set<String>>(emptySet()) }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = toolset?.title ?: "工具资源",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenBrowser("https://www.kungal.com/toolset/$toolsetId") }) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = "在浏览器打开")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading && toolset == null -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null && toolset == null -> ErrorBox(
                state.error!!,
                onRetry = { vm.load() },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            toolset == null -> EmptyBox("未找到该工具资源", Modifier.fillMaxSize().padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding).imePadding(),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "intro") {
                    ToolsetIntroCard(
                        toolset = toolset,
                        app = app,
                        ratingPending = state.ratingPending,
                        onRate = { rating ->
                            if (app.isLoggedIn) vm.ratePracticality(rating) else onOpenLogin()
                        },
                        onOpenUser = onOpenUser,
                        onOpenBrowser = onOpenBrowser,
                    )
                }
                item(key = "actions") {
                    ToolsetActionBar(
                        app = app,
                        toolset = toolset,
                        favoritePending = state.favoritePending,
                        followWall = state.followWall,
                        followPending = state.followPending,
                        deleting = state.deleting,
                        onFavorite = {
                            if (app.isLoggedIn) vm.toggleFavorite() else onOpenLogin()
                        },
                        onToggleFollow = {
                            if (app.isLoggedIn) vm.toggleWallFollow() else onOpenLogin()
                        },
                        onAddResource = {
                            if (app.isLoggedIn) addingResource = true else onRequireLoginThenUpload()
                        },
                        onEdit = { onEditToolset(toolsetId) },
                        onDelete = { confirmDeleteToolset = true },
                    )
                }
                item(key = "resources-header") {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 2.dp)) {
                        Text(
                            "Galgame 工具下载资源",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            "您可以在这里添加或下载 Galgame 工具资源，S3 对象存储是直链不限速的下载方式",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (toolset.toolsetResources.isEmpty()) {
                    item(key = "resources-empty") {
                        EmptyBox("暂无 Galgame 工具下载资源")
                    }
                } else {
                    items(toolset.toolsetResources.size, key = { toolset.toolsetResources[it].id }) { index ->
                        val resource = toolset.toolsetResources[index]
                        ToolsetResourceCard(
                            resource = resource,
                            download = state.downloads[resource.id],
                            downloadPending = state.downloadPending == resource.id,
                            noteExpanded = resource.id in expandedNotes,
                            onToggleNote = {
                                expandedNotes = if (resource.id in expandedNotes) {
                                    expandedNotes - resource.id
                                } else {
                                    expandedNotes + resource.id
                                }
                            },
                            onFetchDownload = { vm.fetchDownload(resource) },
                            onEdit = { editingResource = resource },
                            onDelete = { deletingResource = resource },
                            onOpenBrowser = onOpenBrowser,
                        )
                    }
                }
                item(key = "comments") {
                    WallCommentSection(
                        app = app,
                        subjectType = "toolset",
                        subjectId = toolset.id,
                        maxLength = 5000,
                        title = "工具评论",
                        onOpenUser = onOpenUser,
                        onOpenLogin = onOpenLogin,
                    )
                }
            }
        }
    }

    if (confirmDeleteToolset && toolset != null) {
        AlertDialog(
            onDismissRequest = { confirmDeleteToolset = false },
            title = { Text("确定删除该工具？") },
            text = { Text("删除这个工具将会消耗 3 萌萌点，删除是永久性的，不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDeleteToolset = false
                    vm.delete { onDeleted() }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteToolset = false }) { Text("取消") } },
        )
    }

    deletingResource?.let { resource ->
        AlertDialog(
            onDismissRequest = { deletingResource = null },
            title = { Text("确定删除该工具资源吗？") },
            text = { Text("删除资源将会消耗您 3 萌萌点，资源将会永久删除，不可恢复。") },
            confirmButton = {
                TextButton(onClick = {
                    deletingResource = null
                    vm.deleteResource(resource)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deletingResource = null }) { Text("取消") } },
        )
    }

    editingResource?.let { resource ->
        ToolsetResourceEditorSheet(
            app = app,
            toolsetId = toolsetId,
            resource = resource,
            onDismiss = { editingResource = null },
            onSaved = {
                editingResource = null
                vm.load()
            },
        )
    }

    if (addingResource) {
        ToolsetResourceEditorSheet(
            app = app,
            toolsetId = toolsetId,
            resource = null,
            onDismiss = { addingResource = false },
            onSaved = {
                addingResource = false
                vm.load()
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ToolsetIntroCard(
    toolset: Toolset,
    app: KungalApp,
    ratingPending: Boolean,
    onRate: (Int) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                toolset.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Chip(labelOfToolset(ToolsetReleaseOptions, toolset.releaseChannel))
                Chip(labelOfToolset(ToolsetPlatformOptions, toolset.platform))
                Chip(labelOfToolset(ToolsetLanguageOptions, toolset.interfaceLanguage))
                Chip(labelOfToolset(ToolsetTypeOptions, toolset.toolsetType))
            }
            if (toolset.content?.children?.isNotEmpty() == true) {
                ContentDocumentView(toolset.content)
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "工具实用性分布",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "平均 ",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    toolset.practicalityAverage?.let { String.format(java.util.Locale.US, "%.1f", it) } ?: "暂无",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    if (toolset.practicalityAverage != null) " / 5.0" else "",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            PracticalityBars(toolset.practicalityDistribution, toolset.viewer?.practicalityRating)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "实用性评分",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.weight(1f),
                )
                for (star in 1..5) {
                    val selected = (toolset.viewer?.practicalityRating ?: 0) >= star
                    Icon(
                        if (selected) Icons.Filled.Star else Icons.Outlined.StarBorder,
                        contentDescription = "$star 星",
                        modifier = Modifier
                            .size(24.dp)
                            .clickable(enabled = !ratingPending) { onRate(star) },
                        tint = if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "${toolset.viewer?.practicalityRating ?: "—"} / 5",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(
                    url = toolset.author.avatar?.url,
                    seed = toolset.author.name,
                    modifier = Modifier.size(24.dp).clickable { onOpenUser(toolset.author.id) },
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    toolset.author.name ?: "已注销用户",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "${toolset.viewCount} 浏览 · ${toolset.downloadCount} 下载",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (toolset.homepageUrls.isNotEmpty()) {
                Text("主页 / 项目", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                toolset.homepageUrls.forEach { url ->
                    Text(
                        url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onOpenBrowser(url) },
                    )
                }
            }
            if (toolset.aliases.isNotEmpty()) {
                Text("别名", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    toolset.aliases.forEach { Chip(it) }
                }
            }
        }
    }
}

/** 1–5 星人数横向柱状图（本人评分的那一档高亮）。 */
@Composable
private fun PracticalityBars(distribution: List<Int>, myRating: Int?) {
    val max = (distribution.maxOrNull() ?: 0).coerceAtLeast(1)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (index in 0 until 5) {
            val count = distribution.getOrNull(index) ?: 0
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${index + 1} 星",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(32.dp),
                )
                Box(
                    Modifier
                        .weight(1f)
                        .height(14.dp)
                        .background(
                            MaterialTheme.colorScheme.surfaceContainerHigh,
                            RoundedCornerShape(4.dp),
                        ),
                ) {
                    if (count > 0) {
                        Box(
                            Modifier
                                .fillMaxWidth(count.toFloat() / max)
                                .fillMaxHeight()
                                .background(
                                    if (myRating == index + 1) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.primary.copy(alpha = 0.55f),
                                    RoundedCornerShape(4.dp),
                                ),
                        )
                    }
                }
                Spacer(Modifier.width(6.dp))
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(28.dp),
                )
            }
        }
    }
}

@Composable
private fun ToolsetActionBar(
    app: KungalApp,
    toolset: Toolset,
    favoritePending: Boolean,
    followWall: Boolean?,
    followPending: Boolean,
    deleting: Boolean,
    onFavorite: () -> Unit,
    onToggleFollow: () -> Unit,
    onAddResource: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val favorited = toolset.viewer?.hasFavorited == true
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = onFavorite,
            enabled = !favoritePending,
            colors = if (favorited) {
                androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                )
            } else {
                androidx.compose.material3.ButtonDefaults.filledTonalButtonColors()
            },
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(
                if (favorited) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (favorited) "已收藏 ${toolset.favoriteCount}" else "收藏 ${toolset.favoriteCount}",
                style = MaterialTheme.typography.labelMedium,
            )
        }
        FilledTonalButton(
            onClick = onToggleFollow,
            enabled = !followPending,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Icon(
                if (followWall == true) Icons.Outlined.Notifications else Icons.Outlined.NotificationsOff,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                if (followWall == true) "已订阅评论" else "订阅评论",
                style = MaterialTheme.typography.labelMedium,
            )
        }
        FilledTonalButton(
            onClick = onAddResource,
            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text("上传 / 添加资源", style = MaterialTheme.typography.labelMedium)
        }
        if (toolset.viewer?.canEdit == true) {
            TextButton(onClick = onEdit) {
                Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(3.dp))
                Text("修改", style = MaterialTheme.typography.labelMedium)
            }
        }
        if (toolset.viewer?.canDelete == true) {
            TextButton(onClick = onDelete, enabled = !deleting) {
                Icon(
                    Icons.Outlined.Delete,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.error,
                )
                Spacer(Modifier.width(3.dp))
                Text("删除", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
            }
        }
    }
}

@Composable
private fun ToolsetResourceCard(
    resource: ToolsetResource,
    download: ToolsetDownload?,
    downloadPending: Boolean,
    noteExpanded: Boolean,
    onToggleNote: () -> Unit,
    onFetchDownload: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    val context = LocalContext.current
    val isFile = resource.type == "file"
    val size = if (isFile) {
        formatToolsetFileSize(resource.archive?.fileSize ?: 0)
    } else {
        resource.link?.sizeLabel.orEmpty()
    }
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Chip(if (isFile) "对象存储下载" else "自定义链接下载")
                    if (size.isNotBlank()) Chip(size)
                    Chip("${resource.downloadCount} 人下载")
                }
                if (resource.viewer?.canEdit == true) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑资源", modifier = Modifier.size(18.dp))
                    }
                }
                if (resource.viewer?.canDelete == true) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除资源",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Button(
                    onClick = onFetchDownload,
                    enabled = !downloadPending,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    if (downloadPending) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text(if (download != null) "重新获取" else "获取链接", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }

            download?.let { data ->
                val url = data.downloadUrl
                if (url.isNullOrBlank()) {
                    Text(
                        "这个资源没有登记下载链接，链接可能写在提取码或备注里",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    Text(
                        url,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable { onOpenBrowser(url) },
                    )
                    val expiresAt = data.expiresAt
                    if (!expiresAt.isNullOrBlank()) {
                        Text(
                            "链接有效至 ${relativeTime(expiresAt)}（由系统浏览器下载）",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    TextButton(onClick = { AndroidActions.copyText(context, "下载链接", url) }) {
                        Text("复制链接", style = MaterialTheme.typography.labelSmall)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (data.extractionCode.isNotBlank()) {
                        FilledTonalButton(onClick = {
                            AndroidActions.copyText(context, "提取码", data.extractionCode)
                        }) { Text("提取码 ${data.extractionCode}") }
                    }
                    if (data.archivePassword.isNotBlank()) {
                        FilledTonalButton(onClick = {
                            AndroidActions.copyText(context, "解压码", data.archivePassword)
                        }) { Text("解压码 ${data.archivePassword}") }
                    }
                }
            }

            val note = resource.note
            if (!note.isNullOrBlank()) {
                Text(
                    note,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = if (noteExpanded) 100 else 3,
                    overflow = TextOverflow.Ellipsis,
                )
                if (note.length > 80 || note.contains('\n')) {
                    Text(
                        if (noteExpanded) "收起" else "展开全部",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable(onClick = onToggleNote),
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(
                    url = resource.poster.avatar?.url,
                    seed = resource.poster.name,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    "${resource.poster.name ?: "已注销用户"} · ${relativeTime(resource.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
