package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.Account
import com.kungal.core.model.Me
import com.kungal.core.model.NsfwDisplayResult
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 当前登录账号的读取接口（对应 v1 /me、/me/account、/me/nsfw-display）。 */
class MeApi(private val client: KungalClient) {

    @Serializable
    private data class NsfwDisplayBody(
        @SerialName("nsfw_display") val nsfwDisplay: String,
    )

    suspend fun me(): Me = client.get("/me", deserializer = Me.serializer())

    suspend fun account(): Account = client.get("/me/account", deserializer = Account.serializer())

    /**
     * 修改成人向内容显示方式（hide / blur / show）。设置跟随 NextMoe 账号，
     * 所有站点生效；服务端同时把新姿态写回当前 cookie 会话。
     */
    suspend fun setNsfwDisplay(display: String): NsfwDisplayResult {
        val body = KungalJson.encodeToString(
            NsfwDisplayBody.serializer(),
            NsfwDisplayBody(display),
        )
        return client.putJson(
            path = "/me/nsfw-display",
            body = body,
            deserializer = NsfwDisplayResult.serializer(),
        )
    }
}
