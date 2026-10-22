package com.kungal.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PagedListContentKeysTest {

    @Test
    fun `正常 id 原样保留`() {
        val keys = pageItemKeys(listOf("1", "2", "3"))
        assertEquals(listOf<Any>("1", "2", "3"), keys)
    }

    @Test
    fun `空字符串与 null 退回下标，且不重复`() {
        val keys = pageItemKeys(listOf("", null, "  ", "4"))
        assertEquals("4", keys[3])
        assertEquals(keys.size, keys.toSet().size)
        assertTrue(keys.take(3).none { it is String })
    }

    @Test
    fun `重复 id 只保留第一次，后面的退回下标`() {
        val keys = pageItemKeys(listOf("7", "7", "8", "7"))
        assertEquals("7", keys[0])
        assertEquals("8", keys[2])
        assertNotEquals(keys[0], keys[1])
        assertNotEquals(keys[0], keys[3])
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun `全部是空 id 也不崩`() {
        val keys = pageItemKeys(List(5) { "" })
        assertEquals(5, keys.toSet().size)
    }

    @Test
    fun `key 列表落后于 items 时按 index 兜底而不是越界`() {
        // 复现 0.8.20 闪退现场：items 已经有 3 项，itemKeys 还是空列表
        val stale = emptyList<Any>()
        val keys = (0..2).map { index -> pageItemKeyAt(stale, index) }
        assertEquals(3, keys.toSet().size)
    }

    @Test
    fun `落后的 key 列表与下标兜底不会撞车`() {
        val stale = pageItemKeys(listOf("1", "2"))
        val keys = (0..3).map { index -> pageItemKeyAt(stale, index) }
        assertEquals(keys.size, keys.toSet().size)
        assertEquals("1", keys[0])
        assertEquals("2", keys[1])
    }
}
