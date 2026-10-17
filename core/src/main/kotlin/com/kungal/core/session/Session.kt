package com.kungal.core.session

import kotlinx.serialization.Serializable

/**
 * 网页登录会话：kungal_session 不透明 cookie（90 天滑动续期，服务端自动刷新 OAuth token）。
 * cookie 值的来源 = WebView 登录成功后从 CookieManager 捕获；这里只做保存与注入。
 *
 * 安全约定：本对象只存 cookie 原值，不存任何账号密码；上层持久化时不得写日志。
 */
@Serializable
data class Session(
    val cookie: String,
    val savedAt: Long = 0,
) {
    /** 直接作为 API 请求的 Cookie 头值。 */
    val cookieHeader: String get() = cookie
}

/** 进程内会话持有者；持久化由 app 层负责（DataStore）。 */
class SessionHolder {
    @Volatile
    var current: Session? = null
        private set

    val isLoggedIn: Boolean get() = current != null

    fun restore(session: Session?) {
        current = session
    }

    fun clear() {
        current = null
    }
}
