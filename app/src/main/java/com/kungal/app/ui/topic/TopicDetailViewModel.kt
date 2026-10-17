package com.kungal.app.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.core.model.Reply
import com.kungal.core.model.Topic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class TopicDetailViewModel(
    private val app: KungalApp,
    private val topicId: String,
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val topic: Topic? = null,
        val replies: List<Reply> = emptyList(),
        val repliesCursor: String? = null,
        val repliesEndReached: Boolean = false,
        val loadingReplies: Boolean = false,
        val error: String? = null,
        val likeBusy: Boolean = false,
        val replySending: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { app.topicApi.detail(topicId) }
                .onSuccess { topic ->
                    _state.update { it.copy(loading = false, topic = topic) }
                    // 浏览计数（与网页打开话题一致）；失败静默，不影响阅读
                    runCatching { app.topicApi.markView(topicId) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e)) }
                }
        }
        loadReplies(reset = true)
    }

    fun loadReplies(reset: Boolean = false) {
        val current = _state.value
        if (current.loadingReplies) return
        if (!reset && current.repliesCursor == null) return
        viewModelScope.launch {
            _state.update { it.copy(loadingReplies = true) }
            runCatching {
                app.topicApi.replies(
                    topicId = topicId,
                    cursor = if (reset) null else current.repliesCursor,
                    limit = 20,
                )
            }.onSuccess { page ->
                _state.update {
                    it.copy(
                        loadingReplies = false,
                        replies = if (reset) page.items else it.replies + page.items,
                        repliesCursor = page.nextCursor,
                        repliesEndReached = page.nextCursor == null,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loadingReplies = false, message = friendlyError(e)) }
            }
        }
    }

    /** 点赞/取消点赞：乐观更新 + 失败回滚（saku 同款模式）。 */
    fun toggleLike() {
        val topic = _state.value.topic ?: return
        if (_state.value.likeBusy) return
        val liked = topic.viewer?.hasLiked == true
        val prev = topic
        val next = topic.copy(
            likeCount = (topic.likeCount + if (liked) -1 else 1).coerceAtLeast(0),
            viewer = (topic.viewer ?: com.kungal.core.model.TopicViewer()).copy(hasLiked = !liked),
        )
        _state.update { it.copy(topic = next, likeBusy = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.react(topicId, "like", active = !liked) }
                .onSuccess { engagement ->
                    _state.update { s ->
                        val t = s.topic ?: return@update s
                        s.copy(
                            likeBusy = false,
                            topic = t.copy(
                                likeCount = engagement.likeCount,
                                reactions = engagement.reactions,
                                viewer = engagement.viewer ?: t.viewer,
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(topic = prev, likeBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun sendReply(markdown: String) {
        val text = markdown.trim()
        if (text.isEmpty() || _state.value.replySending) return
        _state.update { it.copy(replySending = true) }
        viewModelScope.launch {
            runCatching {
                app.topicApi.createReply(topicId, text, UUID.randomUUID().toString())
            }.onSuccess { reply ->
                _state.update {
                    it.copy(
                        replySending = false,
                        replies = it.replies + reply,
                        message = "回复成功",
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(replySending = false, message = friendlyError(e)) }
            }
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }
}
