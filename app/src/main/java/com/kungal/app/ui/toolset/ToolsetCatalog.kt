package com.kungal.app.ui.toolset

import com.kungal.app.ui.galgame.FilterOption

/** 工具集词汇表（与站点一致）。 */
val ToolsetTypeOptions: List<FilterOption> = listOf(
    FilterOption("emulator", "模拟器相关"),
    FilterOption("translator", "翻译相关"),
    FilterOption("extractor", "解包工具"),
    FilterOption("converter", "资源转换工具"),
    FilterOption("debug", "开发者工具"),
    FilterOption("launcher", "游戏管理工具"),
    FilterOption("script", "自动化脚本"),
    FilterOption("docs", "文档相关"),
    FilterOption("others", "其它"),
)

val ToolsetPlatformOptions: List<FilterOption> = listOf(
    FilterOption("windows", "Windows"),
    FilterOption("mac", "macOS"),
    FilterOption("linux", "Linux"),
    FilterOption("emulator", "模拟器"),
    FilterOption("others", "其它"),
)

val ToolsetLanguageOptions: List<FilterOption> = listOf(
    FilterOption("zh-cn", "简体中文"),
    FilterOption("zh-tw", "繁体中文"),
    FilterOption("ja-jp", "日语"),
    FilterOption("en-us", "英语"),
    FilterOption("others", "其它"),
)

val ToolsetReleaseOptions: List<FilterOption> = listOf(
    FilterOption("stable", "稳定版本"),
    FilterOption("beta", "公测版本"),
    FilterOption("alpha", "内测版本"),
    FilterOption("rc", "候选版本"),
)

/** 排序字段（值 = 字段名，方向单独选择）。 */
val ToolsetSortFields: List<FilterOption> = listOf(
    FilterOption("resource_updated", "资源更新时间"),
    FilterOption("created", "发布时间"),
    FilterOption("view", "浏览数"),
)

fun labelOfToolset(options: List<FilterOption>, value: String): String =
    options.firstOrNull { it.value == value }?.label ?: value

fun toolsetSortToken(field: String, order: String): String {
    val normalized = if (field in setOf("resource_updated", "created", "view")) field else "resource_updated"
    return "${normalized}_$order"
}

/** 文件体积展示（工具资源 archive.file_size）。 */
fun formatToolsetFileSize(bytes: Long): String {
    if (bytes <= 0) return "大小未知"
    val mb = bytes / 1024.0 / 1024.0
    return if (mb >= 1024) String.format(java.util.Locale.US, "%.2f GB", mb / 1024)
    else String.format(java.util.Locale.US, "%.0f MB", mb)
}
