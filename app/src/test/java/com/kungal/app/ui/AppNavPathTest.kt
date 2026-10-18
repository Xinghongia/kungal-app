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

    @Test
    fun `月历分享链接解析视图与同人开关`() {
        assertEquals(
            CalendarLink(view = "upcoming", doujin = true),
            parseCalendarPath("/galgame-calendar?view=upcoming&doujin=1"),
        )
        assertEquals(
            CalendarLink(view = "pending", year = 2027),
            parseCalendarPath("/galgame-calendar?view=pending&year=2027"),
        )
        assertEquals(
            CalendarLink(view = "month", month = "2026-10"),
            parseCalendarPath("/galgame-calendar?view=month&month=2026-10"),
        )
        assertEquals(CalendarLink(), parseCalendarPath("/galgame-calendar"))
    }

    @Test
    fun `月历链接忽略非法参数`() {
        assertEquals(
            CalendarLink(),
            parseCalendarPath("/galgame-calendar?view=xxx&month=2026-1&year=abc&doujin=0"),
        )
    }

    @Test
    fun `情报月归档链接解析年月与随行筛选`() {
        assertEquals(
            NewsMonthLink(year = 2026, month = 10),
            parseNewsMonthPath("/news/2026/10"),
        )
        assertEquals(
            NewsMonthLink(year = 2026, month = 10, lane = "column", source = "ymgal"),
            parseNewsMonthPath("/news/2026/10?lane=column&source=ymgal"),
        )
        assertEquals(
            NewsMonthLink(year = 2025, month = 1),
            parseNewsMonthPath("/news/2025/1?lane=bad"),
        )
    }

    @Test
    fun `情报详情与非法月归档不当作月归档`() {
        assertNull(parseNewsMonthPath("/news/4760"))
        assertNull(parseNewsMonthPath("/news/2026/13"))
        assertNull(parseNewsMonthPath("/news/2026"))
        assertNull(parseNewsMonthPath("/news/mine"))
    }
}
