package com.halashasneen.truthtest.ui.test

import com.halashasneen.truthtest.audio.VoiceAnalysis
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TruthPresentationTest {
    @Test
    fun breakdownAlwaysStaysInsideDisplayRange() {
        val breakdown = TruthPresentation.breakdown(
            VoiceAnalysis(
                score = 72,
                averagePitchHz = 180.0,
                jitter = 0.04,
                shimmer = 0.18,
                pauseRatio = 0.22,
                rms = 0.04,
                usable = true
            )
        )
        val values = listOf(
            breakdown.stability,
            breakdown.confidencePattern,
            breakdown.hesitationControl,
            breakdown.energy,
            breakdown.responseFlow
        )
        assertTrue(values.all { it in 0..100 })
    }

    @Test
    fun resultBadgeUsesStableThresholds() {
        assertEquals(ResultBadge.TRUTH_MASTER, TruthPresentation.badge(95))
        assertEquals(ResultBadge.UNSHAKABLE, TruthPresentation.badge(84))
        assertEquals(ResultBadge.SMOOTH_TALKER, TruthPresentation.badge(70))
        assertEquals(ResultBadge.POKER_FACE, TruthPresentation.badge(50))
        assertEquals(ResultBadge.TRUTH_ROOKIE, TruthPresentation.badge(30))
    }
}
