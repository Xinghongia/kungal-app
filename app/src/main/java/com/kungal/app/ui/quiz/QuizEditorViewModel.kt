package com.kungal.app.ui.quiz

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.QuizCreate
import com.kungal.core.model.QuizPatch
import com.kungal.core.model.WorkSummary
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class QuizEditorState(
    val loading: Boolean = false,
    val error: String? = null,
    val editing: Boolean = false,
    val quizType: String = "single",
    val category: String = "trivia",
    val difficulty: Int = 3,
    val spoilerLevel: String = "none",
    val prompt: String = "",
    val description: String = "",
    val explanation: String = "",
    val showExplanation: Boolean = false,
    val choices: List<String> = listOf("", ""),
    val correctIndexes: Set<Int> = setOf(0),
    val judgeAnswer: Boolean = true,
    val works: List<WorkSummary> = emptyList(),
    val isWorkHidden: Boolean = false,
    val workQuery: String = "",
    val workResults: List<WorkSummary> = emptyList(),
    val searchingWorks: Boolean = false,
    val submitting: Boolean = false,
)

/**
 * 出题/编辑（BP-05 U4，对应网页 GalgameQuizForm）。
 * 选项 2–20 个、各 ≤200 字、不重复；单选唯一正确、多选至少一个；判断用正确/错误。
 * 关联作品最多 20 部，可搜索作品名添加。
 */
