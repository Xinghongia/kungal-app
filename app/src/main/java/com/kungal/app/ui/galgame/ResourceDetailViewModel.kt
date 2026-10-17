package com.kungal.app.ui.galgame

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.GalgameResource
import com.kungal.core.model.ResourceDownload
import com.kungal.core.model.ResourceRelocation
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import java.util.UUID

data class ResourceDetailState(
    val resourceId: String = "",
    val resource: GalgameResource? = null,
    val loading: Boolean = true,
    val error: String? = null,
    /** 404 且没有迁移记录：资源已不存在。 */
    val notFound: Boolean = false,
    /** 404 但资源搬到 LetMoe：给出去处。 */
    val relocation: ResourceRelocation? = null,
    val download: ResourceDownload? = null,
    val downloadLoading: Boolean = false,
    val downloadError: String? = null,
    /** checking / alive / dead / unchecked / error */
    val reportStatus: String? = null,
    val recommendations: List<GalgameResource> = emptyList(),
    val deleting: Boolean = false,
)

/** 资源详情页（BP-03 U3，对应网页 /galgame/resource/{id}）。 */
class ResourceDetailViewModel(
    private val app: KungalApp,
    initialResourceId: String,
) : ViewModel() {

    private val _state = MutableStateFlow(ResourceDetailState(resourceId = initialResourceId))
    val state: StateFlow<ResourceDetailState> = _state.asStateFlow()

    init {
        load(initialResourceId)
    }

    fun refresh() = load(_state.value.resourceId)

    private fun load(resourceId: String, redirects: Int = 0) {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    resourceId = resourceId,
                    loading = true,
                    error = null,
                    notFound = false,
                    relocation = null,
                )
            }
            runCatching { app.resourceApi.resource(resourceId) }
                .onSuccess { resource ->
                    _state.update {
                        it.copy(
                            resource = resource,
                            loading = false,
                            error = null,
                        )
                    }
                    loadRecommendations(resource)
                }
                .onFailure { e ->
                    val status = (e as? KungalApiException)?.httpStatus
                    if (status == 404 && redirects == 0) {
                        // 404：先查是否迁到 LetMoe；查不到就如实显示不存在。
                        runCatching { app.resourceApi.relocation(resourceId) }
                            .onSuccess { moved ->
                                _state.update {
                                    it.copy(loading = false, notFound = true, relocation = moved)
                                }
                            }
                            .onFailure {
                                _state.update { it.copy(loading = false, notFound = true) }
                            }
                    } else {
                        _state.update { it.copy(loading = false, error = friendlyError(e)) }
                    }
                }
        }
    }

    private fun loadRecommendations(resource: GalgameResource) {
        val workId = resource.work?.id ?: return
        viewModelScope.launch {
            runCatching { app.resourceApi.workResources(workId, limit = 100) }
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            recommendations = page.items
                                .filter { item -> item.id != resource.id }
                                .take(6),
                        )
                    }
                }
        }
    }

    fun fetchDownload() {
        val current = _state.value
        if (current.download != null || current.downloadLoading) return
        _state.update { it.copy(downloadLoading = true, downloadError = null) }
        viewModelScope.launch {
            runCatching { app.resourceApi.download(current.resourceId) }
                .onSuccess { download ->
                    _state.update { it.copy(download = download, downloadLoading = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(downloadLoading = false, downloadError = friendlyError(e))
                    }
                }
        }
    }

    fun reportExpiry() {
        val current = _state.value
        if (!app.isLoggedIn || current.reportStatus == "checking") return
        _state.update { it.copy(reportStatus = "checking") }
        viewModelScope.launch {
            runCatching {
                app.resourceApi.reportExpiry(
                    current.resourceId,
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            }.onSuccess { result ->
                _state.update { state ->
                    state.copy(
                        reportStatus = result.verdict,
                        resource = state.resource?.let {
                            if (result.state == "expired") it.copy(state = "expired") else it
                        },
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(reportStatus = "error") }
                AppMessages.show(friendlyError(e))
            }
        }
    }

    fun markValid() {
        val current = _state.value
        if (!app.isLoggedIn) return
        viewModelScope.launch {
            runCatching {
                app.resourceApi.patch(
                    current.resourceId,
                    buildJsonObject { put("state", JsonPrimitive("valid")) },
                )
            }.onSuccess { updated ->
                _state.update { it.copy(resource = updated) }
                AppMessages.show("已重新标记为有效")
            }.onFailure { AppMessages.show(friendlyError(it)) }
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val current = _state.value
        if (!app.isLoggedIn || current.deleting) return
        _state.update { it.copy(deleting = true) }
        viewModelScope.launch {
            runCatching { app.resourceApi.delete(current.resourceId) }
                .onSuccess {
                    _state.update { it.copy(deleting = false) }
                    AppMessages.show("删除资源成功")
                    onDeleted()
                }
                .onFailure {
                    _state.update { it.copy(deleting = false) }
                    AppMessages.show(friendlyError(it))
                }
        }
    }
}
