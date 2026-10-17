package com.kungal.app.ui.galgame

import com.kungal.core.model.AspectScores
import org.junit.Assert.assertEquals
import org.junit.Test

class GalgameRatingUiTest {

    @Test
    fun `评分标签与游玩状态中文映射`() {
        assertEquals("强烈推荐", recommendLabel("strong_yes"))
        assertEquals("部分剧透", ratingSpoilerLabel("portion"))
        assertEquals("全通", playStateLabel("done_all"))
        assertEquals("未记录", playStateLabel(null))
        assertEquals("剧情作", gameTypeLabel("plot"))
        assertEquals("unknown", recommendLabel("unknown"))
    }

    @Test
    fun `八维评分只展示已填维度`() {
        val items = aspectItems(AspectScores(art = 8, story = 7, replayValue = 9))
        assertEquals(listOf("画面" to 8, "剧情" to 7, "重玩" to 9), items)
        assertEquals(emptyList<Pair<String, Int>>(), aspectItems(AspectScores()))
    }

    @Test
    fun `雷达图固定八轴且未填维度按0`() {
        val axes = aspectRadarItems(AspectScores(art = 10, route = 5))
        assertEquals(
            listOf("画面", "剧情", "音乐", "角色", "分支", "系统", "配音", "重玩"),
            axes.map { it.first },
        )
        assertEquals(listOf(10, 0, 0, 0, 5, 0, 0, 0), axes.map { it.second })
    }
}
