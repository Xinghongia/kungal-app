package com.kungal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.core.model.Account
import com.kungal.core.model.Me
import com.kungal.core.net.KungalApiException
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.UserProfile
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class ProfileViewModel(private val app: KungalApp) : ViewModel() {

    sealed interface UiState {
        data object LoggedOut : UiState
        data object Loading : UiState
        data class Error(val message: String) : UiState
        data class Ready(
            val me: Me,
            val account: Account,
            /** 与网站主页头部一致的 8 项统计；加载失败时为 null（界面显示 —）。 */
            val profile: UserProfile? = null,
        ) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val _checkingIn = MutableStateFlow(false)
    val checkingIn: StateFlow<Boolean> = _checkingIn.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun clearMessage() {
        _message.value = null
    }

    /** 每日签到；重复签到由服务端返回 ALREADY_EXISTS，如实提示。 */
    fun checkIn() {
        if (_checkingIn.value) return
        _checkingIn.value = true
        viewModelScope.launch {
            runCatching { app.meApi.checkIn(UUID.randomUUID().toString()) }
                .onSuccess { result ->
                    _message.value = "签到成功，获得 ${result.moemoepointAwarded} 萌萌点"
                    // 签到响应已经带回最新余额与签到日期，直接更新当前卡片，
                    // 不再整页重新加载（否则页面会闪一下加载态）
                    _state.update { current ->
                        if (current is UiState.Ready) {
                            current.copy(
                                me = current.me.copy(
                                    moemoepoint = result.moemoepoint,
                                    hasCheckedInToday = true,
                                ),
                                profile = current.profile?.copy(moemoepoint = result.moemoepoint),
                            )
                        } else {
                            current
                        }
                    }
                }
                .onFailure { e ->
                    val code = (e as? KungalApiException)?.problem?.code
                    _message.value = if (code == "ALREADY_EXISTS") "今天已经签到过了" else friendlyError(e)
                }
            _checkingIn.value = false
        }
    }

    fun refresh() {
        if (!app.client.session.isLoggedIn) {
            _state.value = UiState.LoggedOut
            return
        }
        // 已有数据时静默刷新：不把页面切回加载态，避免整页闪一下
        if (_state.value !is UiState.Ready) {
            _state.value = UiState.Loading
        }
        viewModelScope.launch {
            runCatching {
                val me = async { app.meApi.me() }
                val account = async { app.meApi.account() }
                val accountValue = account.await()
                // 主页统计单独容错：拿不到时账号卡仍可用，数字显示「—」
                val profile = async {
                    runCatching { app.userApi.profile(accountValue.id) }.getOrNull()
                }
                Triple(me.await(), accountValue, profile.await())
            }.onSuccess { (me, account, profile) ->
                // 顺便同步内容显示方式（可能在上次启动时没取到）
                app.applyAccount(account)
                _state.value = UiState.Ready(me, account, profile)
            }.onFailure { e ->
                if (e is KungalApiException && e.requiresLogin) {
                    // 会话过期：清掉本地会话，避免每个页面反复 401
                    runCatching { app.logout() }
                    _state.value = UiState.LoggedOut
                } else {
                    _state.value = UiState.Error(friendlyError(e))
                }
            }
        }
    }
}
