package com.kungal.app.ui.user

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.Chip
import com.kungal.app.ui.components.ErrorBox
import com.kungal.app.ui.components.KungalSnackbarHost
import com.kungal.app.ui.components.KungalTopAppBar
import com.kungal.app.ui.components.LoadingBox
import com.kungal.app.ui.topic.friendlyError
import com.kungal.core.model.Account
import com.kungal.core.model.AccountPrices
import com.kungal.core.model.ActivitySettings
import com.kungal.core.model.Me
import com.kungal.core.model.PrivacySettings
import com.kungal.core.model.ProfileSettings
import com.kungal.core.model.UserProfile
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class SettingsBundle(
    val account: Account,
    val me: Me,
    val ownProfile: UserProfile,
    val prices: AccountPrices?,
    val profile: ProfileSettings?,
    val privacy: PrivacySettings?,
    val activity: ActivitySettings?,
)

class ProfileSettingsViewModel(private val app: KungalApp) : ViewModel() {

    data class State(
        val loading: Boolean = true,
        val error: String? = null,
        val account: Account? = null,
        val me: Me? = null,
        val name: String = "",
        val bio: String = "",
        val bioOriginal: String = "",
        val renameCost: Int? = null,
        val signInRequired: Boolean = false,
        val playRecordsPublic: Boolean = true,
        val activityHidden: Boolean = false,
        val savingName: Boolean = false,
        val savingBio: Boolean = false,
        val uploading: Boolean = false,
        val message: String? = null,
    )

    private val _state = MutableStateFlow(State())
    val state: StateFlow<State> = _state.asStateFlow()

    private var signInJob: Job? = null
    private var playRecordsJob: Job? = null
    private var activityJob: Job? = null

    init {
        load()
    }

