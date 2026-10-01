package com.halashasneen.truthtest.share

import com.google.gson.Gson
import com.halashasneen.truthtest.data.model.ChallengeType
import com.halashasneen.truthtest.data.model.QuestionPack
import com.halashasneen.truthtest.data.model.SocialMode
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Base64

data class ChallengePayload(
    val version: Int = 1,
    val mode: String,
    val pack: String,
    val challengeType: String,
    val questionId: String? = null,
    val questionText: String? = null,
    val questionIntensity: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

object ChallengePayloadCodec {
    const val SCHEME = "truthtest"
    const val HOST = "challenge"
    private const val PREFIX = "truthtest://challenge?data="
    private const val MAX_TOKEN_LENGTH = 2_048
    private const val MAX_QUESTION_LENGTH = 220
    private const val CHECKSUM_CHARS = 16
    private val gson = Gson()

    fun encode(payload: ChallengePayload): String {
        require(validateFields(payload)) { "Invalid challenge payload" }
        val json = gson.toJson(payload)
        val encoded = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(json.toByteArray(StandardCharsets.UTF_8))
        val token = encoded + "." + checksum(encoded)
        require(token.length <= MAX_TOKEN_LENGTH) { "Challenge payload is too large" }
        return PREFIX + token
    }

    fun decode(raw: String?): ChallengePayload? {
        if (raw.isNullOrBlank() || raw.length > MAX_TOKEN_LENGTH + PREFIX.length + 32) return null
        if (!raw.startsWith(PREFIX, ignoreCase = false)) return null
        val token = raw.removePrefix(PREFIX)
        if (token.length > MAX_TOKEN_LENGTH || token.contains('&') || token.contains('#')) return null
        val split = token.lastIndexOf('.')
        if (split <= 0 || split >= token.lastIndex) return null
        val encoded = token.substring(0, split)
        val suppliedChecksum = token.substring(split + 1)
        val expectedChecksum = checksum(encoded)
        if (!MessageDigest.isEqual(
                expectedChecksum.toByteArray(StandardCharsets.US_ASCII),
                suppliedChecksum.toByteArray(StandardCharsets.US_ASCII)
            )
        ) return null

        val json = runCatching {
            String(
                Base64.getUrlDecoder().decode(encoded),
                StandardCharsets.UTF_8
            )
        }.getOrNull() ?: return null

        val payload = runCatching {
            gson.fromJson(json, ChallengePayload::class.java)
        }.getOrNull() ?: return null
        return payload.takeIf(::validateFields)
    }

    private fun validateFields(payload: ChallengePayload): Boolean {
        if (payload.version != 1) return false
        if (SocialMode.entries.none { it.storageKey == payload.mode }) return false
        if (QuestionPack.entries.none { it.storageKey == payload.pack }) return false
        if (ChallengeType.entries.none { it.storageKey == payload.challengeType }) return false
        if ((payload.questionText?.length ?: 0) > MAX_QUESTION_LENGTH) return false
        if ((payload.questionId?.length ?: 0) > 96) return false
        if ((payload.questionIntensity?.length ?: 0) > 16) return false
        return true
    }

    private fun checksum(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }.take(CHECKSUM_CHARS)
    }
}
