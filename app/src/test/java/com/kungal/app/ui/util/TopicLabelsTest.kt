package com.kungal.app.ui.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TopicLabelsTest {

    @Test
    fun `分类键映射为站点中文名`() {
        assertEquals("Galgame", TopicLabels.category("galgame"))
        assertEquals("技术交流", TopicLabels.category("technique"))
        assertEquals("其它话题", TopicLabels.category("others"))
    }

    @Test
    fun `版块键映射为站点中文名`() {
        assertEquals("实用技术", TopicLabels.section("t-practical"))
        assertEquals("资讯", TopicLabels.section("g-news"))
        assertEquals("新作消息", TopicLabels.section("g-releases"))
        assertEquals("个人随笔", TopicLabels.section("o-essay"))
        assertEquals("逆向工程", TopicLabels.section("t-crack"))
    }

    @Test
    fun `未知键原样返回`() {
        assertEquals("future-block", TopicLabels.section("future-block"))
        assertEquals("future-cat", TopicLabels.category("future-cat"))
    }

    @Test
    fun `筛选选项覆盖三个分类`() {
        assertEquals(4, TopicLabels.filterOptions.size)
        assertEquals(listOf("galgame", "technique", "others"), TopicLabels.filterOptions.drop(1).map { it.first })
    }
}