class QuizEditorViewModel(
    private val app: KungalApp,
    private val quizId: String?,
    private val initialWorkId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(QuizEditorState(editing = quizId != null))
    val state: StateFlow<QuizEditorState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var createKey: String? = null
    private var createSignature: String? = null

    init {
        if (quizId != null) {
            loadSource()
        } else if (initialWorkId != null) {
            loadInitialWork(initialWorkId)
        }
    }

    private fun loadSource() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching {
                val source = app.quizApi.source(quizId!!)
                val works = runCatching { app.quizApi.detail(quizId).works }.getOrDefault(emptyList())
                source to works
            }.onSuccess { (source, works) ->
                val known = works.associateBy { it.id }
                val selected = source.workIds.map { id ->
                    known[id] ?: WorkSummary(id = id, displayName = "#$id")
                }
                _state.update {
                    it.copy(
                        loading = false,
                        editing = true,
                        quizType = source.quizType,
                        category = source.quizCategory,
                        difficulty = source.difficulty,
                        spoilerLevel = source.spoilerLevel,
                        prompt = source.promptText,
                        description = source.descriptionMarkdown,
                        explanation = source.explanationMarkdown,
                        showExplanation = source.explanationMarkdown.isNotBlank(),
                        choices = if (source.quizType == "judge") {
                            listOf("", "")
                        } else {
                            source.choices.ifEmpty { listOf("", "") }
                        },
                        correctIndexes = source.correctChoiceIndexes.toSet(),
                        judgeAnswer = source.isStatementTrue ?: true,
                        works = selected,
                        isWorkHidden = source.isWorkHidden,
                    )
                }
            }.onFailure {
                _state.update { s -> s.copy(loading = false, error = friendlyError(it)) }
            }
        }
    }

    private fun loadInitialWork(workId: String) {
        viewModelScope.launch {
            runCatching { app.galgameApi.work(workId, includeNsfw = true) }
                .onSuccess { work ->
                    val summary = WorkSummary(
                        id = work.id,
                        displayName = work.displayName,
                        latin = work.latin,
                        localized = work.localized,
                        cover = work.cover,
                        isNsfw = work.isNsfw,
                    )
                    _state.update { it.copy(works = listOf(summary)) }
                }
        }
    }

    fun setType(type: String) {
        if (_state.value.editing) return
        _state.update {
            it.copy(
                quizType = type,
                correctIndexes = if (type == "multiple") emptySet() else setOf(0),
                choices = it.choices.ifEmpty { listOf("", "") },
            )
        }
    }

    fun setCategory(value: String) = _state.update { it.copy(category = value) }

    fun setDifficulty(value: Int) = _state.update { it.copy(difficulty = value.coerceIn(1, 10)) }

    fun setSpoiler(value: String) = _state.update { it.copy(spoilerLevel = value) }

    fun setPrompt(value: String) = _state.update { it.copy(prompt = value.take(200)) }

    fun setDescription(value: String) = _state.update { it.copy(description = value.take(20000)) }

    fun setExplanation(value: String) = _state.update { it.copy(explanation = value.take(2000)) }

    fun showExplanation() = _state.update { it.copy(showExplanation = true) }

    fun setWorkHidden(value: Boolean) = _state.update { it.copy(isWorkHidden = value) }

    fun setChoice(index: Int, value: String) {
        _state.update {
            if (index !in it.choices.indices) return@update it
            it.copy(choices = it.choices.toMutableList().also { list -> list[index] = value.take(200) })
        }
    }

    fun addChoice() {
        _state.update {
            if (it.choices.size >= 20) it else it.copy(choices = it.choices + "")
        }
    }

    fun removeChoice(index: Int) {
        _state.update {
            if (it.choices.size <= 2 || index !in it.choices.indices) return@update it
            val next = it.choices.toMutableList().also { list -> list.removeAt(index) }
            val correct = it.correctIndexes
                .filter { it != index }
                .map { if (it > index) it - 1 else it }
                .toSet()
            it.copy(choices = next, correctIndexes = correct)
        }
    }

    fun toggleCorrect(index: Int) {
        _state.update {
            if (index !in it.choices.indices) return@update it
            val next = when (it.quizType) {
                "single" -> setOf(index)
                else -> if (index in it.correctIndexes) {
                    it.correctIndexes - index
                } else {
                    it.correctIndexes + index
                }
            }
            it.copy(correctIndexes = next)
        }
    }

    fun setJudgeAnswer(value: Boolean) = _state.update { it.copy(judgeAnswer = value) }

    fun setWorkQuery(value: String) {
        _state.update { it.copy(workQuery = value) }
        searchJob?.cancel()
        val query = value.trim()
        if (query.isBlank()) {
            _state.update { it.copy(workResults = emptyList(), searchingWorks = false) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(400)
            _state.update { it.copy(searchingWorks = true) }
            runCatching {
                app.searchApi.works(q = query, limit = 10, includeNsfw = true)
            }.onSuccess { page ->
                _state.update { s ->
                    s.copy(
                        workResults = page.items.filter { work -> s.works.none { it.id == work.id } },
                        searchingWorks = false,
                    )
                }
            }.onFailure {
                _state.update { s -> s.copy(workResults = emptyList(), searchingWorks = false) }
            }
        }
    }

    fun addWork(work: WorkSummary) {
        _state.update {
            if (it.works.size >= 20 || it.works.any { w -> w.id == work.id }) {
                it
            } else {
                it.copy(works = it.works + work, workQuery = "", workResults = emptyList())
            }
        }
    }

    fun removeWork(workId: String) {
        _state.update { it.copy(works = it.works.filterNot { w -> w.id == workId }) }
    }

    /** 校验并提交；成功后回调题目 id。 */
    fun submit(onSaved: (String) -> Unit) {
        val current = _state.value
        if (current.submitting) return
        validateQuizText(current.prompt, current.explanation, current.description)?.let {
            AppMessages.show(it)
            return
        }
        validateQuizChoices(current.quizType, current.choices, current.correctIndexes.sorted())
            ?.let {
                AppMessages.show(it)
                return
            }

        val isJudge = current.quizType == "judge"
        val cleanChoices = if (isJudge) emptyList() else current.choices.map { it.trim() }
        val correct = if (isJudge) {
            emptyList()
        } else if (current.quizType == "single") {
            current.correctIndexes.sorted()
        } else {
            current.correctIndexes.sorted()
        }
        val workIds = current.works.map { it.id }

        viewModelScope.launch {
            _state.update { it.copy(submitting = true) }
            if (quizId == null) {
                val body = QuizCreate(
                    quizType = current.quizType,
                    quizCategory = current.category,
                    difficulty = current.difficulty,
                    spoilerLevel = current.spoilerLevel,
                    promptText = current.prompt.trim(),
                    descriptionMarkdown = current.description,
                    explanationMarkdown = current.explanation,
                    choices = cleanChoices,
                    correctChoiceIndexes = correct,
                    isStatementTrue = if (isJudge) current.judgeAnswer else null,
                    workIds = workIds,
                    isWorkHidden = current.isWorkHidden,
                )
                val signature = body.toString()
                if (createKey == null || createSignature != signature) {
                    createKey = UUID.randomUUID().toString()
                    createSignature = signature
                }
                runCatching { app.quizApi.create(body, createKey!!) }
                    .onSuccess { quiz ->
                        createKey = null
                        createSignature = null
                        _state.update { it.copy(submitting = false) }
                        AppMessages.show("题目已发布")
                        onSaved(quiz.id)
                    }
                    .onFailure {
                        _state.update { s -> s.copy(submitting = false) }
                        AppMessages.show(friendlyError(it))
                    }
            } else {
                val body = QuizPatch(
                    quizType = current.quizType,
                    quizCategory = current.category,
                    difficulty = current.difficulty,
                    spoilerLevel = current.spoilerLevel,
                    promptText = current.prompt.trim(),
                    descriptionMarkdown = current.description,
                    explanationMarkdown = current.explanation,
                    choices = cleanChoices,
                    correctChoiceIndexes = correct,
                    isStatementTrue = if (isJudge) current.judgeAnswer else null,
                    workIds = workIds,
                    isWorkHidden = current.isWorkHidden,
                )
                runCatching { app.quizApi.update(quizId, body) }
                    .onSuccess { quiz ->
                        _state.update { it.copy(submitting = false) }
                        AppMessages.show("已保存修改")
                        onSaved(quiz.id)
                    }
                    .onFailure {
                        _state.update { s -> s.copy(submitting = false) }
                        AppMessages.show(friendlyError(it))
                    }
            }
        }
    }
}
