package com.kungal.app.ui.toolset

import java.util.Locale

/**
 * 工具集文件上传规则（与站点 config/upload.ts 及服务端 apiv1/helpers.go 对齐）。
 * 服务端是最终权威；这里只做选择文件时的前置校验，避免把必然失败的请求发出去。
 */
const val UPLOAD_MB = 1024L * 1024L
const val MAX_SMALL_FILE_SIZE = 50 * UPLOAD_MB
const val MAX_LARGE_FILE_SIZE = 2L * 1024 * UPLOAD_MB
const val USER_DAILY_UPLOAD_LIMIT = 100 * UPLOAD_MB
const val MOEMOEPOINT_SINGLE_MB_DIVISOR = 10L

data class ToolsetUploadQuota(
    val moemoepoint: Int,
    val usedToday: Long,
    val bypass: Boolean,
) {
    /** 今日总额度：100MB + 萌萌点 × 1MB；白名单账号不限（按 2GB 计）。 */
    val dailyBudget: Long =
        if (bypass) MAX_LARGE_FILE_SIZE else USER_DAILY_UPLOAD_LIMIT + moemoepoint * UPLOAD_MB

    /** 白名单账号不受每日用量限制（网页同样只按 2GB 展示）。 */
    val dailyRemaining: Long =
        if (bypass) MAX_LARGE_FILE_SIZE else (dailyBudget - usedToday).coerceAtLeast(0)

    /** 单文件上限：min(max(100MB, 萌萌点/10 MB), 2GB)；白名单账号直接 2GB。 */
    val maxSingleFile: Long =
        if (bypass) {
            MAX_LARGE_FILE_SIZE
        } else {
            minOf(
                maxOf(USER_DAILY_UPLOAD_LIMIT, (moemoepoint / MOEMOEPOINT_SINGLE_MB_DIVISOR) * UPLOAD_MB),
                MAX_LARGE_FILE_SIZE,
            )
        }
}

fun isValidToolsetArchiveName(name: String): Boolean =
    Regex("""\.(7z|zip|rar)$""", RegexOption.IGNORE_CASE).containsMatchIn(name)

/** 返回 null 表示通过；否则返回给用户看的错误文案。 */
fun checkToolsetUploadFile(name: String, size: Long, quota: ToolsetUploadQuota): String? = when {
    !isValidToolsetArchiveName(name) -> "仅支持 .7z、.zip、.rar 压缩格式"
    size < 1 -> "文件为空，无法上传"
    size > MAX_LARGE_FILE_SIZE -> "文件超过最大限制 ${formatUploadMb(MAX_LARGE_FILE_SIZE)} MB"
    size > quota.dailyRemaining ->
        "超出今日可用上传额度，剩余 ${formatUploadMb(quota.dailyRemaining)} MB"
    size > quota.maxSingleFile ->
        "单文件超过限制，最大 ${formatUploadMb(quota.maxSingleFile)} MB"
    else -> null
}

/** 额度与上限的 MB 文案（最多两位小数，去掉无意义的 0）。 */
fun formatUploadMb(bytes: Long): String =
    String.format(Locale.US, "%.2f", bytes / 1024.0 / 1024.0)
        .trimEnd('0')
        .trimEnd('.')
        .ifEmpty { "0" }
