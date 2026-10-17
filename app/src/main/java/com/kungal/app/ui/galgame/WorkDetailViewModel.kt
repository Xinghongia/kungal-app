package com.kungal.app.ui.galgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.Work
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WorkDetailState(
    val workId: String = "",
    val work: Work? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val likePending: Boolean = false,
    /** null = 未登录或尚未读到；true = 已在某个收藏夹里。 */
    val hasFavorited: Boolean? = null,
    val favoriteCount: Int = 0,
    val votedCoverId: String? = null,
    val votingCoverId: String? = null,
    val coverCounts: Map<String, Int> = emptyMap(),
)

class WorkDetailViewModel(
    private val app: KungalApp,
    initialWorkId: String,
    includeNsfw: Boolean,
) : ViewModel() {

    private val _state = MutableStateFlow(
        WorkDetailState(
            workId = initialWorkId,
            loading = true,
        ),
    )
    val state: StateFlow<WorkDetailState> = _state.asStateFlow()

    private var includeNsfw = includeNsfw
    private var loadJob: Job? = null

    init {
        load(initialWorkId)
    }

    fun setIncludeNsfw(value: Boolean) {
        if (includeNsfw == value) return
        includeNsfw = value
        load(_state.value.workId)
    }

    fun refresh() = load(_state.value.workId)

    private fun load(workId: String, redirects: Int = 0) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            _state.update { it.copy(workId = workId, loading = true, error = null) }
            runCatching { app.galgameApi.work(workId, includeNsfw) }
                .onSuccess { work ->
                    _state.update {
                        it.copy(
                            workId = work.id,
                            work = work,
                            loading = false,
                            error = null,
                            favoriteCount = work.favoriteCount,
                            coverCounts = work.covers.associate { cover ->
                                cover.id to cover.voteCount
                            },
                            hasFavorited = null,
                        )
                    }
                    loadViewerState(work.id)
                }
                .onFailure { e ->
                    val merged = (e as? KungalApiException)?.problem
                        ?.takeIf { it.code == "ENTITY_MERGED" }
                        ?.currentId
                    if (merged != null && redirects < 2) {
                        load(merged, redirects + 1)
                        return@onFailure
                    }
                    _state.update {
                        it.copy(loading = false, error = friendlyError(e))
                    }
                }
        }
    }

    /** 登录用户补充读取：收藏夹归属与封面选票。 */
    private fun loadViewerState(workId: String) {
        if (!app.isLoggedIn) {
            _state.update { it.copy(hasFavorited = null, votedCoverId = null) }
            return
        }
        viewModelScope.launch {
            runCatching { app.galgameApi.myWorkStates(listOf(workId)) }
                .onSuccess { result ->
                    val mine = result.items.firstOrNull { it.workId == workId }
                    val collected = mine?.library?.collectionIds?.isNotEmpty()
                    if (collected != null) {
                        _state.update { it.copy(hasFavorited = collected) }
                    }
                }
        }
        viewModelScope.launch {
            runCatching { app.galgameApi.myCoverVotes(listOf(workId)) }
                .onSuccess { result ->
                    _state.update {
                        it.copy(
                            votedCoverId = result.items.firstOrNull { vote ->
                                vote.workId == workId
                            }?.votedCoverId,
                        )
                    }
                }
        }
    }

    fun toggleLike() {
        val current = _state.value
        val work = current.work ?: return
        if (!app.isLoggedIn || current.likePending) return
        val next = !(work.viewer?.hasLiked ?: false)
        _state.update {
            it.copy(
                likePending = true,
                work = it.work?.copy(
                    likeCount = (it.work.likeCount + if (next) 1 else -1).coerceAtLeast(0),
                    viewer = it.work.viewer?.copy(hasLiked = next)
                        ?: com.kungal.core.model.WorkViewer(hasLiked = next),
                ),
            )
        }
        viewModelScope.launch {
            runCatching { app.galgameApi.setWorkLike(work.id, next) }
                .onSuccess { result ->
                    val nowLiked = result.viewer?.hasLiked ?: next
                    _state.update {
                        it.copy(
                            likePending = false,
                            work = it.work?.copy(
                                likeCount = result.likeCount,
                                viewer = it.work.viewer?.copy(hasLiked = nowLiked)
                                    ?: com.kungal.core.model.WorkViewer(hasLiked = nowLiked),
                            ),
                        )
                    }
                    AppMessages.show(if (nowLiked) "已点赞" else "已取消点赞")
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            likePending = false,
                            work = it.work?.copy(
                                likeCount = work.likeCount,
                                viewer = it.work.viewer?.copy(
                                    hasLiked = work.viewer?.hasLiked ?: false,
                                ),
                            ),
                        )
                    }
                    AppMessages.show(friendlyError(e))
                }
        }
    }

    /** 收藏夹选择保存后回填心形状态与收藏数。 */
    fun applyFavoriteState(favorited: Boolean) {
        _state.update { current ->
            val previous = current.hasFavorited
            val delta = when {
                previous == null -> if (favorited) 1 else 0
                previous == favorited -> 0
                favorited -> 1
                else -> -1
            }
            current.copy(
                hasFavorited = favorited,
                favoriteCount = (current.favoriteCount + delta).coerceAtLeast(0),
            )
        }
    }

    /** 封面投票；已投同一张时撤票，投另一张会移动选票。 */
    fun voteCover(coverId: String) {
        val current = _state.value
        val work = current.work ?: return
        if (!app.isLoggedIn || current.votingCoverId != null) return
        val unvote = current.votedCoverId == coverId
        val previous = current
        _state.update { state ->
            val counts = state.coverCounts.toMutableMap()
            if (unvote) {
                counts[coverId] = ((counts[coverId] ?: 0) - 1).coerceAtLeast(0)
            } else {
                state.votedCoverId?.let { old ->
                    counts[old] = ((counts[old] ?: 0) - 1).coerceAtLeast(0)
                }
                counts[coverId] = (counts[coverId] ?: 0) + 1
            }
            state.copy(
                votingCoverId = coverId,
                coverCounts = counts,
                votedCoverId = if (unvote) null else coverId,
            )
        }
        viewModelScope.launch {
            runCatching { app.galgameApi.setCoverVote(work.id, coverId, voted = !unvote) }
                .onSuccess { result ->
                    _state.update { state ->
                        state.copy(
                            votingCoverId = null,
                            coverCounts = state.coverCounts + (result.coverId to result.voteCount),
                            votedCoverId = if (result.viewer?.hasVoted == true) result.coverId else null,
                        )
                    }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(
                            votingCoverId = null,
                            coverCounts = previous.coverCounts,
                            votedCoverId = previous.votedCoverId,
                        )
                    }
                    AppMessages.show(friendlyError(e))
                }
        }
    }
}
