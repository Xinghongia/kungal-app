package com.kungal.app.ui.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.kungal.app.KungalApp
import com.kungal.core.api.AccountApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * 原生账号密码登录 / 注册（与网页同一套 NextMoe OAuth 流程）。
 * 密码只保存在内存里，登录成功后立即清空，不写入磁盘或日志。
 */
class LoginViewModel(private val app: KungalApp) : ViewModel() {

    enum class Mode { LOGIN, REGISTER }

    data class UiState(
        val mode: Mode = Mode.LOGIN,
        val account: String = "",
        val password: String = "",
        val name: String = "",
        val email: String = "",
        val code: String = "",
        val confirmPassword: String = "",
        val busy: Boolean = false,
        val sendingCode: Boolean = false,
        val cooldown: Int = 0,
        val codeSent: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(UiState())
    val state = _state.asStateFlow()

    private var cooldownJob: Job? = null

    fun setMode(mode: Mode) {
        _state.update { it.copy(mode = mode, error = null, codeSent = false) }
    }

    fun setAccount(value: String) = _state.update { it.copy(account = value, error = null) }
    fun setPassword(value: String) = _state.update { it.copy(password = value, error = null) }
    fun setName(value: String) = _state.update { it.copy(name = value, error = null) }
    fun setEmail(value: String) = _state.update { it.copy(email = value, error = null) }
    fun setCode(value: String) = _state.update { it.copy(code = value.filter(Char::isDigit).take(6)) }
    fun setConfirmPassword(value: String) =
        _state.update { it.copy(confirmPassword = value, error = null) }

    fun login(onDone: () -> Unit) {
        val current = _state.value
        if (current.busy) return
        LoginRules.loginError(current.account, current.password)?.let { error ->
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                app.authApi.loginAndAuthorize(current.account.trim(), current.password)
            }.onSuccess { cookie ->
                app.onLoggedIn(cookie)
                _state.update {
                    it.copy(busy = false, password = "", confirmPassword = "", error = null)
                }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(busy = false, error = messageOf(e)) }
            }
        }
    }

    fun sendCode() {
        val current = _state.value
        if (current.sendingCode || current.cooldown > 0) return
        LoginRules.registerAccountError(current.name, current.email)?.let { error ->
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(sendingCode = true, error = null) }
        viewModelScope.launch {
            runCatching {
                app.authApi.sendRegisterCode(current.name.trim(), current.email.trim())
            }.onSuccess {
                _state.update { it.copy(sendingCode = false, codeSent = true, cooldown = 60) }
                startCooldown()
            }.onFailure { e ->
                _state.update { it.copy(sendingCode = false, error = messageOf(e)) }
            }
        }
    }

    fun register(onDone: () -> Unit) {
        val current = _state.value
        if (current.busy) return
        LoginRules.registerError(
            current.name,
            current.email,
            current.password,
            current.confirmPassword,
            current.code,
        )?.let { error ->
            _state.update { it.copy(error = error) }
            return
        }
        _state.update { it.copy(busy = true, error = null) }
        viewModelScope.launch {
            runCatching {
                app.authApi.registerAndAuthorize(
                    name = current.name.trim(),
                    email = current.email.trim(),
                    password = current.password,
                    code = current.code,
                )
            }.onSuccess { cookie ->
                app.onLoggedIn(cookie)
                _state.update {
                    it.copy(busy = false, password = "", confirmPassword = "", code = "", error = null)
                }
                onDone()
            }.onFailure { e ->
                _state.update { it.copy(busy = false, error = messageOf(e)) }
            }
        }
    }

    private fun startCooldown() {
        cooldownJob?.cancel()
        cooldownJob = viewModelScope.launch {
            for (remaining in 60 downTo 1) {
                _state.update { it.copy(cooldown = remaining) }
                delay(1_000)
            }
            _state.update { it.copy(cooldown = 0) }
        }
    }

    private fun messageOf(e: Throwable): String = when (e) {
        is AccountApiException -> e.message ?: "操作失败，请重试"
        is IOException -> "网络连接失败，请检查网络后重试"
        else -> e.message ?: "操作失败，请重试"
    }
}
