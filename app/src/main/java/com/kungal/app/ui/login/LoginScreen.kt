package com.kungal.app.ui.login

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.kungal.app.KungalApp
import kotlinx.coroutines.launch

private const val LOGIN_URL = "https://www.kungal.com/"
private const val SESSION_COOKIE = "kungal_session"

/**
 * 网页登录页：App 内 WebView 走站点自己的登录（账号统一在 account.nextmoe.com）。
 * 不代填账号密码、不模拟登录请求；只观察站点自己种下的 kungal_session cookie，
 * 拿到即持久化完成登录。
 */
@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginScreen(
    app: KungalApp,
    onDone: () -> Unit,
    onCancel: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var finished by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    fun captureSession(): Boolean {
        val cookie = CookieManager.getInstance().getCookie("https://www.kungal.com").orEmpty()
        val value = cookie
            .split(';')
            .map { it.trim() }
            .firstOrNull { it.startsWith("$SESSION_COOKIE=") }
            ?: return false
        if (finished) return true
        finished = true
        scope.launch {
            app.onLoggedIn(value)
            onDone()
        }
        return true
    }

    BackHandler {
        if (webView?.canGoBack() == true) webView?.goBack() else onCancel()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("登录") },
                navigationIcon = {
                    IconButton(onClick = { if (webView?.canGoBack() == true) webView?.goBack() else onCancel() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            Text(
                text = "在下方网页里登录或注册（账号由 NextMoe·未萌 统一管理）。登录成功会自动返回。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        WebView(context).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    loading = true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    loading = false
                                    captureSession()
                                }
                            }
                            CookieManager.getInstance().setAcceptCookie(true)
                            loadUrl(LOGIN_URL)
                            webView = this
                        }
                    },
                )
                if (loading) {
                    Text(
                        text = "网页加载中…",
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
