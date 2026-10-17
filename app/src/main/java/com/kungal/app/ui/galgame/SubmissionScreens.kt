package com.kungal.app.ui.galgame

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.WorkSubmissionCandidate
import com.kungal.core.model.WorkSubmissionSummary
import com.kungal.core.model.claimStateLabel
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

private const val SEARCH_LIMIT = 12

@Composable
fun ClaimStateChip(state: String) {
    val color = claimStateColor(state)
    Text(
        text = claimStateLabel(state),
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(color.copy(alpha = 0.14f))
            .padding(horizontal = 8.dp, vertical = 2.dp),
    )
}

@Composable
private fun claimStateColor(state: String): Color = when (state) {
    "live" -> MaterialTheme.colorScheme.tertiary
    "draft" -> MaterialTheme.colorScheme.primary
    "pending" -> MaterialTheme.colorScheme.secondary
    "declined" -> MaterialTheme.colorScheme.error
    else -> MaterialTheme.colorScheme.onSurfaceVariant
}

private fun candidateTitle(hit: WorkSubmissionCandidate): String =
    catalogTitle(hit.workSummary.displayName, hit.workSummary.localized, hit.workSummary.id)

// ---------- ① 发布 Galgame：搜索查重 ----------

/**
 * 发布向导第一屏（对齐网页 /edit/galgame/publish）：
 * 先搜索资料库，已有作品打开词条；确实没有才进入新建申请。
 */
