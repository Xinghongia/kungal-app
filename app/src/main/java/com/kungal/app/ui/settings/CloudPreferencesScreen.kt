package com.kungal.app.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.FilterChips
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.components.SettingsGroup
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.net.KungalApiException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.put

/**
 * 云端偏好（/me/preferences）：把网页「Galgame 卡片显示」等跨站点设置带进 App。
 * 只改认识的键，保留 feed_tabs 等其余字段；写入带 If-Match 版本校验。
 */
class CloudPreferencesViewModel(private val app: KungalApp) : ViewModel() {

    data class Values(
        val showPlatform: Boolean = true,
        val showRating: Boolean = true,
        val showViewLike: Boolean = true,
        val showUpdateTime: Boolean = true,
        val showNsfwBadge: Boolean = false,
        val showCompany: Boolean = true,
        val showJapaneseName: Boolean = false,
        val isOpenInNewTab: Boolean = false,
        val preferOriginalName: Boolean = false,
        val noResource: Boolean = false,
        val allOriginalLanguages: Boolean = false,
        val phoneCardColumns: Int = 3,
        val rounded: String = "md",
        val showKohaku: Boolean = false,
    )

    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val saving: Boolean = false,
        val unavailable: Boolean = false,
        val message: String? = null,
        val values: Values = Values(),
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var version = 0L
    private var doc = JsonObject(emptyMap())
    private val pending = linkedMapOf<String, JsonElement>()
    private var saveJob: Job? = null

    init {
        load()
    }

    fun clearMessage() = _state.update { it.copy(message = null) }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching { app.meApi.preferences() }
                .onSuccess { prefs ->
                    version = prefs.version
                    doc = prefs.doc
                    _state.value = State(loading = false, values = readValues(prefs.doc))
                }
                .onFailure { e -> _state.value = State(loading = false, error = readError(e)) }
        }
    }

    fun setBoolean(key: String, value: Boolean) {
        if (_state.value.loading) return
        _state.update { it.copy(values = applyBoolean(it.values, key, value)) }
        pending[key] = JsonPrimitive(value)
        scheduleSave()
    }

    fun setPhoneColumns(value: Int) {
        if (_state.value.loading) return
        _state.update { it.copy(values = it.values.copy(phoneCardColumns = value)) }
        pending["phone_card_columns"] = JsonPrimitive(value)
        scheduleSave()
    }

    fun setRounded(value: String) {
        if (_state.value.loading) return
        _state.update { it.copy(values = it.values.copy(rounded = value)) }
        pending["rounded"] = JsonPrimitive(value)
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(500)
            save()
        }
    }

    private suspend fun save(retry: Boolean = true) {
        if (pending.isEmpty()) return
        val changes = pending.toMap()
        val merged = buildJsonObject {
            doc.forEach { (key, value) -> put(key, value) }
            changes.forEach { (key, value) -> put(key, value) }
        }
        _state.update { it.copy(saving = true) }
        runCatching { app.meApi.savePreferences(merged, version) }
            .onSuccess { saved ->
                version = saved.version
                doc = if (saved.doc.isEmpty()) merged else saved.doc
                changes.keys.forEach { pending.remove(it) }
                _state.update {
                    it.copy(
                        saving = false,
                        values = if (saved.doc.isEmpty()) it.values else readValues(saved.doc),
                    )
                }
            }
            .onFailure { e ->
                if (retry && e is KungalApiException && e.httpStatus == 412) {
                    // 别处改过：拉最新文档，把本地改动重新合并后再写一次
                    runCatching { app.meApi.preferences() }.onSuccess { fresh ->
                        version = fresh.version
                        doc = fresh.doc
                        changes.forEach { (key, value) -> pending[key] = value }
                        save(retry = false)
                        return@onFailure
                    }
                }
                _state.update { it.copy(saving = false, message = readError(e)) }
            }
    }

    private fun readError(e: Throwable): String =
        if (e is KungalApiException && e.problem?.code == "SCOPE_REQUIRED") {
            "这次登录还没有授权云端偏好，请退出登录后重新登录再试。"
        } else {
            friendlyError(e)
        }

    private fun readValues(doc: JsonObject): Values = Values(
        showPlatform = doc.bool("show_platform", true),
        showRating = doc.bool("show_rating", true),
        showViewLike = doc.bool("show_view_like", true),
        showUpdateTime = doc.bool("show_update_time", true),
        showNsfwBadge = doc.bool("show_nsfw_badge", false),
        showCompany = doc.bool("show_company", true),
        showJapaneseName = doc.bool("show_japanese_name", false),
        isOpenInNewTab = doc.bool("is_open_in_new_tab", false),
        preferOriginalName = doc.bool("prefer_original_name", false),
        noResource = doc.bool("no_resource", false),
        allOriginalLanguages = doc.bool("all_original_languages", false),
        phoneCardColumns = doc.int("phone_card_columns", 3).coerceIn(2, 3),
        rounded = doc.string("rounded", "md").takeIf {
            it in setOf("none", "sm", "md", "lg")
        } ?: "md",
        showKohaku = doc.bool("show_kohaku", false),
    )

    private fun applyBoolean(values: Values, key: String, value: Boolean): Values = when (key) {
        "show_platform" -> values.copy(showPlatform = value)
        "show_rating" -> values.copy(showRating = value)
        "show_view_like" -> values.copy(showViewLike = value)
        "show_update_time" -> values.copy(showUpdateTime = value)
        "show_nsfw_badge" -> values.copy(showNsfwBadge = value)
        "show_company" -> values.copy(showCompany = value)
        "show_japanese_name" -> values.copy(showJapaneseName = value)
        "is_open_in_new_tab" -> values.copy(isOpenInNewTab = value)
        "prefer_original_name" -> values.copy(preferOriginalName = value)
        "no_resource" -> values.copy(noResource = value)
        "all_original_languages" -> values.copy(allOriginalLanguages = value)
        "show_kohaku" -> values.copy(showKohaku = value)
        else -> values
    }
}

