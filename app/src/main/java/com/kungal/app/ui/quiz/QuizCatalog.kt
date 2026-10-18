package com.kungal.app.ui.quiz

import com.kungal.core.model.QuizSubmission
import com.kungal.core.model.QuizSummary

/** 题库的文案、筛选项与纯展示逻辑（与网页 constants/galgame-quiz.ts 对齐）。 */

val QuizTypeOptions: List<Pair<String, String>> = listOf(
    "single" to "单选",
    "multiple" to "多选",
    "judge" to "判断",
)

val QuizTypeDescriptions: Map<String, String> = mapOf(
    "single" to "给出多个选项，只有一个正确答案",
    "multiple" to "给出多个选项，有一个或多个正确答案",
    "judge" to "判断一句话是否正确",
)

val QuizCategoryOptions: List<Pair<String, String>> = listOf(
    "plot" to "剧情",
    "character" to "角色",
    "system" to "系统",
    "music" to "音乐",
    "voice" to "声优",
    "company" to "会社",
    "trivia" to "常识",
    "other" to "其他",
)

val QuizSpoilerOptions: List<Pair<String, String>> = listOf(
    "none" to "无剧透",
    "portion" to "部分剧透",
    "serious" to "严重剧透",
)

val QuizSortOptions: List<Pair<String, String>> = listOf(
    "bumped_at" to "最近更新",
    "created" to "创建时间",
    "view_1d" to "日浏览数",
    "view_7d" to "周浏览数",
    "view_30d" to "月浏览数",
    "view" to "总浏览数",
    "difficulty" to "难度",
    "answer_count" to "作答数",
)

val QuizDifficultyOptions: List<Pair<String, String>> =
    (1..10).map { "$it" to "$it · ${quizDifficultyLabel(it)}" }

fun quizTypeLabel(value: String): String =
    QuizTypeOptions.firstOrNull { it.first == value }?.second ?: value

fun quizCategoryLabel(value: String): String =
    QuizCategoryOptions.firstOrNull { it.first == value }?.second ?: value

fun quizSpoilerLabel(value: String): String =
    QuizSpoilerOptions.firstOrNull { it.first == value }?.second ?: value

fun quizDifficultyLabel(difficulty: Int): String = when {
    difficulty <= 2 -> "入门"
    difficulty <= 4 -> "简单"
    difficulty <= 6 -> "普通"
    difficulty <= 8 -> "困难"
    else -> "地狱"
}

/** 题库排序由字段 + 升降序拼成（网页同款）。 */
fun quizSort(field: String, descending: Boolean): String =
    "${field}_${if (descending) "desc" else "asc"}"

/** 正确率百分比；无人作答时为 null。 */
fun quizCorrectRate(answerCount: Int, correctCount: Int): Int? =
    if (answerCount > 0) Math.round(correctCount * 100f / answerCount) else null

fun quizCorrectRate(quiz: QuizSummary): Int? =
    quizCorrectRate(quiz.answerCount, quiz.correctCount)

/** 作答记录里「选 A、C」/「答: 正确」的展示文本（与网页同口径）。 */
fun formatQuizSubmission(type: String, submission: QuizSubmission?): String {
    if (submission == null) return ""
    if (type == "judge") {
        return when (submission.isStatementTrue) {
            true -> "答: 正确"
            false -> "答: 错误"
            null -> ""
        }
    }
    if (submission.choiceIndexes.isEmpty()) return "未选"
    return "选 " + submission.choiceIndexes
        .sorted()
        .joinToString("、") { ('A' + it).toString() }
}

/** 选项校验（网页同款）：2–20 个、非空、≤200 字、不重复、至少一个正确答案。 */
fun validateQuizChoices(
    type: String,
    choices: List<String>,
    correctIndexes: List<Int>,
): String? {
    if (type == "judge") return null
    val options = choices.map { it.trim() }
    if (options.size < 2) return "至少需要 2 个选项"
    if (options.size > 20) return "最多 20 个选项"
    if (options.any { it.isEmpty() }) return "选项内容不能为空"
    if (options.any { it.length > 200 }) return "选项长度不能超过 200 字"
    if (options.toSet().size != options.size) return "选项不能重复"
    if (type == "single") {
        if (correctIndexes.size != 1 || correctIndexes[0] !in options.indices) {
            return "请标记唯一正确答案"
        }
    } else {
        if (correctIndexes.isEmpty()) return "请至少标记一个正确答案"
        if (correctIndexes.any { it !in options.indices }) return "正确答案超出选项范围"
    }
    return null
}

/** 题干/解析的基本校验；返回 null 表示通过。 */
fun validateQuizText(prompt: String, explanation: String, description: String): String? {
    if (prompt.isBlank()) return "请填写题干"
    if (prompt.length > 200) return "题干不能超过 200 字"
    if (description.length > 20000) return "题目描述不能超过 20000 字"
    if (explanation.length > 2000) return "解析不能超过 2000 字"
    return null
}

/** 题库详情里未作答时的输入状态。 */
data class QuizAnswerInput(
    val type: String = "single",
    val singleChoice: Int = -1,
    val multipleChoices: Set<Int> = emptySet(),
    val judgeAnswer: Boolean? = null,
) {
    fun isReady(): Boolean = when (type) {
        "single" -> singleChoice >= 0
        "multiple" -> multipleChoices.isNotEmpty()
        "judge" -> judgeAnswer != null
        else -> false
    }

    fun toSubmission(): QuizSubmission = when (type) {
        "single" -> QuizSubmission(choiceIndexes = listOf(singleChoice))
        "multiple" -> QuizSubmission(choiceIndexes = multipleChoices.sorted())
        else -> QuizSubmission(
            choiceIndexes = emptyList(),
            isStatementTrue = judgeAnswer,
        )
    }

    fun reset(type: String) = QuizAnswerInput(type = type)
}
