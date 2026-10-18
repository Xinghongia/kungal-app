package com.kungal.app.ui.news

import com.kungal.core.model.NewsSubmission
import com.kungal.core.model.NewsSubmissionCreate
import com.kungal.core.model.NewsSubmissionPatch

/** 与网页 `validations/news.ts` 相同的字数上限。 */
const val NEWS_TITLE_MAX = 200
const val NEWS_PREVIEW_MAX = 200
const val NEWS_CONTENT_MAX = 20000
const val NEWS_SOURCE_URL_MAX = 1024

/** 服务端封面上限 4,000,000 字节。 */
const val NEWS_BANNER_MAX_BYTES = 4_000_000

data class NewsFormValues(
    val title: String = "",
    val preview: String = "",
    val contentMarkdown: String = "",
    val sourceUrl: String = "",
)

/**
 * 投稿表单校验，规则与网页 zod schema 一致；
 * 返回第一条错误文案，全部通过返回 null。
 */
fun validateNewsForm(values: NewsFormValues): String? {
    val title = values.title.trim()
    val preview = values.preview.trim()
    val content = values.contentMarkdown
    val sourceUrl = values.sourceUrl.trim()
    return when {
        title.isEmpty() -> "请填写情报标题"
        title.length > NEWS_TITLE_MAX -> "标题不能超过 $NEWS_TITLE_MAX 个字"
        preview.isEmpty() -> "请填写导语"
        preview.length > NEWS_PREVIEW_MAX -> "导语不能超过 $NEWS_PREVIEW_MAX 个字"
        content.length > NEWS_CONTENT_MAX -> "正文不能超过 $NEWS_CONTENT_MAX 个字"
        sourceUrl.length > NEWS_SOURCE_URL_MAX -> "原文链接过长"
        sourceUrl.isNotEmpty() && !sourceUrl.matches(Regex("""^https?://\S+$""", RegexOption.IGNORE_CASE)) ->
            "原文链接必须以 http:// 或 https:// 开头"
        content.trim().isEmpty() && sourceUrl.isEmpty() -> "正文和原文链接至少要填写一项"
        else -> null
    }
}

/** 新建请求体：空白正文/链接不发送（与网页一致）。 */
fun newsCreateBody(
    values: NewsFormValues,
    bannerHash: String,
): NewsSubmissionCreate = NewsSubmissionCreate(
    title = values.title.trim(),
    preview = values.preview.trim(),
    contentMarkdown = values.contentMarkdown.takeIf { it.trim().isNotEmpty() },
    sourceUrl = values.sourceUrl.trim().takeIf { it.isNotEmpty() },
    bannerImageHash = bannerHash.takeIf { it.isNotEmpty() },
)

/**
 * 编辑请求体：只发与初始值不同的字段；[bannerHash] 为当前有效封面哈希
 * （空串表示移除封面）。没有任何变化时返回 null，不发送请求。
 */
fun newsPatchBody(
    initial: NewsSubmission,
    values: NewsFormValues,
    bannerHash: String,
): NewsSubmissionPatch? {
    val title = values.title.trim()
    val preview = values.preview.trim()
    val sourceUrl = values.sourceUrl.trim()
    val body = NewsSubmissionPatch(
        title = title.takeIf { it != initial.title },
        preview = preview.takeIf { it != initial.preview },
        contentMarkdown = values.contentMarkdown.takeIf { it != initial.contentMarkdown },
        sourceUrl = sourceUrl.takeIf { it != initial.sourceUrl },
        bannerImageHash = bannerHash.takeIf { it != initial.banner?.hash.orEmpty() },
    )
    return if (
        body.title == null && body.preview == null && body.contentMarkdown == null &&
        body.sourceUrl == null && body.bannerImageHash == null
    ) {
        null
    } else {
        body
    }
}

/** 待审核与已发布可以编辑；未通过与已撤回是终态。 */
fun isNewsSubmissionEditable(state: String): Boolean =
    state == "pending" || state == "published"

fun newsSubmissionStateLabel(state: String): String = when (state) {
    "pending" -> "待审核"
    "published" -> "已发布"
    "rejected" -> "未通过"
    "withdrawn" -> "已撤回"
    else -> state
}
