package com.kungal.app.ui.galgame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalgameEntityFiltersTest {

    @Test
    fun `实体作品筛选把排序与单值轴映射到查询`() {
        val filters = EntityWorksFilters(
            sortField = "views",
            sortOrder = "asc",
            platform = "win",
            language = "zh-cn",
            resourceType = "patch",
            gameType = "plot",
            via = "imprint",
        )
        val query = filters.toQuery(includeNsfw = true, page = 3)
        assertEquals(3, query.page)
        assertEquals(24, query.limit)
        assertEquals("view_asc", query.sort)
        assertEquals("win", query.resourcePlatform)
        assertNull(query.resourceRuntime)
        assertEquals("zh-cn", query.resourceLanguage)
        assertEquals("patch", query.resourceType)
        assertEquals("plot", query.gameType)
        assertEquals("imprint", query.via)
        assertEquals(true, query.includeNsfw)
        assertEquals("true", query.toQuery()["include_nsfw"].toString())
    }

    @Test
    fun `模拟器平台转为运行环境筛选`() {
        val query = EntityWorksFilters(platform = "emulator").toQuery(includeNsfw = false)
        assertNull(query.resourcePlatform)
        assertEquals("emulator", query.resourceRuntime)
    }

    @Test
    fun `实体作品查询省略空轴`() {
        val map = EntityWorksFilters().toQuery(includeNsfw = false).toQuery()
        assertEquals("resource_updated_desc", map["sort"])
        assertEquals("false", map["include_nsfw"].toString())
        assertNull(map["resource_platform"])
        assertNull(map["resource_language"])
        assertNull(map["resource_type"])
        assertNull(map["via"])
        assertEquals(false, map.containsKey("exclude_tag_ids"))
    }
}
