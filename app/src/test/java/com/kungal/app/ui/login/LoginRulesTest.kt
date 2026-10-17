package com.kungal.app.ui.login

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LoginRulesTest {

    @Test
    fun `登录只要求账号和密码非空`() {
        assertEquals("请输入账号（用户名或邮箱）", LoginRules.loginError("", "x"))
        assertEquals("请输入密码", LoginRules.loginError("someone", ""))
        assertNull(LoginRules.loginError("someone", "x"))
    }

    @Test
    fun `注册用户名规则与网页一致`() {
        assertNull(LoginRules.registerAccountError("abc_123", "a@b.co"))
        assertNull(LoginRules.registerAccountError("张三", "a@b.co"))
        assertEquals(
            "用户名为 1–17 个字符，只能使用字母、数字或 !~_@#$%^&*()+=-，不能包含空格",
            LoginRules.registerAccountError("has space", "a@b.co"),
        )
        assertEquals(
            "用户名为 1–17 个字符，只能使用字母、数字或 !~_@#$%^&*()+=-，不能包含空格",
            LoginRules.registerAccountError("a".repeat(18), "a@b.co"),
        )
    }

    @Test
    fun `注册邮箱格式`() {
        assertEquals("邮箱格式不正确", LoginRules.registerAccountError("abc", "not-an-email"))
        assertEquals("邮箱格式不正确", LoginRules.registerAccountError("abc", "a@b"))
        assertNull(LoginRules.registerAccountError("abc", "a@b.co"))
    }

    @Test
    fun `注册密码确认与验证码`() {
        assertEquals(
            "密码长度至少为 6 位",
            LoginRules.registerError("abc", "a@b.co", "12345", "12345", "123456"),
        )
        assertEquals(
            "两次输入的密码不一致",
            LoginRules.registerError("abc", "a@b.co", "123456", "123457", "123456"),
        )
        assertEquals(
            "请输入 6 位验证码",
            LoginRules.registerError("abc", "a@b.co", "123456", "123456", "12345"),
        )
        assertNull(LoginRules.registerError("abc", "a@b.co", "123456", "123456", "123456"))
    }
}
