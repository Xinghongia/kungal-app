package com.kungal.app.ui.topic

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.core.model.Lottery
import com.kungal.core.model.Poll
import com.kungal.core.model.Reply
import com.kungal.core.model.ReplyComment
import com.kungal.core.model.Topic
import com.kungal.core.model.TopicState
import com.kungal.core.model.TopicSubscription
import com.kungal.core.model.TopicViewer
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** 回复/评论的编辑目标（弹窗用）。 */
data class EditTarget(
    val id: String,
    val text: String,
)

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
        /** floor_asc / floor_desc */
        val replySort: String = "floor_asc",
        val error: String? = null,
        val reactionBusy: Boolean = false,
        val replyBusyId: String? = null,
        val replySending: Boolean = false,
        val commentSendingReplyId: String? = null,
        val actionBusy: Boolean = false,
        val replySaving: Boolean = false,
        val commentSaving: Boolean = false,
        val message: String? = null,
        val state: TopicState? = null,
        val subscription: TopicSubscription? = null,
        val polls: List<Poll> = emptyList(),
        val lotteries: List<Lottery> = emptyList(),
        val editingReply: EditTarget? = null,
        val editingComment: EditTarget? = null,
        val replyIdBeingEdited: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val isLoggedIn: Boolean get() = app.isLoggedIn
    private var markedRead = false

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
                    loadExtras(topic)
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e)) }
                }
        }
        loadReplies(reset = true)
    }

    private fun loadExtras(topic: Topic) {
        if (isLoggedIn) {
            viewModelScope.launch {
                runCatching { app.topicApi.states(listOf(topicId)) }
                    .onSuccess { states -> _state.update { it.copy(state = states.items.firstOrNull()) } }
            }
            viewModelScope.launch {
                runCatching { app.topicApi.subscription(topicId) }
                    .onSuccess { sub -> _state.update { it.copy(subscription = sub) } }
            }
        }
        if (topic.miniApps.contains("poll")) {
            viewModelScope.launch {
                runCatching { app.topicApi.polls(topicId) }
                    .onSuccess { page -> _state.update { it.copy(polls = page.items) } }
            }
        }
        if (topic.miniApps.contains("lottery")) {
            viewModelScope.launch {
                runCatching { app.topicApi.lotteries(topicId, includeNsfw = true) }
                    .onSuccess { page -> _state.update { it.copy(lotteries = page.items) } }
            }
        }
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
                    sort = current.replySort,
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
                maybeMarkRead()
            }.onFailure { e ->
                _state.update { it.copy(loadingReplies = false, message = friendlyError(e)) }
            }
        }
    }

    fun setReplySort(sort: String) {
        if (sort == _state.value.replySort) return
        _state.update {
            it.copy(replySort = sort, replies = emptyList(), repliesCursor = null, repliesEndReached = false)
        }
        loadReplies(reset = true)
    }

    /** 打开话题后把已读楼层推进到当前最后一层（订阅 watching 时才被服务端保留）。 */
    private fun maybeMarkRead() {
        if (markedRead || !isLoggedIn) return
        val lastFloor = _state.value.replies.maxOfOrNull { it.floor } ?: return
        markedRead = true
        viewModelScope.launch {
            runCatching { app.topicApi.markRead(topicId, lastFloor) }
                .onSuccess { sub -> _state.update { it.copy(subscription = sub) } }
        }
    }

    // ---------- 话题反应 ----------

    fun toggleReaction(token: String) {
        val topic = _state.value.topic ?: return
        if (_state.value.reactionBusy) return
        val active = when (token) {
            "like" -> topic.viewer?.hasLiked == true
            "dislike" -> topic.viewer?.hasDisliked == true
            else -> topic.reactions.firstOrNull { it.reaction == token }?.viewer?.hasReacted == true
        }
        _state.update { it.copy(reactionBusy = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.react(topicId, token, active = !active) }
                .onSuccess { engagement ->
                    _state.update { s ->
                        val t = s.topic ?: return@update s
                        s.copy(
                            reactionBusy = false,
                            topic = t.copy(
                                likeCount = engagement.likeCount,
                                dislikeCount = engagement.dislikeCount,
                                favoriteCount = engagement.favoriteCount,
                                upvoteCount = engagement.upvoteCount,
                                reactions = engagement.reactions,
                                viewer = engagement.viewer ?: t.viewer,
                            ),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(reactionBusy = false, message = friendlyError(e)) }
                }
        }
    }

    /** 兼容旧入口：点赞。 */
    fun toggleLike() = toggleReaction("like")

    // ---------- 回复操作 ----------

    fun toggleReplyReaction(replyId: String, token: String) {
        val reply = _state.value.replies.firstOrNull { it.id == replyId } ?: return
        if (_state.value.replyBusyId != null) return
        val active = when (token) {
            "like" -> reply.viewer?.hasLiked == true
            "dislike" -> reply.viewer?.hasDisliked == true
            else -> reply.reactions.firstOrNull { it.reaction == token }?.viewer?.hasReacted == true
        }
        _state.update { it.copy(replyBusyId = replyId) }
        viewModelScope.launch {
            runCatching { app.topicApi.reactReply(replyId, token, active = !active) }
                .onSuccess { engagement ->
                    updateReply(replyId) {
                        it.copy(
                            likeCount = engagement.likeCount,
                            dislikeCount = engagement.dislikeCount,
                            reactions = engagement.reactions,
                            viewer = engagement.viewer ?: it.viewer,
                        )
                    }
                    _state.update { it.copy(replyBusyId = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(replyBusyId = null, message = friendlyError(e)) }
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

    fun startEditReply(replyId: String) {
        viewModelScope.launch {
            runCatching { app.topicApi.replySource(replyId) }
                .onSuccess { source ->
                    _state.update {
                        it.copy(
                            editingReply = EditTarget(replyId, source.contentMarkdown),
                            replyIdBeingEdited = replyId,
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(message = friendlyError(e)) } }
        }
    }

    fun updateEditingReply(text: String) = _state.update {
        it.copy(editingReply = it.editingReply?.copy(text = text))
    }

    fun cancelEditReply() = _state.update { it.copy(editingReply = null, replyIdBeingEdited = null) }

    fun submitEditReply() {
        val target = _state.value.editingReply ?: return
        if (target.text.isBlank() || _state.value.replySaving) return
        _state.update { it.copy(replySaving = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.updateReply(target.id, target.text.trim()) }
                .onSuccess { updated ->
                    updateReply(target.id) { updated.copy(comments = it.comments) }
                    _state.update {
                        it.copy(replySaving = false, editingReply = null, replyIdBeingEdited = null, message = "回复已更新")
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(replySaving = false, message = friendlyError(e)) }
                }
        }
    }

    fun deleteReply(replyId: String) {
        if (_state.value.replyBusyId != null) return
        _state.update { it.copy(replyBusyId = replyId) }
        viewModelScope.launch {
            runCatching { app.topicApi.deleteReply(replyId) }
                .onSuccess {
                    _state.update { s ->
                        s.copy(
                            replyBusyId = null,
                            replies = s.replies.filterNot { it.id == replyId },
                            message = "回复已删除",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(replyBusyId = null, message = friendlyError(e)) }
                }
        }
    }

    fun pinReply(replyId: String) {
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.pinReply(topicId, replyId) }
                .onSuccess { topic ->
                    _state.update {
                        it.copy(actionBusy = false, topic = topic, message = "已置顶该回复")
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun unpinReply() {
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.unpinReply(topicId) }
                .onSuccess { topic ->
                    _state.update { it.copy(actionBusy = false, topic = topic, message = "已取消置顶") }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun setBestAnswer(replyId: String) {
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.setBestAnswer(topicId, replyId) }
                .onSuccess { topic ->
                    _state.update { it.copy(actionBusy = false, topic = topic, message = "已设为最佳答案") }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun clearBestAnswer() {
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.clearBestAnswer(topicId) }
                .onSuccess { topic ->
                    _state.update { it.copy(actionBusy = false, topic = topic, message = "已取消最佳答案") }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = friendlyError(e)) }
                }
        }
    }

    // ---------- 楼中楼评论 ----------

    fun sendComment(replyId: String, text: String, parentCommentId: String? = null) {
        val body = text.trim()
        if (body.isEmpty() || _state.value.commentSendingReplyId != null) return
        _state.update { it.copy(commentSendingReplyId = replyId) }
        viewModelScope.launch {
            runCatching {
                app.topicApi.createComment(replyId, body, parentCommentId, UUID.randomUUID().toString())
            }.onSuccess { comment ->
                updateReply(replyId) { it.copy(comments = it.comments + comment) }
                _state.update { it.copy(commentSendingReplyId = null, message = "评论成功") }
            }.onFailure { e ->
                _state.update { it.copy(commentSendingReplyId = null, message = friendlyError(e)) }
            }
        }
    }

    fun toggleCommentLike(replyId: String, commentId: String) {
        val reply = _state.value.replies.firstOrNull { it.id == replyId } ?: return
        val comment = reply.comments.firstOrNull { it.id == commentId } ?: return
        val active = comment.viewer?.hasLiked == true
        viewModelScope.launch {
            runCatching { app.topicApi.likeComment(commentId, active = !active) }
                .onSuccess { updated ->
                    updateComment(replyId, commentId) { updated }
                }
                .onFailure { e -> _state.update { it.copy(message = friendlyError(e)) } }
        }
    }

    fun startEditComment(commentId: String) {
        viewModelScope.launch {
            runCatching { app.topicApi.commentSource(commentId) }
                .onSuccess { source ->
                    _state.update { it.copy(editingComment = EditTarget(commentId, source.text)) }
                }
                .onFailure { e -> _state.update { it.copy(message = friendlyError(e)) } }
        }
    }

    fun updateEditingComment(text: String) = _state.update {
        it.copy(editingComment = it.editingComment?.copy(text = text))
    }

    fun cancelEditComment() = _state.update { it.copy(editingComment = null) }

    fun submitEditComment() {
        val target = _state.value.editingComment ?: return
        if (target.text.isBlank() || _state.value.commentSaving) return
        _state.update { it.copy(commentSaving = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.updateComment(target.id, target.text.trim()) }
                .onSuccess { updated ->
                    _state.update { s ->
                        s.copy(
                            commentSaving = false,
                            editingComment = null,
                            message = "评论已更新",
                            replies = s.replies.map { reply ->
                                if (reply.comments.none { it.id == target.id }) reply
                                else reply.copy(comments = reply.comments.map { if (it.id == target.id) updated else it })
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(commentSaving = false, message = friendlyError(e)) }
                }
        }
    }

    fun deleteComment(replyId: String, commentId: String) {
        viewModelScope.launch {
            runCatching { app.topicApi.deleteComment(commentId) }
                .onSuccess {
                    updateReply(replyId) { reply ->
                        reply.copy(comments = reply.comments.filterNot { it.id == commentId })
                    }
                    _state.update { it.copy(message = "评论已删除") }
                }
                .onFailure { e -> _state.update { it.copy(message = friendlyError(e)) } }
        }
    }

    // ---------- 收藏 / 订阅 / 推 / 隐藏 ----------

    fun toggleFavorite() {
        val topic = _state.value.topic ?: return
        if (_state.value.actionBusy) return
        val active = topic.viewer?.hasFavorited == true
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.favorite(topicId, active = !active) }
                .onSuccess { engagement ->
                    _state.update { s ->
                        val t = s.topic ?: return@update s
                        s.copy(
                            actionBusy = false,
                            topic = t.copy(
                                favoriteCount = engagement.favoriteCount,
                                viewer = engagement.viewer ?: t.viewer,
                            ),
                            state = s.state?.copy(hasFavorited = !active),
                            message = if (active) "已取消收藏" else "已收藏",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun setSubscription(level: String) {
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.setSubscription(topicId, level) }
                .onSuccess { sub ->
                    _state.update {
                        it.copy(
                            actionBusy = false,
                            subscription = sub,
                            message = "通知设置已更新：" + notificationLevelLabel(level),
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun upvote(note: String?, onDone: () -> Unit) {
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching { app.topicApi.upvote(topicId, note?.trim()?.takeIf { it.isNotEmpty() }, UUID.randomUUID().toString()) }
                .onSuccess {
                    _state.update { s ->
                        val t = s.topic ?: return@update s
                        s.copy(
                            actionBusy = false,
                            topic = t.copy(upvoteCount = t.upvoteCount + 1),
                            message = "已推这个话题",
                        )
                    }
                    onDone()
                }
                .onFailure { e ->
                    _state.update { it.copy(actionBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun setHidden(hidden: Boolean) {
        if (_state.value.actionBusy) return
        _state.update { it.copy(actionBusy = true) }
        viewModelScope.launch {
            runCatching {
                app.topicApi.setTopicState(topicId, if (hidden) "hidden" else "published")
            }.onSuccess { topic ->
                _state.update {
                    it.copy(actionBusy = false, topic = topic, message = if (hidden) "话题已隐藏" else "话题已恢复")
                }
            }.onFailure { e ->
                _state.update { it.copy(actionBusy = false, message = friendlyError(e)) }
            }
        }
    }

    // ---------- 投票 / 抽奖 ----------

    fun votePoll(pollId: String, optionIds: List<String>) {
        viewModelScope.launch {
            runCatching { app.topicApi.votePoll(pollId, optionIds) }
                .onSuccess { poll ->
                    _state.update { s ->
                        s.copy(polls = s.polls.map { if (it.id == pollId) poll else it }, message = "投票成功")
                    }
                }
                .onFailure { e -> _state.update { it.copy(message = friendlyError(e)) } }
        }
    }

    fun clearPollVote(pollId: String) {
        viewModelScope.launch {
            runCatching { app.topicApi.clearPollVote(pollId) }
                .onSuccess { poll ->
                    _state.update { s ->
                        s.copy(polls = s.polls.map { if (it.id == pollId) poll else it }, message = "已撤销投票")
                    }
                }
                .onFailure { e -> _state.update { it.copy(message = friendlyError(e)) } }
        }
    }

    fun enterLottery(lotteryId: String) {
        viewModelScope.launch {
            runCatching { app.topicApi.enterLottery(lotteryId) }
                .onSuccess { lottery ->
                    replaceLottery(lottery)
                    _state.update { it.copy(message = "已参与抽奖") }
                }
                .onFailure { e -> _state.update { it.copy(message = friendlyError(e)) } }
        }
    }

    fun withdrawLottery(lotteryId: String) {
        viewModelScope.launch {
            runCatching { app.topicApi.withdrawLottery(lotteryId) }
                .onSuccess { lottery ->
                    replaceLottery(lottery)
                    _state.update { it.copy(message = "已退出抽奖") }
                }
                .onFailure { e -> _state.update { it.copy(message = friendlyError(e)) } }
        }
    }

    private fun replaceLottery(lottery: Lottery) {
        _state.update { s ->
            s.copy(lotteries = s.lotteries.map { if (it.id == lottery.id) lottery else it })
        }
    }

    fun consumeMessage() {
        _state.update { it.copy(message = null) }
    }

    private fun updateReply(replyId: String, transform: (Reply) -> Reply) {
        _state.update { s ->
            s.copy(replies = s.replies.map { if (it.id == replyId) transform(it) else it })
        }
    }

    private fun updateComment(replyId: String, commentId: String, transform: (ReplyComment) -> ReplyComment) {
        updateReply(replyId) { reply ->
            reply.copy(comments = reply.comments.map { if (it.id == commentId) transform(it) else it })
        }
    }
}

fun notificationLevelLabel(level: String): String = when (level) {
    "watching" -> "关注（每条回复）"
    "muted" -> "屏蔽"
    else -> "正常（仅提及）"
}

/** 反应写操作失败的统一提示。 */
fun reactionError(e: Throwable): String = when (e) {
    is KungalApiException -> friendlyError(e)
    else -> friendlyError(e)
}
