package com.kungal.app.ui.news

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.SubcomposeAsyncImage
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.NewsSubmission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/** 投稿状态胶囊：待审核 / 已发布 / 未通过 / 已撤回（颜色对齐网页）。 */
@Composable
fun NewsStateChip(state: String) {
    val color = when (state) {
        "pending" -> MaterialTheme.colorScheme.secondary
        "published" -> MaterialTheme.colorScheme.tertiary
        "rejected" -> MaterialTheme.colorScheme.error
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Text(
        text = newsSubmissionStateLabel(state),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

/**
 * 我的情报投稿（对应网页 /news/mine）：状态、封面、导语、编辑与撤回；
 * 游标分页「加载更多」；从表单返回时自动刷新。
 */
@Composable
fun NewsMineScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenNews: () -> Unit,
    onCreate: () -> Unit,
    onOpenEditor: (String) -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenLogin: () -> Unit,
) {
    var items by remember { mutableStateOf<List<NewsSubmission>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var withdrawTarget by remember { mutableStateOf<NewsSubmission?>(null) }
    var resumedOnce by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                if (resumedOnce) reload += 1 else resumedOnce = true
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(reload) {
        if (!app.isLoggedIn) return@LaunchedEffect
        loading = true
        error = null
        runCatching { app.newsSubmissionApi.mySubmissions(limit = 20) }
            .onSuccess {
                items = it.items
                nextCursor = it.nextCursor
            }
            .onFailure { error = friendlyError(it) }
        loading = false
    }

    fun withdraw(item: NewsSubmission) {
        if (busyId != null) return
        busyId = item.id
        scope.launch {
            runCatching { app.newsSubmissionApi.withdraw(item.id) }
                .onSuccess { updated ->
                    items = items.map { if (it.id == updated.id) updated else it }
                    AppMessages.show("已撤回")
                }
                .onFailure { AppMessages.show(friendlyError(it)) }
            busyId = null
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "我的情报投稿",
                subtitle = "待审核 / 已发布 / 未通过 / 已撤回",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(onClick = { reload += 1 }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新")
                    }
                },
            )
        },
    ) { padding ->
        if (!app.isLoggedIn) {
            Column(
                modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("登录后才能查看投稿", style = MaterialTheme.typography.titleMedium)
                Text(
                    "审核状态与你的投稿内容只对本人可见。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenLogin) { Text("去登录") }
            }
            return@Scaffold
        }

        when {
            loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            error != null -> ErrorBox(
                error!!,
                onRetry = { reload += 1 },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            items.isEmpty() -> Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                EmptyBox("你还没有投稿过情报")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onCreate) { Text("发布情报") }
                    TextButton(onClick = onOpenNews) { Text("Galgame 情报") }
                }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items.size, key = { items[it].id }) { index ->
                    val item = items[index]
                    NewsSubmissionRow(
                        item = item,
                        busy = busyId == item.id,
                        onOpen = { if (item.state == "published") onOpenItem(item.id) },
                        onEdit = { onOpenEditor(item.id) },
                        onWithdraw = { withdrawTarget = item },
                    )
                }
                if (nextCursor != null) {
                    item(key = "more") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                            TextButton(
                                onClick = {
                                    val cursor = nextCursor ?: return@TextButton
                                    if (loadingMore) return@TextButton
                                    loadingMore = true
                                    scope.launch {
                                        runCatching {
                                            app.newsSubmissionApi.mySubmissions(cursor = cursor, limit = 20)
                                        }.onSuccess {
                                            items = items + it.items
                                            nextCursor = it.nextCursor
                                        }.onFailure { AppMessages.show(friendlyError(it)) }
                                        loadingMore = false
                                    }
                                },
                                enabled = !loadingMore,
                            ) { Text(if (loadingMore) "加载中…" else "加载更多") }
                        }
                    }
                }
            }
        }
    }

    withdrawTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { withdrawTarget = null },
            title = { Text("确定撤回这条情报吗？") },
            text = {
                Text("撤回后它会从 Galgame 情报中移除，并且不能再编辑或重新发布。")
            },
            confirmButton = {
                TextButton(onClick = {
                    withdrawTarget = null
                    withdraw(item)
                }) { Text("撤回") }
            },
            dismissButton = {
                TextButton(onClick = { withdrawTarget = null }) { Text("取消") }
            },
        )
    }
}

