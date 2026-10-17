package com.kungal.app.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppNavPathTest {

    @Test
    fun `用户主页普通路径回到动态`() {
        assertEquals("123" to null, parseUserTabPath("/user/123"))
        assertEquals("123" to null, parseUserTabPath("/user/123/topic"))
    }

    @Test
    fun `用户工具集路径进工具集标签`() {
        assertEquals("123" to "toolset", parseUserTabPath("/user/123/toolset"))
    }

    @Test
    fun `用户资源路径按类型映射标签`() {
        assertEquals("123" to "resource", parseUserTabPath("/user/123/resource/valid"))
        assertEquals("123" to "resource-expire", parseUserTabPath("/user/123/resource/expire"))
        assertEquals(
            "123" to "resource-like",
            parseUserTabPath("/user/123/resource/galgame-resource-like"),
        )
        assertEquals("123" to "resource", parseUserTabPath("/user/123/resource"))
    }

    @Test
    fun `非数字用户 id 不接管`() {
        assertNull(parseUserTabPath("/user/abc"))
        assertNull(parseUserTabPath("/user/abc/resource/valid"))
    }
}
