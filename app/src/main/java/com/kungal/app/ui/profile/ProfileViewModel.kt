package com.kungal.app.ui.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.core.model.Account
import com.kungal.core.model.Me
import com.kungal.core.net.KungalApiException
import com.kungal.app.ui.topic.friendlyError
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel(private val app: KungalApp) : ViewModel() {

    sealed interface UiState {
        data object LoggedOut : UiState
        data object Loading : UiState
        data class Error(val message: String) : UiState
        data class Ready(val me: Me, val account: Account) : UiState
    }

    private val _state = MutableStateFlow<UiState>(UiState.Loading)
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refresh() {
        if (!app.client.session.isLoggedIn) {
            _state.value = UiState.LoggedOut
            return
        }
        _state.value = UiState.Loading
        viewModelScope.launch {
            runCatching {
                val me = async { app.meApi.me() }
                val account = async { app.meApi.account() }
                me.await() to account.await()
            }.onSuccess { (me, account) ->
                // 顺便同步内容显示方式（可能在上次启动时没取到）
                app.applyAccount(account)
                _state.value = UiState.Ready(me, account)
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
