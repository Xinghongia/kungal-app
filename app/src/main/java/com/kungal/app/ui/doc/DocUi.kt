package com.kungal.app.ui.doc

import com.kungal.app.ui.content.contentPlainText
import com.kungal.app.ui.galgame.FilterOption
import com.kungal.core.model.ContentDocument
import com.kungal.core.model.Doc
import com.kungal.core.model.HeadingNode

/** 文档分类（网页 constants/doc.ts）。 */
val DocCategoryLabels: Map<String, String> = mapOf(
    "galgame" to "Galgame",
    "notice" to "网站公告",
    "kun" to "关于鲲",
    "other" to "其它",
)

val DocCategoryOptions: List<FilterOption> = listOf(
    FilterOption("galgame", "Galgame"),
    FilterOption("notice", "网站公告"),
    FilterOption("kun", "关于鲲"),
    FilterOption("other", "其它"),
)

fun docCategoryLabel(category: String): String = DocCategoryLabels[category] ?: category

/** 文档目录项：顶层块下标 + 标题层级 + 纯文本标题。 */
data class DocHeading(
    val blockIndex: Int,
    val depth: Int,
    val text: String,
)

/** 从正文里抽目录：只取顶层 heading 节点（与网页 contentHeadings 一致）。 */
fun docHeadings(doc: Doc?): List<DocHeading> =
    doc?.content?.children?.mapIndexedNotNull { index, node ->
        val heading = node as? HeadingNode ?: return@mapIndexedNotNull null
        val text = contentPlainText(ContentDocument(children = heading.children)).trim()
        if (text.isBlank()) null else DocHeading(index, heading.depth, text)
    }.orEmpty()

/**
 * 上一篇 / 下一篇：网页 Footer 用 published_desc 的全量文档列表，
 * 传入的 [docs] 必须已按该顺序排列，返回 (上一篇, 下一篇)。
 */
fun docNeighbors(docs: List<Doc>, slug: String): Pair<Doc?, Doc?> {
    val index = docs.indexOfFirst { it.slug == slug }
    if (index < 0) return null to null
    return docs.getOrNull(index - 1) to docs.getOrNull(index + 1)
}
