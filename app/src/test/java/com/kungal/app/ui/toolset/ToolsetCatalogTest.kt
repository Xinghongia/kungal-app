package com.kungal.app.ui.toolset

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolsetCatalogTest {

    @Test
    fun `排序 token 按字段与方向拼接`() {
        assertEquals("resource_updated_desc", toolsetSortToken("resource_updated", "desc"))
        assertEquals("created_asc", toolsetSortToken("created", "asc"))
        assertEquals("view_desc", toolsetSortToken("view", "desc"))
        assertEquals("resource_updated_asc", toolsetSortToken("unknown", "asc"))
    }

    @Test
    fun `文件大小格式化`() {
        assertEquals("大小未知", formatToolsetFileSize(0))
        assertEquals("73 MB", formatToolsetFileSize(76_560_285))
        assertEquals("2.00 GB", formatToolsetFileSize(2L * 1024 * 1024 * 1024))
    }

    @Test
    fun `词汇表标签回退原值`() {
        assertEquals("解包工具", labelOfToolset(ToolsetTypeOptions, "extractor"))
        assertEquals("unknown-token", labelOfToolset(ToolsetTypeOptions, "unknown-token"))
    }
}
