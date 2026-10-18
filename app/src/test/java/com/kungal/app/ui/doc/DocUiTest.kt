package com.kungal.app.ui.doc

import com.kungal.core.model.ContentDocument
import com.kungal.core.model.Doc
import com.kungal.core.model.HeadingNode
import com.kungal.core.model.ParagraphNode
import com.kungal.core.model.StrongNode
import com.kungal.core.model.TextNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DocUiTest {

    private fun doc(slug: String, category: String = "notice") =
        Doc(id = slug, slug = slug, title = "文档 $slug", docCategory = category)

    @Test
    fun `文档分类中文标签`() {
        assertEquals("Galgame", docCategoryLabel("galgame"))
        assertEquals("网站公告", docCategoryLabel("notice"))
        assertEquals("关于鲲", docCategoryLabel("kun"))
        assertEquals("其它", docCategoryLabel("other"))
        assertEquals("mystery", docCategoryLabel("mystery"))
    }

    @Test
    fun `目录只取顶层标题并记录块下标`() {
        val detail = doc("article-copyright").copy(
            content = ContentDocument(
                children = listOf(
                    HeadingNode(
                        depth = 2,
                        anchor = "第一条-版权归属",
                        children = listOf(TextNode("第一条 版权归属")),
                    ),
                    ParagraphNode(children = listOf(TextNode("正文"))),
                    HeadingNode(
                        depth = 3,
                        anchor = "小节",
                        children = listOf(StrongNode(children = listOf(TextNode("小节标题")))),
                    ),
                    ParagraphNode(),
                ),
            ),
        )
        val headings = docHeadings(detail)
        assertEquals(2, headings.size)
        assertEquals(DocHeading(0, 2, "第一条 版权归属"), headings[0])
        assertEquals(DocHeading(2, 3, "小节标题"), headings[1])
        assertEquals(emptyList<DocHeading>(), docHeadings(detail.copy(content = null)))
    }

    @Test
    fun `上一篇下一篇按传入顺序取相邻项`() {
        val docs = listOf(doc("newest"), doc("middle"), doc("oldest"))
        val (prev, next) = docNeighbors(docs, "middle")
        assertEquals("newest", prev?.slug)
        assertEquals("oldest", next?.slug)

        assertEquals("middle", docNeighbors(docs, "newest").second?.slug)
        assertEquals("middle", docNeighbors(docs, "oldest").first?.slug)

        val (nonePrev, noneNext) = docNeighbors(docs, "missing")
        assertNull(nonePrev)
        assertNull(noneNext)
    }
}
