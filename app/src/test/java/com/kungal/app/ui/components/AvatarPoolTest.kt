package com.kungal.app.ui.components

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AvatarPoolTest {

    @Test
    fun `与网页一致按用户名挑选默认头像`() {
        // 真实样本：话题 4323 回复 #1 的用户在网页上渲染的是池内第 47 张
        // （hash("战神金刚") % 64），这里校验同一算法得到同一张图。
        val picked = AvatarPool.pick("战神金刚", AvatarPool.urls.value)
        assertEquals(
            "https://image.kungal.iloveren.link/18/d3/18d3223bb64bc1a2a38b60c519bb666dc0d222b3ee2a48f115d4089060186cc9_128.webp",
            picked,
        )
    }

    @Test
    fun `同一用户名稳定 不同用户名可能不同`() {
        val pool = AvatarPool.urls.value
        assertEquals(AvatarPool.pick("阿瞳", pool), AvatarPool.pick("阿瞳", pool))
        assertEquals(64, pool.size)
    }

    @Test
    fun `空池返回 null 交给占位图`() {
        assertNull(AvatarPool.pick("阿瞳", emptyList()))
    }
}
