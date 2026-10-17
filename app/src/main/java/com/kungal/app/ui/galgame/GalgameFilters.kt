package com.kungal.app.ui.galgame

import com.kungal.core.model.WorkSummary
import java.util.Locale

/** 一枚筛选项（值 + 中文标签），与站点词汇表一致。 */
data class FilterOption(val value: String, val label: String)

/** 资源平台筛选项；「模拟器」不是真实平台，选中后走运行环境轴。 */
val GalgamePlatformOptions: List<FilterOption> = listOf(
    FilterOption("win", "Windows 电脑版"),
    FilterOption("and", "安卓手机版"),
    FilterOption("emulator", "模拟器"),
    FilterOption("ios", "iOS"),
    FilterOption("mac", "macOS"),
    FilterOption("lin", "Linux"),
    FilterOption("web", "网页版"),
    FilterOption("mob", "功能机"),
    FilterOption("swi", "Nintendo Switch"),
    FilterOption("sw2", "Nintendo Switch 2"),
    FilterOption("n3d", "Nintendo 3DS"),
    FilterOption("nds", "Nintendo DS"),
    FilterOption("wii", "Wii"),
    FilterOption("wiu", "Wii U"),
    FilterOption("gba", "Game Boy Advance"),
    FilterOption("gbc", "Game Boy Color"),
    FilterOption("nes", "红白机 Famicom"),
    FilterOption("sfc", "超任 Super Famicom"),
    FilterOption("ps1", "PlayStation"),
    FilterOption("ps2", "PlayStation 2"),
    FilterOption("ps3", "PlayStation 3"),
    FilterOption("ps4", "PlayStation 4"),
    FilterOption("ps5", "PlayStation 5"),
    FilterOption("psp", "PlayStation Portable"),
    FilterOption("psv", "PlayStation Vita"),
    FilterOption("xb1", "Xbox"),
    FilterOption("xb3", "Xbox 360"),
    FilterOption("xbo", "Xbox One"),
    FilterOption("xxs", "Xbox Series X/S"),
    FilterOption("sat", "世嘉土星 Saturn"),
    FilterOption("smd", "Mega Drive"),
    FilterOption("scd", "Mega-CD"),
    FilterOption("drc", "Dreamcast"),
    FilterOption("pce", "PC Engine"),
    FilterOption("pcf", "PC-FX"),
    FilterOption("tdo", "3DO"),
    FilterOption("p88", "PC-88"),
    FilterOption("p98", "PC-98"),
    FilterOption("x1s", "Sharp X1"),
    FilterOption("x68", "Sharp X68000"),
    FilterOption("fm7", "FM-7"),
    FilterOption("fm8", "FM-8"),
    FilterOption("fmt", "FM Towns"),
    FilterOption("msx", "MSX"),
    FilterOption("dos", "DOS"),
    FilterOption("dvd", "DVD 播放机"),
    FilterOption("bdp", "蓝光播放机"),
    FilterOption("vnd", "VNDS"),
    FilterOption("oth", "其它平台"),
)

val GalgameEmulatorRuntimeOptions: List<FilterOption> = listOf(
    FilterOption("native-win", "Windows 原生"),
    FilterOption("native-and", "安卓直装"),
    FilterOption("tyranor-next", "Tyranor Next"),
    FilterOption("yukihub", "YukiHub"),
    FilterOption("native-ios", "iOS 原生"),
    FilterOption("winlator", "Winlator"),
    FilterOption("gamehub", "盖世游戏 GameHub"),
    FilterOption("kirikiroid2", "Kirikiroid2 / XP3Player"),
    FilterOption("krkrsdl2", "krkrsdl2（需逐作打包）"),
    FilterOption("onscripter", "ONScripter (ONS)"),
    FilterOption("joiplay", "JoiPlay"),
    FilterOption("easyrpg", "EasyRPG"),
    FilterOption("renpy-android", "Ren'Py 安卓版"),
    FilterOption("tyranor", "Tyranor"),
    FilterOption("other", "其它运行环境"),
)

val GalgameLanguageOptions: List<FilterOption> = listOf(
    FilterOption("zh-cn", "简体中文"),
    FilterOption("zh-tw", "繁体中文"),
    FilterOption("ja-jp", "日语"),
    FilterOption("en-us", "英语"),
    FilterOption("other", "其他语言"),
)

