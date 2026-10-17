package com.kungal.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Test

class PaginationBarTest {

    @Test
    fun `总页数按总数与每页条数向上取整`() {
        assertEquals(0, pageCount(0, 24))
        assertEquals(1, pageCount(1, 24))
        assertEquals(24, pageCount(24, 1))
        assertEquals(1, pageCount(24, 24))
        assertEquals(2, pageCount(25, 24))
        assertEquals(406, pageCount(20291, 50))
        assertEquals(0, pageCount(10, 0))
    }

    @Test
    fun `页数少时全部显示`() {
        assertEquals(listOf<Int?>(1, 2, 3, 4, 5, 6, 7), pageTokens(1, 7))
        assertEquals(listOf<Int?>(1, 2, 3), pageTokens(2, 3))
    }

    @Test
    fun `首页显示首尾与当前页附近`() {
        assertEquals(listOf<Int?>(1, 2, 3, null, 406), pageTokens(1, 406))
    }

    @Test
    fun `中间页两侧显示省略号`() {
        assertEquals(
            listOf<Int?>(1, null, 98, 99, 100, 101, 102, null, 406),
            pageTokens(100, 406),
        )
    }

    @Test
    fun `末页固定显示最后一页`() {
        assertEquals(listOf<Int?>(1, null, 404, 405, 406), pageTokens(406, 406))
    }
}
