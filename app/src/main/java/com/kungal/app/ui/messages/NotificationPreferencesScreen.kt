package com.kungal.app.ui.messages

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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.kungal.app.ui.components.TabChips
import com.kungal.app.ui.topic.friendlyError
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class NotificationPreferencesViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val mutedTypes: Set<String> = emptySet(),
        val saving: Boolean = false,
        val error: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        load()
    }

    fun clearMessage() {
        _message.value = null
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { app.notificationApi.preferences() }
                .onSuccess { prefs ->
                    _state.value = State(loading = false, mutedTypes = prefs.mutedTypes.toSet())
                }
                .onFailure { e ->
                    _state.value = State(loading = false, error = friendlyError(e))
                }
        }
    }

    /** 开关语义：enabled=true 表示不静音。使用单类型接口，失败回滚。 */
    fun setEnabled(type: String, enabled: Boolean) {
        if (_state.value.saving) return
        val previous = _state.value.mutedTypes
        val optimistic = if (enabled) previous - type else previous + type
        _state.update { it.copy(mutedTypes = optimistic, saving = true) }
        viewModelScope.launch {
            runCatching {
                if (enabled) app.notificationApi.unmuteType(type)
                else app.notificationApi.muteType(type)
            }.onSuccess { prefs ->
                _state.update { it.copy(mutedTypes = prefs.mutedTypes.toSet(), saving = false) }
                app.refreshMessageBadge()
            }.onFailure { e ->
                _state.update { it.copy(mutedTypes = previous, saving = false) }
                _message.value = friendlyError(e)
            }
        }
    }
}

/** 通知偏好：按网页分组逐项开关，改动立即保存。 */
@Composable
fun NotificationPreferencesScreen(
    app: KungalApp,
    onBack: () -> Unit,
) {
    val vm: NotificationPreferencesViewModel = viewModel { NotificationPreferencesViewModel(app) }
    val state by vm.state.collectAsState()
    val message by vm.message.collectAsState()
    val snackbar = remember { SnackbarHostState() }
    var activeKey by rememberSaveable { mutableStateOf(NotificationCatalog.preferenceGroups.first().key) }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbar.showSnackbar(text)
        vm.clearMessage()
    }

    val groups = NotificationCatalog.preferenceGroups
    val active = groups.firstOrNull { it.key == activeKey } ?: groups.first()

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "通知设置",
                subtitle = "改动会立即保存到账号",
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        snackbarHost = { KungalSnackbarHost(snackbar) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(top = 4.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "关闭某类通知后，它不会再点亮红点，也不会出现在通知列表里，" +
                    "而是收进「已静音的消息」，你随时可以去那里查看。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )

            when {
                state.loading -> LoadingBox(Modifier.padding(vertical = 24.dp))
                state.error != null -> ErrorBox(message = state.error!!, onRetry = vm::load)
                else -> {
                    TabChips(
                        labels = groups.map { it.label },
                        selected = active.label,
                        onSelect = { label ->
                            groups.firstOrNull { it.label == label }?.let { activeKey = it.key }
                        },
                    )
                    SettingsGroup(title = active.label, modifier = Modifier.padding(horizontal = 16.dp)) {
                        active.types.forEach { type ->
                            val enabled = type !in state.mutedTypes
                            item(
                                headline = NotificationCatalog.typeLabel(type),
                                supporting = if (type == "chat") {
                                    "客户端私信接入后按此开关生效"
                                } else {
                                    null
                                },
                                onClick = if (state.saving) {
                                    null
                                } else {
                                    { vm.setEnabled(type, !enabled) }
                                },
                                trailing = {
                                    Switch(
                                        checked = enabled,
                                        enabled = !state.saving,
                                        onCheckedChange = { vm.setEnabled(type, it) },
                                    )
                                },
                            )
                        }
                    }
                    Text(
                        text = "私信开关控制私信未读是否计入消息角标；具体的私信会话在后续批次接入。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
        }
    }
}
