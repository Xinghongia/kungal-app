package com.kungal.app.ui.galgame

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResourceLinkParserTest {

    @Test
    fun `识别网盘分享文本里的链接与提取码`() {
        val parsed = parseResourceLinkInput(
            "链接: https://pan.baidu.com/s/1AbCdEf 提取码: 8ejh 复制这段内容后打开百度网盘",
        )
        assertEquals(listOf("https://pan.baidu.com/s/1AbCdEf"), parsed.links)
        assertEquals("8ejh", parsed.extractionCode)
        assertEquals("", parsed.archivePassword)
    }

    @Test
    fun `多个链接用中文逗号分隔并去掉尾随标点`() {
        val parsed = parseResourceLinkInput(
            "https://pan.quark.cn/s/abc，magnet:?xt=urn:btih:abcdef0123456789。解压密码：aPassword",
        )
        assertEquals(2, parsed.links.size)
        assertEquals("https://pan.quark.cn/s/abc", parsed.links[0])
        assertEquals("magnet:?xt=urn:btih:abcdef0123456789", parsed.links[1])
        assertEquals("aPassword", parsed.archivePassword)
    }

    @Test
    fun `从链接查询参数取提取码`() {
        val parsed = parseResourceLinkInput("https://pan.baidu.com/s/1x?pwd=ab12")
        assertEquals("ab12", parsed.extractionCode)
    }

    @Test
    fun `解压密码不会被误当成提取码`() {
        val parsed = parseResourceLinkInput(
            "https://example.com/a.zip 解压密码: zxcv",
        )
        assertEquals("", parsed.extractionCode)
        assertEquals("zxcv", parsed.archivePassword)
    }

    @Test
    fun `没有链接时返回空结果`() {
        val parsed = parseResourceLinkInput("只是普通文字")
        assertTrue(parsed.links.isEmpty())
        assertFalse(parsed.hasLinks)
    }

    @Test
    fun `体积与运行环境规则`() {
        assertTrue(resourceSizeValid("919", "MB"))
        assertTrue(resourceSizeValid("1.25", "GB"))
        assertFalse(resourceSizeValid("1.234", "GB"))
        assertFalse(resourceSizeValid("", "GB"))
        assertEquals("919 GB", joinResourceSize("919", "GB"))
        assertEquals("" to "GB", splitResourceSize(""))
        assertEquals("1.5" to "GB", splitResourceSize("1.5 GB"))
        assertTrue(resourceTypeHasRuntime("game"))
        assertTrue(resourceTypeHasRuntime("patch"))
        assertFalse(resourceTypeHasRuntime("ost"))
    }
}
