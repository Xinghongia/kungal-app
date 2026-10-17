package com.kungal.app.ui.galgame

import com.kungal.core.model.CatalogIntro
import com.kungal.core.model.WorkExternalRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GalgameUiTextTest {

    @Test
    fun `简介优先简中再繁中日语英语`() {
        val intros = listOf(
            CatalogIntro(locale = "en", value = "English"),
            CatalogIntro(locale = "zh-Hant", value = "繁體"),
            CatalogIntro(locale = "zh-Hans", value = "简体"),
        )
        assertEquals("zh-Hans", pickIntro(intros)?.locale)
        assertEquals("zh-Hant", pickIntro(intros.filter { it.locale != "zh-Hans" })?.locale)
        assertNull(pickIntro(emptyList()))
        assertNull(pickIntro(listOf(CatalogIntro(locale = "en", value = "  "))))
    }

    @Test
    fun `简介纯文本清理保留换行并去掉标记`() {
        val source = "**粗体** 和 *斜体*\\\n第二行\n\n[链接](https://example.com) `code`"
        val text = markdownToPlainText(source)
        assertEquals("粗体 和 斜体\n第二行\n\n链接 code", text)
    }

    @Test
    fun `站点链接标签优先站点名再按域名`() {
        assertEquals("官方网站", catalogLinkLabel("official_site", "https://example.com"))
        assertEquals("VNDB", catalogLinkLabel("vndb", "https://vndb.org/v1"))
        assertEquals("维基百科", catalogLinkLabel("web", "https://ja.wikipedia.org/wiki/X"))
        assertEquals("example.com", catalogLinkLabel("unknown", "https://www.example.com/a"))
    }

    @Test
    fun `身份 id 生成 VNDB Bangumi 与批评空间链接`() {
        val links = identityLinks(
            listOf(
                WorkExternalRef("vndb", "v123"),
                WorkExternalRef("bangumi", "456"),
                WorkExternalRef("erogamescape", "789"),
            ),
        )
        assertEquals("VNDB v123", links[0].label)
        assertEquals("https://vndb.org/v123", links[0].url)
        assertEquals("https://bgm.tv/subject/456", links[1].url)
        assertEquals(
            "https://erogamescape.dyndns.org/~ap2/ero/toukei_kaiseki/game.php?game=789",
            links[2].url,
        )
    }

    @Test
    fun `分级与语言标签`() {
        assertEquals("R18", contentRatingLabel("r18"))
        assertEquals("全年龄", contentRatingLabel("all_ages"))
        assertEquals("日语", originalLanguageLabel("ja-jp"))
        assertEquals("简体中文", originalLanguageLabel("zh-Hans"))
        assertEquals("未公布", originalLanguageLabel(null))
    }

    @Test
    fun `游玩时长格式`() {
        assertEquals("0 分钟", formatMinutes(0))
        assertEquals("45 分钟", formatMinutes(45))
        assertEquals("2 小时", formatMinutes(120))
        assertEquals("2 小时 30 分", formatMinutes(150))
    }

    @Test
    fun `封面类型与角色类型标签`() {
        assertEquals("主封面", coverSlotLabel("main"))
        assertEquals("其它", coverSlotLabel("weird"))
        assertEquals("主要角色", characterKindLabel("main"))
        assertEquals("轻度剧透", spoilerLabel("minor"))
        assertEquals("开发商", attributionRoleLabel("developer"))
    }
}
