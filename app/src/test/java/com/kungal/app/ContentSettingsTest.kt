package com.kungal.app

import com.kungal.core.model.ContentStance
import org.junit.Assert.assertEquals
import org.junit.Test

class ContentSettingsTest {

    @Test
    fun `fromWire 解析三种值并兜底隐藏`() {
        assertEquals(NsfwDisplayMode.SHOW, NsfwDisplayMode.fromWire("show"))
        assertEquals(NsfwDisplayMode.BLUR, NsfwDisplayMode.fromWire("blur"))
        assertEquals(NsfwDisplayMode.HIDE, NsfwDisplayMode.fromWire("hide"))
        assertEquals(NsfwDisplayMode.HIDE, NsfwDisplayMode.fromWire(null))
        assertEquals(NsfwDisplayMode.HIDE, NsfwDisplayMode.fromWire("unknown"))
    }

    @Test
    fun `账号姿态按成年确认折叠`() {
        assertEquals(NsfwDisplayMode.HIDE, NsfwDisplayMode.fromAccount(null))
        assertEquals(
            NsfwDisplayMode.HIDE,
            NsfwDisplayMode.fromAccount(ContentStance(isAdultConfirmed = false, nsfwDisplay = "show")),
        )
        assertEquals(
            NsfwDisplayMode.SHOW,
            NsfwDisplayMode.fromAccount(ContentStance(isAdultConfirmed = true, nsfwDisplay = "show")),
        )
        assertEquals(
            NsfwDisplayMode.BLUR,
            NsfwDisplayMode.fromAccount(ContentStance(isAdultConfirmed = true, nsfwDisplay = "blur")),
        )
        assertEquals(
            NsfwDisplayMode.HIDE,
            NsfwDisplayMode.fromAccount(ContentStance(isAdultConfirmed = true, nsfwDisplay = null)),
        )
    }
}
