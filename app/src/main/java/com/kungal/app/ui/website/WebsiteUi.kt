package com.kungal.app.ui.website

import com.kungal.core.model.WebsiteCategory
import com.kungal.core.model.WebsiteCategoryRef
import com.kungal.core.model.WebsiteEntry
import com.kungal.core.model.WebsiteTag
import com.kungal.core.model.WebsiteTagGroup

/** 语言代码 → 中文（与网页 constants/galgameWebsite.ts 一致），未知代码原样显示。 */
val WebsiteLanguageLabels: Map<String, String> = mapOf(
    "en-us" to "英语",
    "ja-jp" to "日语",
    "zh-cn" to "简体中文",
    "zh-tw" to "繁体中文",
)

fun websiteLanguageLabel(language: String): String =
    WebsiteLanguageLabels[language] ?: language.ifBlank { "未知" }

/** 运行状态文案：normal 正常 / unreachable 无法访问 / closed 已关站。 */
fun websiteStateLabel(state: String): String = when (state) {
    "unreachable" -> "无法访问"
    "closed" -> "已关站"
    else -> "正常"
}

/** 只在非正常时显示状态角标（与网页 `KUN_WEBSITE_STATUS_CHIP` 一致）。 */
fun websiteStateBadge(state: String): String? = when (state) {
    "unreachable" -> "无法访问"
    "closed" -> "已关站"
    else -> null
}

/** 网站图标：优先站点图标，其次第三方 favicon，都没有返回空串。 */
fun websiteIconUrl(icon: com.kungal.core.model.ImageInfo?, external: String?): String =
    icon?.url?.takeIf { it.isNotBlank() } ?: external.orEmpty()

/** 访问链接：与网页一致，自动补 https 并带上 utm_source。 */
fun websiteVisitUrl(host: String): String {
    val raw = host.trim()
    if (raw.isEmpty()) return ""
    val withScheme = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "https://$raw"
    val base = withScheme.substringBefore('#')
    return if (base.contains('?')) "$base&utm_source=www.kungal.com" else "$base?utm_source=www.kungal.com"
}

/** 资料库排序：分数降序，同分按 id 数值降序（网页 `byScore`）。 */
fun websiteSorted(entries: List<WebsiteEntry>): List<WebsiteEntry> =
    entries.sortedWith(
        compareByDescending<WebsiteEntry> { it.score }
            .thenByDescending { it.id.toLongOrNull() ?: 0L },
    )

/** 搜索匹配标题 / 描述 / 域名（不区分大小写），与网页一致。 */
fun websiteMatches(entry: WebsiteEntry, query: String): Boolean {
    val q = query.trim().lowercase()
    if (q.isEmpty()) return true
    return entry.title.lowercase().contains(q) ||
        entry.description.lowercase().contains(q) ||
        entry.host.lowercase().contains(q)
}

data class WebsiteSection(
    val category: WebsiteCategoryRef,
    val sites: List<WebsiteEntry>,
)

/**
 * 总览分组：按分类顺序分组、组内按分数排序、空组去掉；
 * 已关站单独成区（网页在列表末尾用「已关站」存档区展示）。
 */
fun websiteSections(
    entries: List<WebsiteEntry>,
    categories: List<WebsiteCategory>,
): Pair<List<WebsiteSection>, List<WebsiteEntry>> {
    val visible = entries.filter { it.state != "closed" }
    val byCategory = visible.groupBy { it.websiteCategory.id }
    val open = categories.mapNotNull { category ->
        val sites = byCategory[category.id].orEmpty()
        if (sites.isEmpty()) null else WebsiteSection(
            category = WebsiteCategoryRef(id = category.id, slug = category.slug, label = category.label),
            sites = websiteSorted(sites),
        )
    }
    // 分类接口没覆盖到的分类（数据刚改过时）也不丢，放到最后。
    val known = categories.map { it.id }.toSet()
    val extras = byCategory.filterKeys { it !in known }
        .map { (_, sites) ->
            WebsiteSection(category = sites.first().websiteCategory, sites = websiteSorted(sites))
        }
        .sortedByDescending { it.sites.first().score }
    val closed = websiteSorted(entries.filter { it.state == "closed" })
    return (open + extras) to closed
}

