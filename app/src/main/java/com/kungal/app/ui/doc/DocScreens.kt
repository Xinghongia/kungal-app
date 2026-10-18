package com.kungal.app.ui.doc

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalImage
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.ContentDocument
import com.kungal.core.model.Doc
import kotlinx.coroutines.launch

private const val DOC_PAGE_SIZE = 100
private const val DOC_MAX_PAGES = 20

/** 文档全量拉取：列表页按 position_asc，详情页的索引/上下篇按 published_desc。 */
internal suspend fun loadAllDocs(app: KungalApp, sort: String): List<Doc> {
    val all = mutableListOf<Doc>()
    var cursor: String? = null
    var page = 0
    do {
        val result = app.docApi.list(cursor = cursor, limit = DOC_PAGE_SIZE, sort = sort)
        all += result.items
        cursor = result.nextCursor
        page++
    } while (cursor != null && page < DOC_MAX_PAGES)
    return all
}

/** 帮助文档列表（BP-05 U9，对应网页 /doc）：分类筛选在本地做，卡片进详情。 */
@Composable
fun DocListScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenDoc: (String) -> Unit,
) {
    var docs by remember { mutableStateOf<List<Doc>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var reload by remember { mutableIntStateOf(0) }
    var category by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(reload) {
        if (!refreshing) loading = true
        error = null
        runCatching { loadAllDocs(app, "position_asc") }
            .onSuccess { docs = it }
            .onFailure {
                if (refreshing) AppMessages.show(friendlyError(it)) else error = friendlyError(it)
            }
        loading = false
        refreshing = false
    }

    val visible = if (category.isBlank()) docs else docs.filter { it.docCategory == category }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 帮助文档",
                subtitle = "发布、交流与资源相关问题的说明",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            error != null -> ErrorBox(
                error!!,
                onRetry = { reload++ },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> KungalPullToRefresh(
                isRefreshing = refreshing,
                onRefresh = {
                    if (!refreshing) {
                        refreshing = true
                        reload++
                    }
                },
                modifier = Modifier.fillMaxSize().padding(padding),
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    item(key = "filters") {
                        Column {
                            FilterChips(
                                options = listOf("" to "全部") +
                                    DocCategoryOptions.map { it.value to it.label },
                                selected = category,
                                onSelect = { category = it },
                            )
                            Text(
                                "${visible.size} 篇文档",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                            )
                        }
                    }
                    if (visible.isEmpty()) {
                        item(key = "empty") { EmptyBox("暂时没有找到任何文档") }
                    } else {
                        items(visible, key = { it.slug }) { doc ->
                            DocCard(
                                doc = doc,
                                onClick = { onOpenDoc(doc.slug) },
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DocCard(doc: Doc, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = modifier.fillMaxWidth(),
    ) {
        KungalImage(
            url = doc.banner?.url,
            contentDescription = doc.title,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
        )
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(docCategoryLabel(doc.docCategory))
                Text(
                    doc.publishedAt.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (doc.isPinned) Chip("置顶")
            }
            Text(
                doc.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            if (doc.description.isNotBlank()) {
                Text(
                    doc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * 文档详情（网页 /doc/{slug}）：横幅、分类与浏览数、目录、正文、上一篇/下一篇；
 * 右上角「文档索引」按分类列出全部文档，可直接切换。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocDetailScreen(
    app: KungalApp,
    slug: String,
    onBack: () -> Unit,
    onOpenDoc: (String) -> Unit,
) {
    var doc by remember(slug) { mutableStateOf<Doc?>(null) }
    var docs by remember(slug) { mutableStateOf<List<Doc>>(emptyList()) }
    var loading by remember(slug) { mutableStateOf(true) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember(slug) { mutableStateOf<String?>(null) }
    var reload by remember(slug) { mutableIntStateOf(0) }
    var showIndex by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(slug, reload) {
        if (!refreshing) loading = true
        error = null
        runCatching {
            val detail = app.docApi.detail(slug)
            val all = runCatching { loadAllDocs(app, "published_desc") }.getOrDefault(emptyList())
            detail to all
        }.onSuccess { (detail, all) ->
            doc = detail
            docs = all
        }.onFailure {
            if (refreshing && doc != null) AppMessages.show(friendlyError(it)) else error = friendlyError(it)
        }
        loading = false
        refreshing = false
    }

    val headings = docHeadings(doc)
    val nodes = doc?.content?.children.orEmpty()

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = doc?.title ?: "帮助文档",
                subtitle = doc?.let { docCategoryLabel(it.docCategory) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    if (doc != null) {
                        IconButton(onClick = { showIndex = true }) {
                            Icon(Icons.Outlined.MenuBook, contentDescription = "文档索引")
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            loading && doc == null -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            error != null && doc == null -> ErrorBox(
                error!!,
                onRetry = { reload++ },
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            doc == null -> EmptyBox(
                "未找到该文档",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> {
                val data = doc!!
                KungalPullToRefresh(
                    isRefreshing = refreshing,
                    onRefresh = {
                        if (!refreshing) {
                            refreshing = true
                            reload++
                        }
                    },
                    modifier = Modifier.fillMaxSize().padding(padding),
                ) {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 28.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        item(key = "header") {
                            DocHeader(
                                doc = data,
                                headings = headings,
                                onHeadingClick = { heading ->
                                    scope.launch {
                                        listState.animateScrollToItem(1 + heading.blockIndex)
                                    }
                                },
                            )
                        }
                        itemsIndexed(nodes, key = { index, _ -> "doc-block-$index" }) { _, node ->
                            ContentDocumentView(
                                document = ContentDocument(children = listOf(node)),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            )
                        }
                        item(key = "footer") {
                            val (prev, next) = docNeighbors(docs, slug)
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                HorizontalDivider(Modifier.padding(bottom = 4.dp))
                                prev?.let {
                                    OutlinedButton(
                                        onClick = { onOpenDoc(it.slug) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            "上一篇 · ${it.title}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                                next?.let {
                                    OutlinedButton(
                                        onClick = { onOpenDoc(it.slug) },
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(
                                            "下一篇 · ${it.title}",
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showIndex) {
        ModalBottomSheet(onDismissRequest = { showIndex = false }) {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                Text(
                    "文档索引",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 520.dp)) {
                    DocCategoryOptions.forEach { option ->
                        val list = docs.filter { it.docCategory == option.value }
                        if (list.isNotEmpty()) {
                            item(key = "index-cat-${option.value}") {
                                Text(
                                    option.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                                )
                            }
                            items(list, key = { it.slug }) { item ->
                                val current = item.slug == slug
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable {
                                            showIndex = false
                                            if (!current) onOpenDoc(item.slug)
                                        }
                                        .background(
                                            if (current) {
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
                                            } else {
                                                androidx.compose.ui.graphics.Color.Transparent
                                            },
                                        )
                                        .padding(horizontal = 8.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = if (current) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f),
                                    )
                                    if (current) {
                                        Text(
                                            "当前",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.padding(bottom = 20.dp)) }
                }
            }
        }
    }
}

@Composable
private fun DocHeader(
    doc: Doc,
    headings: List<DocHeading>,
    onHeadingClick: (DocHeading) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        KungalImage(
            url = doc.banner?.url,
            contentDescription = doc.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .aspectRatio(16f / 9f)
                .clip(RoundedCornerShape(12.dp)),
        )
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                doc.title,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Chip(docCategoryLabel(doc.docCategory))
                Text(
                    "${doc.viewCount} 次浏览",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                doc.author.name?.takeIf { it.isNotBlank() }?.let { name ->
                    Text(
                        name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Text(
                buildString {
                    append("发布于 ${doc.publishedAt.take(10)}")
                    doc.editedAt?.takeIf { it.isNotBlank() }?.let { append(" · 编辑于 ${it.take(10)}") }
                },
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (doc.description.isNotBlank()) {
                Text(
                    doc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .padding(10.dp),
                )
            }
        }
        if (headings.isNotEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        "文档目录",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    val minDepth = headings.minOf { it.depth }
                    headings.forEach { heading ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onHeadingClick(heading) }
                                .padding(
                                    start = (8 + (heading.depth - minDepth).coerceIn(0, 3) * 12).dp,
                                    top = 7.dp,
                                    bottom = 7.dp,
                                    end = 8.dp,
                                ),
                        ) {
                            Text(
                                heading.text,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}
