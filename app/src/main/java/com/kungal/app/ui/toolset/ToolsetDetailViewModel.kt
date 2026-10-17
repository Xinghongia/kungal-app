package com.kungal.app.ui.toolset

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.Toolset
import com.kungal.core.model.ToolsetDownload
import com.kungal.core.model.ToolsetResource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ToolsetDetailState(
    val toolsetId: String = "",
    val toolset: Toolset? = null,
    val loading: Boolean = true,
    val error: String? = null,
    val favoritePending: Boolean = false,
    val ratingPending: Boolean = false,
    val followWall: Boolean? = null,
    val followPending: Boolean = false,
    val downloads: Map<String, ToolsetDownload> = emptyMap(),
    val downloadPending: String? = null,
    val deleting: Boolean = false,
    val actionMessage: String? = null,
)

/** 工具集详情（BP-03 U4，对应网页 /toolset/{id}）。 */
class ToolsetDetailViewModel(
    private val app: KungalApp,
    private val toolsetId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ToolsetDetailState(toolsetId = toolsetId))
    val state: StateFlow<ToolsetDetailState> = _state.asStateFlow()

    init {
        load()
        loadWallFollow()
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(loading = true, error = null) }
            runCatching { app.toolsetApi.toolset(toolsetId) }
                .onSuccess { toolset ->
                    _state.update { it.copy(toolset = toolset, loading = false, error = null) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = friendlyError(e)) }
                }
        }
    }

    private fun loadWallFollow() {
        if (!app.isLoggedIn) return
        viewModelScope.launch {
            runCatching { app.notificationApi.wallState("toolset", toolsetId) }
                .onSuccess { wall ->
                    _state.update { it.copy(followWall = wall.isFollowing) }
                }
        }
    }

    fun toggleWallFollow() {
        if (!app.isLoggedIn || _state.value.followPending) return
        val next = !(_state.value.followWall ?: false)
        _state.update { it.copy(followPending = true) }
        viewModelScope.launch {
            runCatching {
                if (next) {
                    app.notificationApi.followWall("toolset", toolsetId)
                } else {
                    app.notificationApi.unfollowWall("toolset", toolsetId)
                }
            }.onSuccess { wall ->
                _state.update { it.copy(followWall = wall.isFollowing, followPending = false) }
                AppMessages.show(if (wall.isFollowing) "已订阅评论" else "已取消订阅")
            }.onFailure { e ->
                _state.update { it.copy(followPending = false) }
                AppMessages.show(friendlyError(e))
            }
        }
    }

    fun toggleFavorite() {
        val current = _state.value
        val toolset = current.toolset ?: return
        if (!app.isLoggedIn || current.favoritePending) return
        val next = !(toolset.viewer?.hasFavorited ?: false)
        _state.update {
            it.copy(
                favoritePending = true,
                toolset = it.toolset?.copy(
                    favoriteCount = (it.toolset.favoriteCount + if (next) 1 else -1).coerceAtLeast(0),
                    viewer = (it.toolset.viewer ?: com.kungal.core.model.ToolsetViewer())
                        .copy(hasFavorited = next),
                ),
            )
        }
        viewModelScope.launch {
            runCatching { app.toolsetApi.setFavorite(toolsetId, next) }
                .onSuccess { result ->
                    val now = result.viewer?.hasFavorited ?: next
                    _state.update { state ->
                        state.copy(
                            favoritePending = false,
                            toolset = state.toolset?.copy(
                                favoriteCount = result.favoriteCount,
                                viewer = (state.toolset.viewer ?: com.kungal.core.model.ToolsetViewer())
                                    .copy(hasFavorited = now),
                            ),
                        )
                    }
                    AppMessages.show(if (now) "已收藏" else "已取消收藏")
                }
                .onFailure { e ->
                    _state.update { state ->
                        state.copy(
                            favoritePending = false,
                            toolset = state.toolset?.copy(
                                favoriteCount = current.toolset.favoriteCount,
                                viewer = current.toolset.viewer,
                            ),
                        )
                    }
                    AppMessages.show(friendlyError(e))
                }
        }
    }

    fun ratePracticality(rating: Int) {
        val current = _state.value
        if (current.toolset == null) return
        if (!app.isLoggedIn || current.ratingPending) return
        _state.update { it.copy(ratingPending = true) }
        viewModelScope.launch {
            runCatching { app.toolsetApi.ratePracticality(toolsetId, rating) }
                .onSuccess { result ->
                    _state.update { state ->
                        state.copy(
                            ratingPending = false,
                            toolset = state.toolset?.copy(
                                practicalityAverage = result.practicalityAverage,
                                practicalityCount = result.practicalityCount,
                                practicalityDistribution = result.practicalityDistribution,
                                viewer = (state.toolset.viewer ?: com.kungal.core.model.ToolsetViewer())
                                    .copy(practicalityRating = result.viewer?.practicalityRating ?: rating),
                            ),
                        )
                    }
                    AppMessages.show("已评分：$rating 星")
                }
                .onFailure { e ->
                    _state.update { it.copy(ratingPending = false) }
                    AppMessages.show(friendlyError(e))
                }
        }
    }

    fun fetchDownload(resource: ToolsetResource) {
        val current = _state.value
        if (current.downloads.containsKey(resource.id) || current.downloadPending != null) return
        _state.update { it.copy(downloadPending = resource.id) }
        viewModelScope.launch {
            runCatching { app.toolsetApi.downloadResource(toolsetId, resource.id) }
                .onSuccess { download ->
                    _state.update { state ->
                        state.copy(
                            downloadPending = null,
                            downloads = state.downloads + (resource.id to download),
                            toolset = state.toolset?.let { ts ->
                                ts.copy(
                                    downloadCount = ts.downloadCount + 1,
                                    toolsetResources = ts.toolsetResources.map { item ->
                                        if (item.id == resource.id) {
                                            item.copy(downloadCount = item.downloadCount + 1)
                                        } else {
                                            item
                                        }
                                    },
                                )
                            },
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(downloadPending = null) }
                    AppMessages.show(friendlyError(e))
                }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        if (!app.isLoggedIn || _state.value.deleting) return
        _state.update { it.copy(deleting = true) }
        viewModelScope.launch {
            runCatching { app.toolsetApi.delete(toolsetId) }
                .onSuccess {
                    _state.update { it.copy(deleting = false) }
                    AppMessages.show("已删除该工具")
                    onDeleted()
                }
                .onFailure {
                    _state.update { it.copy(deleting = false) }
                    AppMessages.show(friendlyError(it))
                }
        }
    }

    fun deleteResource(resource: ToolsetResource) {
        if (!app.isLoggedIn) return
        viewModelScope.launch {
            runCatching { app.toolsetApi.deleteResource(toolsetId, resource.id) }
                .onSuccess {
                    _state.update { state ->
                        state.copy(
                            toolset = state.toolset?.copy(
                                toolsetResources = state.toolset.toolsetResources
                                    .filterNot { it.id == resource.id },
                            ),
                        )
                    }
                    AppMessages.show("删除成功")
                }
                .onFailure { AppMessages.show(friendlyError(it)) }
        }
    }
}
