package com.kungal.app.ui.galgame

import com.kungal.core.model.CompanyRef
import com.kungal.core.model.LocalizedText
import com.kungal.core.model.WorkSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalgameFiltersTest {

    @Test
    fun `资源排序键转成 OpenAPI token`() {
        assertEquals("resource_updated_desc", workSortToken("time", "desc"))
        assertEquals("view_asc", workSortToken("views", "asc"))
        assertEquals("view_7d_desc", workSortToken("view_7d", "desc"))
        assertEquals("release_date_asc", workSortToken("release_date", "asc"))
        assertEquals("rating_desc", workSortToken("rating", "desc"))
    }

    @Test
    fun `资料库排序只有发售日期可升降序`() {
        assertEquals("popularity_desc", librarySortToken("popularity", "asc"))
        assertEquals("updated_desc", librarySortToken("time", "asc"))
        assertEquals("released_desc", librarySortToken("release_date", "desc"))
        assertEquals("released_asc", librarySortToken("release_date", "asc"))
    }

    @Test
    fun `模拟器选项转为运行环境轴`() {
        assertEquals(
            emptyList<String>() to listOf("emulator"),
            splitPlatformAxis(setOf("emulator"), emptySet()),
        )
        assertEquals(
            listOf("win") to listOf("yukihub", "winlator"),
            splitPlatformAxis(setOf("win", "emulator"), setOf("yukihub", "winlator")),
        )
        assertEquals(
            listOf("win", "and") to emptyList<String>(),
            splitPlatformAxis(setOf("win", "and"), setOf("yukihub")),
        )
    }

    @Test
    fun `标题优先简体中文名`() {
        val work = WorkSummary(
            id = "1",
            displayName = "Japanese Name",
            localized = mapOf("zh-Hans" to LocalizedText("中文名")),
        )
        assertEquals("中文名", workTitle(work))
        assertEquals(
            "Japanese Name",
            workTitle(work.copy(localized = emptyMap())),
        )
    }

    @Test
    fun `会社名优先简体中文`() {
        val work = WorkSummary(
            maker = CompanyRef(
                id = "9",
                displayName = "Brand",
                localized = mapOf("zh-Hans" to LocalizedText("中文会社")),
            ),
        )
        assertEquals("中文会社", companyName(work))
        assertNull(companyName(work.copy(maker = null)))
    }

    @Test
    fun `评分只在有人评分时显示一位小数`() {
        assertNull(ratingText(WorkSummary(ratingScore = 8.2, ratingCount = 0)))
        assertNull(ratingText(WorkSummary(ratingScore = null, ratingCount = 3)))
        assertEquals("8.2", ratingText(WorkSummary(ratingScore = 8.2, ratingCount = 3)))
        assertEquals("10.0", ratingText(WorkSummary(ratingScore = 10.0, ratingCount = 1)))
    }

    @Test
    fun `发售日期缺失时与网页文案一致`() {
        assertEquals("发售日期未知", releaseText(WorkSummary()))
        assertEquals("2024-03-29", releaseText(WorkSummary(releaseDate = "2024-03-29")))
    }

    @Test
    fun `平台标签最多三枚`() {
        val work = WorkSummary(resourcePlatforms = listOf("win", "and", "swi", "ps5"))
        assertEquals(listOf("Windows 电脑版", "安卓手机版", "Nintendo Switch"), platformLabels(work))
    }
}