    fun load() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            runCatching {
                coroutineScope {
                    val account = app.meApi.account()
                    val me = async { app.meApi.me() }
                    val ownProfile = async { app.userApi.profile(account.id) }
                    val prices = async { runCatching { app.meApi.accountPrices() }.getOrNull() }
                    val profile = async { runCatching { app.meApi.profileSettings() }.getOrNull() }
                    val privacy = async { runCatching { app.meApi.privacySettings() }.getOrNull() }
                    val activity = async { runCatching { app.meApi.activitySettings() }.getOrNull() }
                    SettingsBundle(
                        account = account,
                        me = me.await(),
                        ownProfile = ownProfile.await(),
                        prices = prices.await(),
                        profile = profile.await(),
                        privacy = privacy.await(),
                        activity = activity.await(),
                    )
                }
            }.onSuccess { bundle ->
                _state.update {
                    it.copy(
                        loading = false,
                        account = bundle.account,
                        me = bundle.me,
                        name = bundle.account.name.orEmpty(),
                        bio = bundle.ownProfile.bio.orEmpty(),
                        bioOriginal = bundle.ownProfile.bio.orEmpty(),
                        renameCost = bundle.prices?.renameCost,
                        signInRequired = bundle.profile?.isSignInRequired ?: false,
                        playRecordsPublic = bundle.privacy?.isPlayRecordsPublic ?: true,
                        activityHidden = bundle.activity?.isHidden ?: false,
                    )
                }
            }.onFailure { e ->
                _state.update { it.copy(loading = false, error = friendlyError(e)) }
            }
        }
    }

    fun updateName(value: String) {
        _state.update { it.copy(name = value.take(17)) }
    }

    fun updateBio(value: String) {
        _state.update { it.copy(bio = value.take(107)) }
    }

    /** 昵称保存（调用方负责先确认改名费用）。 */
    fun saveName() {
        val current = _state.value
        val name = current.name.trim()
        if (name.isBlank() || name == current.account?.name) return
        if (current.savingName) return
        _state.update { it.copy(savingName = true, message = null) }
        viewModelScope.launch {
            runCatching { app.meApi.updateProfile(name = name) }
                .onSuccess { profile ->
                    app.notifyAccountChanged()
                    _state.update {
                        it.copy(
                            savingName = false,
                            account = it.account?.copy(name = profile.name),
                            name = profile.name.orEmpty(),
                            message = "昵称已更新",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(savingName = false, message = friendlyError(e)) }
                }
        }
    }

    fun saveBio() {
        val current = _state.value
        val bio = current.bio
        if (bio == current.bioOriginal || current.savingBio) return
        _state.update { it.copy(savingBio = true, message = null) }
        viewModelScope.launch {
            runCatching { app.meApi.updateProfile(bio = bio) }
                .onSuccess { profile ->
                    app.notifyAccountChanged()
                    _state.update {
                        it.copy(
                            savingBio = false,
                            bio = profile.bio.orEmpty(),
                            bioOriginal = profile.bio.orEmpty(),
                            message = "签名已更新",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(savingBio = false, message = friendlyError(e)) }
                }
        }
    }

    fun uploadAvatar(bytes: ByteArray, fileName: String, contentType: String) {
        if (_state.value.uploading) return
        _state.update { it.copy(uploading = true, message = null) }
        viewModelScope.launch {
            runCatching { app.meApi.uploadAvatar(bytes, fileName, contentType) }
                .onSuccess { image ->
                    app.notifyAccountChanged()
                    _state.update {
                        it.copy(
                            uploading = false,
                            account = it.account?.copy(avatar = image),
                            message = "头像已更新",
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(uploading = false, message = friendlyError(e)) }
                }
        }
    }

    fun setSignInRequired(value: Boolean) {
        if (_state.value.signInRequired == value) return
        _state.update { it.copy(signInRequired = value, message = null) }
        signInJob = savePrivacy(
            previous = signInJob,
            value = value,
            apply = { on -> _state.update { it.copy(signInRequired = on) } },
            call = { app.meApi.setProfileSignInRequired(value).isSignInRequired },
        )
    }

    fun setPlayRecordsPublic(value: Boolean) {
        if (_state.value.playRecordsPublic == value) return
        _state.update { it.copy(playRecordsPublic = value, message = null) }
        playRecordsJob = savePrivacy(
            previous = playRecordsJob,
            value = value,
            apply = { on -> _state.update { it.copy(playRecordsPublic = on) } },
            call = { app.meApi.setPlayRecordsPublic(value).isPlayRecordsPublic },
        )
    }

    fun setActivityHidden(value: Boolean) {
        if (_state.value.activityHidden == value) return
        _state.update { it.copy(activityHidden = value, message = null) }
        activityJob = savePrivacy(
            previous = activityJob,
            value = value,
            apply = { on -> _state.update { it.copy(activityHidden = on) } },
            call = { app.meApi.setActivityHidden(value).isHidden },
        )
    }

    /**
     * 隐私开关点击后立即切换界面，不等网络；300ms 防抖后写回，连续点按只发最后一次。
     * 保存期间开关保持可点（不禁用，避免颜色变灰和「卡住」的观感）；失败时回滚并提示。
     */
    private fun savePrivacy(
        previous: Job?,
        value: Boolean,
        apply: (Boolean) -> Unit,
        call: suspend () -> Boolean,
    ): Job {
        previous?.cancel()
        return viewModelScope.launch {
            try {
                delay(300)
                apply(call())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Throwable) {
                apply(!value)
                _state.update { it.copy(message = friendlyError(e)) }
            }
        }
    }

    fun clearMessage() = _state.update { it.copy(message = null) }
}

/**
 * 资料设置（网页 /user/{id}/setting）：头像、昵称、签名与三项隐私开关。
 * 改名按站点价格扣萌萌点，保存前二次确认。
 */
@Composable
fun ProfileSettingsScreen(
    app: KungalApp,
    onBack: () -> Unit,
    onOpenAccountCenter: () -> Unit,
) {
    val vm: ProfileSettingsViewModel = viewModel { ProfileSettingsViewModel(app) }
    val state by vm.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    var confirmRename by remember { mutableStateOf(false) }

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            vm.clearMessage()
        }
    }

    val pickAvatar = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val bytes = withContext(Dispatchers.IO) {
                runCatching {
                    context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                }.getOrNull()
            }
            if (bytes == null) {
                snackbarHost.showSnackbar("无法读取这张图片")
                return@launch
            }
            if (bytes.size > 4 * 1024 * 1024) {
                snackbarHost.showSnackbar("头像不能超过 4 MiB")
                return@launch
            }
            val mime = context.contentResolver.getType(uri) ?: "image/png"
            val ext = when (mime) {
                "image/jpeg" -> "jpg"
                "image/webp" -> "webp"
                "image/gif" -> "gif"
                "image/avif" -> "avif"
                else -> "png"
            }
            vm.uploadAvatar(bytes, "avatar.$ext", mime)
        }
    }

    if (confirmRename) {
        AlertDialog(
            onDismissRequest = { confirmRename = false },
            title = { Text("确认修改昵称") },
            text = {
                val cost = state.renameCost ?: 0
                Text(
                    if (cost > 0) "改名将消耗 $cost 萌萌点，修改后立即生效。确定继续吗？"
                    else "修改昵称后立即生效，确定继续吗？",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    confirmRename = false
                    vm.saveName()
                }) { Text("确定") }
            },
            dismissButton = { TextButton(onClick = { confirmRename = false }) { Text("取消") } },
        )
    }

    Scaffold(
        topBar = {
            KungalTopAppBar(
                title = "资料设置",
                subtitle = "头像、昵称、签名与隐私",
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
                state.error!!,
                onRetry = vm::load,
                modifier = Modifier.fillMaxSize().padding(padding),
            )
            else -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SectionHeader("头像")
                Row(verticalAlignment = Alignment.CenterVertically) {
                    FramedAvatar(
                        avatarUrl = state.account?.avatar?.url,
                        frame = null,
                        size = 84.dp,
                        seed = state.account?.name,
                    )
                    Spacer(Modifier.width(16.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Button(
                            onClick = { pickAvatar.launch("image/*") },
                            enabled = !state.uploading,
                        ) { Text(if (state.uploading) "上传中…" else "更换头像") }
                        Text(
                            "PNG / JPEG / WebP / GIF / AVIF，最大 4 MiB",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                SectionHeader("昵称")
                OutlinedTextField(
                    value = state.name,
                    onValueChange = vm::updateName,
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    supportingText = {
                        Text("1–17 位；改名会消耗萌萌点" + (state.renameCost?.let { "（当前 $it）" } ?: ""))
                    },
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(
                        onClick = { confirmRename = true },
                        enabled = !state.savingName &&
                            state.name.trim().isNotBlank() &&
                            state.name.trim() != state.account?.name,
                    ) { Text(if (state.savingName) "保存中…" else "保存昵称") }
                    if ((state.renameCost ?: 0) > 0) {
                        Chip("花费 ${state.renameCost} 萌萌点")
                    }
                }

                SectionHeader("签名")
                OutlinedTextField(
                    value = state.bio,
                    onValueChange = vm::updateBio,
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 6,
                    supportingText = { Text("${state.bio.length}/107，纯文本") },
                )
                Button(
                    onClick = vm::saveBio,
                    enabled = !state.savingBio && state.bio != state.bioOriginal,
                ) { Text(if (state.savingBio) "保存中…" else "保存签名") }

                SectionHeader("隐私")
                PrivacySwitchRow(
                    title = "主页仅登录可见",
                    description = "开启后，未登录访客看不到你的主页与内容列表",
                    checked = state.signInRequired,
                    onCheckedChange = vm::setSignInRequired,
                )
                PrivacySwitchRow(
                    title = "公开游玩记录",
                    description = "关闭后，其他人查看你的游戏库会显示「未公开」",
                    checked = state.playRecordsPublic,
                    onCheckedChange = vm::setPlayRecordsPublic,
                )
                PrivacySwitchRow(
                    title = "隐藏动态",
                    description = "开启后，你的动作不会出现在关注者的动态流里",
                    checked = state.activityHidden,
                    onCheckedChange = vm::setActivityHidden,
                )

                SectionHeader("账号中心")
                Text(
                    "邮箱、密码与长介绍由 NextMoe 账号中心管理，App 内不保存这些凭据。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(onClick = onOpenAccountCenter) { Text("打开账号中心") }
                Spacer(Modifier.size(24.dp))
            }
        }
    }
}

@Composable
private fun PrivacySwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Surface(
        shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onCheckedChange(!checked) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Text(
                    description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