@Composable
private fun NewsSubmissionRow(
    item: NewsSubmission,
    busy: Boolean,
    onOpen: () -> Unit,
    onEdit: () -> Unit,
    onWithdraw: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NewsStateChip(item.state)
                Text(
                    item.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .then(if (item.state == "published") Modifier.clickable(onClick = onOpen) else Modifier),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                item.banner?.let { banner ->
                    KungalImage(
                        url = banner.url,
                        contentDescription = item.title,
                        modifier = Modifier
                            .width(104.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(8.dp)),
                    )
                }
                Text(
                    item.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    relativeTime(item.publishedAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                if (isNewsSubmissionEditable(item.state)) {
                    TextButton(onClick = onEdit, enabled = !busy) { Text("编辑") }
                }
                if (item.state == "published") {
                    TextButton(onClick = onWithdraw, enabled = !busy) {
                        Text(if (busy) "撤回中…" else "撤回")
                    }
                }
            }
        }
    }
}

/**
 * 发布 / 编辑情报（对应网页 /edit/news 与 /edit/news/{id}）：
 * 标题、导语、16:9 封面（提交时才上传）、Markdown 正文、原文链接。
 */
@Composable
fun NewsEditorScreen(
    app: KungalApp,
    submissionId: String?,
    onBack: () -> Unit,
    onSaved: (created: Boolean) -> Unit,
    onOpenMine: () -> Unit,
) {
    val vm: NewsEditorViewModel = viewModel(key = "news-editor-${submissionId.orEmpty()}") {
        NewsEditorViewModel(app, submissionId)
    }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val submission = state.submission
    var title by remember(submission?.id) { mutableStateOf(submission?.title.orEmpty()) }
    var preview by remember(submission?.id) { mutableStateOf(submission?.preview.orEmpty()) }
    var content by remember(submission?.id) { mutableStateOf(submission?.contentMarkdown.orEmpty()) }
    var sourceUrl by remember(submission?.id) { mutableStateOf(submission?.sourceUrl.orEmpty()) }
    var pendingBanner by remember(submission?.id) { mutableStateOf<PreparedBanner?>(null) }
    var bannerUri by remember(submission?.id) { mutableStateOf<Uri?>(null) }
    var existingBannerUrl by remember(submission?.id) {
        mutableStateOf(submission?.banner?.url.orEmpty())
    }
    var preparingBanner by remember { mutableStateOf(false) }

    val pickBanner = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            preparingBanner = true
            runCatching { withContext(Dispatchers.IO) { prepareNewsBanner(context, uri) } }
                .onSuccess {
                    pendingBanner = it
                    bannerUri = uri
                    existingBannerUrl = ""
                }
                .onFailure { AppMessages.show(it.message ?: friendlyError(it)) }
            preparingBanner = false
        }
    }

    fun removeBanner() {
        pendingBanner = null
        bannerUri = null
        existingBannerUrl = ""
    }

    fun save() {
        val values = NewsFormValues(
            title = title,
            preview = preview,
            contentMarkdown = content,
            sourceUrl = sourceUrl,
        )
        vm.save(
            values = values,
            pendingBanner = pendingBanner,
            removeBanner = submission?.banner != null && existingBannerUrl.isBlank() && pendingBanner == null,
            onResult = { success, created, message ->
                AppMessages.show(message)
                if (success) {
                    // 已上传的封面换成服务端地址，避免再次保存时重复消耗上传额度。
                    pendingBanner = null
                    bannerUri = null
                    existingBannerUrl = vm.state.value.submission?.banner?.url.orEmpty()
                    onSaved(created)
                }
            },
        )
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = if (submissionId == null) "发布 Gal 情报" else "编辑 Gal 情报",
                subtitle = "由 NextMoe 审核员人工审核",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (submissionId == null) {
                        TextButton(onClick = onOpenMine) { Text("我的投稿") }
                    }
                    TextButton(onClick = { save() }, enabled = !state.saving) {
                        if (state.saving) {
                            CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Text(if (submissionId == null) "提交投稿" else "保存修改")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null -> ErrorBox(
                state.error!!,
                onRetry = { vm.retry() },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            submission != null && !isNewsSubmissionEditable(submission.state) -> EmptyBox(
                "这条投稿已经未通过审核或已撤回，不能再编辑",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .imePadding()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                if (submission?.state == "published") {
                    Surface(
                        color = MaterialTheme.colorScheme.secondaryContainer,
                        shape = RoundedCornerShape(12.dp),
                    ) {
                        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(
                                "这条情报已经发布",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium,
                            )
                            Text(
                                "保存修改后它会暂时下线，重新审核通过后再次公开。",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = title,
                    onValueChange = { if (it.length <= NEWS_TITLE_MAX) title = it },
                    label = { Text("标题") },
                    placeholder = { Text("一句话说清这条情报") },
                    singleLine = true,
                    supportingText = { Text("${title.length}/$NEWS_TITLE_MAX") },
                    modifier = Modifier.fillMaxWidth(),
                )

                OutlinedTextField(
                    value = preview,
                    onValueChange = { if (it.length <= NEWS_PREVIEW_MAX) preview = it },
                    label = { Text("导语") },
                    placeholder = { Text("显示在情报列表里的简短介绍") },
                    minLines = 3,
                    supportingText = { Text("${preview.length}/$NEWS_PREVIEW_MAX") },
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("封面（可选）", style = MaterialTheme.typography.titleSmall)
                Text(
                    "显示在情报卡片和详情页顶部，会裁切为 16:9 的横图；封面不可包含 R18 等敏感内容。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedCard(
                    onClick = { if (!preparingBanner) pickBanner.launch("image/*") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    val shape = RoundedCornerShape(12.dp)
                    when {
                        bannerUri != null -> SubcomposeAsyncImage(
                            model = bannerUri,
                            contentDescription = "封面预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape),
                        )
                        existingBannerUrl.isNotBlank() -> KungalImage(
                            url = existingBannerUrl,
                            contentDescription = "封面预览",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(shape),
                        )
                        else -> Box(
                            Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                            contentAlignment = Alignment.Center,
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Icon(
                                    Icons.Outlined.AddPhotoAlternate,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                Text(
                                    if (preparingBanner) "处理中…" else "选择封面",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
                if (bannerUri != null || existingBannerUrl.isNotBlank()) {
                    TextButton(onClick = ::removeBanner) { Text("移除封面") }
                }

                OutlinedTextField(
                    value = content,
                    onValueChange = { if (it.length <= NEWS_CONTENT_MAX) content = it },
                    label = { Text("正文（Markdown，不支持 HTML）") },
                    placeholder = { Text("原创内容写在这里；转载请只写摘要并附上原文链接") },
                    minLines = 8,
                    supportingText = { Text("${content.length}/$NEWS_CONTENT_MAX") },
                    modifier = Modifier.fillMaxWidth().height(240.dp),
                )

                OutlinedTextField(
                    value = sourceUrl,
                    onValueChange = { if (it.length <= NEWS_SOURCE_URL_MAX) sourceUrl = it },
                    label = { Text("原文链接") },
                    placeholder = { Text("https://...") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Uri,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    "正文与原文链接至少填写一项；转载内容建议附上原文链接。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Button(
                    onClick = { save() },
                    enabled = !state.saving,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.saving) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Icon(Icons.Outlined.EditNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (submissionId == null) "提交投稿" else "保存修改")
                    }
                }
                Spacer(Modifier.height(8.dp))
            }
        }
    }
}
