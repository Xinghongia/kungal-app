package com.kungal.app.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.CommentSearchHit
import com.kungal.core.model.ReplySearchHit
import com.kungal.core.model.Topic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SearchViewModel(
    private val app: KungalApp,
    private val includeNsfw: Boolean,
) : ViewModel() {

    data class State(
        val tab: Int = 0,
        val loading: Boolean = false,
        val searched: Boolean = false,
        val searchedQuery: String = "",
        val page: Int = 1,
        val hasMore: Boolean = false,
        val topics: List<Topic> = emptyList(),
        val replies: List<ReplySearchHit> = emptyList(),
        val comments: List<CommentSearchHit> = emptyList(),
        val error: String? = null,
    ) {
        val isEmpty: Boolean
            get() = when (tab) {
                0 -> topics.isEmpty()
                1 -> replies.isEmpty()
                else -> comments.isEmpty()
            }
    }

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    fun setTab(tab: Int, query: String) {
        if (tab == _state.value.tab) return
        _state.update { it.copy(tab = tab) }
        if (query.isNotBlank() && _state.value.searchedQuery != query.trim()) {
            search(query)
        } else if (_state.value.searched && isEmptyForTab(tab)) {
            search(query.ifBlank { _state.value.searchedQuery })
        }
    }

    fun search(query: String) {
        val q = query.trim()
        if (q.isEmpty() || _state.value.loading) return
        _state.update {
            it.copy(
                loading = true,
                error = null,
                searched = true,
                searchedQuery = q,
                page = 1,
            )
        }
        load(q, page = 1, append = false)
    }

    fun loadMore(query: String) {
        val q = query.trim().ifBlank { _state.value.searchedQuery }
        if (q.isEmpty() || _state.value.loading) return
        _state.update { it.copy(loading = true) }
        load(q, page = _state.value.page + 1, append = true)
    }

    private fun load(query: String, page: Int, append: Boolean) {
        val tab = _state.value.tab
        viewModelScope.launch {
            runCatching {
                when (tab) {
                    0 -> {
                        val pageData = app.topicApi.searchTopics(query, page = page, includeNsfw = includeNsfw)
                        _state.update { s ->
                            s.copy(
                                loading = false,
                                page = page,
                                hasMore = pageData.items.size >= 20,
                                topics = if (append) s.topics + pageData.items else pageData.items,
                            )
                        }
                    }
                    1 -> {
                        val pageData = app.topicApi.searchReplies(query, page = page, includeNsfw = includeNsfw)
                        _state.update { s ->
                            s.copy(
                                loading = false,
                                page = page,
                                hasMore = pageData.items.size >= 20,
                                replies = if (append) s.replies + pageData.items else pageData.items,
                            )
                        }
                    }
                    else -> {
                        val pageData = app.topicApi.searchComments(query, page = page, includeNsfw = includeNsfw)
                        _state.update { s ->
                            s.copy(
                                loading = false,
                                page = page,
                                hasMore = pageData.items.size >= 20,
                                comments = if (append) s.comments + pageData.items else pageData.items,
                            )
                        }
                    }
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = friendlyError(e)) }
            }
        }
    }

    private fun isEmptyForTab(tab: Int): Boolean = when (tab) {
        0 -> _state.value.topics.isEmpty()
        1 -> _state.value.replies.isEmpty()
        else -> _state.value.comments.isEmpty()
    }
}
