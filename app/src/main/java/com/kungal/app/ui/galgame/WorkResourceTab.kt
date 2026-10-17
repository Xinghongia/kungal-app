package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ThumbUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.outlined.ThumbUp
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.SheetMessageHost
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.content.documentPlainText
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.AndroidActions
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.GalgameResource
import com.kungal.core.model.MoyuPatchResource
import com.kungal.core.model.ResourceDownload
import com.kungal.core.model.ResourceSource
import com.kungal.core.model.Work
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

// ---------- 网盘分组 ----------

private data class ProviderBucket(val key: String, val label: String, val match: List<String>)

private val ProviderBuckets = listOf(
    ProviderBucket("baidu", "百度网盘", listOf("百度")),
    ProviderBucket("quark", "夸克网盘", listOf("夸克")),
    ProviderBucket("caiyun", "移动云盘", listOf("和彩云", "移动云盘", "139")),
    ProviderBucket("pan123", "123云盘", listOf("123")),
    ProviderBucket("xunlei", "迅雷云盘", listOf("迅雷")),
    ProviderBucket("lanzou", "蓝奏云盘", listOf("蓝奏")),
)

private fun bucketOf(resource: GalgameResource): ProviderBucket {
    for (bucket in ProviderBuckets) {
        if (resource.providerNames.any { name -> bucket.match.any { name.contains(it) } }) {
            return bucket
        }
    }
    return ProviderBucket("other", "其他下载", emptyList())
}

/** 按网盘分组；组内失效资源排最后，其余按 id 稳定排序（网页为随机序）。 */
private fun groupResourcesByProvider(
    resources: List<GalgameResource>,
): List<Pair<ProviderBucket, List<GalgameResource>>> {
    val grouped = resources.groupBy(::bucketOf)
    val order = ProviderBuckets + ProviderBucket("other", "其他下载", emptyList())
    return order.mapNotNull { bucket ->
        val items = grouped[bucket]?.sortedWith(
            compareBy({ it.isExpired }, { it.id.hashCode() }),
        )
        if (items.isNullOrEmpty()) null else bucket to items
    }
}