/** 标签颜色分档：传奇标签（slug 以 0 结尾）最高，其余按价值分档。 */
enum class WebsiteTagTone { LEGENDARY, HIGH, MEDIUM, NEUTRAL, NEGATIVE, LOW }

fun websiteTagTone(tag: WebsiteTag): WebsiteTagTone = when {
    tag.slug.endsWith("0") -> WebsiteTagTone.LEGENDARY
    tag.level >= 10 -> WebsiteTagTone.HIGH
    tag.level >= 5 -> WebsiteTagTone.MEDIUM
    tag.level >= 0 -> WebsiteTagTone.NEUTRAL
    tag.level >= -5 -> WebsiteTagTone.NEGATIVE
    else -> WebsiteTagTone.LOW
}

/** 分数配色：>200 橙、>100 绿、>0 默认、其余红（网页 Card.vue）。 */
enum class WebsiteScoreTone { TOP, GOOD, NEUTRAL, BAD }

fun websiteScoreTone(score: Int): WebsiteScoreTone = when {
    score > 200 -> WebsiteScoreTone.TOP
    score > 100 -> WebsiteScoreTone.GOOD
    score > 0 -> WebsiteScoreTone.NEUTRAL
    else -> WebsiteScoreTone.BAD
}

/**
 * 理论最高分：单选分组取该组最高档，其余组的正分全部累加（网页 TagVisualization）。
 * 结果至少为 1，避免除零。
 */
fun websiteMaxScore(tags: List<WebsiteTag>, groups: List<WebsiteTagGroup>): Int {
    val multi = groups.associate { it.id to it.isMultiSelect }
    val best = mutableMapOf<String, Int>()
    var loose = 0
    for (tag in tags) {
        val level = maxOf(tag.level, 0)
        val groupId = tag.websiteTagGroupId
        if (groupId == null || multi[groupId] != false) {
            loose += level
        } else {
            best[groupId] = maxOf(best[groupId] ?: 0, level)
        }
    }
    return maxOf(best.values.sum() + loose, 1)
}

data class WebsiteTagGroupStat(
    val key: String,
    val label: String,
    val score: Int,
    val tags: List<WebsiteTag>,
)

/** 按标签分组聚合价值（未分组标签归入「其他指标」），组间按分数降序。 */
fun websiteTagGroupStats(
    tags: List<WebsiteTag>,
    groups: List<WebsiteTagGroup>,
): List<WebsiteTagGroupStat> {
    val labels = groups.associate { it.id to (it.label.ifBlank { it.slug }) }
    val grouped = tags.groupBy { it.websiteTagGroupId ?: "ungrouped" }
    return grouped.map { (key, groupTags) ->
        WebsiteTagGroupStat(
            key = key,
            label = labels[key] ?: "其他指标",
            score = groupTags.sumOf { it.level },
            tags = groupTags,
        )
    }.sortedByDescending { it.score }
}

data class WebsiteScoreSegment(
    val stat: WebsiteTagGroupStat,
    /** 0–1 的起点与宽度，按各组分数绝对值占比分配。 */
    val start: Float,
    val width: Float,
)

/** 价值条分段：按绝对值占比分配宽度（负分组同样占宽），与网页分段一致。 */
fun websiteScoreSegments(stats: List<WebsiteTagGroupStat>): List<WebsiteScoreSegment> {
    val totalAbs = stats.sumOf { kotlin.math.abs(it.score) }
    if (totalAbs == 0) return emptyList()
    var cursor = 0f
    return stats.map { stat ->
        val width = kotlin.math.abs(stat.score).toFloat() / totalAbs
        val segment = WebsiteScoreSegment(stat = stat, start = cursor, width = width)
        cursor += width
        segment
    }
}
