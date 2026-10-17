package com.kungal.app.ui.galgame

import com.kungal.core.model.CatalogIntro
import com.kungal.core.model.LocalizedText
import com.kungal.core.model.Work
import com.kungal.core.model.WorkExternalRef
import java.net.URL

/** 资料库词条标题：优先简体中文名。 */
fun workTitle(work: Work): String = catalogTitle(work.displayName, work.localized, work.id)

fun catalogTitle(displayName: String, localized: Map<String, LocalizedText>, id: String): String =
    localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() }
        ?: displayName.ifBlank { "#$id" }

/** 简介语言优先级：简中、繁中、日语、英语，最后取第一条。 */
fun pickIntro(intros: List<CatalogIntro>): CatalogIntro? {
    for (locale in listOf("zh-Hans", "zh-Hant", "ja", "en")) {
        intros.firstOrNull { it.locale == locale && it.value.isNotBlank() }?.let { return it }
    }
    return intros.firstOrNull { it.value.isNotBlank() }
}

fun introLanguageLabel(locale: String): String = when (locale) {
    "zh-Hans" -> "简体中文"
    "zh-Hant" -> "繁体中文"
    "ja" -> "日语"
    "en" -> "英语"
    else -> locale
}

fun introSourceLabel(source: String?): String? = when (source) {
    null, "" -> null
    "curated" -> "本站编辑"
    "vndb" -> "VNDB"
    "bangumi" -> "Bangumi"
    "erogamescape" -> "批评空间"
    "dlsite" -> "DLsite"
    else -> source
}

/**
 * 简介在网页端也是按纯文本显示的（markdownToText + 保留换行）。
 * 这里做同样的清理：去掉转义反斜杠与常见 Markdown 标记，保留段落换行。
 */
fun markdownToPlainText(source: String): String {
    var text = source.replace("\\r\\n", "\n").replace("\\\n", "\n").replace("\\", "")
    text = Regex("!\\[[^]]*]\\([^)]*\\)").replace(text, "")
    text = Regex("\\[([^]]+)]\\(([^)]+)\\)").replace(text) { it.groupValues[1] }
    text = text.replace(Regex("(\\*\\*|__)"), "")
    text = text.replace(Regex("(^|\\s)[*_]([^*_\\n]+)[*_]"), "$1$2")
    text = text.replace(Regex("`([^`]*)`"), "$1")
    text = text.replace(Regex("(?m)^#{1,6}\\s*"), "")
    return text.trim()
}

private val linkSiteLabels = mapOf(
    "official_site" to "官方网站",
    "twitter" to "X",
    "cien" to "Ci-en",
    "steam" to "Steam",
    "pixiv" to "pixiv",
    "vndb" to "VNDB",
    "bangumi" to "Bangumi",
    "erogamescape" to "批评空间",
    "dlsite" to "DLsite",
    "dmm" to "DMM",
    "getchu" to "Getchu",
)

private val linkHostLabels = listOf(
    "ci-en.dlsite.com" to "Ci-en",
    "wikipedia.org" to "维基百科",
    "wikidata.org" to "Wikidata",
    "youtube.com" to "YouTube",
    "nicovideo.jp" to "niconico",
    "x.com" to "X",
    "twitter.com" to "X",
    "pixiv.net" to "pixiv",
    "fanbox.cc" to "pixivFANBOX",
    "fantia.jp" to "Fantia",
    "patreon.com" to "Patreon",
    "booth.pm" to "BOOTH",
    "melonbooks.co.jp" to "Melonbooks",
    "steampowered.com" to "Steam",
    "dlsite.com" to "DLsite",
    "dmm.co.jp" to "DMM",
    "getchu.com" to "Getchu",
    "gamefaqs.gamespot.com" to "GameFAQs",
    "mobygames.com" to "MobyGames",
    "anidb.net" to "AniDB",
    "myanimelist.net" to "MyAnimeList",
    "bgm.tv" to "Bangumi",
    "bangumi.tv" to "Bangumi",
    "vndb.org" to "VNDB",
    "erogamescape.dyndns.org" to "批评空间",
)

fun catalogLinkLabel(site: String, url: String): String {
    linkSiteLabels[site]?.let { return it }
    val host = runCatching { URL(url).host.lowercase().removePrefix("www.") }.getOrNull()
        ?: return site
    val match = linkHostLabels.firstOrNull { (suffix, _) ->
        host == suffix || host.endsWith(".$suffix")
    }
    return match?.second ?: host
}

data class ExternalLink(val label: String, val url: String)

/** 身份 id 链接（VNDB / Bangumi / 批评空间），与网页的 link 模板一致。 */
fun identityLinks(refs: List<WorkExternalRef>): List<ExternalLink> {
    val out = mutableListOf<ExternalLink>()
    refs.firstOrNull { it.site == "vndb" }?.let {
        out += ExternalLink("VNDB ${it.externalId}", "https://vndb.org/${it.externalId}")
    }
    refs.firstOrNull { it.site == "bangumi" }?.let {
        out += ExternalLink("Bangumi ${it.externalId}", "https://bgm.tv/subject/${it.externalId}")
    }
    refs.firstOrNull { it.site == "erogamescape" }?.let {
        out += ExternalLink(
            "批评空间 ${it.externalId}",
            "https://erogamescape.dyndns.org/~ap2/ero/toukei_kaiseki/game.php?game=${it.externalId}",
        )
    }
    return out
}

fun contentRatingLabel(key: String): String = when (key) {
    "all_ages" -> "全年龄"
    "sensitive" -> "敏感"
    "r18" -> "R18"
    else -> key
}

fun originalLanguageLabel(tag: String?): String = when (tag?.lowercase()) {
    null, "" -> "未公布"
    "ja", "ja-jp" -> "日语"
    "zh", "zh-hans", "zh-cn" -> "简体中文"
    "zh-hant", "zh-tw" -> "繁体中文"
    "en", "en-us" -> "英语"
    "ko", "ko-kr" -> "韩语"
    else -> tag
}

fun coverSlotLabel(slot: String): String = when (slot) {
    "main" -> "主封面"
    "pkgfront" -> "盒装正面"
    "dig" -> "数字版"
    "pkgback" -> "封底"
    "pkgcontent" -> "内页"
    "pkgside" -> "书脊"
    "pkgmed" -> "碟面"
    else -> "其它"
}

fun characterKindLabel(kind: String): String = when (kind) {
    "main" -> "主要角色"
    "secondary" -> "次要角色"
    "appears" -> "登场角色"
    else -> "角色"
}

fun spoilerLabel(level: String): String = when (level) {
    "minor" -> "轻度剧透"
    "major" -> "严重剧透"
    else -> "无剧透"
}

fun attributionRoleLabel(role: String): String = when (role) {
    "developer" -> "开发商"
    "publisher" -> "发行商"
    "circle" -> "同人社团"
    "brand" -> "品牌"
    else -> role
}

fun tagKindLabel(kind: String): String = when (kind) {
    "content" -> "内容"
    else -> "属性"
}

fun formatMinutes(minutes: Int): String {
    if (minutes <= 0) return "0 分钟"
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours > 0 && rest > 0 -> "$hours 小时 $rest 分"
        hours > 0 -> "$hours 小时"
        else -> "$rest 分钟"
    }
}

fun externalRatingLabel(site: String): String = when (site) {
    "vndb" -> "VNDB"
    "bangumi" -> "Bangumi"
    "erogamescape" -> "批评空间"
    "dlsite" -> "DLsite"
    else -> site
}