@Composable
fun SubmissionWizardScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenWork: (String) -> Unit,
    onOpenEdit: (String) -> Unit,
    onOpenMySubmissions: () -> Unit,
    onCreateNew: (String) -> Unit,
) {
    var query by remember { mutableStateOf("") }
    var searchedQuery by remember { mutableStateOf("") }
    var hasSearched by remember { mutableStateOf(false) }
    var searching by remember { mutableStateOf(false) }
    var searchFailed by remember { mutableStateOf(false) }
    var hits by remember { mutableStateOf<List<WorkSubmissionCandidate>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loadingMore by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<List<WorkSubmissionSummary>>(emptyList()) }
    var pendingFailed by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    fun search() {
        val text = query.trim()
        if (text.isEmpty()) {
            AppMessages.show("请先输入关键词")
            return
        }
        if (searching) return
        searching = true
        scope.launch {
            val result = runCatching {
                coroutineScope {
                    val found = async {
                        app.submissionApi.candidates(
                            query = text,
                            includeNsfw = nsfwDisplay.allowsNsfw,
                            limit = SEARCH_LIMIT,
                        )
                    }
                    val mine = async {
                        runCatching {
                            app.submissionApi.mySubmissions(
                                states = listOf("pending", "declined"),
                                limit = SEARCH_LIMIT,
                            )
                        }
                    }
                    found.await() to mine.await()
                }
            }
            searching = false
            result.onSuccess { (found, mine) ->
                searchFailed = false
                pendingFailed = mine.isFailure
                pending = mine.getOrNull()?.items.orEmpty()
                hasSearched = true
                searchedQuery = text
                hits = found.items
                nextCursor = found.nextCursor
            }.onFailure {
                searchFailed = true
                hasSearched = false
                AppMessages.show(friendlyError(it))
            }
        }
    }

    fun loadMore() {
        val cursor = nextCursor ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            runCatching {
                app.submissionApi.candidates(
                    query = searchedQuery,
                    includeNsfw = nsfwDisplay.allowsNsfw,
                    cursor = cursor,
                    limit = SEARCH_LIMIT,
                )
            }.onSuccess {
                hits = hits + it.items
                nextCursor = it.nextCursor
            }.onFailure { AppMessages.show(friendlyError(it)) }
            loadingMore = false
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "发布 Galgame",
                subtitle = "先查重，再决定发布资源或新建申请",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onOpenMySubmissions) { Text("我的提交") }
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
                Text("登录后才能发布 Galgame", style = MaterialTheme.typography.titleMedium)
                Text(
                    "没有账号条目时需要登录后提交申请并等待审核。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenLogin) { Text("去登录") }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 24.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        "① 搜索是否已存在",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "先搜索你想发布资源的游戏。资料库已有的作品直接打开详情页发布资源；" +
                            "确实没有的再新建申请。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { query = it },
                        placeholder = { Text("输入游戏名（任意语言）") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { search() }),
                        modifier = Modifier.weight(1f),
                    )
                    Button(onClick = { search() }, enabled = !searching) {
                        if (searching) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("搜索")
                        }
                    }
                }
            }
            item {
                Text(
                    "搜索覆盖资料库中的全部游戏；你自己的待审核 / 已拒绝投稿也会列在这里。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (searchFailed) {
                item {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Text(
                            "资料库暂时无法搜索，请稍后重试。在确认没有同名作品之前，请不要新建申请。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(12.dp),
                        )
                    }
                }
            }

            if (hasSearched) {
                if (pendingFailed) {
                    item {
                        Text(
                            "你的待审核 / 已拒绝投稿暂时读取失败，请到「我的提交」查看，以免重复提交。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
                if (pending.isNotEmpty()) {
                    item {
                        Text(
                            "你的待审 / 已拒草稿",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(pending.size) { index ->
                        val item = pending[index]
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Row(
                                Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Text(
                                            item.displayName.ifBlank { "#${item.workId}" },
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        ClaimStateChip(item.state)
                                    }
                                    item.lastEvent?.note?.takeIf { it.isNotBlank() }?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                TextButton(onClick = { onOpenEdit(item.workId) }) { Text("继续编辑") }
                            }
                        }
                    }
                }
                if (hits.isNotEmpty()) {
                    item {
                        Text(
                            "匹配的 Galgame",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                    items(hits.size) { index ->
                        val hit = hits[index]
                        CandidateRow(
                            hit = hit,
                            blurCover = nsfwDisplay.blursMedia,
                            onOpenWork = { onOpenWork(hit.workSummary.id) },
                        )
                    }
                    if (nextCursor != null) {
                        item {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                                TextButton(onClick = { loadMore() }, enabled = !loadingMore) {
                                    if (loadingMore) {
                                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                                        Spacer(Modifier.width(8.dp))
                                    }
                                    Text("加载更多")
                                }
                            }
                        }
                    }
                }
                if (pending.isEmpty() && !pendingFailed && hits.isEmpty()) {
                    item {
                        Surface(
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            shape = RoundedCornerShape(14.dp),
                        ) {
                            Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("没有找到匹配的 Galgame", fontWeight = FontWeight.Medium)
                                Text(
                                    "确认确实没有后，用下方「新建 Galgame 申请」提交。",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            item {
                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                Column(
                    Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "② 都没有？新建申请",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "仅用于 VNDB / 资料库未收录的原创 / 同人 / 独立作品。提交后进入审核队列，审核通过才会公开。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        Button(onClick = { onCreateNew(query.trim()) }) { Text("新建 Galgame 申请") }
                    }
                }
            }
        }
    }
}

@Composable
private fun CandidateRow(
    hit: WorkSubmissionCandidate,
    blurCover: Boolean,
    onOpenWork: () -> Unit,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(14.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val image = hit.workSummary.banner?.url ?: hit.workSummary.cover?.url
            if (hit.workSummary.isNsfw && blurCover) {
                com.kungal.app.ui.components.NsfwCoverImage(
                    url = image,
                    modifier = Modifier
                        .width(112.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp)),
                )
            } else {
                KungalImage(
                    url = image,
                    contentDescription = candidateTitle(hit),
                    modifier = Modifier
                        .width(112.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(10.dp)),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(
                    candidateTitle(hit),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                hit.workSummary.releaseDate?.takeIf { it.isNotBlank() }?.let {
                    Text(
                        it,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (hit.workSummary.isNsfw) Chip("NSFW")
            }
            if (hit.state == "pending") {
                Text(
                    "他人投稿审核中",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TextButton(onClick = onOpenWork) { Text("查看 / 发布资源") }
            }
        }
    }
}

// ---------- 我的提交 ----------

/** 我的 Galgame 提交：状态、拒绝原因，以及继续编辑 / 提交审核 / 撤回 / 删除。 */
@Composable
fun MySubmissionsScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenLogin: () -> Unit,
    onOpenEdit: (String) -> Unit,
    onCreateNew: () -> Unit,
) {
    var items by remember { mutableStateOf<List<WorkSubmissionSummary>>(emptyList()) }
    var nextCursor by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var busyId by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableStateOf(0) }
    var withdrawTarget by remember { mutableStateOf<WorkSubmissionSummary?>(null) }
    var deleteTarget by remember { mutableStateOf<WorkSubmissionSummary?>(null) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(reload) {
        if (!app.isLoggedIn) return@LaunchedEffect
        loading = true
        error = null
        runCatching {
            app.submissionApi.mySubmissions(
                states = listOf("pending", "declined", "draft"),
                limit = 20,
            )
        }.onSuccess {
            items = it.items
            nextCursor = it.nextCursor
        }.onFailure { error = friendlyError(it) }
        loading = false
    }

    fun act(item: WorkSubmissionSummary, successMessage: String, block: suspend (etag: String?) -> Unit) {
        if (busyId != null) return
        busyId = item.workId
        scope.launch {
            val version = runCatching { app.submissionApi.submission(item.workId).etag }
            if (version.isFailure) {
                AppMessages.show(friendlyError(version.exceptionOrNull()!!))
            } else {
                runCatching { block(version.getOrNull()) }
                    .onSuccess {
                        AppMessages.show(successMessage)
                        reload += 1
                    }
                    .onFailure { AppMessages.show(friendlyError(it)) }
            }
            busyId = null
        }
    }

    fun submitForReview(item: WorkSubmissionSummary) = act(item, "已提交审核") { etag ->
        app.submissionApi.updateState(item.workId, "pending", ifMatch = etag)
    }

    fun withdraw(item: WorkSubmissionSummary) = act(item, "已撤回") { etag ->
        app.submissionApi.updateState(item.workId, "draft", ifMatch = etag)
    }

    fun delete(item: WorkSubmissionSummary) = act(item, "已删除") { etag ->
        app.submissionApi.deleteDraft(item.workId, ifMatch = etag)
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "我的 Galgame 提交",
                subtitle = "审核中 / 已拒绝 / 草稿",
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
                Text("登录后才能查看提交", style = MaterialTheme.typography.titleMedium)
                Text(
                    "审核状态与拒绝原因只对提交者本人可见。",
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
                EmptyBox("还没有提交过 Galgame 申请")
                Button(onClick = onCreateNew) { Text("新建 Galgame 申请") }
            }
            else -> LazyColumn(
                Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    start = 12.dp,
                    end = 12.dp,
                    top = 4.dp,
                    bottom = 24.dp,
                ),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(items.size) { index ->
                    val item = items[index]
                    SubmissionRow(
                        item = item,
                        busy = busyId == item.workId,
                        onEdit = { onOpenEdit(item.workId) },
                        onSubmit = { submitForReview(item) },
                        onWithdraw = { withdrawTarget = item },
                        onDelete = { deleteTarget = item },
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
                                            app.submissionApi.mySubmissions(
                                                states = listOf("pending", "declined", "draft"),
                                                cursor = cursor,
                                                limit = 20,
                                            )
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
            title = { Text("确定撤回这条申请吗？") },
            text = {
                Text("撤回后该条目会退回草稿状态，不再公开展示。你填写的内容不会丢失，随时可以重新提交。")
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

    deleteTarget?.let { item ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("确定删除这条草稿吗？") },
            text = {
                Text("草稿会被彻底移除，无法恢复。已经发布过资源的条目不会被删除。")
            },
            confirmButton = {
                TextButton(onClick = {
                    deleteTarget = null
                    delete(item)
                }) { Text("删除") }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("取消") }
            },
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubmissionRow(
    item: WorkSubmissionSummary,
    busy: Boolean,
    onEdit: () -> Unit,
    onSubmit: () -> Unit,
    onWithdraw: () -> Unit,
    onDelete: () -> Unit,
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
                Text(
                    item.displayName.ifBlank { "#${item.workId}" },
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ClaimStateChip(item.state)
            }

            val firstAt = item.firstActedAt
            val lastAt = item.lastEvent?.createdAt
            if (!firstAt.isNullOrBlank() || !lastAt.isNullOrBlank()) {
                val parts = buildList {
                    firstAt?.takeIf { it.isNotBlank() }?.let { add("提交于 ${relativeTime(it)}") }
                    lastAt?.takeIf { it.isNotBlank() && it != firstAt }?.let {
                        add("最后处理 ${relativeTime(it)}")
                    }
                }
                Text(
                    parts.joinToString(" · "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (item.state == "declined") {
                item.lastEvent?.let { event ->
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Text(
                            "由 ${event.actor.name ?: "已注销用户"} 拒绝" +
                                (event.note?.takeIf { it.isNotBlank() }?.let { "，原因：$it" } ?: ""),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
                        )
                    }
                }
            }

            FlowRow(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp, Alignment.End),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (busy) {
                    CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(4.dp))
                }
                TextButton(onClick = onEdit, enabled = !busy) { Text("编辑") }
                if (item.viewer?.canSubmit == true) {
                    TextButton(onClick = onSubmit, enabled = !busy) {
                        Text(if (item.state == "declined") "重新提交" else "提交审核")
                    }
                }
                if (item.viewer?.canWithdraw == true) {
                    TextButton(onClick = onWithdraw, enabled = !busy) { Text("撤回") }
                }
                if (item.viewer?.canDelete == true) {
                    TextButton(onClick = onDelete, enabled = !busy) { Text("删除") }
                }
            }
        }
    }
}
