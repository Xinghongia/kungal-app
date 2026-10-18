package com.kungal.app.ui.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.EmojiEmotions
import androidx.compose.material.icons.outlined.Save
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AvatarImage
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.StickerPickerDialog
import com.kungal.core.model.UserRef
import com.kungal.core.model.stickerMarkdown
import kotlinx.coroutines.delay
import com.kungal.app.ui.topic.TopicCategorySections

/** 分类显示名（与网站一致）。 */
private val editorCategories = listOf(
    "galgame" to "Galgame",
    "technique" to "技术交流",
    "others" to "其它",
)

private val accessScopes = listOf(
    "public" to "公开",
    "login" to "登录可见",
    "role" to "指定角色",
    "users" to "指定用户",
)

private val accessRoles = listOf(
    "creator" to "创作者",
    "moderator" to "版主",
    "admin" to "管理员",
    "ren" to "莲",
)

/**
 * 话题编辑器：新建/编辑话题。支持 Markdown 正文、图片上传、分类与版块、
 * NSFW、访问范围、封面选择与草稿保存。
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun TopicEditorScreen(
    app: KungalApp,
    topicId: String?,
    draftId: String?,
    onBack: () -> Unit,
    onOpenDrafts: () -> Unit,
    onPublished: (String) -> Unit,
) {
    val vm: TopicEditorViewModel = viewModel(
        key = "editor-${topicId ?: draftId ?: "new"}",
    ) { TopicEditorViewModel(app, topicId, draftId) }
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var stickerOpen by remember { mutableStateOf(false) }
    var mentionResults by remember { mutableStateOf<List<UserRef>>(emptyList()) }
    val mentionQuery = remember(state.content) {
        Regex("""(?:^|\s)@([^\s@]{0,50})$""").find(state.content)?.groupValues?.get(1)
    }

    LaunchedEffect(mentionQuery, app.isLoggedIn) {
        val query = mentionQuery?.takeIf { it.isNotBlank() }
        if (query == null || !app.isLoggedIn) {
            mentionResults = emptyList()
            return@LaunchedEffect
        }
        delay(300)
        runCatching { app.userApi.resolveUsers(q = query, limit = 8) }
            .onSuccess { mentionResults = it.items }
            .onFailure { mentionResults = emptyList() }
    }

    fun applyMention(name: String) {
        val current = state.content
        val at = current.lastIndexOf('@')
        val tail = if (at >= 0) current.substring(at + 1) else ""
        vm.setContent(
            if (at >= 0 && tail.none { it.isWhitespace() }) {
                current.substring(0, at) + "@$name "
            } else {
                current + "@$name "
            },
        )
        mentionResults = emptyList()
    }

    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            val bytes = runCatching { context.contentResolver.openInputStream(uri)?.use { it.readBytes() } }
                .getOrNull()
            if (bytes != null) {
                vm.uploadImage(
                    bytes = bytes,
                    fileName = uri.lastPathSegment?.substringAfterLast('/') ?: "image",
                    contentType = context.contentResolver.getType(uri) ?: "image/jpeg",
                )
            }
        }
    }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }
    LaunchedEffect(state.error) {
        state.error?.let {
            snackbar.showSnackbar(it)
            vm.consumeError()
        }
    }
    LaunchedEffect(state.publishedTopicId) {
        state.publishedTopicId?.let {
            vm.consumePublished()
            onPublished(it)
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = if (topicId != null) "编辑话题" else "发布话题",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenDrafts) {
                        Icon(Icons.Outlined.Description, contentDescription = "草稿箱")
                    }
                    TextButton(
                        onClick = { vm.submit() },
                        enabled = !state.saving && !state.loading,
                    ) {
                        if (state.saving) {
                            CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (topicId != null) "保存" else "发布")
                        }
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbar) },
    ) { padding ->
        if (state.loading) {
            LoadingBox(Modifier.fillMaxSize().padding(padding))
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.title,
                onValueChange = vm::setTitle,
                label = { Text("标题") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            SectionLabel("分类")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                editorCategories.forEach { (key, label) ->
                    FilterChip(
                        selected = state.category == key,
                        onClick = { vm.setCategory(key) },
                        label = { Text(label) },
                    )
                }
            }

            val sections = TopicCategorySections[state.category].orEmpty()
            if (sections.isNotEmpty()) {
                SectionLabel("版块（1–3 个）")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    sections.forEach { section ->
                        FilterChip(
                            selected = section.key in state.sections,
                            onClick = { vm.toggleSection(section.key) },
                            label = { Text(section.label) },
                        )
                    }
                }
            }

            SectionLabel("正文（Markdown）")
            OutlinedTextField(
                value = state.content,
                onValueChange = vm::setContent,
                placeholder = { Text("支持 Markdown：标题、列表、代码块、图片、@提及…") },
                modifier = Modifier.fillMaxWidth().height(280.dp),
            )
            if (mentionResults.isNotEmpty() && mentionQuery != null) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    tonalElevation = 2.dp,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(Modifier.padding(vertical = 4.dp)) {
                        mentionResults.take(6).forEach { user ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { applyMention(user.name ?: user.id) }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                AvatarImage(
                                    url = user.avatar?.url,
                                    seed = user.name ?: user.id,
                                    modifier = Modifier.size(24.dp),
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    user.name ?: user.id,
                                    style = MaterialTheme.typography.bodyMedium,
                                )
                            }
                        }
                    }
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(
                    onClick = { pickImage.launch("image/*") },
                    enabled = !state.uploading,
                ) {
                    if (state.uploading) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(6.dp))
                        Text("上传中…")
                    } else {
                        Icon(Icons.Outlined.AddPhotoAlternate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("插入图片")
                    }
                }
                TextButton(onClick = { stickerOpen = true }) {
                    Icon(Icons.Outlined.EmojiEmotions, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("插入表情")
                }
                Spacer(Modifier.weight(1f))
                Text(
                    "单张 ≤10MiB",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.images.isNotEmpty()) {
                SectionLabel("已上传图片（点按设为封面，最多 9 张）")
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(state.images, key = { it.hash }) { image ->
                        UploadedImageCard(
                            image = image,
                            isCover = image.hash in state.coverHashes,
                            onToggleCover = { vm.toggleCover(image.hash) },
                            onRemove = { vm.removeImage(image.hash) },
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("NSFW 标记", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "成人向内容请在「显示/模糊」设置下正常展示",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = state.isNsfw, onCheckedChange = vm::setNsfw)
            }

            SectionLabel("访问范围")
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                accessScopes.forEach { (key, label) ->
                    FilterChip(
                        selected = state.accessScope == key,
                        onClick = { vm.setAccessScope(key) },
                        label = { Text(label) },
                    )
                }
            }
            when (state.accessScope) {
                "role" -> FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    accessRoles.forEach { (key, label) ->
                        FilterChip(
                            selected = key in state.accessRoles,
                            onClick = { vm.toggleRole(key) },
                            label = { Text(label) },
                        )
                    }
                }
                "users" -> OutlinedTextField(
                    value = state.accessUserIds,
                    onValueChange = vm::setAccessUserIds,
                    label = { Text("用户 UID（多个用逗号分隔）") },
                    modifier = Modifier.fillMaxWidth(),
                )
                "login" -> Text(
                    "未登录访客打不开；不会进入首页动态与排行榜。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TextButton(
                    onClick = { vm.saveDraft() },
                    enabled = !state.savingDraft && !state.saving,
                    modifier = Modifier.weight(1f),
                ) {
                    Icon(Icons.Outlined.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (state.savingDraft) "保存中…" else "存草稿")
                }
                Button(
                    onClick = { vm.submit() },
                    enabled = !state.saving && !state.loading,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (topicId != null) "保存修改" else "发布话题")
                }
            }
        }
    }
    if (stickerOpen) {
        StickerPickerDialog(
            load = { app.stickerPacks() },
            onPick = { sticker ->
                val markdown = stickerMarkdown(sticker)
                if (markdown.isNotBlank()) {
                    val base = state.content.trimEnd()
                    vm.setContent(if (base.isEmpty()) markdown else "$base\n\n$markdown\n")
                }
                stickerOpen = false
            },
            onDismiss = { stickerOpen = false },
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun UploadedImageCard(
    image: com.kungal.core.model.UploadedImage,
    isCover: Boolean,
    onToggleCover: () -> Unit,
    onRemove: () -> Unit,
) {
    Box(Modifier.size(104.dp)) {
        KungalImage(
            url = image.url,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .border(
                    width = if (isCover) 2.dp else 1.dp,
                    color = if (isCover) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(10.dp),
                )
                .clickable(onClick = onToggleCover),
        )
        if (isCover) {
            Icon(
                Icons.Outlined.CheckCircle,
                contentDescription = "已设为封面",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(4.dp)
                    .size(20.dp)
                    .background(Color.White, RoundedCornerShape(50)),
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.align(Alignment.TopEnd).size(28.dp),
        ) {
            Icon(
                Icons.Outlined.Close,
                contentDescription = "移除图片",
                modifier = Modifier
                    .size(18.dp)
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.5f), RoundedCornerShape(50)),
                tint = Color.White,
            )
        }
    }
}

/** 草稿箱：列出草稿，点按继续编辑；长按删除在行尾按钮。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DraftsScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenDraft: (String) -> Unit,
) {
    val vm: DraftsViewModel = viewModel { DraftsViewModel(app) }
    val state by vm.state.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbar.showSnackbar(it)
            vm.consumeMessage()
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "草稿箱",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbar) },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.items.isEmpty() -> Text(
                "还没有草稿。在编辑器里点「存草稿」就会出现在这里。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(padding).padding(32.dp),
            )
            else -> androidx.compose.foundation.lazy.LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                items(state.items, key = { it.id }) { draft ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onOpenDraft(draft.id) }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = draft.title.ifBlank { "（无标题草稿）" },
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = draft.summary,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = { vm.delete(draft.id) }) {
                            Icon(
                                Icons.Outlined.Close,
                                contentDescription = "删除草稿",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}
