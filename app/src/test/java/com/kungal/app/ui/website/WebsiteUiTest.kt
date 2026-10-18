package com.kungal.app.ui.website

import com.kungal.core.model.WebsiteCategory
import com.kungal.core.model.WebsiteCategoryRef
import com.kungal.core.model.WebsiteEntry
import com.kungal.core.model.WebsiteTag
import com.kungal.core.model.WebsiteTagGroup
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WebsiteUiTest {

    private fun site(
        id: String,
        score: Int,
        state: String = "normal",
        category: String = "3",
        title: String = "站点 $id",
        host: String = "site$id.example",
        description: String = "描述 $id",
    ) = WebsiteEntry(
        id = id,
        host = host,
        title = title,
        description = description,
        websiteCategory = WebsiteCategoryRef(id = category, slug = "resource", label = "资源站"),
        state = state,
        score = score,
    )

    private fun tag(
        id: String,
        level: Int,
        group: String? = null,
        slug: String = "tag$id",
    ) = WebsiteTag(id = id, slug = slug, label = "标签 $id", level = level, websiteTagGroupId = group)

    @Test
    fun `语言与状态文案`() {
        assertEquals("简体中文", websiteLanguageLabel("zh-cn"))
        assertEquals("日语", websiteLanguageLabel("ja-jp"))
        assertEquals("英语", websiteLanguageLabel("en-us"))
        assertEquals("klingon", websiteLanguageLabel("klingon"))
        assertEquals("未知", websiteLanguageLabel(""))

        assertEquals("正常", websiteStateLabel("normal"))
        assertEquals("无法访问", websiteStateLabel("unreachable"))
        assertEquals("已关站", websiteStateLabel("closed"))
        assertNull(websiteStateBadge("normal"))
        assertEquals("无法访问", websiteStateBadge("unreachable"))
        assertEquals("已关站", websiteStateBadge("closed"))
    }

    @Test
    fun `访问链接补协议并带 utm`() {
        assertEquals(
            "https://www.letmoe.com?utm_source=www.kungal.com",
            websiteVisitUrl("www.letmoe.com"),
        )
        assertEquals(
            "http://example.com/a?x=1&utm_source=www.kungal.com",
            websiteVisitUrl("http://example.com/a?x=1"),
        )
        assertEquals("", websiteVisitUrl("  "))
    }

    @Test
    fun `排序先按分数再按 id 数值降序`() {
        val sorted = websiteSorted(
            listOf(site("9", 90), site("100", 90), site("7", 220)),
        )
        assertEquals(listOf("7", "100", "9"), sorted.map { it.id })
    }

    @Test
    fun `搜索匹配标题描述与域名`() {
        val entry = site("1", 10, title = "Yukie ACG", host = "www.yukie.moe", description = "收藏每一份喜欢")
        assertTrue(websiteMatches(entry, ""))
        assertTrue(websiteMatches(entry, "yukie"))
        assertTrue(websiteMatches(entry, "收藏"))
        assertTrue(websiteMatches(entry, "moe"))
        assertFalse(websiteMatches(entry, "不存在的关键词"))
    }

    @Test
    fun `按分类分组并单独列出已关站`() {
        val categories = listOf(
            WebsiteCategory(id = "3", slug = "resource", label = "资源站", sortOrder = 10),
            WebsiteCategory(id = "2", slug = "community", label = "社区站", sortOrder = 20),
            WebsiteCategory(id = "4", slug = "other", label = "其他", sortOrder = 40),
        )
        val entries = listOf(
            site("1", 50, category = "3"),
            site("2", 120, category = "3"),
            site("3", 10, category = "2"),
            site("4", 999, state = "closed", category = "3"),
        )
        val (sections, closed) = websiteSections(entries, categories)
        assertEquals(2, sections.size)
        assertEquals("3", sections[0].category.id)
        assertEquals(listOf("2", "1"), sections[0].sites.map { it.id })
        assertEquals("2", sections[1].category.id)
        assertEquals(listOf("4"), closed.map { it.id })
    }

    @Test
    fun `分类接口缺少的分类也不丢`() {
        val (sections, _) = websiteSections(
            listOf(site("1", 10, category = "99")),
            listOf(WebsiteCategory(id = "3", slug = "resource", label = "资源站")),
        )
        assertEquals(1, sections.size)
        assertEquals("99", sections[0].category.id)
    }

    @Test
    fun `标签颜色分档`() {
        assertEquals(WebsiteTagTone.LEGENDARY, websiteTagTone(tag("1", 5, slug = "ad0")))
        assertEquals(WebsiteTagTone.HIGH, websiteTagTone(tag("2", 10)))
        assertEquals(WebsiteTagTone.MEDIUM, websiteTagTone(tag("3", 5)))
        assertEquals(WebsiteTagTone.NEUTRAL, websiteTagTone(tag("4", 0)))
        assertEquals(WebsiteTagTone.NEGATIVE, websiteTagTone(tag("5", -5)))
        assertEquals(WebsiteTagTone.LOW, websiteTagTone(tag("6", -6)))
    }

    @Test
    fun `分数配色阈值`() {
        assertEquals(WebsiteScoreTone.TOP, websiteScoreTone(201))
        assertEquals(WebsiteScoreTone.GOOD, websiteScoreTone(200))
        assertEquals(WebsiteScoreTone.GOOD, websiteScoreTone(101))
        assertEquals(WebsiteScoreTone.NEUTRAL, websiteScoreTone(100))
        assertEquals(WebsiteScoreTone.NEUTRAL, websiteScoreTone(1))
        assertEquals(WebsiteScoreTone.BAD, websiteScoreTone(0))
        assertEquals(WebsiteScoreTone.BAD, websiteScoreTone(-10))
    }

    @Test
    fun `最高分单选组取最高档 多选与未分组累加正分`() {
        val groups = listOf(
            WebsiteTagGroup(id = "1", slug = "ad", label = "广告度", isMultiSelect = false),
            WebsiteTagGroup(id = "2", slug = "open", label = "开放度", isMultiSelect = true),
        )
        val tags = listOf(
            tag("1", 20, group = "1"),
            tag("2", 10, group = "1"),
            tag("3", 10, group = "2"),
            tag("4", 5, group = "2"),
            tag("5", 7, group = null),
            tag("6", -100, group = null),
        )
        // 广告度单选取 20；开放度多选 10+5；未分组 7（负数不计入上限）
        assertEquals(42, websiteMaxScore(tags, groups))
    }

    @Test
    fun `没有任何正分时最高分至少为 1`() {
        assertEquals(1, websiteMaxScore(listOf(tag("1", -10, group = null)), emptyList()))
        assertEquals(1, websiteMaxScore(emptyList(), emptyList()))
    }

    @Test
    fun `标签按分组聚合按分数降序`() {
        val groups = listOf(
            WebsiteTagGroup(id = "1", slug = "ad", label = "广告度"),
            WebsiteTagGroup(id = "2", slug = "open", label = "开放度"),
        )
        val stats = websiteTagGroupStats(
            listOf(
                tag("1", 20, group = "1"),
                tag("2", 10, group = "1"),
                tag("3", -5, group = "2"),
                tag("4", 3, group = null),
            ),
            groups,
        )
        assertEquals(listOf("1", "ungrouped", "2"), stats.map { it.key })
        assertEquals(30, stats[0].score)
        assertEquals("广告度", stats[0].label)
        assertEquals("其他指标", stats[1].label)
        assertEquals(-5, stats[2].score)
    }

    @Test
    fun `价值条按绝对值占比分段`() {
        val stats = listOf(
            WebsiteTagGroupStat("1", "广告度", 30, emptyList()),
            WebsiteTagGroupStat("2", "开放度", -10, emptyList()),
        )
        val segments = websiteScoreSegments(stats)
        assertEquals(2, segments.size)
        assertEquals(0f, segments[0].start, 0.0001f)
        assertEquals(0.75f, segments[0].width, 0.0001f)
        assertEquals(0.75f, segments[1].start, 0.0001f)
        assertEquals(0.25f, segments[1].width, 0.0001f)
        assertTrue(websiteScoreSegments(emptyList()).isEmpty())
        assertTrue(
            websiteScoreSegments(listOf(WebsiteTagGroupStat("1", "零", 0, emptyList()))).isEmpty(),
        )
    }

    @Test
    fun `图标优先站点图标再退回第三方`() {
        assertEquals("", websiteIconUrl(null, null))
        assertEquals("https://cdn/favicon.ico", websiteIconUrl(null, "https://cdn/favicon.ico"))
        assertEquals(
            "https://image.kungal/abc.webp",
            websiteIconUrl(
                com.kungal.core.model.ImageInfo(url = "https://image.kungal/abc.webp"),
                "https://cdn/favicon.ico",
            ),
        )
    }
}
