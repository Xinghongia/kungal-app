package com.kungal.app.ui.galgame

import com.kungal.core.model.ReleaseCalendarMonth
import com.kungal.core.model.WorkSummary
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalendarUiTest {

    private fun work(
        id: String,
        date: String?,
        precision: String? = "day",
    ) = WorkSummary(
        id = id,
        displayName = "作品 $id",
        releaseDate = date,
        releaseDatePrecision = precision,
    )

    private fun monthData(vararg works: WorkSummary) = ReleaseCalendarMonth(
        calendarMonth = "2026-10",
        todayDate = "2026-10-17",
        items = works.toList(),
        itemCount = works.size,
        hasPrev = true,
        hasNext = true,
        prevMonth = "2026-09",
        nextMonth = "2026-11",
    )

    @Test
    fun `月份标签用中文年月`() {
        assertEquals("2026 年 10 月", calendarMonthLabel("2026-10"))
        assertEquals("2027 年 1 月", calendarMonthLabel("2027-01"))
        assertEquals("bad", calendarMonthLabel("bad"))
    }

    @Test
    fun `月份网格周日开头且补齐整周`() {
        val cells = calendarMonthCells("2026-10")
        // 2026-10-01 是周四，周日起始需要 4 个空位；31 天后补到 35 格。
        assertEquals(35, cells.size)
        assertEquals(listOf(null, null, null, null), cells.take(4))
        assertEquals(1, cells[4])
        assertEquals(31, cells[34])
        // 2026-02 是 28 天，2026-02-01 是周日。
        assertEquals(28, calendarMonthCells("2026-02").size)
        assertTrue(calendarMonthCells("bad").isEmpty())
    }

    @Test
    fun `今日置顶其余按日升序待定收尾`() {
        val data = monthData(
            work("late", "2026-10-20"),
            work("today", "2026-10-17"),
            work("early", "2026-10-05"),
            work("unknown", null, precision = null),
            work("monthly", "2026-10-01", precision = "month"),
        )
        val rows = calendarDayRows(data)

        assertEquals(listOf(17, 5, 20, 0), rows.map { it.day })
        assertEquals(CalendarDayStatus.TODAY, rows[0].status)
        assertEquals("周六", rows[0].weekday)
        assertEquals(CalendarDayStatus.PAST, rows[1].status)
        assertEquals(CalendarDayStatus.FUTURE, rows[2].status)
        assertTrue(rows[3].isBucket)
        assertEquals(listOf("unknown", "monthly"), rows[3].items.map { it.id })
        assertEquals(mapOf(17 to 1, 5 to 1, 20 to 1), calendarDayCounts(rows))
    }

    @Test
    fun `非本月日期归入待定`() {
        val data = monthData(work("other-month", "2026-09-30"))
        val rows = calendarDayRows(data)
        assertEquals(1, rows.size)
        assertTrue(rows[0].isBucket)
    }

    @Test
    fun `过去月份不标今日`() {
        val data = ReleaseCalendarMonth(
            calendarMonth = "2026-09",
            todayDate = "2026-10-17",
            items = listOf(work("a", "2026-09-03"), work("b", "2026-09-28")),
        )
        val rows = calendarDayRows(data)
        assertEquals(listOf(3, 28), rows.map { it.day })
        assertTrue(rows.all { it.status == CalendarDayStatus.PAST })
        assertEquals("周四", rows[0].weekday)
    }

    @Test
    fun `空月份没有日行`() {
        val data = ReleaseCalendarMonth(calendarMonth = "2026-10", todayDate = "2026-10-17")
        assertTrue(calendarDayRows(data).isEmpty())
        assertTrue(calendarDayCounts(calendarDayRows(data)).isEmpty())
        assertNull(calendarReleaseDay(work("x", "2026-10-01", precision = "month"), "2026-10"))
    }
}
