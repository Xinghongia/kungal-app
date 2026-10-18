package com.kungal.app.ui.activity

/** 更新日志类型 → 中文标签（网页 constants/update.ts）。 */
val UpdateLogChangeTypeLabels: Map<String, String> = mapOf(
    "feat" to "增加功能",
    "perf" to "性能优化",
    "fix" to "错误修复",
    "style" to "样式修改",
    "mod" to "功能更改",
    "chore" to "其它修改",
    "sec" to "安全提升",
    "refactor" to "代码重构",
    "docs" to "文档修改",
    "test" to "测试用例",
)

fun updateLogChangeTypeLabel(type: String): String =
    UpdateLogChangeTypeLabels[type] ?: type

val TodoStateLabels: Map<String, String> = mapOf(
    "pending" to "待处理",
    "in_progress" to "进行中",
    "done" to "已完成",
    "discarded" to "已废弃",
)

val TodoProjectLabels: Map<String, String> = mapOf(
    "forum" to "论坛",
    "patch" to "补丁站",
)

fun todoStateLabel(state: String): String = TodoStateLabels[state] ?: state

fun todoProjectLabel(project: String): String = TodoProjectLabels[project] ?: project
