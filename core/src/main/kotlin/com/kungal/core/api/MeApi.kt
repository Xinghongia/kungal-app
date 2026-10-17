package com.kungal.core.api

import com.kungal.core.KungalClient
import com.kungal.core.KungalJson
import com.kungal.core.model.Account
import com.kungal.core.model.AccountPrices
import com.kungal.core.model.ActivitySettings
import com.kungal.core.model.CheckInResult
import com.kungal.core.model.CloudPreferences
import com.kungal.core.model.CreatorApplication
import com.kungal.core.model.CreatorApplicationCreate
import com.kungal.core.model.CreatorStatus
import com.kungal.core.model.ImageInfo
import com.kungal.core.model.Me
import com.kungal.core.model.MoemoepointEntry
import com.kungal.core.model.NsfwDisplayResult
import com.kungal.core.model.PermissionList
import com.kungal.core.model.PrivacySettings
import com.kungal.core.model.ProfileSettings
import com.kungal.core.model.ProfileUpdate
import com.kungal.core.net.PageList
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/** 当前登录账号的读取与资料设置接口（/me、/me/account、/me/profile、/me/avatar、隐私、签到）。 */
class MeApi(private val client: KungalClient) {

    @Serializable
    private data class NsfwDisplayBody(
        @SerialName("nsfw_display") val nsfwDisplay: String,
    )

    @Serializable
    private data class ProfilePatchBody(
        val name: String? = null,
        val bio: String? = null,
    )

    @Serializable
    private data class ProfileSettingsBody(
        @SerialName("is_sign_in_required") val isSignInRequired: Boolean,
    )

    @Serializable
    private data class PrivacySettingsBody(
        @SerialName("is_play_records_public") val isPlayRecordsPublic: Boolean,
    )

    @Serializable
    private data class ActivitySettingsBody(
        @SerialName("is_hidden") val isHidden: Boolean,
    )

    @Serializable
    private data class PreferencesBody(
        val doc: JsonObject,
    )

    suspend fun me(): Me = client.get("/me", deserializer = Me.serializer())

    suspend fun account(): Account = client.get("/me/account", deserializer = Account.serializer())

    /** 当前账号持有的权限键；只用于决定是否展示审阅入口，写操作仍由服务端复核。 */
    suspend fun permissions(): PermissionList = client.get(
        path = "/me/permissions",
        deserializer = PermissionList.serializer(),
    )

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

    /** 改名价格（公开接口）；null 表示站点当前不收改名费。 */
    suspend fun accountPrices(): AccountPrices = client.get(
        path = "/account-prices",
        deserializer = AccountPrices.serializer(),
    )

    /** 修改昵称或签名；只发提供的字段。改名会按站点价格扣萌萌点。 */
    suspend fun updateProfile(name: String? = null, bio: String? = null): ProfileUpdate {
        val body = KungalJson.encodeToString(
            ProfilePatchBody.serializer(),
            ProfilePatchBody(name = name, bio = bio),
        )
        return client.patchJson(
            path = "/me/profile",
            body = body,
            deserializer = ProfileUpdate.serializer(),
        )
    }

    /** 上传头像（multipart，图片类型，≤4 MiB）。 */
    suspend fun uploadAvatar(
        bytes: ByteArray,
        fileName: String,
        contentType: String,
    ): ImageInfo = client.putMultipart(
        path = "/me/avatar",
        fileBytes = bytes,
        fileName = fileName,
        contentType = contentType,
        deserializer = ImageInfo.serializer(),
    )

    suspend fun profileSettings(): ProfileSettings = client.get(
        path = "/me/profile-settings",
        deserializer = ProfileSettings.serializer(),
    )

    suspend fun setProfileSignInRequired(required: Boolean): ProfileSettings {
        val body = KungalJson.encodeToString(
            ProfileSettingsBody.serializer(),
            ProfileSettingsBody(required),
        )
        return client.putJson(
            path = "/me/profile-settings",
            body = body,
            deserializer = ProfileSettings.serializer(),
        )
    }

    suspend fun privacySettings(): PrivacySettings = client.get(
        path = "/me/privacy-settings",
        deserializer = PrivacySettings.serializer(),
    )

    suspend fun setPlayRecordsPublic(isPublic: Boolean): PrivacySettings {
        val body = KungalJson.encodeToString(
            PrivacySettingsBody.serializer(),
            PrivacySettingsBody(isPublic),
        )
        return client.patchJson(
            path = "/me/privacy-settings",
            body = body,
            deserializer = PrivacySettings.serializer(),
        )
    }

    suspend fun activitySettings(): ActivitySettings = client.get(
        path = "/me/activity-settings",
        deserializer = ActivitySettings.serializer(),
    )

    suspend fun setActivityHidden(hidden: Boolean): ActivitySettings {
        val body = KungalJson.encodeToString(
            ActivitySettingsBody.serializer(),
            ActivitySettingsBody(hidden),
        )
        return client.putJson(
            path = "/me/activity-settings",
            body = body,
            deserializer = ActivitySettings.serializer(),
        )
    }

    /** 每日签到；奖励随机 0–7 萌萌点。当日重复签到返回 ALREADY_EXISTS。 */
    suspend fun checkIn(idempotencyKey: String): CheckInResult = client.postNoBodyJson(
        path = "/me/check-ins",
        idempotencyKey = idempotencyKey,
        deserializer = CheckInResult.serializer(),
    )

    /** 萌萌点流水（游标分页）；reason 可筛选收入/支出原因。 */
    suspend fun moemoepointEntries(
        cursor: String? = null,
        limit: Int = 20,
        reason: String? = null,
    ): PageList<MoemoepointEntry> = client.get(
        path = "/me/moemoepoint-entries",
        query = mapOf("cursor" to cursor, "limit" to limit, "reason" to reason),
        deserializer = PageList.serializer(MoemoepointEntry.serializer()),
    )

    /** 创作者资格与最近一次申请。 */
    suspend fun creatorStatus(): CreatorStatus = client.get(
        path = "/me/creator-status",
        deserializer = CreatorStatus.serializer(),
    )
    /** 提交创作者申请；必须带幂等键。 */
    suspend fun applyCreator(statement: String, idempotencyKey: String): CreatorApplication {
        val body = KungalJson.encodeToString(
            CreatorApplicationCreate.serializer(),
            CreatorApplicationCreate(statement.trim()),
        )
        return client.postJson(
            path = "/me/creator-applications",
            body = body,
            idempotencyKey = idempotencyKey,
            deserializer = CreatorApplication.serializer(),
        )
    }

    /** 云端偏好（跨站点，Galgame 卡片展示等）。 */
    suspend fun preferences(): CloudPreferences = client.get(
        path = "/me/preferences",
        deserializer = CloudPreferences.serializer(),
    )

    /**
     * 写回云端偏好。version 非空时带 If-Match 做版本校验；
     * 服务端 412 表示文档已被别处修改，调用方应重新拉取合并后再写。
     */
    suspend fun savePreferences(doc: JsonObject, version: Long? = null): CloudPreferences {
        val body = KungalJson.encodeToString(PreferencesBody.serializer(), PreferencesBody(doc))
        val headers = version?.let { mapOf("If-Match" to "\"$it\"") } ?: emptyMap()
        return client.putJson(
            path = "/me/preferences",
            body = body,
            headers = headers,
            deserializer = CloudPreferences.serializer(),
        )
    }
}
