package com.kungal.app.ui.quiz

import com.kungal.core.model.QuizSubmission
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class QuizCatalogTest {

    @Test
    fun `难度档位与网页一致`() {
        assertEquals("入门", quizDifficultyLabel(1))
        assertEquals("入门", quizDifficultyLabel(2))
        assertEquals("简单", quizDifficultyLabel(3))
        assertEquals("简单", quizDifficultyLabel(4))
        assertEquals("普通", quizDifficultyLabel(5))
        assertEquals("困难", quizDifficultyLabel(7))
        assertEquals("地狱", quizDifficultyLabel(9))
        assertEquals("地狱", quizDifficultyLabel(10))
    }

    @Test
    fun `排序字段拼升降序`() {
        assertEquals("bumped_at_desc", quizSort("bumped_at", true))
        assertEquals("answer_count_asc", quizSort("answer_count", false))
    }

    @Test
    fun `正确率按人数计算`() {
        assertNull(quizCorrectRate(0, 0))
        assertEquals(38, quizCorrectRate(16, 6))
        assertEquals(33, quizCorrectRate(3, 1))
        assertEquals(100, quizCorrectRate(5, 5))
    }

    @Test
    fun `作答记录展示`() {
        assertEquals(
            "选 A、C",
            formatQuizSubmission("multiple", QuizSubmission(choiceIndexes = listOf(2, 0))),
        )
        assertEquals("选 B", formatQuizSubmission("single", QuizSubmission(listOf(1))))
        assertEquals("未选", formatQuizSubmission("single", QuizSubmission(emptyList())))
        assertEquals(
            "答: 正确",
            formatQuizSubmission("judge", QuizSubmission(isStatementTrue = true)),
        )
        assertEquals(
            "答: 错误",
            formatQuizSubmission("judge", QuizSubmission(isStatementTrue = false)),
        )
        assertEquals("", formatQuizSubmission("single", null))
    }

    @Test
    fun `选项校验与网页规则一致`() {
        assertEquals("至少需要 2 个选项", validateQuizChoices("single", listOf("A"), listOf(0)))
        assertEquals(
            "选项内容不能为空",
            validateQuizChoices("single", listOf("A", " "), listOf(0)),
        )
        assertEquals(
            "选项长度不能超过 200 字",
            validateQuizChoices("multiple", listOf("A", "B".repeat(201)), listOf(0)),
        )
        assertEquals(
            "选项不能重复",
            validateQuizChoices("multiple", listOf("A", "A"), listOf(0)),
        )
        assertEquals(
            "请标记唯一正确答案",
            validateQuizChoices("single", listOf("A", "B"), listOf(0, 1)),
        )
        assertEquals(
            "请至少标记一个正确答案",
            validateQuizChoices("multiple", listOf("A", "B"), emptyList()),
        )
        assertNull(validateQuizChoices("single", listOf("A", "B"), listOf(1)))
        assertNull(validateQuizChoices("multiple", listOf("A", "B"), listOf(0, 1)))
        assertNull(validateQuizChoices("judge", emptyList(), emptyList()))
    }

    @Test
    fun `题目与解析长度校验`() {
        assertEquals("请填写题干", validateQuizText(" ", "", ""))
        assertEquals("题干不能超过 200 字", validateQuizText("x".repeat(201), "", ""))
        assertEquals("题目描述不能超过 20000 字", validateQuizText("题", "", "x".repeat(20001)))
        assertEquals("解析不能超过 2000 字", validateQuizText("题", "x".repeat(2001), ""))
        assertNull(validateQuizText("题", "解析", "描述"))
    }

    @Test
    fun `作答输入就绪判断与提交体`() {
        assertFalse(QuizAnswerInput(type = "single").isReady())
        val single = QuizAnswerInput(type = "single", singleChoice = 2)
        assertTrue(single.isReady())
        assertEquals(listOf(2), single.toSubmission().choiceIndexes)

        assertFalse(QuizAnswerInput(type = "multiple").isReady())
        val multiple = QuizAnswerInput(type = "multiple", multipleChoices = setOf(2, 0))
        assertEquals(listOf(0, 2), multiple.toSubmission().choiceIndexes)

        assertFalse(QuizAnswerInput(type = "judge").isReady())
        val judge = QuizAnswerInput(type = "judge", judgeAnswer = true)
        assertTrue(judge.isReady())
        assertEquals(true, judge.toSubmission().isStatementTrue)
        assertTrue(judge.toSubmission().choiceIndexes.isEmpty())
    }
}
