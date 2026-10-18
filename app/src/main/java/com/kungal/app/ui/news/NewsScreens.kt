package com.kungal.app.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Newspaper
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
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
import com.kungal.app.ui.content.ContentDocumentView
import com.kungal.app.ui.galgame.FilterOption
import com.kungal.app.ui.galgame.FilterOptionSheet
import com.kungal.app.ui.galgame.FilterPill
import com.kungal.app.ui.topic.friendlyError
import com.kungal.app.ui.util.fullDateTime
import com.kungal.app.ui.util.relativeTime
import com.kungal.core.model.NewsArchive
import com.kungal.core.model.NewsItem
import com.kungal.core.model.NewsMonth
import com.kungal.core.model.NewsSource

private const val NEWS_MONTH_PAGE_SIZE = 50

private enum class NewsFilterSheet { SOURCE, YEAR, MONTH }

/**
 * 情报总览（BP-05 U5，对应网页 /news）：
 * 全部 / 情报 / 专栏、来源、年份与月份筛选；条目按 (发布日期, 来源) 分组；
 * 游标分页，触底自动加载，也可点「加载更多」。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsListScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenMonth: (year: Int, month: Int, lane: String?, source: String?) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val vm: NewsFeedViewModel = viewModel(key = "news-feed") { NewsFeedViewModel(app) }
    val state by vm.state.collectAsState()

    var lane by rememberSaveable { mutableStateOf("") }
    var source by rememberSaveable { mutableStateOf("") }
    var year by rememberSaveable { mutableIntStateOf(0) }
    var month by rememberSaveable { mutableIntStateOf(0) }
    var sheet by remember { mutableStateOf<NewsFilterSheet?>(null) }

    val filters = NewsFilters(
        lane = lane.ifBlank { null },
        source = source.ifBlank { null },
        year = year.takeIf { it > 0 },
        month = month.takeIf { it > 0 },
    )

    LaunchedEffect(filters) { vm.load(filters) }

    var sources by remember { mutableStateOf<List<NewsSource>>(emptyList()) }
    var archive by remember { mutableStateOf<NewsArchive?>(null) }
    LaunchedEffect(Unit) {
        runCatching { app.newsApi.sources() }.onSuccess { sources = it }
    }
    LaunchedEffect(lane, source, year) {
        archive = runCatching {
            app.newsApi.archive(
                lane = lane.ifBlank { null },
                source = source.ifBlank { null },
                year = year.takeIf { it > 0 },
            )
        }.getOrNull()
    }
    val sourcesByKey = remember(sources) { sources.associateBy { it.key } }

    val listState = rememberLazyListState()
    val shouldLoadMore by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val last = info.visibleItemsInfo.lastOrNull()?.index ?: -1
            last >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(shouldLoadMore) {
        if (shouldLoadMore) vm.loadMore()
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 情报",
                subtitle = "业界新闻、新作情报与专栏",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            state = listState,
            contentPadding = PaddingValues(bottom = 24.dp),
        ) {
            item(key = "lanes") {
                FilterChips(
                    options = listOf("" to "全部", "news" to "情报", "column" to "专栏"),
                    selected = lane,
                    onSelect = {
                        lane = it
                        month = 0
                    },
                )
            }
            item(key = "filters") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    FilterPill(
                        label = sourcesByKey[source]?.displayName ?: "全部来源",
                        active = source.isNotBlank(),
                        onClick = { sheet = NewsFilterSheet.SOURCE },
                    )
                    FilterPill(
                        label = if (year > 0) "$year 年" else "全部年份",
                        active = year > 0,
                        onClick = { sheet = NewsFilterSheet.YEAR },
                    )
                    FilterPill(
                        label = if (month > 0) "$month 月" else "全年",
                        active = month > 0,
                        onClick = { if (year > 0) sheet = NewsFilterSheet.MONTH },
                    )
                    if (filters.isFiltered) {
                        TextButton(
                            onClick = {
                                lane = ""
                                source = ""
                                year = 0
                                month = 0
                            },
                        ) { Text("清除筛选") }
                    }
                }
            }
            if (year > 0 && month > 0) {
                item(key = "month-detail") {
                    TextButton(
                        onClick = {
                            onOpenMonth(
                                year,
                                month,
                                lane.ifBlank { null },
                                source.ifBlank { null },
                            )
                        },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) { Text("查看 $year 年 $month 月详情") }
                }
            }
            if (!state.loading && state.error == null) {
                item(key = "count") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            "共 ${state.total} 条情报",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        if (year > 0) {
                            Chip(if (month > 0) "$year 年 $month 月" else "$year 年")
                        }
                    }
                }
            }

            when {
                state.loading -> item(key = "loading") { LoadingBox() }
                state.error != null && state.items.isEmpty() -> item(key = "error") {
                    ErrorBox(state.error!!, onRetry = { vm.retry() })
                }
                state.items.isEmpty() -> item(key = "empty") {
                    EmptyBox("没有符合条件的情报")
                }
                else -> {
                    newsGroups(state.items).forEachIndexed { index, group ->
                        item(key = "group-$index-${group.key}") {
                            NewsGroupHeader(
                                group = group,
                                source = sourcesByKey[group.sourceKey],
                                onOpenUrl = onOpenUrl,
                            )
                        }
                        items(group.items, key = { it.id }) { item ->
                            NewsCard(
                                item = item,
                                sourceName = newsSourceName(sourcesByKey[item.newsSource]),
                                onOpenItem = onOpenItem,
                                onOpenUrl = onOpenUrl,
                            )
                        }
                    }
                    item(key = "footer") {
                        NewsFooter(
                            loadingMore = state.loadingMore,
                            ended = state.ended,
                            error = state.error,
                            onLoadMore = { vm.loadMore() },
                        )
                    }
                }
            }

            if (sources.isNotEmpty()) {
                item(key = "sources") {
                    NewsSourcesSection(sources = sources, onOpenUrl = onOpenUrl)
                }
            }
        }
    }

    when (sheet) {
        NewsFilterSheet.SOURCE -> FilterOptionSheet(
            title = "来源",
            options = sources.map { FilterOption(it.key, it.displayName) },
            selected = setOfNotNull(source.ifBlank { null }),
            multi = false,
            onToggle = { value ->
                source = if (source == value) "" else value
                month = 0
            },
            onClear = { source = ""; month = 0 },
            onDismiss = { sheet = null },
        )
        NewsFilterSheet.YEAR -> FilterOptionSheet(
            title = "年份",
            options = archive?.years.orEmpty().map { FilterOption(it.year.toString(), "${it.year} 年 · ${it.count}") },
            selected = if (year > 0) setOf(year.toString()) else emptySet(),
            multi = false,
            onToggle = { value ->
                val next = value.toIntOrNull() ?: 0
                year = if (year == next) 0 else next
                month = 0
            },
            onClear = { year = 0; month = 0 },
            onDismiss = { sheet = null },
        )
        NewsFilterSheet.MONTH -> FilterOptionSheet(
            title = "$year 年月份",
            options = archive?.months.orEmpty().map { FilterOption(it.month.toString(), "${it.month} 月 · ${it.count}") },
            selected = if (month > 0) setOf(month.toString()) else emptySet(),
            multi = false,
            onToggle = { value ->
                val next = value.toIntOrNull() ?: 0
                month = if (month == next) 0 else next
            },
            onClear = { month = 0 },
            onDismiss = { sheet = null },
        )
        null -> Unit
    }
}

/** 月归档（对应网页 /news/{year}/{month}）：月份切换、按日筛选、页码分页。 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun NewsMonthScreen(
    app: KungalApp,
    year: Int,
    month: Int,
    lane: String?,
    source: String?,
    onBack: () -> Unit,
    onOpenItem: (String) -> Unit,
    onOpenMonth: (year: Int, month: Int, lane: String?, source: String?) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    var summary by remember(year, month, lane, source) { mutableStateOf<NewsMonth?>(null) }
    var archive by remember(year, lane, source) { mutableStateOf<NewsArchive?>(null) }
    var sources by remember { mutableStateOf<List<NewsSource>>(emptyList()) }
    var error by remember(year, month, lane, source) { mutableStateOf<String?>(null) }
    var loading by remember(year, month, lane, source) { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }
    var day by rememberSaveable(year, month) { mutableIntStateOf(0) }

    LaunchedEffect(year, month, lane, source, reload) {
        loading = true
        error = null
        runCatching {
            val monthData = app.newsApi.month(year, month, lane = lane, source = source)
            val archiveData = app.newsApi.archive(lane = lane, source = source, year = year)
            summary = monthData
            archive = archiveData
        }.onFailure { error = friendlyError(it) }
        loading = false
    }
    LaunchedEffect(Unit) {
        runCatching { app.newsApi.sources() }.onSuccess { sources = it }
    }
    val sourcesByKey = remember(sources) { sources.associateBy { it.key } }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "$year 年 $month 月情报",
                subtitle = "Galgame 业界新闻存档",
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
            else -> PagedListContent(
                key = listOf(year, month, lane, source, day),
                load = { page, _ ->
                    val result = app.newsApi.monthItems(
                        year = year,
                        month = month,
                        page = page,
                        limit = NEWS_MONTH_PAGE_SIZE,
                        lane = lane,
                        source = source,
                        day = day.takeIf { it > 0 },
                    )
                    PageResult(
                        items = newsRows(result.items),
                        ended = page * NEWS_MONTH_PAGE_SIZE >= result.total,
                        total = result.total,
                    )
                },
                emptyText = if (day > 0) "这一天没有情报" else "这个月还没有收录情报",
                modifier = Modifier.fillMaxSize().padding(padding),
                pagination = true,
                pageSize = NEWS_MONTH_PAGE_SIZE,
                keyOf = { row ->
                    when (row) {
                        is NewsListRow.Header -> "h-${row.group.items.first().id}"
                        is NewsListRow.Item -> "i-${row.item.id}"
                    }
                },
                header = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!archive?.months.isNullOrEmpty()) {
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                archive!!.months.forEach { entry ->
                                    FilterChip(
                                        selected = entry.month == month,
                                        enabled = entry.count > 0,
                                        onClick = {
                                            if (entry.month != month) {
                                                onOpenMonth(year, entry.month, lane, source)
                                            }
                                        },
                                        label = {
                                            Text(
                                                "${entry.month} 月 · ${entry.count}",
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        val days = summary?.days.orEmpty().filter { it.count > 0 }
                        if (days.isNotEmpty()) {
                            Text(
                                "按发布日期",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                            FlowRow(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                FilterChip(
                                    selected = day == 0,
                                    onClick = { day = 0 },
                                    label = {
                                        Text(
                                            "整月 ${summary?.itemCount ?: 0}",
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    },
                                )
                                days.forEach { entry ->
                                    FilterChip(
                                        selected = day == entry.day,
                                        onClick = { day = if (day == entry.day) 0 else entry.day },
                                        label = {
                                            Text(
                                                "${entry.day} 日 · ${entry.count}",
                                                style = MaterialTheme.typography.labelSmall,
                                            )
                                        },
                                    )
                                }
                            }
                        }
                        if (day > 0) {
                            Text(
                                "$month 月 $day 日",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(horizontal = 16.dp),
                            )
                        }
                        val shownCount = if (day > 0) {
                            summary?.days?.firstOrNull { it.day == day }?.count ?: 0
                        } else {
                            summary?.itemCount ?: 0
                        }
                        Text(
                            "共 $shownCount 条情报",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
                        )
                    }
                },
            ) { row ->
                when (row) {
                    is NewsListRow.Header -> NewsGroupHeader(
                        group = row.group,
                        source = sourcesByKey[row.group.sourceKey],
                        onOpenUrl = onOpenUrl,
                    )
                    is NewsListRow.Item -> NewsCard(
                        item = row.item,
                        sourceName = newsSourceName(sourcesByKey[row.item.newsSource]),
                        onOpenItem = onOpenItem,
                        onOpenUrl = onOpenUrl,
                    )
                }
            }
        }
    }
}

/** 情报详情（对应网页 /news/{id}）：横幅、署名、导语、本站正文与原文链接。 */
@Composable
fun NewsDetailScreen(
    app: KungalApp,
    newsItemId: String,
    onBack: () -> Unit,
    onOpenUrl: (String) -> Unit,
    onOpenUser: (String) -> Unit,
) {
    var item by remember(newsItemId) { mutableStateOf<NewsItem?>(null) }
    var sources by remember { mutableStateOf<List<NewsSource>>(emptyList()) }
    var error by remember(newsItemId) { mutableStateOf<String?>(null) }
    var loading by remember(newsItemId) { mutableStateOf(true) }
    var reload by remember { mutableStateOf(0) }

    LaunchedEffect(newsItemId, reload) {
        loading = true
        error = null
        runCatching { app.newsApi.item(newsItemId) }
            .onSuccess { item = it }
            .onFailure { error = friendlyError(it) }
        runCatching { app.newsApi.sources() }.onSuccess { sources = it }
        loading = false
    }
    val source = remember(item, sources) {
        item?.let { current -> sources.firstOrNull { it.key == current.newsSource } }
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "情报详情",
                subtitle = source?.displayName,
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
            item == null -> EmptyBox(
                "未找到该情报",
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> {
                val news = item!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    val banner = news.banner
                    if (banner?.url?.isNotBlank() == true) {
                        val ratio = if ((banner.width ?: 0) > 0 && (banner.height ?: 0) > 0) {
                            banner.width!!.toFloat() / banner.height!!
                        } else {
                            16f / 9f
                        }
                        KungalImage(
                            url = banner.url,
                            contentDescription = news.title,
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(ratio.coerceIn(0.5f, 2.5f))
                                .clip(RoundedCornerShape(12.dp)),
                        )
                    }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (news.lane == "column") Chip("专栏")
                        Chip(newsSourceName(source))
                    }
                    Text(
                        news.title,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        news.submitter?.let { user ->
                            AvatarImage(
                                url = user.avatar?.url,
                                seed = user.name,
                                modifier = Modifier.size(20.dp),
                            )
                            Text(
                                user.name.orEmpty(),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable {
                                    if (user.id.isNotBlank()) onOpenUser(user.id)
                                },
                            )
                        }
                        Text(
                            fullDateTime(news.publishedAt),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (news.preview.isNotBlank()) {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
                            Box(
                                Modifier
                                    .width(3.dp)
                                    .fillMaxHeight()
                                    .background(MaterialTheme.colorScheme.primary),
                            )
                            Text(
                                news.preview,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                    if (news.content.children.isNotEmpty()) {
                        ContentDocumentView(news.content)
                    }
                    if (news.sourceUrl.isNotBlank() || !source?.attribution.isNullOrBlank()) {
                        HorizontalDivider()
                        if (news.sourceUrl.isNotBlank()) {
                            TextButton(
                                onClick = { onOpenUrl(news.sourceUrl) },
                                contentPadding = PaddingValues(horizontal = 4.dp),
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Outlined.OpenInNew,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(if (news.hasBody) "原文链接" else "阅读原文")
                            }
                        }
                        source?.attribution?.takeIf { it.isNotBlank() }?.let {
                            Text(
                                it,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
    }
}

/** 日期 + 来源抬头：同一天同一来源的条目共用。 */
@Composable
private fun NewsGroupHeader(
    group: NewsGroup,
    source: NewsSource?,
    onOpenUrl: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                Icons.Outlined.Newspaper,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Text(
                group.date,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
            )
            Text(
                relativeTime(group.items.first().publishedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.weight(1f))
            val sourceName = newsSourceName(source)
            val homepage = source?.homepageUrl.orEmpty()
            Text(
                sourceName,
                style = MaterialTheme.typography.labelSmall,
                color = if (homepage.isNotBlank()) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = if (homepage.isNotBlank()) {
                    Modifier.clickable { onOpenUrl(homepage) }
                } else {
                    Modifier
                },
            )
        }
        source?.attribution?.takeIf { it.isNotBlank() }?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** 情报卡片：横幅 + 标题 + 导语 + 时间/来源/动作。 */
@Composable
private fun NewsCard(
    item: NewsItem,
    sourceName: String,
    onOpenItem: (String) -> Unit,
    onOpenUrl: (String) -> Unit,
) {
    val target = newsTarget(item)
    val open: () -> Unit = when (target) {
        NewsItemTarget.DETAIL -> ({ onOpenItem(item.id) })
        NewsItemTarget.SOURCE -> ({ onOpenUrl(item.sourceUrl) })
        NewsItemTarget.NONE -> ({})
    }
    Card(
        onClick = open,
        enabled = target != NewsItemTarget.NONE,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            val banner = item.banner
            if (banner != null) {
                KungalImage(
                    url = banner.url,
                    contentDescription = item.title,
                    modifier = Modifier
                        .width(104.dp)
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(8.dp)),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (item.lane == "column") Chip("专栏")
                    Text(
                        item.title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    item.preview,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        relativeTime(item.publishedAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        "·",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        item.submitter?.name ?: sourceName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    if (target != NewsItemTarget.NONE) {
                        Text(
                            if (target == NewsItemTarget.DETAIL) "阅读全文" else "阅读原文",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun NewsFooter(
    loadingMore: Boolean,
    ended: Boolean,
    error: String?,
    onLoadMore: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        when {
            loadingMore -> LoadingBox()
            error != null -> {
                Text(
                    error,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                TextButton(onClick = onLoadMore) { Text("重试") }
            }
            ended -> Text(
                "没有更多情报了",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            else -> TextButton(onClick = onLoadMore) { Text("加载更多") }
        }
    }
}

/** 网页移动端末尾的「情报来源」卡片。 */
@Composable
private fun NewsSourcesSection(
    sources: List<NewsSource>,
    onOpenUrl: (String) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            "情报来源",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Medium,
        )
        sources.forEachIndexed { index, source ->
            if (index > 0) HorizontalDivider()
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    source.displayName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = if (source.homepageUrl.isNotBlank()) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    modifier = if (source.homepageUrl.isNotBlank()) {
                        Modifier.clickable { onOpenUrl(source.homepageUrl) }
                    } else {
                        Modifier
                    },
                )
                if (source.attribution.isNotBlank()) {
                    Text(
                        source.attribution,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (source.homepageUrl.isNotBlank()) {
                        TextButton(
                            onClick = { onOpenUrl(source.homepageUrl) },
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("站点主页", style = MaterialTheme.typography.labelSmall) }
                    }
                    if (source.columnUrl.isNotBlank()) {
                        TextButton(
                            onClick = { onOpenUrl(source.columnUrl) },
                            contentPadding = PaddingValues(0.dp),
                        ) { Text("专栏", style = MaterialTheme.typography.labelSmall) }
                    }
                }
            }
        }
    }
}
