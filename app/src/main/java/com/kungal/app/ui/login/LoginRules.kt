package com.kungal.app.ui.login

/**
 * 登录 / 注册表单规则，与 NextMoe 账号中心网页一致：
 * 用户名 1–17 个字符（字母、数字或 !~_@#$%^&*()+=-，不含空格与不可见字符）、
 * 邮箱常规格式、密码至少 6 位、验证码 6 位。
 */
object LoginRules {

    private val usernamePattern =
        Regex("""^[\p{L}\p{N}!~_@#$%^&*()+=\-]{1,17}$""")
    private val emailPattern = Regex("""^[^\s@]+@[^\s@]+\.[^\s@]+$""")
    private val codePattern = Regex("""^\d{6}$""")

    fun loginError(account: String, password: String): String? = when {
        account.isBlank() -> "请输入账号（用户名或邮箱）"
        password.isEmpty() -> "请输入密码"
        else -> null
    }

    /** 发送验证码前只需要用户名与邮箱合法。 */
    fun registerAccountError(name: String, email: String): String? = when {
        name.isBlank() -> "请输入用户名"
        name.any { it.isWhitespace() || it.isISOControl() } ||
            !usernamePattern.matches(name) ->
            "用户名为 1–17 个字符，只能使用字母、数字或 !~_@#$%^&*()+=-，不能包含空格"
        email.isBlank() -> "请输入邮箱"
        !emailPattern.matches(email) -> "邮箱格式不正确"
        else -> null
    }

    fun registerError(
        name: String,
        email: String,
        password: String,
        confirmPassword: String,
        code: String,
    ): String? = registerAccountError(name, email) ?: when {
        password.length < 6 -> "密码长度至少为 6 位"
        password != confirmPassword -> "两次输入的密码不一致"
        !codePattern.matches(code) -> "请输入 6 位验证码"
        else -> null
    }
}
