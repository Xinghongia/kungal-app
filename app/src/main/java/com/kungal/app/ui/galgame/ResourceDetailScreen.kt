package com.kungal.app.ui.galgame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.util.AndroidActions
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.GalgameResource
import com.kungal.core.model.ResourceDownload

/**
 * 资源详情页（BP-03 U3）：作品卡、资源面板、下载、管理、同作品推荐与评论墙。
 * 详情 404 时查询迁移记录：搬到 LetMoe 的资源给出去处，否则如实显示未找到。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResourceDetailScreen(
    app: KungalApp,
    resourceId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenResourceList: () -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    val vm: ResourceDetailViewModel = viewModel(key = "resource-detail-$resourceId") {
        ResourceDetailViewModel(app, resourceId)
    }
    val state by vm.state.collectAsState()
    val resource = state.resource
    var editing by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    var confirmMarkValid by remember { mutableStateOf(false) }
    var confirmReport by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "资源详情",
                subtitle = resource?.work?.displayName,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { onOpenBrowser("https://www.kungal.com/galgame/resource/$resourceId") }) {
                        Icon(Icons.Outlined.OpenInNew, contentDescription = "在浏览器打开")
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null -> ErrorBox(
                state.error!!,
                onRetry = { vm.refresh() },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            state.notFound -> ResourceMissing(
                movedUrl = state.relocation?.url,
                onOpenBrowser = onOpenBrowser,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            resource == null -> EmptyBox("未找到这个资源", Modifier.fillMaxSize().padding(padding))
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item(key = "hero") {
                    ResourceHero(
                        resource = resource,
                        blurMedia = nsfwDisplay.blursMedia,
                        onOpenWork = onOpenWork,
                        onOpenResourceList = onOpenResourceList,
                    )
                }
                item(key = "panel") {
                    ResourcePanel(
                        app = app,
                        resource = resource,
                        download = state.download,
                        downloadLoading = state.downloadLoading,
                        downloadError = state.downloadError,
                        reportStatus = state.reportStatus,
                        deleting = state.deleting,
                        onFetchDownload = { vm.fetchDownload() },
                        onEdit = { editing = true },
                        onDelete = { confirmDelete = true },
                        onMarkValid = { confirmMarkValid = true },
                        onReport = {
                            if (app.isLoggedIn) confirmReport = true else onOpenLogin()
                        },
                        onOpenWork = { onOpenWork(resource.work?.id.orEmpty()) },
                        onOpenBrowser = onOpenBrowser,
                    )
                }
                if (state.recommendations.isNotEmpty()) {
                    item(key = "recommendations") {
                        RecommendationCard(
                            resources = state.recommendations,
                            onOpenResource = onOpenResource,
                            onOpenResourceList = onOpenResourceList,
                        )
                    }
                }
                item(key = "comments") {
                    WallCommentSection(
                        app = app,
                        subjectType = "galgame_resource",
                        subjectId = resource.id,
                        maxLength = 5000,
                        title = "资源评论",
                        onOpenUser = onOpenUser,
                        onOpenLogin = onOpenLogin,
                    )
                }
            }
        }
    }

    if (editing && resource != null) {
        ResourceEditorSheet(
            app = app,
            workId = resource.work?.id.orEmpty(),
            resourceId = resource.id,
            onDismiss = { editing = false },
            onSaved = {
                editing = false
                vm.refresh()
            },
        )
    }

    if (confirmDelete && resource != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除 Galgame 资源链接") },
            text = { Text("这会将资源永久删除，并扣除发布者获得的 3 萌萌点，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete { onBack() }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }

    if (confirmMarkValid) {
        AlertDialog(
            onDismissRequest = { confirmMarkValid = false },
            title = { Text("重新标记有效") },
            text = { Text("资源链接修复后，或核实过链接依然可用时，可以重新标记为有效。") },
            confirmButton = {
                TextButton(onClick = {
                    confirmMarkValid = false
                    vm.markValid()
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { confirmMarkValid = false }) { Text("取消") } },
        )
    }

    if (confirmReport) {
        AlertDialog(
            onDismissRequest = { confirmReport = false },
            title = { Text("报告资源链接失效") },
            text = {
                Text(
                    "系统会先用网盘官方接口核验链接是否真的失效：确认失效才会标记并通知发布者；" +
                        "若链接仍可访问则不会标记。恶意报告将被处罚。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmReport = false
                    vm.reportExpiry()
                }) { Text("报告失效", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { confirmReport = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ResourceMissing(
    movedUrl: String?,
    onOpenBrowser: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxWidth().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (movedUrl != null) {
            Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(28.dp))
            Text("这个资源已迁移到一起萌 (LetMoe)", style = MaterialTheme.typography.titleMedium)
            Text(
                "本站不再展示这个资源，可以前往一起萌的同一个作品编号继续查看。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = { onOpenBrowser(movedUrl) }) { Text("前往一起萌") }
        } else {
            Text("未找到对应的 Galgame 资源，或该资源已被移除。", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun ResourceHero(
    resource: GalgameResource,
    blurMedia: Boolean,
    onOpenWork: (String) -> Unit,
    onOpenResourceList: () -> Unit,
) {
    val work = resource.work
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                Modifier
                    .width(110.dp)
                    .aspectRatio(5f / 7f)
                    .clip(RoundedCornerShape(10.dp)),
            ) {
                if (work?.isNsfw == true && blurMedia) {
                    NsfwCoverImage(url = work.cover?.url, modifier = Modifier.fillMaxSize())
                } else {
                    KungalImage(
                        url = work?.cover?.url,
                        contentDescription = work?.displayName,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        work?.displayName ?: "未知作品",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { work?.id?.let(onOpenWork) },
                    )
                    if (work?.isNsfw == true) {
                        Text(
                            "NSFW",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError,
                            modifier = Modifier
                                .clip(RoundedCornerShape(5.dp))
                                .background(MaterialTheme.colorScheme.error)
                                .padding(horizontal = 5.dp, vertical = 1.dp),
                        )
                    } else {
                        Chip("SFW")
                    }
                }
                Spacer(Modifier.weight(1f))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilledTonalButton(
                        onClick = onOpenResourceList,
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text("浏览更多资源", style = MaterialTheme.typography.labelMedium) }
                    Button(
                        onClick = { work?.id?.let(onOpenWork) },
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                    ) { Text("查看这个 Galgame 的更多资源", style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourcePanel(
    app: KungalApp,
    resource: GalgameResource,
    download: ResourceDownload?,
    downloadLoading: Boolean,
    downloadError: String?,
    reportStatus: String?,
    deleting: Boolean,
    onFetchDownload: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkValid: () -> Unit,
    onReport: () -> Unit,
    onOpenWork: () -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    val context = LocalContext.current
    val isOwner = app.currentUserId.collectAsState().value == resource.author.id
    val canEdit = resource.viewer?.canEdit == true
    val canDelete = resource.viewer?.canDelete == true
    val workName = resource.work?.displayName ?: ""
    val platform = resource.resourcePlatforms.firstOrNull()?.let { labelOf(GalgamePlatformOptions, it) }
        ?: resource.resourceRuntimes.firstOrNull()?.let { labelOf(GalgameEmulatorRuntimeOptions, it) }
        ?: ""
    val language = resource.resourceLanguages.firstOrNull()?.let { labelOf(GalgameLanguageOptions, it) } ?: ""
    val typeLabel = labelOf(GalgameResourceTypeOptions, resource.resourceType)

    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                listOf(workName, platform, language, "${typeLabel}资源下载")
                    .filter { it.isNotBlank() }
                    .joinToString(" "),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                "若资源链接失效，或出现问题，请点击反馈资源问题前往 Galgame 的评论区，及时向资源发布者反馈或贡献新的下载资源。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Chip(typeLabel)
                resource.resourceLanguages.forEach { Chip(labelOf(GalgameLanguageOptions, it)) }
                resource.resourcePlatforms.forEach { Chip(labelOf(GalgamePlatformOptions, it)) }
                resource.resourceRuntimes.forEach { Chip(labelOf(GalgameEmulatorRuntimeOptions, it)) }
                if (resource.size.isNotBlank()) Chip(resource.size)
                Chip("下载 ${resource.downloadCount}")
                Chip("浏览 ${resource.viewCount}")
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${resource.author.name ?: "已注销用户"} 发布于 ${relativeTime(resource.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (resource.isExpired) {
                    Icon(
                        Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.error,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("失效", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                } else {
                    Icon(
                        Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(3.dp))
                    Text("有效", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                }
            }

            if (resource.content?.children?.isNotEmpty() == true) {
                Text("下载备注信息", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                ContentDocumentView(resource.content)
            }

            // 下载区
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ),
            ) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            "${typeLabel}下载链接",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f),
                        )
                        if (resource.providerNames.isNotEmpty()) {
                            Text(
                                resource.providerNames.joinToString(" / "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(6.dp))
                        Button(
                            onClick = onFetchDownload,
                            enabled = !downloadLoading,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            if (downloadLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(14.dp),
                                    strokeWidth = 2.dp,
                                )
                            } else {
                                Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("获取链接", style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    when {
                        downloadError != null -> Text(
                            downloadError,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                        download != null -> {
                            if (download.downloadUrls.isEmpty()) {
                                Text(
                                    "这个资源没有登记下载链接，链接可能写在提取码或备注里",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text("点击链接由系统浏览器/网盘 App 打开：", style = MaterialTheme.typography.labelSmall)
                                download.downloadUrls.forEachIndexed { index, url ->
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "${index + 1}. $url",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary,
                                            maxLines = 3,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable { onOpenBrowser(url) },
                                        )
                                        TextButton(onClick = {
                                            AndroidActions.copyText(context, "链接", url)
                                        }) { Text("复制", style = MaterialTheme.typography.labelSmall) }
                                    }
                                }
                            }
                            if (download.extractionCode.isNotBlank() || download.archivePassword.isNotBlank()) {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    if (download.extractionCode.isNotBlank()) {
                                        FilledTonalButton(onClick = {
                                            AndroidActions.copyText(context, "提取码", download.extractionCode)
                                        }) { Text("提取码 ${download.extractionCode}") }
                                    }
                                    if (download.archivePassword.isNotBlank()) {
                                        FilledTonalButton(onClick = {
                                            AndroidActions.copyText(context, "解压码", download.archivePassword)
                                        }) { Text("解压码 ${download.archivePassword}") }
                                    }
                                }
                            }
                            resource.dlsite?.let { offer ->
                                Text(
                                    "支持正版：DLsite 购买",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.clickable { onOpenBrowser(offer.purchaseUrl) },
                                )
                            }
                        }
                    }
                }
            }

            reportStatus?.let { ResourceExpiryStatusBox(it) }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (canEdit) {
                    TextButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(3.dp))
                        Text("编辑", style = MaterialTheme.typography.labelMedium)
                    }
                }
                if (canDelete) {
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
                if (!isOwner && !resource.isExpired) {
                    TextButton(onClick = onReport) {
                        Text(
                            "报告链接过期",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (resource.isExpired && canEdit) {
                    TextButton(onClick = onMarkValid) {
                        Text("重新标记有效", style = MaterialTheme.typography.labelMedium)
                    }
                }
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onOpenWork) {
                    Text("反馈资源问题", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    resources: List<GalgameResource>,
    onOpenResource: (String) -> Unit,
    onOpenResourceList: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "这部作品的其它资源",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = onOpenResourceList) {
                    Text("全部资源", style = MaterialTheme.typography.labelMedium)
                }
            }
            resources.forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenResource(item.id) }
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            item.title.ifBlank { labelOf(GalgameResourceTypeOptions, item.resourceType) },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            listOf(
                                labelOf(GalgameResourceTypeOptions, item.resourceType),
                                item.size,
                                item.providerNames.joinToString(" / "),
                            ).filter { it.isNotBlank() }.joinToString(" · "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    if (item.isExpired) {
                        Chip("已失效")
                    } else {
                        Text(
                            "下载 ${item.downloadCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
