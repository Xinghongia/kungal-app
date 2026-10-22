package com.kungal.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kungal.app.ui.topic.friendlyError
import kotlinx.coroutines.launch

/** 一页结果：items + 下一页游标（页码接口可留空）+ 是否到底 + 总数（页码模式用）。 */
data class PageResult<T>(
    val items: List<T>,
    val nextCursor: String? = null,
    val ended: Boolean = false,
    val total: Int = 0,
)

/** 列表项的兜底 key：接口给出的 key 为空或重复时用它，避免 LazyColumn 直接崩溃。 */
private data class FallbackItemKey(val index: Int)

/**
 * 逐项决定 LazyColumn 的 key：空字符串、null 与重复 key 都退回下标。
 * 返回的 key 保证两两不同。
 */
internal fun pageItemKeys(rawKeys: List<Any?>): List<Any> {
    val used = HashSet<Any>(rawKeys.size)
    return rawKeys.mapIndexed { index, raw ->
        if (raw != null && !(raw is String && raw.isBlank()) && used.add(raw)) {
            raw
        } else {
            FallbackItemKey(index).also { used.add(it) }
        }
    }
}

/**
 * 取第 [index] 项的 key。LazyColumn 的 item provider 可能先于父组合观察到新的 items，
 * 此时 [keys] 还是上一次组合的结果，短于当前项数，必须兜底而不是越界。
 */
internal fun pageItemKeyAt(keys: List<Any>, index: Int): Any =
    keys.getOrNull(index) ?: FallbackItemKey(index)

/**
 * 通用分页列表：首屏加载、触底自动加载（游标模式）/ 页码分页（pagination=true）、
 * 失败重试、空态。
 * key 变化（筛选条件/用户变化）时整体重来，并回到列表顶部。
 * header 会作为列表第一项一起滚动（用于用户主页这类「头部跟随内容滑动」的页面）。
 * 顶部不留 contentPadding，保证与不使用本组件的标签页头部起始位置一致。
 */
@Composable
fun <T> PagedListContent(
    key: Any,
    load: suspend (page: Int, cursor: String?) -> PageResult<T>,
    emptyText: String,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(bottom = 8.dp),
    header: (@Composable () -> Unit)? = null,
    keyOf: (T) -> Any? = { null },
    /** true = 页码式接口：底部显示分页器；false = 游标式接口：触底加载更多。 */
    pagination: Boolean = false,
    pageSize: Int = 24,
    itemContent: @Composable (T) -> Unit,
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var items by remember(key) { mutableStateOf<List<T>>(emptyList()) }
    var loading by remember(key) { mutableStateOf(true) }
    var error by remember(key) { mutableStateOf<String?>(null) }
    var page by remember(key) { mutableStateOf(1) }
    var total by remember(key) { mutableStateOf(0) }
    var cursor by remember(key) { mutableStateOf<String?>(null) }
    var ended by remember(key) { mutableStateOf(false) }
    var reloadToken by remember(key) { mutableStateOf(0) }
    var refreshing by remember(key) { mutableStateOf(false) }

    // 接口偶尔给出空 id / 重复 id；直接交给 LazyColumn 会因 key 冲突崩溃，这里逐项去重后兜底到下标。
    val itemKeys = remember(items) { pageItemKeys(items.map { keyOf(it) }) }

    // 切换筛选条件/二级菜单时回到顶部，避免沿用上一个列表的滚动位置
    LaunchedEffect(key) { listState.scrollToItem(0) }

    LaunchedEffect(key, reloadToken) {
        loading = true
        error = null
        runCatching { load(1, null) }
            .onSuccess { result ->
                items = result.items
                cursor = result.nextCursor
                page = 1
                total = result.total
                ended = result.ended || (result.items.isEmpty() && result.nextCursor == null)
                loading = false
                refreshing = false
            }
            .onFailure { e ->
                error = friendlyError(e)
                loading = false
                refreshing = false
            }
    }

    fun loadMore() {
        if (loading || ended || pagination) return
        loading = true
        scope.launch {
            runCatching { load(page + 1, cursor) }
                .onSuccess { result ->
                    items = items + result.items
                    cursor = result.nextCursor
                    page += 1
                    total = result.total
                    ended = result.ended || (result.items.isEmpty() && result.nextCursor == null)
                    loading = false
                }
                .onFailure { e ->
                    error = friendlyError(e)
                    loading = false
                }
        }
    }

    fun goToPage(target: Int) {
        if (loading || target < 1 || target == page) return
        loading = true
        error = null
        scope.launch {
            runCatching { load(target, null) }
                .onSuccess { result ->
                    items = result.items
                    page = target
                    cursor = result.nextCursor
                    total = result.total
                    ended = result.ended ||
                        (result.items.isEmpty() && result.nextCursor == null)
                    loading = false
                    listState.scrollToItem(0)
                }
                .onFailure { e ->
                    error = friendlyError(e)
                    loading = false
                }
        }
    }

    if (!pagination) {
        LaunchedEffect(listState, ended, cursor) {
            snapshotFlow {
                val last = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1
                val totalItems = listState.layoutInfo.totalItemsCount
                totalItems > 0 && last >= totalItems - 3
            }.collect { nearEnd -> if (nearEnd) loadMore() }
        }
    }

    KungalPullToRefresh(
        isRefreshing = refreshing,
        onRefresh = {
            if (!refreshing) {
                refreshing = true
                reloadToken += 1
            }
        },
    ) {
        LazyColumn(
            state = listState,
            modifier = modifier.fillMaxSize(),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            if (header != null) {
                item(key = "paged-header") { header() }
            }
            if (items.isEmpty()) {
                item(key = "paged-state") {
                    when {
                        loading -> LoadingBox()
                        error != null -> ErrorBox(error!!, onRetry = { reloadToken += 1 })
                        ended -> EmptyBox(emptyText)
                    }
                }
            } else {
                itemsIndexed(items, key = { index, _ -> pageItemKeyAt(itemKeys, index) }) { _, item ->
                    itemContent(item)
                }
                item(key = "paged-footer") {
                    if (pagination) {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            if (loading) {
                                LoadingBox()
                            } else {
                                if (error != null) {
                                    ErrorBox(error!!, onRetry = { goToPage(page) })
                                }
                                PaginationBar(
                                    page = page,
                                    totalPages = pageCount(total, pageSize),
                                    onPageChange = { goToPage(it) },
                                    enabled = !loading,
                                )
                            }
                        }
                    } else {
                        Column(
                            Modifier.fillMaxWidth().padding(vertical = 12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            when {
                                // 下拉刷新时由顶部插图表示进度，页脚不再重复显示加载占位
                                loading && !refreshing -> LoadingBox()
                                error != null -> ErrorBox(error!!, onRetry = { loadMore() })
                                ended -> Text(
                                    "没有更多了",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                else -> TextButton(onClick = { loadMore() }) { Text("加载更多") }
                            }
                        }
                    }
                }
            }
        }
    }
}
