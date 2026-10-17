package com.kungal.app

import com.kungal.core.model.ContentStance

/**
 * 成人向内容显示方式，与网站设置面板的「内容」一致：
 * - HIDE：不返回 NSFW 内容（列表请求 include_nsfw=false）
 * - BLUR：返回并显示条目，封面打码，点击单张查看
 * - SHOW：直接显示
 *
 * 登录时该设置跟随 NextMoe 账号（服务端 /me/account 的 content_stance）；
 * 未登录时使用本机偏好（网站用 cookie，本应用用 DataStore）。
 */
enum class NsfwDisplayMode(val wire: String, val label: String) {
    HIDE("hide", "隐藏"),
    BLUR("blur", "模糊"),
    SHOW("show", "显示");

    val allowsNsfw: Boolean get() = this != HIDE
    val blursMedia: Boolean get() = this == BLUR

    companion object {
        fun fromWire(value: String?): NsfwDisplayMode =
            entries.firstOrNull { it.wire == value } ?: HIDE

        /** 与网页 foldContentStance 一致：未确认成年时一律隐藏。 */
        fun fromAccount(stance: ContentStance?): NsfwDisplayMode =
            if (stance?.isAdultConfirmed == true) fromWire(stance.nsfwDisplay) else HIDE
    }
}
