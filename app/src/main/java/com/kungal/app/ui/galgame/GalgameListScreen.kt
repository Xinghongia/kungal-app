package com.kungal.app.ui.galgame

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Sort
import androidx.compose.material.icons.outlined.ArrowDownward
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.ArrowUpward
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
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
import com.kungal.core.model.WorkCollectedMonth
import com.kungal.core.model.WorkSummary

private enum class FilterSheet { SORT, PLATFORM, RUNTIME, GAME_TYPE, RESOURCE_TYPE, LANGUAGE, MORE, YEARS }

/**
 * Galgame 资料库列表（BP-02 U1）：
 * mode = RESOURCE 对应网页 /galgame（有本站资源的作品），
 * mode = LIBRARY 对应网页 /gallib（资料库全量作品）。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalgameListScreen(
    app: KungalApp,
    mode: GalgameListMode,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
) {
    val includeNsfw = nsfwDisplay.allowsNsfw
    val vm: GalgameListViewModel = viewModel(key = "galgame-list-$mode") {
        GalgameListViewModel(app, mode, includeNsfw)
    }
    val state by vm.state.collectAsState()
    val gridState = rememberLazyGridState()
    var sheet by remember { mutableStateOf<FilterSheet?>(null) }
    var yearDialogOpen by remember { mutableStateOf(false) }

    LaunchedEffect(includeNsfw) { vm.setIncludeNsfw(includeNsfw) }

    LaunchedEffect(gridState) {
        snapshotFlow {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
            val total = gridState.layoutInfo.totalItemsCount
            total > 0 && last >= total - 4
        }.collect { nearEnd -> if (nearEnd) vm.loadMore() }
    }

    val resourceMode = mode == GalgameListMode.RESOURCE
    val sortOptions = if (resourceMode) GalgameResourceSortFields else GalgameLibrarySortFields
    val sortLabel = labelOf(sortOptions, state.filters.sortField)
    val orderable = resourceMode || state.filters.sortField == "release_date"

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = if (resourceMode) "Galgame 资源资料库" else "Galgame 信息资料库",
                subtitle = if (resourceMode) {
                    "本站已有下载资源的 Galgame"
                } else {
                    "资料库收录的全部 Galgame"
                },
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
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Column(Modifier.fillMaxSize()) {
                FilterRow(
                    resourceMode = resourceMode,
                    filters = state.filters,
                    sortLabel = sortLabel,
                    orderable = orderable,
                    onOpenSort = { sheet = FilterSheet.SORT },
                    onToggleOrder = {
                        vm.setSortOrder(if (state.filters.sortOrder == "desc") "asc" else "desc")
                    },
                    onOpenPlatform = { sheet = FilterSheet.PLATFORM },
                    onOpenRuntime = { sheet = FilterSheet.RUNTIME },
                    onOpenGameType = { sheet = FilterSheet.GAME_TYPE },
                    onOpenResourceType = { sheet = FilterSheet.RESOURCE_TYPE },
                    onOpenLanguage = { sheet = FilterSheet.LANGUAGE },
                    onOpenMore = { sheet = FilterSheet.MORE },
                    onOpenYears = { sheet = FilterSheet.YEARS },
                    onClear = { vm.resetFilters() },
                )

                if (state.total > 0) {
                    Text(
                        text = if (state.totalRelation == "gte") "共 ${state.total}+ 部作品" else "共 ${state.total} 部作品",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                    )
                }

                LazyVerticalGrid(
                    state = gridState,
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    when {
                        state.items.isEmpty() && state.loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                            LoadingBox()
                        }
                        state.items.isEmpty() && state.error != null -> item(span = { GridItemSpan(maxLineSpan) }) {
                            ErrorBox(state.error!!, onRetry = { vm.refresh() })
                        }
                        state.items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                            EmptyBox("没有找到符合条件的 Galgame")
                        }
                        else -> {
                            items(state.items, key = { it.id }) { work ->
                                WorkCard(
                                    work = work,
                                    blurCover = nsfwDisplay.blursMedia,
                                    onClick = { onOpenWork(work.id) },
                                )
                            }
                            item(span = { GridItemSpan(maxLineSpan) }) {
                                Box(
                                    Modifier.fillMaxWidth().padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    when {
                                        state.loadingMore -> LoadingBox()
                                        state.error != null -> ErrorBox(
                                            state.error!!,
                                            onRetry = { vm.loadMore() },
                                        )
                                        state.endReached -> Text(
                                            "没有更多了",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        else -> TextButton(onClick = { vm.loadMore() }) {
                                            Text("加载更多")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    when (sheet) {
        FilterSheet.SORT -> OptionSheet(
            title = "排序",
            options = sortOptions,
            selected = setOf(state.filters.sortField),
            multi = false,
            includeAll = false,
            onToggle = { vm.setSortField(it) },
            onClear = null,
            onDismiss = { sheet = null },
        )
        FilterSheet.PLATFORM -> OptionSheet(
            title = "平台",
            options = GalgamePlatformOptions,
            selected = state.filters.platforms,
            multi = true,
            onToggle = { vm.togglePlatform(it) },
            onClear = { vm.clearPlatforms() },
            onDismiss = { sheet = null },
        )
        FilterSheet.RUNTIME -> OptionSheet(
            title = "模拟器 / 运行环境",
            options = GalgameEmulatorRuntimeOptions,
            selected = state.filters.runtimes,
            multi = true,
            onToggle = { vm.toggleRuntime(it) },
            onClear = { vm.clearRuntimes() },
            onDismiss = { sheet = null },
        )
        FilterSheet.GAME_TYPE -> OptionSheet(
            title = "游戏类型",
            options = GalgameGameTypeOptions,
            selected = setOfNotNull(state.filters.gameType),
            multi = false,
            onToggle = { vm.setGameType(it) },
            onClear = { vm.setGameType(null) },
            onDismiss = { sheet = null },
        )
        FilterSheet.RESOURCE_TYPE -> OptionSheet(
            title = "资源类型",
            options = GalgameResourceTypeOptions,
            selected = setOfNotNull(state.filters.resourceType),
            multi = false,
            onToggle = { vm.setResourceType(it) },
            onClear = { vm.setResourceType(null) },
            onDismiss = { sheet = null },
        )
        FilterSheet.LANGUAGE -> OptionSheet(
            title = "语言",
            options = GalgameLanguageOptions,
            selected = state.filters.languages,
            multi = true,
            onToggle = { vm.toggleLanguage(it) },
            onClear = { vm.clearLanguages() },
            onDismiss = { sheet = null },
        )
        FilterSheet.MORE -> MoreFilterSheet(
            filters = state.filters,
            collectedMonths = state.collectedMonths,
            onApply = {
                vm.applyAdvanced(it)
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        FilterSheet.YEARS -> YearRangeDialog(
            from = state.filters.releasedFrom,
            to = state.filters.releasedTo,
            onApply = { from, to ->
                vm.applyAdvanced(state.filters.copy(releasedFrom = from, releasedTo = to))
                sheet = null
            },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

@Composable
private fun FilterRow(
    resourceMode: Boolean,
    filters: GalgameFilterState,
    sortLabel: String,
    orderable: Boolean,
    onOpenSort: () -> Unit,
    onToggleOrder: () -> Unit,
    onOpenPlatform: () -> Unit,
    onOpenRuntime: () -> Unit,
    onOpenGameType: () -> Unit,
    onOpenResourceType: () -> Unit,
    onOpenLanguage: () -> Unit,
    onOpenMore: () -> Unit,
    onOpenYears: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilterPill(
            label = sortLabel,
            active = true,
            icon = { Icon(Icons.AutoMirrored.Outlined.Sort, contentDescription = null, modifier = Modifier.size(15.dp)) },
            onClick = onOpenSort,
        )
        if (orderable) {
            FilterPill(
                label = if (filters.sortOrder == "desc") "降序" else "升序",
                active = false,
                icon = {
                    Icon(
                        if (filters.sortOrder == "desc") Icons.Outlined.ArrowDownward
                        else Icons.Outlined.ArrowUpward,
                        contentDescription = null,
                        modifier = Modifier.size(15.dp),
                    )
                },
                onClick = onToggleOrder,
            )
        }

        if (resourceMode) {
            FilterPill(
                label = if (filters.platforms.isEmpty()) "平台" else "平台 ${filters.platforms.size}",
                active = filters.platforms.isNotEmpty(),
                onClick = onOpenPlatform,
            )
            if ("emulator" in filters.platforms) {
                FilterPill(
                    label = if (filters.runtimes.isEmpty()) "模拟器" else "模拟器 ${filters.runtimes.size}",
                    active = filters.runtimes.isNotEmpty(),
                    onClick = onOpenRuntime,
                )
            }
            FilterPill(
                label = filters.gameType?.let { labelOf(GalgameGameTypeOptions, it) } ?: "游戏类型",
                active = filters.gameType != null,
                onClick = onOpenGameType,
            )
            FilterPill(
                label = filters.resourceType?.let { labelOf(GalgameResourceTypeOptions, it) } ?: "资源类型",
                active = filters.resourceType != null,
                onClick = onOpenResourceType,
            )
            FilterPill(
                label = if (filters.languages.isEmpty()) "语言" else "语言 ${filters.languages.size}",
                active = filters.languages.isNotEmpty(),
                onClick = onOpenLanguage,
            )
            FilterPill(
                label = if (filters.advancedCount > 0) "更多筛选 · ${filters.advancedCount}" else "更多筛选",
                active = filters.advancedCount > 0,
                icon = { Icon(Icons.Outlined.Tune, contentDescription = null, modifier = Modifier.size(15.dp)) },
                onClick = onOpenMore,
            )
        } else {
            FilterPill(
                label = yearRangeLabel(filters.releasedFrom, filters.releasedTo) ?: "发售年份",
                active = filters.releasedFrom.isNotBlank() || filters.releasedTo.isNotBlank(),
                onClick = onOpenYears,
            )
        }

        if (filters.hasAnyFilter || filters.advancedCount > 0) {
            FilterPill(label = "清空", active = false, icon = {
                Icon(Icons.Outlined.Close, contentDescription = null, modifier = Modifier.size(15.dp))
            }, onClick = onClear)
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    active: Boolean,
    icon: @Composable () -> Unit = {},
    onClick: () -> Unit,
) {
    FilterChip(
        selected = active,
        onClick = onClick,
        label = { Text(label, style = MaterialTheme.typography.labelMedium) },
        leadingIcon = icon,
        trailingIcon = {
            Icon(Icons.Outlined.ArrowDropDown, contentDescription = null, modifier = Modifier.size(16.dp))
        },
    )
}

@Composable
private fun WorkCard(
    work: WorkSummary,
    blurCover: Boolean,
    onClick: () -> Unit,
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Box {
            if (work.isNsfw && blurCover) {
                NsfwCoverImage(
                    url = work.cover?.url,
                    modifier = Modifier.fillMaxWidth().aspectRatio(5f / 7f),
                )
            } else {
                KungalImage(
                    url = work.cover?.url,
                    contentDescription = workTitle(work),
                    modifier = Modifier.fillMaxWidth().aspectRatio(5f / 7f),
                )
            }
            if (work.isNsfw) {
                Text(
                    text = "NSFW",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onError,
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(5.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 5.dp, vertical = 1.dp),
                )
            }
        }
        Column(Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = workTitle(work),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            companyName(work)?.let { company ->
                Text(
                    text = company,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = releaseText(work),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                ratingText(work)?.let { score ->
                    Icon(
                        Icons.Outlined.Star,
                        contentDescription = null,
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = score,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            val platforms = platformLabels(work)
            if (platforms.isNotEmpty()) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    platforms.take(2).forEach { Chip(it) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun OptionSheet(
    title: String,
    options: List<FilterOption>,
    selected: Set<String>,
    multi: Boolean,
    includeAll: Boolean = true,
    onToggle: (String) -> Unit,
    onClear: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                if (onClear != null) {
                    TextButton(onClick = onClear) { Text("清空") }
                }
                TextButton(onClick = onDismiss) { Text("完成") }
            }
            LazyColumn(Modifier.fillMaxWidth().heightIn(max = 460.dp)) {
                if (!multi && includeAll) {
                    item {
                        OptionRow(
                            label = "全部",
                            selected = selected.isEmpty(),
                            onClick = {
                                onClear?.invoke()
                                onDismiss()
                            },
                        )
                    }
                }
                items(options, key = { it.value }) { option ->
                    OptionRow(
                        label = option.label,
                        selected = option.value in selected,
                        onClick = {
                            onToggle(option.value)
                            if (!multi) onDismiss()
                        },
                    )
                }
                item { Spacer(Modifier.size(16.dp)) }
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "已选中",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoreFilterSheet(
    filters: GalgameFilterState,
    collectedMonths: List<WorkCollectedMonth>,
    onApply: (GalgameFilterState) -> Unit,
    onDismiss: () -> Unit,
) {
    var draft by remember {
        mutableStateOf(
            filters.copy(
                includeProviders = filters.includeProviders,
                excludeOnlyProviders = filters.excludeOnlyProviders,
            ),
        )
    }
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .heightIn(max = 620.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("更多筛选", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.weight(1f))
                TextButton(onClick = {
                    draft = filters.copy(
                        includeProviders = emptySet(),
                        excludeOnlyProviders = emptySet(),
                        minRating = 0,
                        minRatingCount = 0,
                        releasedFrom = "",
                        releasedTo = "",
                        releasedMonths = emptySet(),
                        collectedFrom = "",
                        collectedTo = "",
                        collectedMonths = emptySet(),
                        includeAllOriginalLanguages = false,
                    )
                }) { Text("清空") }
            }

            SectionTitle("最低评分")
            ChipRow(GalgameRatingFilterOptions, selected = setOf(draft.minRating.toString())) { value ->
                draft = draft.copy(minRating = value.toIntOrNull() ?: 0)
            }

            SectionTitle("最少评分人数")
            ChipRow(GalgameRatingCountFilterOptions, selected = setOf(draft.minRatingCount.toString())) { value ->
                draft = draft.copy(minRatingCount = value.toIntOrNull() ?: 0)
            }

            SectionTitle("发售年份")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = draft.releasedFrom,
                    onValueChange = { draft = draft.copy(releasedFrom = it.filter(Char::isDigit).take(4)) },
                    label = { Text("起 (YYYY)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                OutlinedTextField(
                    value = draft.releasedTo,
                    onValueChange = { draft = draft.copy(releasedTo = it.filter(Char::isDigit).take(4)) },
                    label = { Text("止 (YYYY)") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
            }
            SectionTitle("发售月份（可多选）")
            MonthChips(
                selected = draft.releasedMonths,
                onToggle = { month ->
                    draft = draft.copy(
                        releasedMonths = if (month in draft.releasedMonths) {
                            draft.releasedMonths - month
                        } else {
                            draft.releasedMonths + month
                        },
                    )
                },
            )

            if (collectedMonths.isNotEmpty()) {
                val years = collectedMonths.map { it.year }.distinct().sortedDescending()
                val selectedYear = draft.collectedFrom.toIntOrNull()
                SectionTitle("收录年份（单选）")
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    years.forEach { year ->
                        val value = year.toString()
                        FilterChip(
                            selected = draft.collectedFrom == value,
                            onClick = {
                                val next = if (draft.collectedFrom == value) "" else value
                                // 与网页一致：按收录时间浏览时把排序切到收录时间
                                draft = draft.copy(
                                    collectedFrom = next,
                                    collectedTo = next,
                                    collectedMonths = emptySet(),
                                    sortField = if (next.isNotBlank() && draft.sortField == "time") {
                                        "created"
                                    } else {
                                        draft.sortField
                                    },
                                )
                            },
                            label = { Text("$year 年") },
                        )
                    }
                }
                if (selectedYear != null) {
                    val months = collectedMonths
                        .filter { it.year == selectedYear }
                        .map { it.month }
                        .distinct()
                        .sorted()
                    SectionTitle("收录月份（可多选）")
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        months.forEach { month ->
                            FilterChip(
                                selected = month in draft.collectedMonths,
                                onClick = {
                                    draft = draft.copy(
                                        collectedMonths = if (month in draft.collectedMonths) {
                                            draft.collectedMonths - month
                                        } else {
                                            draft.collectedMonths + month
                                        },
                                    )
                                },
                                label = { Text("$month 月") },
                            )
                        }
                    }
                }
            }

            SectionTitle("内容提供商（含）")
            ChipRow(GalgameProviderOptions, selected = draft.includeProviders) { value ->
                draft = draft.copy(
                    includeProviders = if (value in draft.includeProviders) {
                        draft.includeProviders - value
                    } else {
                        draft.includeProviders + value
                    },
                )
            }
            SectionTitle("排除只有这些提供商的项目")
            ChipRow(GalgameProviderOptions, selected = draft.excludeOnlyProviders) { value ->
                draft = draft.copy(
                    excludeOnlyProviders = if (value in draft.excludeOnlyProviders) {
                        draft.excludeOnlyProviders - value
                    } else {
                        draft.excludeOnlyProviders + value
                    },
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("显示非日语/中文原语言作品", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "默认只显示原始语言为日语或中文的作品",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = draft.includeAllOriginalLanguages,
                    onCheckedChange = { draft = draft.copy(includeAllOriginalLanguages = it) },
                )
            }

            Button(
                onClick = { onApply(draft) },
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
            ) { Text("应用筛选") }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun ChipRow(
    options: List<FilterOption>,
    selected: Set<String>,
    onToggle: (String) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        options.forEach { option ->
            FilterChip(
                selected = option.value in selected,
                onClick = { onToggle(option.value) },
                label = { Text(option.label) },
            )
        }
    }
}

@Composable
private fun MonthChips(selected: Set<Int>, onToggle: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        (1..12).forEach { month ->
            FilterChip(
                selected = month in selected,
                onClick = { onToggle(month) },
                label = { Text("$month 月") },
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearRangeDialog(
    from: String,
    to: String,
    onApply: (String, String) -> Unit,
    onDismiss: () -> Unit,
) {
    var fromDraft by remember { mutableStateOf(from) }
    var toDraft by remember { mutableStateOf(to) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("发售年份") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = fromDraft,
                    onValueChange = { fromDraft = it.filter(Char::isDigit).take(4) },
                    label = { Text("起始年份 (YYYY)") },
                    singleLine = true,
                )
                OutlinedTextField(
                    value = toDraft,
                    onValueChange = { toDraft = it.filter(Char::isDigit).take(4) },
                    label = { Text("结束年份 (YYYY)") },
                    singleLine = true,
                )
                Text(
                    "留空表示不限",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onApply(fromDraft, toDraft) }) { Text("应用") }
        },
        dismissButton = {
            TextButton(onClick = { onApply("", "") }) { Text("清空") }
        },
    )
}

private val GalgameRatingFilterOptions = listOf(
    FilterOption("0", "评分不限"),
    FilterOption("7", "7 分+"),
    FilterOption("8", "8 分+"),
    FilterOption("9", "9 分+"),
)

private val GalgameRatingCountFilterOptions = listOf(
    FilterOption("0", "人数不限"),
    FilterOption("5", "≥5 人"),
    FilterOption("10", "≥10 人"),
    FilterOption("20", "≥20 人"),
    FilterOption("50", "≥50 人"),
)

private fun yearRangeLabel(from: String, to: String): String? = when {
    from.isNotBlank() && to.isNotBlank() -> if (from == to) "$from 年" else "$from - $to"
    from.isNotBlank() -> "$from 年至今"
    to.isNotBlank() -> "$to 年以前"
    else -> null
}
