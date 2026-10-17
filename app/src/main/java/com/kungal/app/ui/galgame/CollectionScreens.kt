package com.kungal.app.ui.galgame

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
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
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.PageResult
import com.kungal.app.ui.components.PagedListContent
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.CollectionPatchBody
import com.kungal.core.model.CollectionSummary
import com.kungal.core.model.WorkSummary
import kotlinx.coroutines.launch
import java.util.UUID

private val CollectionSortOptions = listOf(
    "favorited_desc" to "最近收藏",
    "favorited_asc" to "最早收藏",
    "released_desc" to "发售日期新→旧",
    "released_asc" to "发售日期旧→新",
)

@Composable
private fun CollectionScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = title,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = { actions() },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) { content() }
    }
}

@Composable
private fun CollectionInfoCard(
    collection: CollectionSummary,
    onOpenUser: (String) -> Unit,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                collection.title.ifBlank { "未命名收藏夹" },
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (collection.visibility == "private") Icons.Outlined.Lock else Icons.Outlined.Public,
                    contentDescription = null,
                    modifier = Modifier.width(15.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    if (collection.visibility == "private") "私密收藏夹" else "公开收藏夹",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Chip("${collection.itemCount} 部作品")
                if (collection.isDefault) Chip("默认收藏夹")
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onOpenUser(collection.owner.id) },
            ) {
                AvatarImage(
                    url = collection.owner.avatar?.url,
                    seed = collection.owner.name,
                    modifier = Modifier.width(24.dp).aspectRatio(1f),
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    collection.owner.name ?: collection.owner.id,
                    style = MaterialTheme.typography.labelMedium,
                )
            }
            if (collection.description.isNotBlank()) {
                Text(collection.description, style = MaterialTheme.typography.bodyMedium)
            }
            if (collection.previewCovers.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    collection.previewCovers.take(4).forEach { cover ->
                        KungalImage(
                            url = cover.url,
                            contentDescription = null,
                            modifier = Modifier
                                .width(64.dp)
                                .aspectRatio(5f / 7f)
                                .clip(RoundedCornerShape(4.dp)),
                        )
                    }
                }
            }
            if (onEdit != null || onDelete != null) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onEdit?.let { TextButton(onClick = it) { Text("编辑收藏夹") } }
                    onDelete?.let {
                        TextButton(onClick = it) {
                            Text("删除收藏夹", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CollectionDetailScreen(
    app: KungalApp,
    collectionId: String,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val includeNsfw = nsfwDisplay.allowsNsfw
    var collection by remember(collectionId) { mutableStateOf<CollectionSummary?>(null) }
    var loading by remember(collectionId) { mutableStateOf(true) }
    var error by remember(collectionId) { mutableStateOf<String?>(null) }
    var reloadToken by remember { mutableStateOf(0) }
    var sort by remember { mutableStateOf("favorited_desc") }
    var editOpen by remember { mutableStateOf(false) }
    var deleteOpen by remember { mutableStateOf(false) }
    var pendingRemove by remember { mutableStateOf<WorkSummary?>(null) }

    LaunchedEffect(collectionId, includeNsfw, reloadToken) {
        loading = true
        error = null
        runCatching { app.collectionApi.collection(collectionId, includeNsfw) }
            .onSuccess { collection = it; loading = false }
            .onFailure { error = friendlyError(it); loading = false }
    }

    CollectionScaffold(
        title = "收藏夹",
        onBack = onBack,
        actions = {
            if (collection?.viewer?.canEdit == true) {
                IconButton(onClick = { editOpen = true }) {
                    Icon(Icons.Outlined.Edit, contentDescription = "编辑收藏夹", modifier = Modifier.width(20.dp))
                }
            }
        },
    ) {
        when {
            loading && collection == null -> LoadingBox()
            error != null && collection == null -> ErrorBox(error!!, onRetry = { reloadToken++ })
            collection == null -> EmptyBox("未找到这个收藏夹")
            else -> PagedListContent(
                key = listOf("collection-works", collectionId, sort, includeNsfw, reloadToken),
                load = { page, _ ->
                    val result = app.collectionApi.works(
                        collectionId = collectionId,
                        page = page,
                        limit = 24,
                        sort = sort,
                        includeNsfw = includeNsfw,
                    )
                    PageResult(result.items, ended = result.items.size < 24 || page * 24 >= result.total)
                },
                emptyText = "这个收藏夹还没有作品",
                modifier = Modifier.fillMaxSize(),
                keyOf = { it.id },
                header = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        CollectionInfoCard(
                            collection = collection!!,
                            onOpenUser = onOpenUser,
                            onEdit = if (collection!!.viewer?.canEdit == true) {
                                { editOpen = true }
                            } else {
                                null
                            },
                            onDelete = if (collection!!.viewer?.canDelete == true) {
                                { deleteOpen = true }
                            } else {
                                null
                            },
                        )
                        FilterChips(
                            options = CollectionSortOptions,
                            selected = sort,
                            onSelect = { sort = it },
                        )
                    }
                },
            ) { work ->
                EntityWorkRow(
                    work = work,
                    blurCover = nsfwDisplay.blursMedia,
                    onClick = { onOpenWork(work.id) },
                    trailing = if (collection?.viewer?.canEdit == true) {
                        {
                            TextButton(onClick = { pendingRemove = work }) { Text("移出") }
                        }
                    } else {
                        null
                    },
                )
            }
        }
    }

    if (editOpen && collection != null) {
        CollectionEditDialog(
            title = "编辑收藏夹",
            initialTitle = collection!!.title,
            initialDescription = collection!!.description,
            initialVisibility = collection!!.visibility,
            submitLabel = "保存",
            onDismiss = { editOpen = false },
            onSubmit = { title, description, visibility ->
                app.collectionApi.update(
                    collectionId,
                    CollectionPatchBody(
                        title = title,
                        description = description,
                        visibility = visibility,
                    ),
                )
            },
            onSaved = {
                editOpen = false
                reloadToken++
            },
        )
    }

    if (deleteOpen && collection != null) {
        AlertDialog(
            onDismissRequest = { deleteOpen = false },
            title = { Text("删除收藏夹") },
            text = { Text("删除后收藏夹与其中的收藏关系都会消失，作品本身不受影响。确定删除吗？") },
            confirmButton = {
                TextButton(onClick = {
                    deleteOpen = false
                    scope.launch {
                        runCatching { app.collectionApi.delete(collectionId) }
                            .onSuccess { onBack() }
                            .onFailure { error = friendlyError(it) }
                    }
                }) { Text("删除", color = MaterialTheme.colorScheme.error) }
            },
            dismissButton = { TextButton(onClick = { deleteOpen = false }) { Text("取消") } },
        )
    }

    pendingRemove?.let { work ->
        AlertDialog(
            onDismissRequest = { pendingRemove = null },
            title = { Text("移出收藏夹") },
            text = { Text("将《${workTitle(work)}》从这个收藏夹移出？") },
            confirmButton = {
                TextButton(onClick = {
                    val target = work
                    pendingRemove = null
                    scope.launch {
                        runCatching { app.collectionApi.setWork(collectionId, target.id, false) }
                            .onSuccess { reloadToken++ }
                            .onFailure { error = friendlyError(it) }
                    }
                }) { Text("移出") }
            },
            dismissButton = { TextButton(onClick = { pendingRemove = null }) { Text("取消") } },
        )
    }
}

@Composable
fun MyCollectionsScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenCollection: (String) -> Unit,
) {
    var createOpen by remember { mutableStateOf(false) }
    var reloadToken by remember { mutableStateOf(0) }
    CollectionScaffold(
        title = "我的收藏夹",
        onBack = onBack,
        actions = {
            IconButton(onClick = { createOpen = true }) {
                Icon(Icons.Outlined.Add, contentDescription = "新建收藏夹")
            }
        },
    ) {
        PagedListContent(
            key = Triple("my-collections", nsfwDisplay.allowsNsfw, reloadToken),
            load = { page, _ ->
                val result = app.collectionApi.myCollections(
                    page = page,
                    limit = 24,
                    includeNsfw = nsfwDisplay.allowsNsfw,
                )
                PageResult(result.items, ended = result.items.size < 24 || page * 24 >= result.total)
            },
            emptyText = "还没有收藏夹，点右上角新建一个吧",
            modifier = Modifier.fillMaxSize(),
            keyOf = { it.id },
            header = {
                Text(
                    "这里包含私密收藏夹；公开收藏夹会出现在你的个人主页。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                )
            },
        ) { collection ->
            CollectionListRow(collection) { onOpenCollection(collection.id) }
        }
    }

    if (createOpen) {
        CollectionEditDialog(
            title = "新建收藏夹",
            initialTitle = "",
            initialDescription = "",
            initialVisibility = "public",
            submitLabel = "创建",
            onDismiss = { createOpen = false },
            onSubmit = { title, description, visibility ->
                app.collectionApi.create(
                    title = title,
                    visibility = visibility,
                    description = description,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            },
            onSaved = {
                createOpen = false
                reloadToken++
            },
        )
    }
}

@Composable
private fun CollectionListRow(collection: CollectionSummary, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                collection.title.ifBlank { "未命名收藏夹" },
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                buildString {
                    append(if (collection.visibility == "private") "私密" else "公开")
                    append(" · ${collection.itemCount} 部作品")
                    if (collection.isDefault) append(" · 默认")
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (collection.previewCovers.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                collection.previewCovers.take(3).forEach { cover ->
                    KungalImage(
                        url = cover.url,
                        contentDescription = null,
                        modifier = Modifier.width(28.dp).aspectRatio(5f / 7f).clip(RoundedCornerShape(3.dp)),
                    )
                }
            }
        }
    }
}

@Composable
private fun CollectionEditDialog(
    title: String,
    initialTitle: String,
    initialDescription: String,
    initialVisibility: String,
    submitLabel: String,
    onDismiss: () -> Unit,
    onSubmit: suspend (title: String, description: String, visibility: String) -> CollectionSummary,
    onSaved: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf(initialTitle) }
    var description by remember { mutableStateOf(initialDescription) }
    var isPublic by remember { mutableStateOf(initialVisibility == "public") }
    var submitting by remember { mutableStateOf(false) }
    var failure by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = { if (!submitting) onDismiss() },
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("名称") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("描述（可选）") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (isPublic) "公开 · 所有人可见" else "私密 · 仅自己可见",
                        modifier = Modifier.weight(1f),
                    )
                    Switch(checked = isPublic, onCheckedChange = { isPublic = it })
                }
                failure?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (submitting) return@TextButton
                    val trimmed = name.trim()
                    if (trimmed.isEmpty()) {
                        failure = "请填写收藏夹名称"
                        return@TextButton
                    }
                    submitting = true
                    failure = null
                    scope.launch {
                        runCatching {
                            onSubmit(
                                trimmed,
                                description.trim(),
                                if (isPublic) "public" else "private",
                            )
                        }.onSuccess {
                            submitting = false
                            onSaved()
                        }.onFailure { e ->
                            submitting = false
                            failure = friendlyError(e)
                        }
                    }
                },
            ) { Text(submitLabel) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !submitting) { Text("取消") }
        },
    )
}
