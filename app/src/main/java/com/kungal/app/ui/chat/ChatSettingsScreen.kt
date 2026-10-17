package com.kungal.app.ui.chat

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.SettingsGroup
import com.kungal.core.model.ChatSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ChatSettingsViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val settings: ChatSettings? = null,
        val saving: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    init {
        load()
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { app.chatApi.settings() }
                .onSuccess { _state.value = State(loading = false, settings = it) }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = ChatCatalog.errorText(e)) }
                }
        }
    }

    /** 乐观更新：点击立即生效，失败回滚。 */
    fun setAllowIncoming(value: String) {
        val previous = _state.value.settings ?: return
        if (_state.value.saving || previous.allowIncoming == value) return
        _state.update {
            it.copy(settings = previous.copy(allowIncoming = value), saving = true, message = null)
        }
        viewModelScope.launch {
            runCatching { app.chatApi.updateSettings(allowIncoming = value) }
                .onSuccess { settings ->
                    _state.update { it.copy(settings = settings, saving = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(settings = previous, saving = false, message = ChatCatalog.errorText(e))
                    }
                }
        }
    }

    fun setAcceptRequests(value: Boolean) {
        val previous = _state.value.settings ?: return
        if (_state.value.saving) return
        _state.update {
            it.copy(settings = previous.copy(acceptRequests = value), saving = true, message = null)
        }
        viewModelScope.launch {
            runCatching { app.chatApi.updateSettings(acceptRequests = value) }
                .onSuccess { settings ->
                    _state.update { it.copy(settings = settings, saving = false) }
                }
                .onFailure { e ->
                    _state.update {
                        it.copy(settings = previous, saving = false, message = ChatCatalog.errorText(e))
                    }
                }
        }
    }
}

/** 私信设置：谁可以直接发消息、是否接收消息请求（与网页 /chat/settings 一致）。 */
@Composable
fun ChatSettingsScreen(
    app: KungalApp,
    onBack: () -> Unit,
) {
    val vm: ChatSettingsViewModel = viewModel { ChatSettingsViewModel(app) }
    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }

    LaunchedEffect(state.message) {
        val text = state.message ?: return@LaunchedEffect
        snackbarHost.showSnackbar(text)
        vm.clearMessage()
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "私信设置",
                subtitle = "设置跟随你的 NextMoe 账号，各站点通用",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbarHost) },
    ) { padding ->
        when {
            state.loading -> LoadingBox(Modifier.fillMaxSize().padding(padding))
            state.error != null -> ErrorBox(
                message = state.error!!,
                onRetry = vm::load,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            state.settings != null -> {
                val settings = state.settings!!
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        "谁的私信可以直接进入对话列表",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    SettingsGroup(title = "直接进入对话列表") {
                        ChatCatalog.allowIncomingOptions.forEach { (value, label) ->
                            item(
                                headline = label,
                                supporting = when (value) {
                                    "all" -> "任何人的私信都直接进入你的对话列表"
                                    "following" -> "其他人的私信先进入「消息请求」"
                                    else -> "所有新对话都先进入「消息请求」"
                                },
                                onClick = if (state.saving) null else {
                                    { vm.setAllowIncoming(value) }
                                },
                                trailing = {
                                    RadioButton(
                                        selected = settings.allowIncoming == value,
                                        onClick = { vm.setAllowIncoming(value) },
                                        enabled = !state.saving,
                                    )
                                },
                            )
                        }
                    }
                    SettingsGroup(title = "消息请求") {
                        item(
                            headline = "接收消息请求",
                            supporting = "关闭后，上面范围之外的人无法给你发私信",
                            onClick = if (state.saving) null else {
                                { vm.setAcceptRequests(!settings.acceptRequests) }
                            },
                            trailing = {
                                Switch(
                                    checked = settings.acceptRequests,
                                    onCheckedChange = { vm.setAcceptRequests(it) },
                                    enabled = !state.saving,
                                )
                            },
                        )
                    }
                    Text(
                        "设置保存在 NextMoe 账号，网页与 App 通用。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
