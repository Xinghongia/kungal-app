package com.kungal.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.QuizAnswer
import com.kungal.core.model.QuizDetail
import com.kungal.core.model.QuizViewer
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class QuizDetailState(
    val quiz: QuizDetail? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val submitting: Boolean = false,
    val favoritePending: Boolean = false,
    val ratingPending: Boolean = false,
    val input: QuizAnswerInput = QuizAnswerInput(),
    val answeredNow: Boolean = false,
    val records: List<QuizAnswer> = emptyList(),
    val recordsCursor: String? = null,
    val recordsLoading: Boolean = false,
    val recordsError: String? = null,
    val deleted: Boolean = false,
)

/**
 * 题目详情与作答（BP-05 U3）。
 * 作答成功后就地更新题目的统计与本人状态，不整页重载；
 * 重复作答（409 ALREADY_EXISTS）按网页口径重载题目，视为已作答。
 */
class QuizDetailViewModel(
    private val app: KungalApp,
    private val quizId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizDetailState())
    val state: StateFlow<QuizDetailState> = _state.asStateFlow()

    private var loadJob: Job? = null
    private var answerKey: String? = null
    private var answerKeySignature: String? = null

    init {
        load()
    }

    fun load() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(loading = it.quiz == null, error = null) }
            runCatching { app.quizApi.detail(quizId) }
                .onSuccess { quiz ->
                    _state.update {
                        it.copy(
                            quiz = quiz,
                            loading = false,
                            error = null,
                            input = if (it.input.type == quiz.quizType && it.quiz != null) {
                                it.input
                            } else {
                                QuizAnswerInput(type = quiz.quizType)
                            },
                        )
                    }
                    loadRecords(reset = true)
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e)) }
                }
        }
    }

    fun selectSingle(index: Int) {
        _state.update { it.copy(input = it.input.copy(singleChoice = index)) }
    }

    fun toggleMultiple(index: Int) {
        _state.update {
            val next = it.input.multipleChoices.toMutableSet()
            if (!next.add(index)) next.remove(index)
            it.copy(input = it.input.copy(multipleChoices = next))
        }
    }

    fun selectJudge(value: Boolean) {
        _state.update { it.copy(input = it.input.copy(judgeAnswer = value)) }
    }

    /** 提交答案；未登录由界面先拦下。 */
    fun submit() {
        val current = _state.value
        val quiz = current.quiz ?: return
        if (current.submitting) return
        if (!current.input.isReady()) {
            AppMessages.show(
                when (quiz.quizType) {
                    "single" -> "请选择一个答案"
                    "multiple" -> "请至少选择一个答案"
                    else -> "请选择正确或错误"
                },
            )
            return
        }
        val submission = current.input.toSubmission()
        val signature = "${submission.choiceIndexes}:${submission.isStatementTrue}"
        if (answerKey == null || answerKeySignature != signature) {
            answerKey = UUID.randomUUID().toString()
            answerKeySignature = signature
        }
        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            runCatching {
                app.quizApi.answer(
                    quizId = quizId,
                    submission = submission,
                    idempotencyKey = answerKey!!,
                )
            }.onSuccess { result ->
                answerKey = null
                answerKeySignature = null
                _state.update { prev ->
                    val answered = result.answer
                    val solution = result.solution
                    val quizNow = prev.quiz
                    val correct = answered?.isCorrect == true
                    prev.copy(
                        submitting = false,
                        answeredNow = true,
                        quiz = quizNow?.copy(
                            solution = solution ?: quizNow.solution,
                            answerCount = quizNow.answerCount + 1,
                            correctCount = quizNow.correctCount + if (correct) 1 else 0,
                            viewer = (quizNow.viewer ?: QuizViewer()).copy(
                                hasAnswered = true,
                                answer = answered,
                            ),
                        ),
                    )
                }
                AppMessages.show(if (result.answer?.isCorrect == true) "回答正确！" else "回答错误")
                if (_state.value.quiz?.isWorkHidden == true) load()
                loadRecords(reset = true)
            }.onFailure { e ->
                _state.update { it.copy(submitting = false) }
                if (e is KungalApiException && e.problem?.code == "ALREADY_EXISTS") {
                    AppMessages.show("你已经作答过这道题")
                    _state.update { it.copy(answeredNow = true) }
                    load()
                } else {
                    AppMessages.show(friendlyError(e))
                }
            }
        }
    }

    fun toggleFavorite() {
        val quiz = _state.value.quiz ?: return
        if (_state.value.favoritePending) return
        val next = !(quiz.viewer?.hasFavorited ?: false)
        viewModelScope.launch {
            _state.update { it.copy(favoritePending = true) }
            runCatching { app.quizApi.setFavorite(quizId, next) }
                .onSuccess { result ->
                    _state.update { prev ->
                        val quizNow = prev.quiz ?: return@update prev
                        prev.copy(
                            favoritePending = false,
                            quiz = quizNow.copy(
                                favoriteCount = result.favoriteCount,
                                viewer = (quizNow.viewer ?: QuizViewer()).copy(
                                    hasFavorited = result.viewer?.hasFavorited ?: next,
                                ),
                            ),
                        )
                    }
                    AppMessages.show(if (next) "已收藏" else "已取消收藏")
                }
                .onFailure {
                    _state.update { prev -> prev.copy(favoritePending = false) }
                    AppMessages.show(friendlyError(it))
                }
        }
    }

    fun rateQuality(rating: Int) {
        val quiz = _state.value.quiz ?: return
        if (_state.value.ratingPending) return
        if (rating !in 1..10) return
        viewModelScope.launch {
            _state.update { it.copy(ratingPending = true) }
            runCatching { app.quizApi.rateQuality(quizId, rating) }
                .onSuccess { result ->
                    _state.update { prev ->
                        val quizNow = prev.quiz ?: return@update prev
                        prev.copy(
                            ratingPending = false,
                            quiz = quizNow.copy(
                                qualityAverage = result.qualityAverage,
                                qualityCount = result.qualityCount,
                                viewer = (quizNow.viewer ?: QuizViewer()).copy(
                                    qualityRating = result.viewer?.qualityRating
                                        ?: quiz.viewer?.qualityRating,
                                ),
                            ),
                        )
                    }
                    AppMessages.show("评分成功")
                }
                .onFailure {
                    _state.update { prev -> prev.copy(ratingPending = false) }
                    AppMessages.show(
                        if (it is KungalApiException && it.problem?.code == "QUIZ_ANSWER_REQUIRED") {
                            "作答后才可以评分"
                        } else {
                            friendlyError(it)
                        },
                    )
                }
        }
    }

    fun loadMoreRecords() {
        val current = _state.value
        if (current.recordsLoading || current.recordsCursor == null) return
        loadRecords(reset = false)
    }

    private fun loadRecords(reset: Boolean) {
        val cursor = if (reset) null else _state.value.recordsCursor
        viewModelScope.launch {
            _state.update {
                it.copy(
                    recordsLoading = true,
                    recordsError = null,
                    records = if (reset) emptyList() else it.records,
                )
            }
            runCatching { app.quizApi.answers(quizId, cursor = cursor, limit = 20) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            records = if (reset) page.items else it.records + page.items,
                            recordsCursor = page.nextCursor,
                            recordsLoading = false,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(recordsLoading = false, recordsError = friendlyError(e))
                    }
                }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        viewModelScope.launch {
            runCatching { app.quizApi.delete(quizId) }
                .onSuccess {
                    _state.update { it.copy(deleted = true) }
                    AppMessages.show("已删除")
                    onDeleted()
                }
                .onFailure { AppMessages.show(friendlyError(it)) }
        }
    }
}
