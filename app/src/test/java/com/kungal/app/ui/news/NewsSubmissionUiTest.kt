package com.kungal.app.ui.news

import com.kungal.core.model.ImageInfo
import com.kungal.core.model.NewsSubmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NewsSubmissionUiTest {

    private val valid = NewsFormValues(
        title = "新作公开",
        preview = "导语",
        contentMarkdown = "正文",
        sourceUrl = "",
    )

    private fun submission(
        title: String = "新作公开",
        preview: String = "导语",
        content: String = "正文",
        sourceUrl: String = "",
        bannerHash: String? = null,
        state: String = "pending",
    ) = NewsSubmission(
        id = "4781",
        state = state,
        title = title,
        preview = preview,
        contentMarkdown = content,
        sourceUrl = sourceUrl,
        banner = bannerHash?.let { ImageInfo(url = "https://image.test/a.webp", hash = it) },
    )

    @Test
    fun `必填与字数校验与网页一致`() {
        assertEquals("请填写情报标题", validateNewsForm(valid.copy(title = "  ")))
        assertEquals(
            "标题不能超过 $NEWS_TITLE_MAX 个字",
            validateNewsForm(valid.copy(title = "a".repeat(NEWS_TITLE_MAX + 1))),
        )
        assertEquals("请填写导语", validateNewsForm(valid.copy(preview = "")))
        assertEquals(
            "导语不能超过 $NEWS_PREVIEW_MAX 个字",
            validateNewsForm(valid.copy(preview = "a".repeat(NEWS_PREVIEW_MAX + 1))),
        )
        assertEquals(
            "正文不能超过 $NEWS_CONTENT_MAX 个字",
            validateNewsForm(valid.copy(contentMarkdown = "a".repeat(NEWS_CONTENT_MAX + 1))),
        )
        assertEquals(
            "正文和原文链接至少要填写一项",
            validateNewsForm(valid.copy(contentMarkdown = "   ", sourceUrl = "")),
        )
        assertEquals(
            "原文链接必须以 http:// 或 https:// 开头",
            validateNewsForm(valid.copy(contentMarkdown = "", sourceUrl = "ftp://a")),
        )
        assertNull(validateNewsForm(valid))
        assertNull(validateNewsForm(valid.copy(contentMarkdown = "", sourceUrl = "https://example.com/a")))
        assertNull(validateNewsForm(valid.copy(sourceUrl = "https://example.com/" + "a".repeat(1000))))
    }

    @Test
    fun `新建请求体省略空正文空链接与空封面`() {
        val body = newsCreateBody(valid.copy(sourceUrl = "  "), bannerHash = "")
        assertEquals("新作公开", body.title)
        assertEquals("导语", body.preview)
        assertEquals("正文", body.contentMarkdown)
        assertNull(body.sourceUrl)
        assertNull(body.bannerImageHash)

        val withBanner = newsCreateBody(valid, bannerHash = "abc")
        assertEquals("abc", withBanner.bannerImageHash)
    }

    @Test
    fun `编辑只发变化字段`() {
        val initial = submission(bannerHash = "old")
        val patch = newsPatchBody(
            initial = initial,
            values = NewsFormValues(
                title = "新标题",
                preview = "导语",
                contentMarkdown = "正文",
                sourceUrl = "https://example.com/x",
            ),
            bannerHash = "new",
        )
        assertEquals("新标题", patch?.title)
        assertNull(patch?.preview)
        assertNull(patch?.contentMarkdown)
        assertEquals("https://example.com/x", patch?.sourceUrl)
        assertEquals("new", patch?.bannerImageHash)
    }

    @Test
    fun `没有改动时不发请求`() {
        val initial = submission()
        assertNull(newsPatchBody(initial, NewsFormValues("新作公开", "导语", "正文", ""), bannerHash = ""))
    }

    @Test
    fun `移除封面发送空哈希`() {
        val patch = newsPatchBody(
            initial = submission(bannerHash = "old"),
            values = NewsFormValues("新作公开", "导语", "正文", ""),
            bannerHash = "",
        )
        assertEquals("", patch?.bannerImageHash)
    }

    @Test
    fun `只有待审核与已发布可编辑`() {
        assertTrue(isNewsSubmissionEditable("pending"))
        assertTrue(isNewsSubmissionEditable("published"))
        assertFalse(isNewsSubmissionEditable("rejected"))
        assertFalse(isNewsSubmissionEditable("withdrawn"))
    }

    @Test
    fun `状态文案`() {
        assertEquals("待审核", newsSubmissionStateLabel("pending"))
        assertEquals("已发布", newsSubmissionStateLabel("published"))
        assertEquals("未通过", newsSubmissionStateLabel("rejected"))
        assertEquals("已撤回", newsSubmissionStateLabel("withdrawn"))
    }
}
