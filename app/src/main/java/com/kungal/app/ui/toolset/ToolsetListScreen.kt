package com.kungal.app.ui.toolset

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.ChatBubbleOutline
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
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
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.PaginationBar
import com.kungal.app.ui.components.pageCount
import com.kungal.app.ui.galgame.FilterOption
import com.kungal.app.ui.galgame.FilterOptionSheet
import com.kungal.app.ui.galgame.FilterPill
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.ToolsetSummary
import kotlinx.coroutines.delay

private enum class ToolsetSheet { TYPE, PLATFORM, RELEASE, SORT }

/**
 * 工具集列表（BP-03 U4，对应网页 /toolset）：搜索 + 类型/平台/版本筛选 +
 * 排序字段与升降序 + 24/页。
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun ToolsetListScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenToolset: (String) -> Unit,
    onCreate: () -> Unit,
    onOpenLogin: () -> Unit,
) {
    val vm: ToolsetListViewModel = viewModel(key = "toolset-list") { ToolsetListViewModel(app) }
    val state by vm.state.collectAsState()
    val listState = rememberLazyListState()
    var search by rememberSaveable { mutableStateOf("") }
    var sheet by remember { mutableStateOf<ToolsetSheet?>(null) }

    LaunchedEffect(search) {
        delay(250)
        vm.setQuery(search)
    }
    LaunchedEffect(state.query, state.type, state.platform, state.release, state.sortField, state.sortOrder) {
        if (listState.firstVisibleItemIndex > 0) listState.scrollToItem(0)
    }
    LaunchedEffect(listState, state.page) {
        if (state.page > 1 && listState.firstVisibleItemIndex > 0) {
            listState.scrollToItem(0)
        }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 工具资源",
                subtitle = if (state.total > 0) "共 ${state.total} 个工具" else null,
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { if (app.isLoggedIn) onCreate() else onOpenLogin() },
                    ) {
                        Icon(Icons.Outlined.Add, contentDescription = "发布工具")
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
                            placeholder = { Text("模糊名字查询") },
                            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                            trailingIcon = {
                                if (search.isNotEmpty()) {
                                    IconButton(onClick = { search = "" }) {
                                        Icon(Icons.Outlined.Clear, contentDescription = "清空")
                                    }
                                }
                            },
                        )
                    }
                }
                item(key = "filters") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(horizontal = 12.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        FilterPill(
                            label = state.type?.let { labelOfToolset(ToolsetTypeOptions, it) } ?: "全部类型",
                            active = state.type != null,
                            onClick = { sheet = ToolsetSheet.TYPE },
                        )
                        FilterPill(
                            label = state.platform?.let { labelOfToolset(ToolsetPlatformOptions, it) } ?: "全部平台",
                            active = state.platform != null,
                            onClick = { sheet = ToolsetSheet.PLATFORM },
                        )
                        FilterPill(
                            label = state.release?.let { labelOfToolset(ToolsetReleaseOptions, it) } ?: "全部版本",
                            active = state.release != null,
                            onClick = { sheet = ToolsetSheet.RELEASE },
                        )
                        FilterPill(
                            label = labelOfToolset(ToolsetSortFields, state.sortField),
                            active = state.sortField != "resource_updated",
                            onClick = { sheet = ToolsetSheet.SORT },
                        )
                        FilterPill(
                            label = if (state.sortOrder == "desc") "降序" else "升序",
                            active = false,
                            icon = {
                                Icon(
                                    if (state.sortOrder == "desc") Icons.Outlined.ArrowDownward
                                    else Icons.Outlined.ArrowUpward,
                                    contentDescription = null,
                                    modifier = Modifier.size(15.dp),
                                )
                            },
                            onClick = {
                                vm.setSortOrder(if (state.sortOrder == "desc") "asc" else "desc")
                            },
                        )
                        TextButton(onClick = { vm.resetFilters() }) {
                            Text("重置", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                when {
                    state.items.isEmpty() && state.loading -> item(key = "loading") { LoadingBox() }
                    state.items.isEmpty() && state.error != null -> item(key = "error") {
                        ErrorBox(state.error!!, onRetry = { vm.retry() })
                    }
                    state.items.isEmpty() -> item(key = "empty") {
                        EmptyBox("没有找到符合条件的工具资源")
                    }
                    else -> {
                        items(state.items, key = { it.id }) { toolset ->
                            ToolsetCard(toolset = toolset, onClick = { onOpenToolset(toolset.id) })
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
                                            ToolsetListViewModel.PAGE_SIZE,
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

    when (sheet) {
        ToolsetSheet.TYPE -> OptionSheet(
            title = "工具类型",
            options = ToolsetTypeOptions,
            selected = state.type,
            onSelect = { vm.setType(it); sheet = null },
            onDismiss = { sheet = null },
        )
        ToolsetSheet.PLATFORM -> OptionSheet(
            title = "平台",
            options = ToolsetPlatformOptions,
            selected = state.platform,
            onSelect = { vm.setPlatform(it); sheet = null },
            onDismiss = { sheet = null },
        )
        ToolsetSheet.RELEASE -> OptionSheet(
            title = "版本",
            options = ToolsetReleaseOptions,
            selected = state.release,
            onSelect = { vm.setRelease(it); sheet = null },
            onDismiss = { sheet = null },
        )
        ToolsetSheet.SORT -> OptionSheet(
            title = "排序字段",
            options = ToolsetSortFields,
            selected = state.sortField,
            onSelect = { value -> vm.setSortField(value ?: "resource_updated"); sheet = null },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

@Composable
private fun OptionSheet(
    title: String,
    options: List<FilterOption>,
    selected: String?,
    onSelect: (String?) -> Unit,
    onDismiss: () -> Unit,
) {
    FilterOptionSheet(
        title = title,
        options = options,
        selected = selected?.let { setOf(it) } ?: emptySet(),
        multi = false,
        onToggle = { value -> onSelect(if (selected == value) null else value) },
        onClear = { onSelect(null) },
        onDismiss = onDismiss,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ToolsetCard(toolset: ToolsetSummary, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        onClick = onClick,
    ) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(Modifier.weight(1f)) {
                    Text(
                        toolset.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        "${labelOfToolset(ToolsetTypeOptions, toolset.toolsetType)} · " +
                            labelOfToolset(ToolsetReleaseOptions, toolset.releaseChannel),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                toolset.practicalityAverage?.let { average ->
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        String.format(java.util.Locale.US, "%.1f", average),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Chip(labelOfToolset(ToolsetPlatformOptions, toolset.platform))
                Chip(labelOfToolset(ToolsetLanguageOptions, toolset.interfaceLanguage))
                toolset.aliases.take(1).forEach { Chip(it) }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                AvatarImage(
                    url = toolset.author.avatar?.url,
                    seed = toolset.author.name,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    toolset.author.name ?: "已注销用户",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Stat(Icons.Outlined.Visibility, toolset.viewCount)
                Spacer(Modifier.width(8.dp))
                Stat(Icons.Outlined.Download, toolset.downloadCount)
                if (toolset.commentCount > 0) {
                    Spacer(Modifier.width(8.dp))
                    Stat(Icons.Outlined.ChatBubbleOutline, toolset.commentCount)
                }
            }
        }
    }
}

@Composable
private fun Stat(icon: androidx.compose.ui.graphics.vector.ImageVector, value: Int) {
    Icon(
        icon,
        contentDescription = null,
        modifier = Modifier.size(13.dp),
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.width(2.dp))
    Text(
        "$value",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

/** 工具集发布入口的占位提示（真正的表单在 U5 接入）。 */
@Composable
fun ToolsetCreatePlaceholder(onBack: () -> Unit) {
    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "发布 Galgame 工具",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        EmptyBox("工具发布表单将在下一批接入", Modifier.fillMaxSize().padding(padding))
    }
}
