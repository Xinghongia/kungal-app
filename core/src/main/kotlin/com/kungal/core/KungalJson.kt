package com.kungal.core

import kotlinx.serialization.json.Json

/**
 * kungal v1 API 的 JSON 约定：
 * - 服务端会随版本增加字段（show an unknown token with a neutral fallback），客户端必须容忍未知键
 * - 可空字段在响应里可能整体缺省（explicitNulls=false 让「缺省」和「null」等价）
 */
val KungalJson: Json = Json {
    ignoreUnknownKeys = true
    explicitNulls = false
    // 请求体的必填字段即使有默认值也要发出（如 access_scope、choice_type）
    encodeDefaults = true
}
