package com.halashasneen.truthtest.share

import com.halashasneen.truthtest.data.model.ChallengeType
import com.halashasneen.truthtest.data.model.QuestionPack
import com.halashasneen.truthtest.data.model.SocialMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChallengePayloadCodecTest {
    @Test
    fun roundTripPreservesVerifiedChallenge() {
        val payload = ChallengePayload(
            mode = SocialMode.CHALLENGE.storageKey,
            pack = QuestionPack.FUNNY.storageKey,
            challengeType = ChallengeType.BEST_OF_THREE.storageKey,
            questionId = "funny-04",
            questionText = "What is your funniest excuse?",
            questionIntensity = "medium",
            createdAt = 1234L
        )
        val encoded = ChallengePayloadCodec.encode(payload)
        assertTrue(encoded.startsWith("truthtest://challenge?data="))
        assertEquals(payload, ChallengePayloadCodec.decode(encoded))
    }

    @Test
    fun rejectsTamperedForeignAndOversizedInputs() {
        val valid = ChallengePayloadCodec.encode(
            ChallengePayload(
                mode = SocialMode.PARTY.storageKey,
                pack = QuestionPack.CLASSIC.storageKey,
                challengeType = ChallengeType.HIGHEST_SCORE.storageKey
            )
        )
        assertNull(ChallengePayloadCodec.decode(valid.dropLast(1) + "x"))
        assertNull(ChallengePayloadCodec.decode("https://example.com/?data=abc"))
        assertNull(ChallengePayloadCodec.decode("truthtest://other?data=abc"))
        assertNull(ChallengePayloadCodec.decode("x".repeat(3_000)))
    }

    @Test(expected = IllegalArgumentException::class)
    fun refusesOversizedQuestionBeforeEncoding() {
        ChallengePayloadCodec.encode(
            ChallengePayload(
                mode = SocialMode.CHALLENGE.storageKey,
                pack = QuestionPack.BOLD.storageKey,
                challengeType = ChallengeType.QUICK_ROUND.storageKey,
                questionText = "q".repeat(221)
            )
        )
    }
}
