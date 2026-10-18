package com.kungal.app.ui.galgame

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.NsfwCoverImage
import com.kungal.app.ui.components.PaginationBar
import com.kungal.app.ui.components.pageCount
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.GalgameResource
import kotlinx.coroutines.delay

/**
 * 全站 Galgame 资源列表（BP-03 U1，对应网页 /galgame/resource）。
 * 搜索按网页 500ms 防抖；分页 50/页；NSFW 跟随内容姿态。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ResourceListScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenResource: (GalgameResource) -> Unit,
) {
    val vm: ResourceListViewModel = viewModel(key = "resource-list") {
        ResourceListViewModel(app, nsfwDisplay.allowsNsfw)
    }
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var search by rememberSaveable { mutableStateOf("") }
    val blurMedia = nsfwDisplay.blursMedia

    LaunchedEffect(nsfwDisplay.allowsNsfw) {
        vm.setIncludeNsfw(nsfwDisplay.allowsNsfw)
    }
    LaunchedEffect(search) {
        delay(500)
        vm.setQuery(search)
    }
    LaunchedEffect(state.query) {
        if (listState.firstVisibleItemIndex > 0) listState.scrollToItem(0)
    }
    LaunchedEffect(state.page) {
        if (state.page > 1 && listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 资源下载",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        KungalPullToRefresh(
            isRefreshing = state.refreshing,
            onRefresh = { vm.refresh() },
            modifier = Modifier.padding(padding),
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item(key = "search") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)) {
                        OutlinedTextField(
                            value = search,
                            onValueChange = { search = it },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            placeholder = { Text("搜索资源备注或 Galgame 名称…") },
                            leadingIcon = {
                                Icon(Icons.Outlined.Search, contentDescription = null)
                            },
                            trailingIcon = {
                                if (search.isNotEmpty()) {
                                    IconButton(onClick = { search = "" }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = "清空")
                                    }
                                }
                            },
                        )
                        if (!state.loading || state.items.isNotEmpty()) {
                            Text(
                                totalText(state),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                            )
                        }
                    }
                }

                when {
                    state.items.isEmpty() && state.loading -> item(key = "loading") { LoadingBox() }
                    state.items.isEmpty() && state.error != null -> item(key = "error") {
                        ErrorBox(state.error!!, onRetry = { vm.retry() })
                    }
                    state.items.isEmpty() -> item(key = "empty") {
                        EmptyBox(
                            if (state.query.isBlank()) "暂无 Galgame 资源" else "没有找到匹配的 Galgame 资源",
                        )
                    }
                    else -> {
                        items(state.items, key = { it.id }) { resource ->
                            ResourceCard(
                                resource = resource,
                                blurNsfw = blurMedia,
                                onClick = { onOpenResource(resource) },
                            )
                        }
                        item(key = "footer") {
                            Column(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                if (state.loading) {
                                    LoadingBox()
                                } else {
                                    if (state.error != null) {
                                        ErrorBox(
                                            state.error!!,
                                            onRetry = { vm.goToPage(state.page) },
                                        )
                                    }
                                    PaginationBar(
                                        page = state.page,
                                        totalPages = pageCount(
                                            state.total,
                                            ResourceListViewModel.PAGE_SIZE,
                                        ),
                                        onPageChange = vm::goToPage,
                                        enabled = !state.loading,
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

private fun totalText(state: ResourceListState): String {
    val gte = state.totalRelation == "gte"
    val count = if (gte) "${state.total}+" else "${state.total}"
    return if (state.query.isBlank()) {
        "总计 $count 个资源"
    } else {
        "搜索结果：$count 个资源"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ResourceCard(
    resource: GalgameResource,
    blurNsfw: Boolean,
    onClick: () -> Unit,
) {
    val work = resource.work
    val title = work?.displayName?.takeIf { it.isNotBlank() }
        ?: resource.title.ifBlank { "资源 #${resource.id}" }
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Row(Modifier.padding(10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                Modifier
                    .width(68.dp)
                    .aspectRatio(5f / 7f)
                    .clip(RoundedCornerShape(8.dp)),
            ) {
                WorkCoverImage(
                    url = work?.cover?.url,
                    isNsfw = work?.isNsfw == true,
                    blurNsfw = blurNsfw,
                    contentDescription = title,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (resource.isExpired) {
                        Spacer(Modifier.width(4.dp))
                        Chip("已失效")
                    }
                }
                if (resource.title.isNotBlank() && work?.displayName?.isNotBlank() == true) {
                    Text(
                        resource.title,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Chip(labelOf(GalgameResourceTypeOptions, resource.resourceType))
                    resource.resourceLanguages.take(1).forEach {
                        Chip(labelOf(GalgameLanguageOptions, it))
                    }
                    resource.resourcePlatforms.take(2).forEach { Chip(platformShortLabel(it)) }
                    resource.resourceRuntimes.take(1).forEach {
                        Chip(labelOf(GalgameEmulatorRuntimeOptions, it))
                    }
                    if (resource.size.isNotBlank()) Chip(resource.size)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Outlined.Download,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${resource.downloadCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(10.dp))
                    Icon(
                        Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        "${resource.viewCount}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (resource.commentCount > 0) {
                        Spacer(Modifier.width(10.dp))
                        Icon(
                            Icons.Outlined.ChatBubbleOutline,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(2.dp))
                        Text(
                            "${resource.commentCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(Modifier.weight(1f))
                    Text(
                        relativeTime(resource.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