val GalgameResourceTypeOptions: List<FilterOption> = listOf(
    FilterOption("game", "游戏本体"),
    FilterOption("collection", "合集"),
    FilterOption("patch", "补丁"),
    FilterOption("crack_fix", "免CD / 修正"),
    FilterOption("mod", "MOD"),
    FilterOption("tool", "工具"),
    FilterOption("walkthrough", "攻略"),
    FilterOption("ost", "原声带"),
    FilterOption("voice", "语音"),
    FilterOption("cg", "鉴赏 CG"),
    FilterOption("wallpaper", "壁纸"),
    FilterOption("artbook", "设定资料"),
    FilterOption("video", "视频相关"),
    FilterOption("other", "其它"),
)

val GalgameGameTypeOptions: List<FilterOption> = listOf(
    FilterOption("ba_saku", "拔作"),
    FilterOption("plot", "剧情作"),
    FilterOption("moe", "萌系"),
    FilterOption("daily", "日常系"),
    FilterOption("uncategorized", "未分类"),
)

val GalgameProviderOptions: List<FilterOption> = listOf(
    FilterOption("baidu", "百度网盘"),
    FilterOption("aliyun", "阿里云盘"),
    FilterOption("quark", "夸克网盘"),
    FilterOption("pan123", "123盘"),
    FilterOption("tianyiyun", "天翼云盘"),
    FilterOption("caiyun", "和彩云"),
    FilterOption("xunlei", "迅雷网盘"),
    FilterOption("uc", "UC网盘"),
    FilterOption("lanzou", "蓝奏云"),
    FilterOption("other", "其他 (自建网盘等不限速)"),
)

/** 资源资料库排序（UI 键 → API token 在 [workSortToken] 里转换）。 */
val GalgameResourceSortFields: List<FilterOption> = listOf(
    FilterOption("time", "更新顺序"),
    FilterOption("created", "收录时间"),
    FilterOption("views", "总浏览数"),
    FilterOption("view_1d", "日浏览数"),
    FilterOption("view_7d", "周浏览数"),
    FilterOption("view_30d", "月浏览数"),
    FilterOption("release_date", "发售日期"),
    FilterOption("rating", "评分"),
)

/** 信息资料库排序（只有发售日期支持升降序）。 */
val GalgameLibrarySortFields: List<FilterOption> = listOf(
    FilterOption("popularity", "热度"),
    FilterOption("release_date", "发售日期"),
    FilterOption("time", "资料更新"),
)

/** 资源资料库的 UI 排序键 → OpenAPI WorkSort。 */
fun workSortToken(field: String, order: String): String {
    val key = when (field) {
        "time" -> "resource_updated"
        "views" -> "view"
        else -> field
    }
    val dir = if (order == "asc") "asc" else "desc"
    return "${key}_$dir"
}

/** 信息资料库的 UI 排序键 → OpenAPI LibraryWorkSort。 */
fun librarySortToken(field: String, order: String): String = when {
    field == "release_date" -> if (order == "asc") "released_asc" else "released_desc"
    field == "time" -> "updated_desc"
    else -> "popularity_desc"
}

/** 平台轴拆分：'emulator' 不发给 resource_platforms，而是转成运行环境筛选。 */
fun splitPlatformAxis(
    platforms: Set<String>,
    runtimes: Set<String>,
): Pair<List<String>, List<String>> {
    val realPlatforms = platforms.filter { it != "emulator" }
    if ("emulator" !in platforms) return realPlatforms to emptyList()
    val runtimeAxis = if (runtimes.isEmpty()) listOf("emulator") else runtimes.toList()
    return realPlatforms to runtimeAxis
}

/** 作品标题：优先简体中文名，其次实体自带名。 */
fun workTitle(work: WorkSummary): String =
    work.localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() }
        ?: work.displayName.ifBlank { "#${work.id}" }

/** 会社名：优先简体中文名。 */
fun companyName(work: WorkSummary): String? = work.maker?.let { company ->
    company.localized["zh-Hans"]?.value?.takeIf { it.isNotBlank() } ?: company.displayName
}

/** 评分展示：保留一位小数；无人评分返回 null。 */
fun ratingText(work: WorkSummary): String? {
    val score = work.ratingScore ?: return null
    if (work.ratingCount <= 0) return null
    return String.format(Locale.US, "%.1f", score)
}

/** 发售日展示：缺失时与网页一样写「发售日期未知」。 */
fun releaseText(work: WorkSummary): String =
    work.releaseDate?.takeIf { it.isNotBlank() } ?: "发售日期未知"

fun labelOf(options: List<FilterOption>, value: String): String =
    options.firstOrNull { it.value == value }?.label ?: value

/** 平台标签，最多取 [max]。 */
fun platformLabels(work: WorkSummary, max: Int = 3): List<String> =
    work.resourcePlatforms.take(max).map { labelOf(GalgamePlatformOptions, it) }