// ---------- 标签页 ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkResourceSection(
    app: KungalApp,
    work: Work,
    state: WorkDetailState,
    onOpenUser: (String) -> Unit,
    onOpenLogin: () -> Unit,
    onOpenBrowser: (String) -> Unit,
    onOpenResource: (String) -> Unit,
    onOpenExpireResources: (String) -> Unit,
    onRefresh: () -> Unit,
    onLike: (GalgameResource) -> Unit,
    onReport: (GalgameResource) -> Unit,
    onMarkValid: (GalgameResource) -> Unit,
    onDelete: (GalgameResource) -> Unit,
) {
    val currentUserId by app.currentUserId.collectAsState()
    var editing by remember { mutableStateOf<GalgameResource?>(null) }
    var creating by remember { mutableStateOf(false) }
    var downloading by remember { mutableStateOf<GalgameResource?>(null) }
    var deleting by remember { mutableStateOf<GalgameResource?>(null) }
    var markingValid by remember { mutableStateOf<GalgameResource?>(null) }
    var reporting by remember { mutableStateOf<GalgameResource?>(null) }

    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (work.isResourcePublishBanned) {
            NoticeCard(
                icon = Icons.Outlined.WarningAmber,
                title = "本游戏已禁止发布下载资源",
                text = "部分游戏可能因为版权方通知，或者其余第三方原因导致不可用，已禁止在本游戏下发布任何下载资源。",
            )
        }
        if (work.isResourceHostedElsewhere) {
            NoticeCard(
                icon = Icons.Outlined.OpenInNew,
                title = "这部作品的资源在一起萌 (LetMoe)",
                text = "本站只收录原语言为日语或中文的作品的下载资源。这部作品的资源请到一起萌查看和发布，那里是同一个作品编号。",
                action = "前往一起萌的这部作品",
                onAction = { onOpenBrowser("https://www.letmoe.com/game/${work.id}") },
            )
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "Galgame 资源链接",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            if (!work.isResourcePublishBanned && !work.isResourceHostedElsewhere) {
                FilledTonalButton(
                    onClick = {
                        if (app.isLoggedIn) creating = true else onOpenLogin()
                    },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加资源", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
        if (currentUserId != null) {
            TextButton(
                onClick = { currentUserId?.let(onOpenExpireResources) },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) {
                Text("批量更改已失效资源链接", style = MaterialTheme.typography.labelMedium)
            }
        }

        when {
            state.resourcesLoading && state.resources.isEmpty() -> LoadingBox()
            state.resourcesError != null && state.resources.isEmpty() -> ErrorBox(
                state.resourcesError,
                onRetry = onRefresh,
            )
            state.resources.isEmpty() -> EmptyBox("这个 Galgame 还没有资源链接，快添加一个吧！")
            else -> {
                val groups = groupResourcesByProvider(state.resources)
                val showHeaders = groups.size > 1
                groups.forEach { (bucket, items) ->
                    if (showHeaders) {
                        Text(
                            "${bucket.label} (${items.size})",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items.forEach { resource ->
                        ResourceCard(
                            app = app,
                            resource = resource,
                            likePending = resource.id in state.resourceLikePending,
                            reportStatus = state.resourceReports[resource.id],
                            onOpenUser = onOpenUser,
                            onLike = { onLike(resource) },
                            onDownload = { downloading = resource },
                            onEdit = { editing = resource },
                            onDelete = { deleting = resource },
                            onMarkValid = { markingValid = resource },
                            onReport = {
                                if (app.isLoggedIn) reporting = resource else onOpenLogin()
                            },
                            onOpenLogin = onOpenLogin,
                            onOpenDetail = { onOpenResource(resource.id) },
                        )
                    }
                }
            }
        }
    }

    // 新建 / 编辑
    if (creating || editing != null) {
        ResourceEditorSheet(
            app = app,
            workId = work.id,
            resourceId = editing?.id,
            onDismiss = {
                creating = false
                editing = null
            },
            onSaved = {
                creating = false
                editing = null
                onRefresh()
            },
        )
    }

    // 下载弹窗
    downloading?.let { resource ->
        ResourceDownloadSheet(
            app = app,
            resource = resource,
            onDismiss = { downloading = null },
            onOpenBrowser = onOpenBrowser,
        )
    }

    // 删除确认
    deleting?.let { resource ->
        AlertDialog(
            onDismissRequest = { deleting = null },
            title = { Text("删除资源链接") },
            text = { Text("这将扣除发布者获得的 3 萌萌点，此操作不可撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    deleting = null
                    onDelete(resource)
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleting = null }) { Text("取消") } },
        )
    }

    // 重新标记有效
    markingValid?.let { resource ->
        AlertDialog(
            onDismissRequest = { markingValid = null },
            title = { Text("重新标记有效") },
            text = { Text("资源链接修复后，或核实过链接依然可用时，可以重新标记为有效。") },
            confirmButton = {
                TextButton(onClick = {
                    markingValid = null
                    onMarkValid(resource)
                }) { Text("确认") }
            },
            dismissButton = { TextButton(onClick = { markingValid = null }) { Text("取消") } },
        )
    }

    // 报告失效
    reporting?.let { resource ->
        AlertDialog(
            onDismissRequest = { reporting = null },
            title = { Text("报告资源链接失效") },
            text = {
                Text(
                    "系统会先用网盘官方接口核验链接是否真的失效：确认失效才会标记并通知发布者；" +
                        "若链接仍可访问则不会标记。恶意报告将被处罚。",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    reporting = null
                    onReport(resource)
                }) { Text("报告失效", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { reporting = null }) { Text("取消") } },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceCard(
    app: KungalApp,
    resource: GalgameResource,
    likePending: Boolean,
    reportStatus: String?,
    onOpenUser: (String) -> Unit,
    onLike: () -> Unit,
    onDownload: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMarkValid: () -> Unit,
    onReport: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenDetail: () -> Unit,
) {
    val note = remember(resource.content) { documentPlainText(resource.content).trim() }
    var noteExpanded by remember { mutableStateOf(false) }
    val isOwner = app.currentUserId.collectAsState().value == resource.author.id
    val canEdit = resource.viewer?.canEdit == true
    val canDelete = resource.viewer?.canDelete == true
    val liked = resource.viewer?.hasLiked == true

    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = if (resource.isExpired) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            },
        ),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(
                    url = resource.author.avatar?.url,
                    seed = resource.author.name,
                    modifier = Modifier.size(28.dp).clickable { onOpenUser(resource.author.id) },
                )
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        resource.author.name ?: "已注销用户",
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        relativeTime(resource.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (resource.isExpired) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = if (resource.isExpired) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        if (resource.isExpired) "失效" else "有效",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (resource.isExpired) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.primary
                        },
                    )
                }
            }

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Chip(labelOf(GalgameResourceTypeOptions, resource.resourceType))
                if (resource.title.isNotBlank()) Chip(resource.title)
                if (resource.size.isNotBlank()) Chip(resource.size)
                resource.resourcePlatforms.forEach { Chip(labelOf(GalgamePlatformOptions, it)) }
                resource.resourceLanguages.forEach { Chip(labelOf(GalgameLanguageOptions, it)) }
                resource.resourceRuntimes.forEach {
                    Chip(labelOf(GalgameEmulatorRuntimeOptions, it))
                }
                resource.versionLabel?.let { Chip(labelOf(ResourceVersionOptions, it)) }
            }

            if (note.isNotBlank()) {
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
                        modifier = Modifier.clickable { noteExpanded = !noteExpanded },
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (resource.providerNames.isNotEmpty()) {
                    Text(
                        resource.providerNames.joinToString(" / "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                } else {
                    Spacer(Modifier.weight(1f))
                }
                Icon(
                    Icons.Outlined.Download,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(2.dp))
                Text(
                    "${resource.downloadCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!isOwner) {
                    Spacer(Modifier.width(6.dp))
                    LikeButton(
                        liked = liked,
                        count = resource.likeCount,
                        enabled = !likePending,
                        onClick = { if (app.isLoggedIn) onLike() else onOpenLogin() },
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (resource.isExpired && canEdit) {
                    TextButton(onClick = onMarkValid) { Text("重新标记有效", style = MaterialTheme.typography.labelMedium) }
                }
                if (canEdit) {
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Outlined.Edit, contentDescription = "编辑", modifier = Modifier.size(18.dp))
                    }
                }
                if (canDelete) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Outlined.Delete,
                            contentDescription = "删除",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                Spacer(Modifier.weight(1f))
                if (!isOwner && !resource.isExpired) {
                    TextButton(onClick = onReport) {
                        Text("报告失效", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onOpenDetail) {
                    Text("详情", style = MaterialTheme.typography.labelMedium)
                }
                Button(
                    onClick = onDownload,
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Icon(Icons.Outlined.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("获取资源", style = MaterialTheme.typography.labelMedium)
                }
            }

            reportStatus?.let { ResourceExpiryStatusBox(it) }
        }
    }
}

@Composable
private fun LikeButton(
    liked: Boolean,
    count: Int,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            if (liked) Icons.Filled.ThumbUp else Icons.Outlined.ThumbUp,
            contentDescription = "点赞",
            modifier = Modifier.size(15.dp),
            tint = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(3.dp))
        Text(
            "$count",
            style = MaterialTheme.typography.labelSmall,
            color = if (liked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
internal fun ResourceExpiryStatusBox(status: String) {    val (icon, line1, line2) = when (status) {
        "checking" -> Triple(Icons.Outlined.ErrorOutline, "正在检测链接有效性，请稍候…", "等待标记失效")
        "alive" -> Triple(Icons.Outlined.CheckCircle, "检测完成：链接仍然有效", "无需标记，链接仍可访问")
        "unchecked" -> Triple(Icons.Outlined.CheckCircle, "检测完成：未能核验链接", "已按失效处理，已通知发布者")
        "dead" -> Triple(Icons.Outlined.CheckCircle, "检测完成：链接已失效", "已标记为失效，已通知发布者")
        else -> Triple(Icons.Outlined.ErrorOutline, "操作失败，请稍后重试", "")
    }
    val color = when (status) {
        "checking", "alive" -> MaterialTheme.colorScheme.primary
        "error" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.primary
    }
    Column(
        Modifier.fillMaxWidth().padding(top = 2.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (status == "checking") {
                CircularProgressIndicator(modifier = Modifier.size(13.dp), strokeWidth = 2.dp)
            } else {
                Icon(icon, contentDescription = null, modifier = Modifier.size(14.dp), tint = color)
            }
            Spacer(Modifier.width(4.dp))
            Text(line1, style = MaterialTheme.typography.labelSmall, color = color)
        }
        if (line2.isNotBlank()) {
            Text(
                line2,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 18.dp),
            )
        }
    }
}

@Composable
private fun NoticeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    text: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
        ),
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                Text(text, style = MaterialTheme.typography.bodySmall)
                if (action != null && onAction != null) {
                    TextButton(
                        onClick = onAction,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
                    ) { Text(action, style = MaterialTheme.typography.labelMedium) }
                }
            }
        }
    }
}

