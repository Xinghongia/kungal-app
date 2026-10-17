package com.kungal.app.ui.galgame

/**
 * 资源分享文本识别（BP-03）：从粘贴的网盘分享文本里提取链接、提取码、解压码。
 * 行为与网页一致：链接支持 http/https/ftp/ftps/magnet/ed2k/thunder，多个链接用逗号分隔；
 * 识别逻辑为本项目自行实现，不复制参考源码。
 */
data class ParsedResourceLinkInput(
    val links: List<String>,
    val extractionCode: String,
    val archivePassword: String,
) {
    val hasLinks: Boolean get() = links.isNotEmpty()
}

private val UrlPattern = Regex(
    """(?:ed2k://\|file\|[^|\r\n]+\|\d+\|[0-9a-fA-F]{32}\|(?:[^|\s]+\|)*/?)|""" +
        """(?:(?:https?|ftp|ftps|thunder)://[^\s<>"'，。！？、；：【】（）\u4e00-\u9fff]+)|""" +
        """(?:magnet:\?[^\s<>"'，。！？、；：【】（）\u4e00-\u9fff]+)""",
    RegexOption.IGNORE_CASE,
)

private val TrailingPunctuation = Regex("""[,.!?，。！？、；：」』"'\]】）>]+$""")

private val ExtractionCodePatterns = listOf(
    Regex("""提取码\s*[：:]\s*([a-zA-Z0-9]+)"""),
    Regex("""访问码\s*[：:]\s*([a-zA-Z0-9]+)"""),
    Regex("""pwd\s*[：:=]\s*([a-zA-Z0-9]+)""", RegexOption.IGNORE_CASE),
)

private val FallbackCodePattern = Regex("""(?<!解压)密码\s*[：:]\s*([a-zA-Z0-9]+)""")

private val ArchivePasswordPatterns = listOf(
    Regex("""解压码\s*[：:]\s*([^\s,，]+)"""),
    Regex("""解压密码\s*[：:]\s*([^\s,，]+)"""),
    Regex("""unzip(?:\s*code|\s*password)?\s*[：:=]\s*([^\s,，]+)""", RegexOption.IGNORE_CASE),
)

private fun cleanUrl(raw: String): String {
    var url = raw.replace(TrailingPunctuation, "")
    while (url.endsWith(")") && url.count { it == ')' } > url.count { it == '(' }) {
        url = url.dropLast(1).replace(TrailingPunctuation, "")
    }
    return url
}

private fun unique(values: List<String>): List<String> {
    val seen = LinkedHashSet<String>()
    values.forEach { if (it.isNotBlank()) seen.add(it) }
    return seen.toList()
}

/** 逗号（中英文）分隔的纯链接文本。 */
fun splitResourceLinkText(raw: String): List<String> =
    raw.split(',', '，').map { it.trim() }.filter { it.isNotEmpty() }

/** 从 URL 查询参数里取 pwd/password/code。 */
private fun queryCode(url: String): String? {
    val query = url.substringAfter('?', "").substringBefore('#')
    if (query.isBlank()) return null
    for (part in query.split('&')) {
        val key = part.substringBefore('=', "").lowercase()
        if (key !in setOf("pwd", "password", "code")) continue
        val value = part.substringAfter('=', "")
        if (value.isNotEmpty()) return value
    }
    return null
}

fun parseResourceLinkInput(raw: String): ParsedResourceLinkInput {
    val text = raw.trim()
    if (text.isEmpty()) return ParsedResourceLinkInput(emptyList(), "", "")

    val links = unique(UrlPattern.findAll(text).map { cleanUrl(it.value) }.toList())
    val queryCodes = links.mapNotNull(::queryCode)

    val textCodes = ExtractionCodePatterns.flatMap { pattern ->
        pattern.findAll(text).mapNotNull { it.groupValues.getOrNull(1) }.toList()
    }
    val codes = unique(queryCodes + textCodes).ifEmpty {
        unique(FallbackCodePattern.findAll(text).mapNotNull { it.groupValues.getOrNull(1) }.toList())
    }
    val passwords = unique(
        ArchivePasswordPatterns.flatMap { pattern ->
            pattern.findAll(text).mapNotNull { it.groupValues.getOrNull(1) }.toList()
        },
    )
    return ParsedResourceLinkInput(
        links = links,
        extractionCode = codes.joinToString(", "),
        archivePassword = passwords.joinToString(", "),
    )
}

/** 资源体积输入：数字 + MB/GB（服务端要求 `N[.NN] MB|GB`）。 */
fun resourceSizeValid(amount: String, unit: String): Boolean {
    if (unit !in setOf("MB", "GB")) return false
    return Regex("""\d{1,6}(\.\d{1,2})?""").matches(amount)
}

fun joinResourceSize(amount: String, unit: String): String =
    if (amount.isBlank()) "" else "$amount $unit"

/** 从 "919 MB" 这类存储值拆出数字与单位，供编辑表单回填。 */
fun splitResourceSize(size: String): Pair<String, String> {
    val match = Regex("""^\s*(\d{1,6}(?:\.\d{1,2})?)\s*(MB|GB)\s*$""", RegexOption.IGNORE_CASE)
        .find(size) ?: return "" to "GB"
    return match.groupValues[1] to match.groupValues[2].uppercase()
}

/** 这些资源类型必须带运行环境（与站点一致）。 */
fun resourceTypeHasRuntime(type: String): Boolean =
    type in setOf("game", "collection", "patch", "crack_fix", "mod", "tool")