private fun JsonObject.bool(key: String, fallback: Boolean): Boolean =
    (this[key] as? JsonPrimitive)?.booleanOrNull ?: fallback

private fun JsonObject.int(key: String, fallback: Int): Int =
    (this[key] as? JsonPrimitive)?.intOrNull ?: fallback

private fun JsonObject.string(key: String, fallback: String): String =
    (this[key] as? JsonPrimitive)?.contentOrNull ?: fallback

@Composable
fun CloudPreferencesScreen(
    app: KungalApp,
    onBack: () -> Unit,
) {
    val vm: CloudPreferencesViewModel = viewModel { CloudPreferencesViewModel(app) }
    val state by vm.state.collectAsState()
    val snackbarHost = remember { SnackbarHostState() }
    val values = state.values

    LaunchedEffect(state.message) {
        val text = state.message ?: return@LaunchedEffect
        snackbarHost.showSnackbar(text)
        vm.clearMessage()
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "云端偏好",
                subtitle = "与网页共用，跨 NextMoe 站点生效",
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
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 16.dp, end = 16.dp, top = 4.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                Text(
                    "这些是 NextMoe 账号的云端设置，网页与 App 共用；修改会自动保存并同步到其他站点。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SettingsGroup(title = "Galgame 卡片") {
                    switchItem(
                        headline = "显示平台",
                        supporting = "Windows / Android 等平台标记",
                        checked = values.showPlatform,
                        onCheckedChange = { vm.setBoolean("show_platform", it) },
                    )
                    switchItem(
                        headline = "显示评分",
                        supporting = "站点综合评分",
                        checked = values.showRating,
                        onCheckedChange = { vm.setBoolean("show_rating", it) },
                    )
                    switchItem(
                        headline = "显示浏览与点赞",
                        supporting = "浏览数、点赞数",
                        checked = values.showViewLike,
                        onCheckedChange = { vm.setBoolean("show_view_like", it) },
                    )
                    switchItem(
                        headline = "显示更新时间",
                        supporting = "作品资料的最近更新",
                        checked = values.showUpdateTime,
                        onCheckedChange = { vm.setBoolean("show_update_time", it) },
                    )
                    switchItem(
                        headline = "显示会社",
                        supporting = "制作会社名称",
                        checked = values.showCompany,
                        onCheckedChange = { vm.setBoolean("show_company", it) },
                    )
                    switchItem(
                        headline = "显示日语原名",
                        supporting = "以副标题形式展示原名",
                        checked = values.showJapaneseName,
                        onCheckedChange = { vm.setBoolean("show_japanese_name", it) },
                    )
                    switchItem(
                        headline = "优先使用原名",
                        supporting = "卡片标题优先显示作品原名",
                        checked = values.preferOriginalName,
                        onCheckedChange = { vm.setBoolean("prefer_original_name", it) },
                    )
                    switchItem(
                        headline = "显示 NSFW 标记",
                        supporting = "成人向作品角标",
                        checked = values.showNsfwBadge,
                        onCheckedChange = { vm.setBoolean("show_nsfw_badge", it) },
                    )
                    block {
                        Text("手机端列数", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "网页窄屏下每行显示的作品数",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilterChips(
                            options = listOf("2" to "两列", "3" to "三列"),
                            selected = values.phoneCardColumns.toString(),
                            onSelect = { vm.setPhoneColumns(it.toIntOrNull() ?: 3) },
                        )
                    }
                    block {
                        Text("卡片圆角", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            "作品卡片的圆角大小",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FilterChips(
                            options = listOf(
                                "none" to "无",
                                "sm" to "小",
                                "md" to "中",
                                "lg" to "大",
                            ),
                            selected = values.rounded,
                            onSelect = vm::setRounded,
                        )
                    }
                }
                SettingsGroup(title = "浏览") {
                    switchItem(
                        headline = "新标签页打开",
                        supporting = "网页端点击作品时在新标签页打开",
                        checked = values.isOpenInNewTab,
                        onCheckedChange = { vm.setBoolean("is_open_in_new_tab", it) },
                    )
                    switchItem(
                        headline = "包含无资源作品",
                        supporting = "列表与排行里也显示暂无资源的作品",
                        checked = values.noResource,
                        onCheckedChange = { vm.setBoolean("no_resource", it) },
                    )
                    switchItem(
                        headline = "包含非中日原作",
                        supporting = "显示原作语言不是日语或中文的作品",
                        checked = values.allOriginalLanguages,
                        onCheckedChange = { vm.setBoolean("all_original_languages", it) },
                    )
                    switchItem(
                        headline = "显示站娘（琥珀）",
                        supporting = "网页右下的看板娘",
                        checked = values.showKohaku,
                        onCheckedChange = { vm.setBoolean("show_kohaku", it) },
                    )
                }
                Text(
                    if (state.saving) "正在保存…" else "已与云端同步",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
