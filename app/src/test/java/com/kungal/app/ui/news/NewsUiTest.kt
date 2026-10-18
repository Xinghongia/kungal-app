package com.kungal.app.ui.news

import com.kungal.core.model.NewsItem
import com.kungal.core.model.NewsSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsUiTest {

    private fun item(
        id: String,
        source: String,
        publishedAt: String,
        hasBody: Boolean = false,
        sourceUrl: String = "",
    ) = NewsItem(
        id = id,
        newsSource = source,
        title = "情报 $id",
        publishedAt = publishedAt,
        hasBody = hasBody,
        sourceUrl = sourceUrl,
    )

    @Test
    fun `同一天同一来源合并成一组`() {
        val groups = newsGroups(
            listOf(
                item("1", "a", "2026-10-11T09:20:51Z"),
                item("2", "a", "2026-10-11T09:20:51Z"),
                item("3", "b", "2026-10-11T09:20:51Z"),
                item("4", "a", "2026-10-09T09:20:51Z"),
            ),
        )
        assertEquals(3, groups.size)
        assertEquals(listOf("1", "2"), groups[0].items.map { it.id })
        assertEquals("a", groups[0].sourceKey)
        assertEquals(listOf("3"), groups[1].items.map { it.id })
        assertEquals("b", groups[1].sourceKey)
        assertEquals(listOf("4"), groups[2].items.map { it.id })
        assertEquals(groups[0].date, groups[1].date)
    }

    @Test
    fun `空列表没有分组`() {
        assertTrue(newsGroups(emptyList()).isEmpty())
    }

    @Test
    fun `发布日期输出 yyyy-MM-dd`() {
        val date = newsDate("2026-10-11T09:20:51Z")
        assertTrue(date.matches(Regex("""\d{4}-\d{2}-\d{2}""")))
        assertEquals("", newsDate(""))
        assertEquals("", newsDate("not-a-time"))
    }

    @Test
    fun `自带正文进详情 转载去原文 都没有则不可点`() {
        assertEquals(
            NewsItemTarget.DETAIL,
            newsTarget(item("1", "community", "", hasBody = true, sourceUrl = "")),
        )
        assertEquals(
            NewsItemTarget.SOURCE,
            newsTarget(item("2", "ymgal", "", sourceUrl = "https://example.com/a")),
        )
        assertEquals(NewsItemTarget.NONE, newsTarget(item("3", "ymgal", "")))
    }

    @Test
    fun `单月分页把分组展开成抬头与条目`() {
        val rows = newsRows(
            listOf(
                item("1", "a", "2026-10-11T09:20:51Z"),
                item("2", "a", "2026-10-11T09:20:51Z"),
                item("3", "b", "2026-10-11T09:20:51Z"),
            ),
        )
        assertEquals(5, rows.size)
        assertTrue(rows[0] is NewsListRow.Header)
        assertEquals("1", (rows[1] as NewsListRow.Item).item.id)
        assertEquals("2", (rows[2] as NewsListRow.Item).item.id)
        assertTrue(rows[3] is NewsListRow.Header)
        assertEquals("3", (rows[4] as NewsListRow.Item).item.id)
    }

    @Test
    fun `来源名称缺失时回退合作站点`() {
        assertEquals("合作站点", newsSourceName(null))
        assertEquals("合作站点", newsSourceName(NewsSource(displayName = "")))
        assertEquals("月幕 Galgame", newsSourceName(NewsSource(displayName = "月幕 Galgame")))
    }

    @Test
    fun `栏目名称`() {
        assertEquals("专栏", newsLaneLabel("column"))
        assertEquals("情报", newsLaneLabel("news"))
        assertEquals("情报", newsLaneLabel(""))
    }
}
