package com.kungal.app.ui.galgame

import com.kungal.core.model.claimStateLabel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SubmissionFormTest {

    private fun draft(
        titles: Map<String, String> = mapOf("zh-Hans" to "示例作品"),
        introductions: Map<String, String> = mapOf("zh-Hans" to "介绍"),
        aliases: List<String> = emptyList(),
        contentLimit: String = "sfw",
        ageLimitR18: Boolean = false,
        originalLanguage: String = "ja",
        releaseDate: String = "",
        releaseDateTba: Boolean = false,
        bannerHash: String? = null,
    ) = SubmissionFormDraft(
        titles = SubmissionFormDraft().titles + titles,
        introductions = SubmissionFormDraft().introductions + introductions,
        aliases = aliases,
        contentLimit = contentLimit,
        ageLimitR18 = ageLimitR18,
        originalLanguage = originalLanguage,
        releaseDate = releaseDate,
        releaseDateTba = releaseDateTba,
        bannerHash = bannerHash,
    )

    @Test
    fun `表单转请求体按语言顺序去空白并映射分级`() {
        val request = buildSubmissionRequest(
            draft = draft(
                titles = mapOf(
                    "en" to "  Sample Work  ",
                    "ja" to "サンプル",
                    "zh-Hant" to "範例",
                ),
                introductions = mapOf("ja" to " 日本語介绍 ", "en" to "   "),
                aliases = listOf(" Sample ", " ", "サンプル"),
                contentLimit = "nsfw",
                ageLimitR18 = true,
                originalLanguage = "zh-Hans",
                releaseDate = "2025-01-31",
                bannerHash = "a".repeat(64),
            ),
        )
        assertEquals(listOf("en", "ja", "zh-Hant"), request.titles.map { it.locale })
        assertEquals("Sample Work", request.titles[0].title)
        assertEquals("範例", request.titles[2].title)
        assertEquals(listOf("Sample", "サンプル"), request.aliases)
        assertEquals(1, request.introductions.size)
        assertEquals("日本語介绍", request.introductions[0].value)
        assertEquals("zh-Hans", request.originalLanguage)
        assertEquals("r18", request.contentRating)
        assertTrue(request.isNsfw)
        assertEquals("2025-01-31", request.releaseDate)
        assertEquals("day", request.releaseDatePrecision)
        assertEquals("a".repeat(64), request.bannerHash)
        assertEquals(false, request.isDuplicateConfirmed)
    }

    @Test
    fun `待定日期不发日期也不发精度`() {
        val request = buildSubmissionRequest(
            draft = draft(releaseDate = "2025-01-31", releaseDateTba = true),
        )
        assertNull(request.releaseDate)
        assertNull(request.releaseDatePrecision)
        assertNull(request.bannerHash)
        assertEquals("all_ages", request.contentRating)
        assertEquals(false, request.isNsfw)
    }

    @Test
    fun `确认重复后请求体带 is_duplicate_confirmed`() {
        val request = buildSubmissionRequest(draft = draft(), isDuplicateConfirmed = true)
        assertTrue(request.isDuplicateConfirmed)
    }

    @Test
    fun `校验按步骤指出第一条错误`() {
        val noTitle = validateSubmissionDraft(draft(titles = emptyMap()))
        assertEquals(1, noTitle?.step)
        assertEquals("至少需要填写一个语言版本的游戏名称", noTitle?.message)

        val tooLongAlias = validateSubmissionDraft(draft(aliases = listOf("x".repeat(501))))
        assertEquals(1, tooLongAlias?.step)
        assertTrue(tooLongAlias!!.message.contains("别名"))

        val noIntro = validateSubmissionDraft(draft(introductions = emptyMap()))
        assertEquals(2, noIntro?.step)
        assertEquals("至少需要填写一个语言版本的游戏介绍", noIntro?.message)

        val noContentLimit = validateSubmissionDraft(draft(contentLimit = ""))
        assertEquals(3, noContentLimit?.step)
        assertEquals("请选择 SFW 或 NSFW", noContentLimit?.message)

        val badDate = validateSubmissionDraft(draft(releaseDate = "2025-13-40"))
        assertEquals(3, badDate?.step)
        assertTrue(badDate!!.message.contains("YYYY-MM-DD"))

        assertNull(validateSubmissionDraft(draft(releaseDate = "2025-01-31")))
        assertNull(validateSubmissionDraft(draft(releaseDateTba = true)))
    }

    @Test
    fun `认领状态与原始语言标签`() {
        assertEquals("已发布", claimStateLabel("live"))
        assertEquals("草稿", claimStateLabel("draft"))
        assertEquals("审核中", claimStateLabel("pending"))
        assertEquals("已拒绝", claimStateLabel("declined"))
        assertEquals("未认领", claimStateLabel("none"))
        assertEquals("日语", submissionOriginalLanguageLabel("ja"))
        assertEquals("葡萄牙语 (巴西)", submissionOriginalLanguageLabel("pt-br"))
        assertEquals("简体中文", submissionOriginalLanguageLabel("zh-Hans"))
        assertTrue(SubmissionOriginalLanguages.none { it.first == "others" })
    }
}
