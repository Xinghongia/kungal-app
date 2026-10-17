package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.model.Account
import com.kungal.core.model.Me

/** 当前登录账号的读取接口（对应 v1 /me、/me/account）。 */
class MeApi(private val client: KungalClient) {

    suspend fun me(): Me = client.get("/me", deserializer = Me.serializer())

    suspend fun account(): Account = client.get("/me/account", deserializer = Account.serializer())
}
