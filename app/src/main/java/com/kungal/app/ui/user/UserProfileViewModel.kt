package com.kungal.app.ui.user

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.ReportCreate
import com.kungal.core.model.ReportReason
import com.kungal.core.model.UserFollowState
import com.kungal.core.model.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

/** 用户主页头部：资料、关注状态、关注通知档位、举报。 */
class UserProfileViewModel(
    private val app: KungalApp,
    private val userId: String,
) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val profile: UserProfile? = null,
        val error: String? = null,
        val follow: UserFollowState? = null,
        val followBusy: Boolean = false,
        /** 关注/取关/举报等动作的提示（成功也提示一次）。 */
        val message: String? = null,
        val reportReasons: List<ReportReason> = emptyList(),
        val reporting: Boolean = false,
        /** 本人主页背景（商店装备的 profile_background 装扮）。 */
        val backgroundUrl: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    val isSelf: Boolean get() = app.currentUserId.value == userId

    /** 本人已装备的主页背景：this_site 优先于 everywhere，找不到就返回 null。 */
    private suspend fun loadOwnBackground(): String? {
        val inventory = runCatching { app.shopApi.inventory() }.getOrNull() ?: return null
        val wornId = inventory.loadout
            .filter { it.slot == "profile_background" }
            .sortedByDescending { it.scope == "this_site" }
            .firstOrNull { it.wornItemId != null }
            ?.wornItemId
            ?: return null
        val item = inventory.items
            .firstOrNull { it.isActive && it.item.id == wornId }
            ?.item
        return item?.artwork?.staticUrl?.takeIf { it.isNotBlank() }
    }

    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            coroutineScope {
                val profileDeferred = async { app.userApi.profile(userId) }
                val backgroundDeferred = if (isSelf && app.isLoggedIn) {
                    async { loadOwnBackground() }
                } else {
                    null
                }
                val followDeferred = if (app.isLoggedIn && !isSelf) {
                    async { runCatching { app.socialApi.followState(userId) }.getOrNull() }
                } else {
                    null
                }
                val profileResult = runCatching { profileDeferred.await() }
                val follow = followDeferred?.await()
                val background = backgroundDeferred?.await()
                profileResult
                    .onSuccess { profile ->
                        _state.update {
                            it.copy(
                                loading = false,
                                profile = profile,
                                follow = follow,
                                backgroundUrl = background,
                            )
                        }
                    }
                    .onFailure { e ->
                        _state.update { it.copy(loading = false, error = friendlyError(e)) }
                    }
            }
        }
    }

    fun toggleFollow() {
        if (!app.isLoggedIn || isSelf) return
        val current = _state.value
        if (current.followBusy) return
        _state.update { it.copy(followBusy = true, message = null) }
        viewModelScope.launch {
            val wasFollowing = current.follow?.isFollowing == true
            runCatching {
                if (wasFollowing) app.socialApi.unfollow(userId) else app.socialApi.follow(userId)
            }.onSuccess { follow ->
                _state.update {
                    it.copy(
                        followBusy = false,
                        follow = follow,
                        message = if (wasFollowing) "已取消关注" else "已关注",
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(followBusy = false, message = friendlyError(e)) }
            }
        }
    }

    /** 关注通知档位：all = 全部动态，feed = 只进关注流。 */
    fun setNotify(level: String) {
        if (!app.isLoggedIn || _state.value.follow?.isFollowing != true) return
        _state.update { it.copy(followBusy = true, message = null) }
        viewModelScope.launch {
            runCatching { app.socialApi.setFollowNotify(userId, level) }
                .onSuccess { follow ->
                    _state.update {
                        it.copy(
                            followBusy = false,
                            follow = follow,
                            message = if (level == "all") "已开启全部动态提醒" else "只接收关注流提醒",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(followBusy = false, message = friendlyError(e)) }
                }
        }
    }

    fun loadReportReasons() {
        if (_state.value.reportReasons.isNotEmpty()) return
        viewModelScope.launch {
            runCatching { app.socialApi.reportReasons() }
                .onSuccess { page -> _state.update { it.copy(reportReasons = page.items) } }
                .onFailure { e -> _state.update { it.copy(message = friendlyError(e)) } }
        }
    }

    fun report(reasonKey: String, note: String) {
        val profile = _state.value.profile ?: return
        if (_state.value.reporting) return
        _state.update { it.copy(reporting = true) }
        viewModelScope.launch {
            runCatching {
                app.socialApi.report(
                    ReportCreate(
                        subjectKind = "user",
                        subjectId = userId,
                        reasonKey = reasonKey,
                        note = note.ifBlank { null },
                        snapshot = profile.name,
                        subjectUrl = "https://www.kungal.com/user/$userId",
                    ),
                    idempotencyKey = UUID.randomUUID().toString(),
                )
            }.onSuccess {
                _state.update { it.copy(reporting = false, message = "举报已提交，站点会尽快处理") }
            }.onFailure { e ->
                _state.update { it.copy(reporting = false, message = friendlyError(e)) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}