// ---------- 下载弹窗 ----------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ResourceDownloadSheet(
    app: KungalApp,
    resource: GalgameResource,
    onDismiss: () -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var download by remember { mutableStateOf<ResourceDownload?>(null) }
    var retry by remember { mutableStateOf(0) }
    val context = androidx.compose.ui.platform.LocalContext.current

    LaunchedEffect(resource.id, retry) {
        loading = true
        error = null
        runCatching { app.resourceApi.download(resource.id) }
            .onSuccess { download = it }
            .onFailure { error = friendlyError(it) }
        loading = false
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        // 长按可选中备注、链接、提取码等文字并复制
        SelectionContainer {
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                SheetMessageHost()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (resource.isExpired) Icons.Outlined.WarningAmber else Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = if (resource.isExpired) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        if (resource.isExpired) "该资源链接已被标记失效" else "该资源链接可用",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Text(
                    "${resource.author.name ?: "已注销用户"} 发布于 ${relativeTime(resource.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (resource.providerNames.isNotEmpty()) {
                    Text(
                        resource.providerNames.joinToString(" / "),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                val noteText = remember(resource.content) { documentPlainText(resource.content).trim() }
                if (noteText.isNotBlank()) {
                    Text("发布者备注", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
                    Text(noteText, style = MaterialTheme.typography.bodySmall)
                }

                when {
                    loading -> Box(Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                    }
                    error != null -> {
                        Text(error!!, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        TextButton(onClick = { retry += 1 }) { Text("重试") }
                    }
                    else -> {
                        val data = download
                        if (data == null) {
                            Text("没有获取到下载信息", style = MaterialTheme.typography.bodySmall)
                        } else {
                            if (data.downloadUrls.isEmpty()) {
                                Text(
                                    "这个资源没有登记下载链接，链接可能写在提取码或备注里",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            } else {
                                Text("点击链接由系统浏览器/网盘 App 打开：", style = MaterialTheme.typography.labelMedium)
                                data.downloadUrls.forEachIndexed { index, url ->
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
                            if (data.extractionCode.isNotBlank() || data.archivePassword.isNotBlank()) {
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
                            Text(
                                "下载游玩后，欢迎回到词条页给这部作品评分，帮助更多人发现好作品。",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
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

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text("关闭") }
                }
            }
        }
    }
}

// ---------- 发布 / 编辑 ----------

private val ResourceVersionOptions = listOf(
    FilterOption("official_latest", "官方最新"),
    FilterOption("stable", "稳定版"),
    FilterOption("mirror", "镜像版"),
    FilterOption("localized", "汉化版"),
    FilterOption("unknown", "未知版本"),
)

private data class ResourceFormState(
    val linkText: String = "",
    val title: String = "",
    val sizeAmount: String = "",
    val sizeUnit: String = "GB",
    val extractionCode: String = "",
    val archivePassword: String = "",
    val resourceType: String = "game",
    val versionLabel: String = "",
    val languages: Set<String> = setOf("zh-cn"),
    val platforms: Set<String> = setOf("win"),
    val runtimes: Set<String> = setOf("native-win"),
    val contentMarkdown: String = "",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResourceEditorSheet(
    app: KungalApp,
    workId: String,
    resourceId: String?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val editing = resourceId != null
    var loadingSource by remember { mutableStateOf(editing) }
    var source by remember { mutableStateOf<ResourceSource?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var form by remember { mutableStateOf(ResourceFormState()) }
    var saving by remember { mutableStateOf(false) }
    var picker by remember { mutableStateOf<String?>(null) }
    var sourceRetry by remember { mutableStateOf(0) }

    LaunchedEffect(resourceId, sourceRetry) {
        if (resourceId == null) return@LaunchedEffect
        loadingSource = true
        runCatching { app.resourceApi.source(resourceId) }
            .onSuccess { src ->
                source = src
                val (amount, unit) = splitResourceSize(src.size)
                form = ResourceFormState(
                    linkText = src.downloadUrls.joinToString(", "),
                    title = src.title,
                    sizeAmount = amount,
                    sizeUnit = unit,
                    extractionCode = src.extractionCode,
                    archivePassword = src.archivePassword,
                    resourceType = src.resourceType,
                    versionLabel = src.versionLabel ?: "",
                    languages = src.resourceLanguages.toSet(),
                    platforms = src.resourcePlatforms.toSet(),
                    runtimes = src.resourceRuntimes.toSet(),
                    contentMarkdown = src.contentMarkdown,
                )
            }
            .onFailure { loadError = friendlyError(it) }
        loadingSource = false
    }

    ModalBottomSheet(onDismissRequest = { if (!saving) onDismiss() }) {
        if (loadingSource) {
            LoadingBox()
            return@ModalBottomSheet
        }
        if (loadError != null) {
            ErrorBox(loadError!!, onRetry = { sourceRetry += 1 })
            return@ModalBottomSheet
        }
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            SheetMessageHost()
            Text(
                if (editing) "重新编辑资源信息" else "发布 Galgame 资源",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                if (editing) {
                    "修改链接 / 提取码 / 备注等信息，保存后立即生效。"
                } else {
                    "为这部 Galgame 提交一份新的资源链接，提交后立即对所有用户可见。"
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = form.linkText,
                onValueChange = { form = form.copy(linkText = it) },
                label = { Text("资源链接（必填）") },
                placeholder = { Text("可直接粘贴网盘分享文本，多链接用逗号分隔") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(
                onClick = {
                    val parsed = parseResourceLinkInput(form.linkText)
                    if (!parsed.hasLinks) {
                        AppMessages.show("没有识别到链接")
                        return@TextButton
                    }
                    form = form.copy(
                        linkText = parsed.links.joinToString(", "),
                        extractionCode = parsed.extractionCode.ifBlank { form.extractionCode },
                        archivePassword = parsed.archivePassword.ifBlank { form.archivePassword },
                    )
                    AppMessages.show("已识别 ${parsed.links.size} 条链接")
                },
                contentPadding = androidx.compose.foundation.layout.PaddingValues(0.dp),
            ) { Text("自动识别链接 / 提取码 / 解压码", style = MaterialTheme.typography.labelMedium) }

            OutlinedTextField(
                value = form.title,
                onValueChange = { form = form.copy(title = it) },
                label = { Text("资源标题（可选）") },
                placeholder = { Text("例如 全年龄补丁 / PC+安卓直装") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.sizeAmount,
                    onValueChange = { raw ->
                        form = form.copy(sizeAmount = raw.filter { it.isDigit() || it == '.' }.take(9))
                    },
                    label = { Text("资源体积（必填）") },
                    placeholder = { Text("3.8") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.weight(1f),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    listOf("MB", "GB").forEach { unit ->
                        FilterChip(
                            selected = form.sizeUnit == unit,
                            onClick = { form = form.copy(sizeUnit = unit) },
                            label = { Text(unit, style = MaterialTheme.typography.labelSmall) },
                        )
                        Spacer(Modifier.width(4.dp))
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = form.extractionCode,
                    onValueChange = { form = form.copy(extractionCode = it) },
                    label = { Text("提取码（可选）") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = form.archivePassword,
                    onValueChange = { form = form.copy(archivePassword = it) },
                    label = { Text("解压码（可选）") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }

            SelectRow(
                label = "资源类型",
                value = labelOf(GalgameResourceTypeOptions, form.resourceType),
                onClick = { picker = "type" },
            )
            SelectRow(
                label = "适配版本（可选）",
                value = if (form.versionLabel.isBlank()) "未选择" else labelOf(ResourceVersionOptions, form.versionLabel),
                onClick = { picker = "version" },
            )
            SelectRow(
                label = "语言（可多选）",
                value = form.languages.map { labelOf(GalgameLanguageOptions, it) }.joinToString("、").ifBlank { "未选择" },
                onClick = { picker = "languages" },
            )
            SelectRow(
                label = "平台（可多选）",
                value = form.platforms.map { platformShortLabel(it) }.joinToString("、").ifBlank { "未选择" },
                onClick = { picker = "platforms" },
            )
            if (resourceTypeHasRuntime(form.resourceType)) {
                SelectRow(
                    label = "运行环境（可多选）",
                    value = form.runtimes.map { labelOf(GalgameEmulatorRuntimeOptions, it) }
                        .joinToString("、").ifBlank { "未选择" },
                    onClick = { picker = "runtimes" },
                )
            }

            OutlinedTextField(
                value = form.contentMarkdown,
                onValueChange = { form = form.copy(contentMarkdown = it) },
                label = { Text("资源备注（可选，支持 Markdown）") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TextButton(onClick = onDismiss, enabled = !saving) { Text("取消") }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (saving) return@Button
                        val links = parseResourceLinkInput(form.linkText).links
                            .ifEmpty { splitResourceLinkText(form.linkText) }
                        val error = validateResourceForm(form, links)
                        if (error != null) {
                            AppMessages.show(error)
                            return@Button
                        }
                        saving = true
                        scope.launch {
                            val result = runCatching {
                                if (editing && resourceId != null) {
                                    val patch = buildResourcePatch(form, source, links)
                                    if (patch.isEmpty()) return@runCatching null
                                    app.resourceApi.patch(resourceId, patch)
                                } else {
                                    app.resourceApi.create(
                                        workId = workId,
                                        body = buildResourceCreateBody(form, links),
                                        idempotencyKey = UUID.randomUUID().toString(),
                                    )
                                }
                            }
                            saving = false
                            result.onSuccess {
                                if (it != null) {
                                    AppMessages.show(if (editing) "资源修改成功" else "资源发布成功")
                                }
                                onSaved()
                            }.onFailure { AppMessages.show(friendlyError(it)) }
                        }
                    },
                    enabled = !saving,
                ) { Text(if (saving) "保存中…" else if (editing) "保存修改" else "发布资源") }
            }
        }
    }

    when (picker) {
        "type" -> SingleChoiceDialog(
            title = "资源类型",
            options = GalgameResourceTypeOptions,
            selected = form.resourceType,
            onSelect = { value ->
                form = form.copy(
                    resourceType = value,
                    runtimes = if (resourceTypeHasRuntime(value)) form.runtimes else emptySet(),
                )
                picker = null
            },
            onDismiss = { picker = null },
        )
        "version" -> SingleChoiceDialog(
            title = "适配版本",
            options = listOf(FilterOption("", "不设置")) + ResourceVersionOptions,
            selected = form.versionLabel,
            onSelect = { form = form.copy(versionLabel = it); picker = null },
            onDismiss = { picker = null },
        )
        "languages" -> MultiChoiceDialog(
            title = "语言（至少一个）",
            options = GalgameLanguageOptions,
            selected = form.languages,
            onToggle = { value ->
                form = form.copy(
                    languages = if (value in form.languages) form.languages - value else form.languages + value,
                )
            },
            onDismiss = { picker = null },
        )
        "platforms" -> MultiChoiceDialog(
            title = "平台",
            options = GalgamePlatformOptions.filter { it.value != "emulator" },
            selected = form.platforms,
            onToggle = { value ->
                form = form.copy(
                    platforms = if (value in form.platforms) form.platforms - value else form.platforms + value,
                )
            },
            onDismiss = { picker = null },
        )
        "runtimes" -> MultiChoiceDialog(
            title = "运行环境",
            options = GalgameEmulatorRuntimeOptions + FilterOption("emulator", "模拟器"),
            selected = form.runtimes,
            onToggle = { value ->
                form = form.copy(
                    runtimes = if (value in form.runtimes) form.runtimes - value else form.runtimes + value,
                )
            },
            onDismiss = { picker = null },
        )
    }
}

private fun validateResourceForm(form: ResourceFormState, links: List<String>): String? {
    if (links.isEmpty()) return "请填写至少一条资源链接"
    if (links.size > 20) return "资源链接最多 20 条"
    if (links.any { it.length > 4096 }) return "单条链接过长"
    if (links.any { !Regex("""^(https?|ftps?|magnet|ed2k|thunder):""", RegexOption.IGNORE_CASE).containsMatchIn(it) }) {
        return "链接格式不正确"
    }
    if (form.languages.isEmpty()) return "请至少选择一种语言"
    if (form.platforms.isEmpty() && form.runtimes.isEmpty()) return "请至少选择一个平台或运行环境"
    if (resourceTypeHasRuntime(form.resourceType) && form.runtimes.isEmpty()) return "该资源类型需要选择运行环境"
    if (!resourceSizeValid(form.sizeAmount, form.sizeUnit)) return "资源体积格式不正确（如 3.8 GB）"
    if (form.title.length > 200) return "资源标题过长"
    if (form.extractionCode.length > 1007 || form.archivePassword.length > 1007) return "提取码/解压码过长"
    if (form.contentMarkdown.length > 10000) return "资源备注过长（最多 10000 字）"
    return null
}

private fun buildResourceCreateBody(form: ResourceFormState, links: List<String>) = buildJsonObject {
    put("resource_type", JsonPrimitive(form.resourceType))
    put("resource_languages", buildJsonArray { form.languages.forEach { add(JsonPrimitive(it)) } })
    put("resource_platforms", buildJsonArray { form.platforms.forEach { add(JsonPrimitive(it)) } })
    if (resourceTypeHasRuntime(form.resourceType)) {
        put("resource_runtimes", buildJsonArray { form.runtimes.forEach { add(JsonPrimitive(it)) } })
    }
    put("title", JsonPrimitive(form.title.trim()))
    put("size", JsonPrimitive(joinResourceSize(form.sizeAmount, form.sizeUnit)))
    put("download_urls", buildJsonArray { links.forEach { add(JsonPrimitive(it)) } })
    put("extraction_code", JsonPrimitive(form.extractionCode.trim()))
    put("archive_password", JsonPrimitive(form.archivePassword.trim()))
    put("content_markdown", JsonPrimitive(form.contentMarkdown))
    if (form.versionLabel.isNotBlank()) put("version_label", JsonPrimitive(form.versionLabel))
}

/** 编辑时只提交变化字段；清空版本标签发送显式 null。 */
private fun buildResourcePatch(
    form: ResourceFormState,
    source: ResourceSource?,
    links: List<String>,
) = buildJsonObject {
    if (source == null) return@buildJsonObject
    if (form.resourceType != source.resourceType) put("resource_type", JsonPrimitive(form.resourceType))
    if (form.title.trim() != source.title) put("title", JsonPrimitive(form.title.trim()))
    if (form.versionLabel != (source.versionLabel ?: "")) {
        put(
            "version_label",
            if (form.versionLabel.isBlank()) JsonNull else JsonPrimitive(form.versionLabel),
        )
    }
    if (form.languages != source.resourceLanguages.toSet()) {
        put("resource_languages", buildJsonArray { form.languages.forEach { add(JsonPrimitive(it)) } })
    }
    if (form.platforms != source.resourcePlatforms.toSet()) {
        put("resource_platforms", buildJsonArray { form.platforms.forEach { add(JsonPrimitive(it)) } })
    }
    if (form.runtimes != source.resourceRuntimes.toSet()) {
        put("resource_runtimes", buildJsonArray { form.runtimes.forEach { add(JsonPrimitive(it)) } })
    }
    val size = joinResourceSize(form.sizeAmount, form.sizeUnit)
    if (size != source.size) put("size", JsonPrimitive(size))
    if (links != source.downloadUrls) {
        put("download_urls", buildJsonArray { links.forEach { add(JsonPrimitive(it)) } })
    }
    if (form.extractionCode.trim() != source.extractionCode) {
        put("extraction_code", JsonPrimitive(form.extractionCode.trim()))
    }
    if (form.archivePassword.trim() != source.archivePassword) {
        put("archive_password", JsonPrimitive(form.archivePassword.trim()))
    }
    if (form.contentMarkdown != source.contentMarkdown) {
        put("content_markdown", JsonPrimitive(form.contentMarkdown))
    }
}

@Composable
private fun SelectRow(label: String, value: String, onClick: () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
            Text("选择", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun SingleChoiceDialog(
    title: String,
    options: List<FilterOption>,
    selected: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(options.size) { index ->
                    val option = options[index]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(option.value) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(option.label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                        if (option.value == selected) {
                            Icon(
                                Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("关闭") } },
    )
}

@Composable
private fun MultiChoiceDialog(
    title: String,
    options: List<FilterOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(options.size) { index ->
                    val option = options[index]
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onToggle(option.value) }
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Checkbox(
                            checked = option.value in selected,
                            onCheckedChange = { onToggle(option.value) },
                        )
                        Text(option.label, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("完成") } },
    )
}

// ---------- 补丁（只读） ----------

@Composable
fun WorkPatchSection(
    state: WorkDetailState,
    onRetry: () -> Unit,
    onOpenBrowser: (String) -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "补丁资源下载",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "下面是从鲲 Galgame 补丁（moyu）获取到的补丁资源，请点击前往下载页面。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when {
            state.patchesLoading -> LoadingBox()
            state.patchesError != null -> ErrorBox(state.patchesError, onRetry = onRetry)
            state.moyuPatches.isEmpty() -> EmptyBox("这个 Galgame 还没有补丁资源")
            else -> state.moyuPatches.forEach { patch ->
                PatchCard(patch = patch, onOpenBrowser = onOpenBrowser)
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PatchCard(patch: MoyuPatchResource, onOpenBrowser: (String) -> Unit) {
    androidx.compose.material3.Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = androidx.compose.material3.CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val name = patch.name
            if (!name.isNullOrBlank()) {
                Text(
                    name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                patch.types.forEach { Chip(patchTokenLabel(it)) }
                if (patch.size.isNotBlank()) Chip(patch.size)
                patch.platforms.forEach { Chip(patchTokenLabel(it)) }
                patch.languages.forEach { Chip(patchTokenLabel(it)) }
                val modelName = patch.modelName
                if (!modelName.isNullOrBlank()) Chip(modelName)
                if (patch.storage.isNotBlank()) {
                    Chip(if (patch.storage == "s3") "S3 对象存储" else "网盘下载")
                }
            }
            val noteMarkdown = patch.noteMarkdown
            if (!noteMarkdown.isNullOrBlank()) {
                Text(
                    noteMarkdown,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${patch.publisher.name ?: "已注销用户"} · 更新于 ${relativeTime(patch.updatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    "下载 ${patch.downloadCount}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                FilledTonalButton(
                    onClick = { onOpenBrowser(patch.webUrl) },
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                ) {
                    Text("前往下载页面", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

/** moyu 的词表由对方站点维护，未知 token 原样显示。 */
private fun patchTokenLabel(token: String): String = when (token.lowercase()) {
    "windows" -> "Windows"
    "android" -> "安卓"
    "linux" -> "Linux"
    "macos" -> "macOS"
    "zh-hans", "zh-cn" -> "简体中文"
    "zh-hant", "zh-tw" -> "繁体中文"
    "ja", "ja-jp" -> "日语"
    "en", "en-us" -> "英语"
    "manual" -> "人工汉化"
    "ai" -> "AI 翻译"
    "machine" -> "机翻"
    "save" -> "存档"
    else -> token
}
