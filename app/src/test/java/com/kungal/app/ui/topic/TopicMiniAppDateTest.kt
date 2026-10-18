package com.kungal.app.ui.topic

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class TopicMiniAppDateTest {

    @Test
    fun `截止日期按当地日终转换成接口要求的 UTC 秒级时间`() {
        val date = LocalDate.of(2026, 12, 31)
        val iso = localDateToIso(date)
        assertTrue(
            "接口只接受 20 位 RFC 3339 UTC：$iso",
            Regex("""\d{4}-\d{2}-\d{2}T\d{2}:\d{2}:\d{2}Z""").matches(iso),
        )
        assertEquals(date, isoToLocalDate(iso))
    }

    @Test
    fun `非法或缺失的时间不会让表单崩溃`() {
        assertEquals(null, isoToLocalDate(null))
        assertEquals(null, isoToLocalDate("2026-12-31"))
        assertEquals(null, isoToLocalDate("not-a-time"))
    }
}
