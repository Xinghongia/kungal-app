package com.kungal.app.ui.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OAuthCallbackTest {

    @Test
    fun `解析成功回调`() {
        val callback = parseOAuthCallback(
            "https://www.kungal.com/auth/callback?code=abc123&state=state-1",
        )
        assertEquals("abc123", callback?.code)
        assertEquals("state-1", callback?.state)
        assertNull(callback?.error)
    }

    @Test
    fun `解析拒绝授权回调`() {
        val callback = parseOAuthCallback(
            "https://www.kungal.com/auth/callback?error=access_denied" +
                "&error_description=%E6%8E%88%E6%9D%83%E8%A2%AB%E6%8B%92%E7%BB%9D",
        )
        assertEquals("access_denied", callback?.error)
        assertEquals("授权被拒绝", callback?.errorDescription)
    }

    @Test
    fun `非回调地址不误判`() {
        assertNull(parseOAuthCallback("https://www.kungal.com/auth/callback-extra?code=abc"))
        assertNull(parseOAuthCallback("https://account.nextmoe.com/oauth/authorize?code=abc"))
        assertNull(parseOAuthCallback("not a url"))
    }
}
