package com.kungal.app.ui.galgame

import com.kungal.core.model.SubmissionIntroduction
import com.kungal.core.model.SubmissionTitle
import com.kungal.core.model.WorkSubmissionCreate
import java.time.LocalDate

/** 投稿表单的多语言标题/介绍封面：key 为 BCP-47 标签，顺序与网页一致。 */
data class SubmissionLocale(val tag: String, val label: String)

val SubmissionLocales = listOf(
    SubmissionLocale("en", "英语"),
    SubmissionLocale("ja", "日语"),
    SubmissionLocale("zh-Hans", "简体中文"),
    SubmissionLocale("zh-Hant", "繁体中文"),
)

/** 资料库（catalog）接受的原始语言闭集；网页去掉「其它」后原样提交。 */
val SubmissionOriginalLanguages = listOf(
    "ja" to "日语",
    "en" to "英语",
    "zh-Hans" to "简体中文",
    "zh-Hant" to "繁体中文",
    "ko" to "韩语",
    "ru" to "俄语",
    "es" to "西班牙语",
    "uk" to "乌克兰语",
    "fr" to "法语",
    "de" to "德语",
    "it" to "意大利语",
    "pt-br" to "葡萄牙语 (巴西)",
    "pt-pt" to "葡萄牙语",
    "pl" to "波兰语",
    "id" to "印尼语",
    "th" to "泰语",
    "vi" to "越南语",
    "tr" to "土耳其语",
    "cs" to "捷克语",
    "ar" to "阿拉伯语",
    "nl" to "荷兰语",
    "sv" to "瑞典语",
)

fun submissionOriginalLanguageLabel(value: String): String =
    SubmissionOriginalLanguages.firstOrNull { it.first == value }?.second ?: value

/** 投稿向导（新建申请）的表单状态；提交时转成 [WorkSubmissionCreate]。 */
data class SubmissionFormDraft(
    val titles: Map<String, String> = SubmissionLocales.associate { it.tag to "" },
    val introductions: Map<String, String> = SubmissionLocales.associate { it.tag to "" },
    val aliases: List<String> = emptyList(),
    val contentLimit: String = "",
    val ageLimitR18: Boolean = false,
    val originalLanguage: String = "ja",
    val releaseDate: String = "",
    val releaseDateTba: Boolean = false,
    val bannerHash: String? = null,
)

/**
 * 表单 → 请求体：丢掉空白的标题/介绍/别名；日期为空或 TBA 时不发，
 * 有日期时精度为 day（与网页的日期输入一致）。
 */
fun buildSubmissionRequest(
    draft: SubmissionFormDraft,
    isDuplicateConfirmed: Boolean = false,
): WorkSubmissionCreate {
    val titles = SubmissionLocales.mapNotNull { locale ->
        draft.titles[locale.tag]?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { SubmissionTitle(locale = locale.tag, title = it) }
    }
    val introductions = SubmissionLocales.mapNotNull { locale ->
        draft.introductions[locale.tag]?.trim()
            ?.takeIf { it.isNotEmpty() }
            ?.let { SubmissionIntroduction(locale = locale.tag, value = it) }
    }
    val releaseDate = if (draft.releaseDateTba) {
        null
    } else {
        draft.releaseDate.trim().takeIf { it.isNotEmpty() }
    }
    return WorkSubmissionCreate(
        titles = titles,
        aliases = draft.aliases.map { it.trim() }.filter { it.isNotEmpty() },
        introductions = introductions,
        originalLanguage = draft.originalLanguage,
        contentRating = if (draft.ageLimitR18) "r18" else "all_ages",
        isNsfw = draft.contentLimit == "nsfw",
        releaseDate = releaseDate,
        releaseDatePrecision = if (releaseDate != null) "day" else null,
        bannerHash = draft.bannerHash?.takeIf { it.isNotBlank() },
        isDuplicateConfirmed = isDuplicateConfirmed,
    )
}

/** 校验失败：指出应回到哪一步（1 名称 / 2 介绍 / 3 分级元数据）与提示。 */
data class SubmissionFormError(val step: Int, val message: String)

/** 与网页相同的提交前校验；返回第一条错误，全部通过返回 null。 */
fun validateSubmissionDraft(draft: SubmissionFormDraft): SubmissionFormError? {
    val titles = SubmissionLocales.mapNotNull { draft.titles[it.tag]?.trim()?.takeIf(String::isNotEmpty) }
    if (titles.isEmpty()) return SubmissionFormError(1, "至少需要填写一个语言版本的游戏名称")
    val aliases = draft.aliases.map { it.trim() }.filter { it.isNotEmpty() }
    if (titles.size + aliases.size > 100) return SubmissionFormError(1, "游戏名称与别名合计最多 100 个")
    if (aliases.any { it.length > 500 }) return SubmissionFormError(1, "每个别名最多 500 个字符")
    if (titles.any { it.length > 500 }) return SubmissionFormError(1, "游戏名称最多 500 个字符")
    val hasIntro = SubmissionLocales.any { draft.introductions[it.tag]?.trim()?.isNotEmpty() == true }
    if (!hasIntro) return SubmissionFormError(2, "至少需要填写一个语言版本的游戏介绍")
    if (draft.contentLimit != "sfw" && draft.contentLimit != "nsfw") {
        return SubmissionFormError(3, "请选择 SFW 或 NSFW")
    }
    if (draft.originalLanguage.isBlank()) return SubmissionFormError(3, "请选择原始语言")
    if (!draft.releaseDateTba) {
        val date = draft.releaseDate.trim()
        if (date.isNotEmpty() && runCatching { LocalDate.parse(date) }.isFailure) {
            return SubmissionFormError(3, "发售日期格式应为 YYYY-MM-DD 或留空")
        }
    }
    return null
}
