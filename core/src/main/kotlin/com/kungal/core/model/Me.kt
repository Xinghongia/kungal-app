package com.kungal.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** 当前登录用户的概要（GET /me）。 */
@Serializable
data class Me(
    @SerialName("object") val obj: String = "me",
    val id: String = "",
    val moemoepoint: Int = 0,
    @SerialName("has_checked_in_today") val hasCheckedInToday: Boolean = false,
    @SerialName("has_unread_messages") val hasUnreadMessages: Boolean = false,
    @SerialName("is_creator") val isCreator: Boolean = false,
    @SerialName("toolset_upload_today_bytes") val toolsetUploadTodayBytes: Long = 0,
)

/** 当前登录账号（GET /me/account）：身份与内容分级姿态。 */
@Serializable
data class Account(
    @SerialName("object") val obj: String = "account",
    val id: String = "",
    val name: String? = null,
    val avatar: ImageInfo? = null,
    val roles: List<String> = emptyList(),
    @SerialName("content_stance") val contentStance: ContentStance? = null,
)

@Serializable
data class ContentStance(
    @SerialName("is_adult_confirmed") val isAdultConfirmed: Boolean = false,
    @SerialName("nsfw_display") val nsfwDisplay: String? = null,
)
