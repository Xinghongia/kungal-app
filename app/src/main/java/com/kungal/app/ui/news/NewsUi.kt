package com.kungal.app.ui.news

import com.kungal.core.model.NewsItem
import com.kungal.core.model.NewsSource
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * 情报流分组：网页按 (发布日期, 来源) 分组（`groupNewsItems`），
 * 同一天同一来源的连续条目共用一个日期抬头。
 */
data class NewsGroup(
    val key: String,
    val date: String,
    val sourceKey: String,
    val items: List<NewsItem>,
)

/** 发布日期（本地时区，yyyy-MM-dd）；解析失败时返回空串。 */
fun newsDate(iso: String): String {
    if (iso.isBlank()) return ""
    return runCatching {
        DateTimeFormatter.ofPattern("yyyy-MM-dd")
            .withZone(ZoneId.systemDefault())
            .format(Instant.parse(iso))
    }.getOrDefault("")
}

/** 按网页规则把扁平的条目列表切成 (日期, 来源) 组。 */
fun newsGroups(items: List<NewsItem>): List<NewsGroup> {
    val groups = mutableListOf<NewsGroup>()
    for (item in items) {
        val date = newsDate(item.publishedAt)
        val key = "$date-${item.newsSource}"
        val last = groups.lastOrNull()
        if (last?.key == key) {
            groups[groups.lastIndex] = last.copy(items = last.items + item)
        } else {
            groups += NewsGroup(key = key, date = date, sourceKey = item.newsSource, items = listOf(item))
        }
    }
    return groups
}

/** 一条情报落在哪个页面：自带正文进详情页，转载去原文。 */
fun newsTarget(item: NewsItem): NewsItemTarget = when {
    item.hasBody -> NewsItemTarget.DETAIL
    item.sourceUrl.isNotBlank() -> NewsItemTarget.SOURCE
    else -> NewsItemTarget.NONE
}

enum class NewsItemTarget { DETAIL, SOURCE, NONE }

fun newsLaneLabel(lane: String): String = if (lane == "column") "专栏" else "情报"

fun newsSourceName(source: NewsSource?): String =
    source?.displayName?.takeIf { it.isNotBlank() } ?: "合作站点"

/** 单月页把每个分组展开成「抬头 + 条目」的扁平行，供分页列表直接渲染。 */
sealed interface NewsListRow {
    data class Header(val group: NewsGroup) : NewsListRow
    data class Item(val item: NewsItem) : NewsListRow
}

fun newsRows(items: List<NewsItem>): List<NewsListRow> = buildList {
    for (group in newsGroups(items)) {
        add(NewsListRow.Header(group))
        group.items.forEach { add(NewsListRow.Item(it)) }
    }
}
