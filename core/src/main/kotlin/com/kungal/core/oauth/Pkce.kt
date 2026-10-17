package com.kungal.core.oauth

import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64

/** 一次 OAuth 授权所需的 PKCE 材料。 */
data class PkceCodes(
    val verifier: String,
    val challenge: String,
    val state: String,
)

/**
 * OAuth PKCE（RFC 7636）：verifier 为 43–128 位 URL 安全随机串，
 * challenge = BASE64URL(SHA256(verifier))，state 用于回调校验。
 */
object Pkce {

    fun generate(random: SecureRandom = SecureRandom()): PkceCodes {
        val verifier = randomToken(random, 64)
        return PkceCodes(
            verifier = verifier,
            challenge = challengeFor(verifier),
            state = randomToken(random, 24),
        )
    }

    fun challengeFor(verifier: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(verifier.toByteArray(Charsets.US_ASCII))
        return Base64.getUrlEncoder().withoutPadding().encodeToString(digest)
    }

    private fun randomToken(random: SecureRandom, bytes: Int): String {
        val buffer = ByteArray(bytes)
        random.nextBytes(buffer)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(buffer)
    }
}
