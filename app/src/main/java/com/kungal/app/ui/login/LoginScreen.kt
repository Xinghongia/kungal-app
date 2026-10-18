package com.kungal.app.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kungal.app.KungalApp
import com.kungal.app.ui.components.AppMessages
import com.kungal.core.api.oauthErrorMessage
import com.kungal.core.oauth.Pkce
import kotlinx.coroutines.launch

/**
 * 登录：默认使用原生账号密码表单（与网页同一套 NextMoe OAuth 流程）；
 * 遇到需要额外验证的账号时，可切换到网页登录兜底。
 */
@Composable
fun LoginScreen(
    app: KungalApp,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    var useWeb by remember { mutableStateOf(false) }
    if (useWeb) {
        WebLoginScreen(
            app = app,
            onDone = onDone,
            onCancel = onCancel,
            onSwitchToNative = { useWeb = false },
        )
    } else {
        NativeLoginScreen(
            app = app,
            onDone = onDone,
            onCancel = onCancel,
            onUseWeb = { useWeb = true },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NativeLoginScreen(
    app: KungalApp,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onUseWeb: () -> Unit,
) {
    val vm: LoginViewModel = viewModel { LoginViewModel(app) }
    val state by vm.state.collectAsState()
    val registerMode = state.mode == LoginViewModel.Mode.REGISTER

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (registerMode) "注册账号" else "登录") },
                navigationIcon = {
                    IconButton(onClick = onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(8.dp))
            Text(
                "kungal",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                "鲲 Galgame 论坛 · 非官方第三方客户端",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = !registerMode,
                    onClick = { vm.setMode(LoginViewModel.Mode.LOGIN) },
                    label = { Text("账号密码登录") },
                    enabled = !state.busy,
                )
                FilterChip(
                    selected = registerMode,
                    onClick = { vm.setMode(LoginViewModel.Mode.REGISTER) },
                    label = { Text("注册新账号") },
                    enabled = !state.busy,
                )
            }
            Spacer(Modifier.height(16.dp))

            Column(
                modifier = Modifier.fillMaxWidth().widthIn(max = 480.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (registerMode) {
                    OutlinedTextField(
                        value = state.name,
                        onValueChange = vm::setName,
                        label = { Text("用户名") },
                        placeholder = { Text("1–17 个字符") },
                        singleLine = true,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.email,
                        onValueChange = vm::setEmail,
                        label = { Text("邮箱") },
                        placeholder = { Text("用于接收注册验证码") },
                        singleLine = true,
                        enabled = !state.busy,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        OutlinedTextField(
                            value = state.code,
                            onValueChange = vm::setCode,
                            label = { Text("验证码") },
                            placeholder = { Text("6 位") },
                            singleLine = true,
                            enabled = !state.busy,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        FilledTonalButton(
                            onClick = vm::sendCode,
                            enabled = !state.busy && !state.sendingCode && state.cooldown == 0,
                        ) {
                            Text(
                                when {
                                    state.sendingCode -> "发送中…"
                                    state.cooldown > 0 -> "${state.cooldown}s 后重发"
                                    else -> "获取验证码"
                                },
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                } else {
                    OutlinedTextField(
                        value = state.account,
                        onValueChange = vm::setAccount,
                        label = { Text("账号") },
                        placeholder = { Text("邮箱或用户名") },
                        singleLine = true,
                        enabled = !state.busy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                PasswordField(
                    value = state.password,
                    onValueChange = vm::setPassword,
                    label = { Text("密码") },
                    enabled = !state.busy,
                )
                if (registerMode) {
                    PasswordField(
                        value = state.confirmPassword,
                        onValueChange = vm::setConfirmPassword,
                        label = { Text("确认密码") },
                        enabled = !state.busy,
                    )
                }

                if (state.codeSent && registerMode) {
                    Text(
                        "验证码已发送到邮箱，请查收（含垃圾箱）；几分钟内有效。",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                state.error?.let { error ->
                    Text(
                        error,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(2.dp))
                Button(
                    onClick = { if (registerMode) vm.register(onDone) else vm.login(onDone) },
                    enabled = !state.busy,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                ) {
                    if (state.busy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    } else {
                        Text(if (registerMode) "注册并登录" else "登录")
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "账号由 NextMoe·未萌 统一管理。密码只用于本次登录，不会保存在本机，也不会写入日志。",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextButton(onClick = onUseWeb, enabled = !state.busy) {
                        Text("改用网页登录 / 注册", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@Composable
private fun PasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    label: @Composable () -> Unit,
    enabled: Boolean,
) {
    var visible by remember { mutableStateOf(false) }
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = label,
        singleLine = true,
        enabled = enabled,
        visualTransformation = if (visible) VisualTransformation.None
        else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        trailingIcon = {
            IconButton(onClick = { visible = !visible }) {
                Icon(
                    if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                    contentDescription = if (visible) "隐藏密码" else "显示密码",
                )
            }
        },
        modifier = Modifier.fillMaxWidth(),
    )
}

/**
 * 网页登录兜底：直接打开 NextMoe 账号中心的 OAuth 授权页，不再经过论坛首页的登录弹窗。
 * 站内登录页在窄屏上会把弹窗定位到屏幕外，绕开它可以避免不同机型显示错位/点不到。
 *
 * 流程：App 生成 PKCE → 账号中心登录或注册 → 授权后回跳站点回调地址 →
 * WebView 拦下回跳，用现有原生逻辑把授权码换成 `kungal_session`。
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
private fun WebLoginScreen(
    app: KungalApp,
    onDone: () -> Unit,
    onCancel: () -> Unit,
    onSwitchToNative: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val pkce = remember { Pkce.generate() }
    val loginUrl = remember(pkce) { app.authApi.buildAuthorizeUrl(pkce) }
    var loading by remember { mutableStateOf(true) }
    var finishing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var handled by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun failWith(message: String) {
        handled = false
        finishing = false
        loading = false
        error = message
    }

    fun handleCallback(url: String): Boolean {
        val callback = parseOAuthCallback(url) ?: return false
        if (handled || finishing) return true
        handled = true
        error = null

        callback.error?.let { errorCode ->
            failWith(oauthErrorMessage(errorCode, callback.errorDescription))
            return true
        }
        if (callback.state != pkce.state) {
            failWith("登录校验失败，请重试")
            return true
        }
        val code = callback.code
        if (code.isNullOrBlank()) {
            failWith("登录失败：未收到授权码")
            return true
        }

        loading = true
        finishing = true
        scope.launch {
            runCatching { app.authApi.exchangeAuthorizationCode(code, pkce.verifier) }
                .onSuccess { cookie ->
                    app.onLoggedIn(cookie)
                    AppMessages.show("登录成功，欢迎回来！")
                    onDone()
                }
                .onFailure { e -> failWith(e.message ?: "登录失败，请重试") }
        }
        return true
    }

    BackHandler {
        if (webView?.canGoBack() == true) webView?.goBack() else onCancel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("网页登录") },
                navigationIcon = {
                    IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() else onCancel() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    TextButton(onClick = onSwitchToNative) { Text("账号密码登录") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .consumeWindowInsets(padding)
                .imePadding(),
        ) {
            Text(
                text = "在下方账号中心页面登录或注册（账号由 NextMoe·未萌 统一管理）。" +
                    "登录成功会自动返回，不经过论坛首页。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            error?.let { message ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(
                        onClick = {
                            error = null
                            loading = true
                            webView?.loadUrl(loginUrl)
                        },
                    ) { Text("重新加载") }
                }
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            // 与手机 Chrome 一致：按站点 viewport 排版并固定字号，
                            // 避免不同机型或系统字体放大造成页面错位、弹窗跑出屏幕。
                            settings.useWideViewPort = true
                            settings.loadWithOverviewMode = true
                            settings.textZoom = 100
                            settings.setSupportZoom(true)
                            settings.builtInZoomControls = true
                            settings.displayZoomControls = false
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?,
                                ): Boolean {
                                    if (request?.isForMainFrame != true) return false
                                    return handleCallback(request.url.toString())
                                }

                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    // 服务端 302 有时不会走 shouldOverrideUrlLoading，这里兜底。
                                    if (url != null && handleCallback(url)) {
                                        view?.stopLoading()
                                        return
                                    }
                                    if (!finishing) {
                                        loading = true
                                        error = null
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    if (!finishing) loading = false
                                }
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            loadUrl(loginUrl)
                            webView = this
                        }
                    },
                )
                if (loading) {
                    Text(
                        text = if (finishing) "正在完成登录…" else "网页加载中…",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.align(Alignment.Center).padding(top = 48.dp),
                    )
                }
            }
        }
    }
}
