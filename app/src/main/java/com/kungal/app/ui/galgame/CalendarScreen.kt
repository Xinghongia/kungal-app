package com.kungal.app.ui.galgame

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Undo
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.NsfwDisplayMode
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.EmptyBox
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalPullToRefresh
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.core.model.ReleaseCalendarMonth
import com.kungal.core.model.ReleaseCalendarUpcoming
import com.kungal.core.model.WorkSummary
import kotlinx.coroutines.launch

/** 月历顶部信息条：图标 + 标题 + 数量。 */
@Composable
private fun CalendarInfoHeader(icon: ImageVector, title: String, count: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium)
        Text(
            count,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** 两列作品网格（与网页 GalgameCard 列表一致）。 */
@Composable
private fun WorkGrid(
    works: List<WorkSummary>,
    blurCover: Boolean,
    onOpenWork: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        works.chunked(2).forEach { pair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                pair.forEach { work ->
                    Box(Modifier.weight(1f)) {
                        GalgameWorkCard(
                            work = work,
                            blurCover = blurCover,
                            onClick = { onOpenWork(work.id) },
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** 懒加载版本：一次 item 放两列，长列表也能正常回收。 */
private fun LazyListScope.workGridItems(
    works: List<WorkSummary>,
    keyPrefix: String,
    blurCover: Boolean,
    onOpenWork: (String) -> Unit,
) {
    items(
        items = works.chunked(2),
        key = { pair -> "$keyPrefix-${pair.joinToString("-") { it.id }}" },
    ) { pair ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            pair.forEach { work ->
                Box(Modifier.weight(1f)) {
                    GalgameWorkCard(
                        work = work,
                        blurCover = blurCover,
                        onClick = { onOpenWork(work.id) },
                    )
                }
            }
            if (pair.size == 1) Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
fun CalendarScreen(
    app: KungalApp,
    nsfwDisplay: NsfwDisplayMode,
    onBack: () -> Unit,
    onOpenWork: (String) -> Unit,
    initialView: String = "",
    initialMonth: String? = null,
    initialYear: Int? = null,
    initialDoujin: Boolean = false,
) {
    val vm: CalendarViewModel = viewModel(key = "galgame-calendar") {
        CalendarViewModel(
            app = app,
            initialView = CalendarView.fromWire(initialView),
            initialMonth = initialMonth,
            initialYear = initialYear,
            includeNsfw = nsfwDisplay.allowsNsfw,
            includeDoujin = initialDoujin,
        )
    }
    val state by vm.state.collectAsState()
    val blurCover = nsfwDisplay.blursMedia

    LaunchedEffect(nsfwDisplay.allowsNsfw) { vm.setIncludeNsfw(nsfwDisplay.allowsNsfw) }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "Galgame 发售月历",
                subtitle = "按发售月份浏览新作 · 月份按日本时间划分",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            FilterChips(
                options = CalendarView.entries.map { it.wire to it.label },
                selected = state.view.wire,
                onSelect = { vm.selectView(CalendarView.fromWire(it)) },
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text("包含同人作品", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "默认只显示原语言为日语的商业作品，开启后加入同人社团的作品",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = state.includeDoujin,
                    onCheckedChange = vm::setIncludeDoujin,
                )
            }
            KungalPullToRefresh(
                isRefreshing = state.refreshing,
                onRefresh = vm::refresh,
                modifier = Modifier.fillMaxWidth().weight(1f),
            ) {
                when (state.view) {
                    CalendarView.MONTH -> MonthView(
                        state = state,
                        blurCover = blurCover,
                        onOpenWork = onOpenWork,
                        onPrev = vm::prevMonth,
                        onNext = vm::nextMonth,
                        onToday = vm::goToday,
                        onRetry = vm::retry,
                    )
                    CalendarView.UPCOMING -> UpcomingView(
                        state = state,
                        blurCover = blurCover,
                        onOpenWork = onOpenWork,
                        onRetry = vm::retry,
                    )
                    CalendarView.PENDING -> PendingView(
                        state = state,
                        blurCover = blurCover,
                        onOpenWork = onOpenWork,
                        onPrevYear = vm::prevYear,
                        onNextYear = vm::nextYear,
                        onRetry = vm::retry,
                    )
                    CalendarView.TBA -> TbaView(
                        state = state,
                        blurCover = blurCover,
                        onOpenWork = onOpenWork,
                        onRetry = vm::retry,
                    )
                }
            }
        }
    }
}

private const val MONTH_HEADER_ITEMS = 2

@Composable
private fun MonthView(
    state: CalendarState,
    blurCover: Boolean,
    onOpenWork: (String) -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
    onRetry: () -> Unit,
) {
    val data = state.month
    if (data == null) {
        if (state.error != null) ErrorBox(state.error, onRetry = onRetry) else LoadingBox()
        return
    }
    val rows = remember(data) { calendarDayRows(data) }
    val counts = remember(rows) { calendarDayCounts(rows) }
    val bucketCount = remember(rows) { rows.firstOrNull { it.isBucket }?.items?.size ?: 0 }
    val todayMonth = data.calendarMonth == data.todayDate.take(7)
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var selected by rememberSaveable(data.calendarMonth) { mutableStateOf<Int?>(null) }

    fun scrollTo(rowIndex: Int) {
        if (rowIndex < 0) return
        scope.launch { listState.animateScrollToItem(MONTH_HEADER_ITEMS + rowIndex) }
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "month-nav") {
            Column {
                MonthNavHeader(
                    data = data,
                    showToday = !todayMonth,
                    onPrev = onPrev,
                    onNext = onNext,
                    onToday = onToday,
                )
                if (state.error != null) ErrorBox(state.error, onRetry = onRetry)
            }
        }
        item(key = "month-grid") {
            MonthGrid(
                data = data,
                counts = counts,
                bucketCount = bucketCount,
                selected = selected,
                todayDay = if (todayMonth) data.todayDate.drop(8).take(2).toIntOrNull() else null,
                onPickDay = { day ->
                    selected = day
                    scrollTo(rows.indexOfFirst { !it.isBucket && it.day == day })
                },
                onPickBucket = {
                    selected = 0
                    scrollTo(rows.indexOfFirst { it.isBucket })
                },
            )
        }
        if (rows.isEmpty()) {
            item(key = "empty") { EmptyBox("本月暂无发售的 Galgame") }
        } else {
            itemsIndexed(
                items = rows,
                key = { _, row -> if (row.isBucket) "bucket" else "day-${row.day}" },
            ) { _, row ->
                DaySection(row = row, blurCover = blurCover, onOpenWork = onOpenWork)
            }
        }
        item(key = "bottom") { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun MonthNavHeader(
    data: ReleaseCalendarMonth,
    showToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onToday: () -> Unit,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onPrev, enabled = data.hasPrev) {
                Icon(Icons.Outlined.ChevronLeft, contentDescription = "上个月")
            }
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    calendarMonthLabel(data.calendarMonth),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    "共 ${data.itemCount} 部",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onNext, enabled = data.hasNext) {
                Icon(Icons.Outlined.ChevronRight, contentDescription = "下个月")
            }
        }
        if (data.isTruncated) {
            Text(
                "本月作品过多，未全部列出",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }
        if (showToday) {
            TextButton(
                onClick = onToday,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.Undo,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(4.dp))
                Text("回到本月")
            }
        }
    }
}

@Composable
private fun MonthGrid(
    data: ReleaseCalendarMonth,
    counts: Map<Int, Int>,
    bucketCount: Int,
    selected: Int?,
    todayDay: Int?,
    onPickDay: (Int) -> Unit,
    onPickBucket: () -> Unit,
) {
    val cells = remember(data.calendarMonth) { calendarMonthCells(data.calendarMonth) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Row(Modifier.fillMaxWidth()) {
            calendarWeekdays.forEach { label ->
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.weight(1f).padding(vertical = 4.dp),
                )
            }
        }
        cells.chunked(7).forEach { week ->
            Row(Modifier.fillMaxWidth()) {
                week.forEach { day ->
                    if (day == null) {
                        Spacer(Modifier.weight(1f).height(48.dp))
                    } else {
                        DayCell(
                            day = day,
                            count = counts[day] ?: 0,
                            isToday = day == todayDay,
                            isSelected = selected == day,
                            onClick = { onPickDay(day) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
        }
        if (bucketCount > 0) {
            Surface(
                onClick = onPickBucket,
                shape = RoundedCornerShape(12.dp),
                color = if (selected == 0) {
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                } else {
                    MaterialTheme.colorScheme.surfaceContainerLow
                },
                border = if (selected == 0) {
                    BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        Icons.Outlined.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text("本月内 · 日期待定", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.weight(1f))
                    Text(
                        "$bucketCount 部",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    day: Int,
    count: Int,
    isToday: Boolean,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val background = when {
        isToday -> MaterialTheme.colorScheme.primary
        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
        else -> Color.Transparent
    }
    val border = when {
        isToday -> null
        isSelected -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
        count > 0 -> BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        else -> null
    }
    val contentColor = when {
        isToday -> MaterialTheme.colorScheme.onPrimary
        count > 0 -> MaterialTheme.colorScheme.onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
    }
    Surface(
        onClick = onClick,
        enabled = count > 0,
        shape = RoundedCornerShape(10.dp),
        color = background,
        border = border,
        modifier = modifier.height(48.dp).padding(2.dp),
    ) {
        Box(Modifier.fillMaxSize()) {
            Text(
                "$day",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal,
                color = contentColor,
                modifier = Modifier.align(Alignment.Center),
            )
            if (count > 0) {
                Text(
                    "$count",
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    modifier = Modifier.align(Alignment.TopEnd).padding(top = 2.dp, end = 5.dp),
                )
            }
        }
    }
}

@Composable
private fun DaySection(
    row: CalendarDayRow,
    blurCover: Boolean,
    onOpenWork: (String) -> Unit,
) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Column(
            modifier = Modifier.width(62.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val container = when (row.status) {
                CalendarDayStatus.TODAY -> MaterialTheme.colorScheme.primary
                CalendarDayStatus.FUTURE -> Color.Transparent
                CalendarDayStatus.PAST -> MaterialTheme.colorScheme.surfaceContainerHigh
            }
            val content = when (row.status) {
                CalendarDayStatus.TODAY -> MaterialTheme.colorScheme.onPrimary
                CalendarDayStatus.FUTURE -> MaterialTheme.colorScheme.primary
                CalendarDayStatus.PAST -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            val border = if (row.status == CalendarDayStatus.FUTURE && !row.isBucket) {
                BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
            } else {
                null
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = container,
                border = border,
                modifier = Modifier.size(52.dp),
            ) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    if (row.isBucket) {
                        Icon(
                            Icons.Outlined.CalendarMonth,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = content,
                        )
                    } else {
                        Text(
                            "${row.day}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = content,
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Text(
                if (row.isBucket) "日期待定" else row.weekday,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (row.status == CalendarDayStatus.TODAY && !row.isBucket) {
                Spacer(Modifier.height(4.dp))
                Chip("今日")
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            WorkGrid(works = row.items, blurCover = blurCover, onOpenWork = onOpenWork)
        }
    }
}

@Composable
private fun UpcomingView(
    state: CalendarState,
    blurCover: Boolean,
    onOpenWork: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val data: ReleaseCalendarUpcoming? = state.upcoming
    if (data == null) {
        if (state.error != null) ErrorBox(state.error, onRetry = onRetry) else LoadingBox()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "upcoming-header") {
            Box(Modifier.padding(horizontal = 12.dp)) {
                CalendarInfoHeader(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "未发售 · 已定档排期",
                    count = "共 ${data.itemCount} 部",
                )
            }
        }
        if (state.error != null) {
            item(key = "upcoming-error") { ErrorBox(state.error, onRetry = onRetry) }
        }
        if (data.entries.isEmpty()) {
            item(key = "upcoming-empty") { EmptyBox("暂无已定档的未发售作品") }
        } else {
            data.entries.forEach { entry ->
                item(key = "upcoming-${entry.calendarMonth}-header") {
                    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Text(
                                calendarMonthLabel(entry.calendarMonth),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                "${entry.items.size} 部",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        if (entry.isTruncated) {
                            Text(
                                "本月作品过多，未全部列出",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                workGridItems(
                    works = entry.items,
                    keyPrefix = "upcoming-${entry.calendarMonth}",
                    blurCover = blurCover,
                    onOpenWork = onOpenWork,
                )
            }
        }
    }
}

@Composable
private fun PendingView(
    state: CalendarState,
    blurCover: Boolean,
    onOpenWork: (String) -> Unit,
    onPrevYear: () -> Unit,
    onNextYear: () -> Unit,
    onRetry: () -> Unit,
) {
    val data = state.pending
    if (data == null) {
        if (state.error != null) ErrorBox(state.error, onRetry = onRetry) else LoadingBox()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "pending-header") {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onPrevYear) {
                    Icon(Icons.Outlined.ChevronLeft, contentDescription = "上一年")
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "${data.year} 年",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        "仅知年份 · 月份待定 · 共 ${data.itemCount} 部",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                IconButton(onClick = onNextYear) {
                    Icon(Icons.Outlined.ChevronRight, contentDescription = "下一年")
                }
            }
            if (data.isTruncated) {
                Text(
                    "作品过多，未全部列出",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
        if (state.error != null) {
            item(key = "pending-error") { ErrorBox(state.error, onRetry = onRetry) }
        }
        if (data.items.isEmpty()) {
            item(key = "pending-empty") { EmptyBox("该年暂无仅知年份的待定作品") }
        } else {
            workGridItems(
                works = data.items,
                keyPrefix = "pending-${data.year}",
                blurCover = blurCover,
                onOpenWork = onOpenWork,
            )
        }
    }
}

@Composable
private fun TbaView(
    state: CalendarState,
    blurCover: Boolean,
    onOpenWork: (String) -> Unit,
    onRetry: () -> Unit,
) {
    val data = state.tba
    if (data == null) {
        if (state.error != null) ErrorBox(state.error, onRetry = onRetry) else LoadingBox()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 4.dp, bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item(key = "tba-header") {
            Box(Modifier.padding(horizontal = 12.dp)) {
                CalendarInfoHeader(
                    icon = Icons.Outlined.CalendarMonth,
                    title = "发售日期未定",
                    count = "共 ${data.itemCount} 部",
                )
            }
            if (data.isTruncated) {
                Text(
                    "作品过多，未全部列出",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                )
            }
        }
        if (state.error != null) {
            item(key = "tba-error") { ErrorBox(state.error, onRetry = onRetry) }
        }
        if (data.items.isEmpty()) {
            item(key = "tba-empty") { EmptyBox("暂无发售日期未定的作品") }
        } else {
            workGridItems(
                works = data.items,
                keyPrefix = "tba",
                blurCover = blurCover,
                onOpenWork = onOpenWork,
            )
        }
    }
}
