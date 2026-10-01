package com.halashasneen.truthtest.ui.test

import com.halashasneen.truthtest.audio.VoiceAnalysis
import kotlin.math.roundToInt

data class TruthBreakdown(
    val stability: Int,
    val confidencePattern: Int,
    val hesitationControl: Int,
    val energy: Int,
    val responseFlow: Int
)

enum class ResultBadge {
    TRUTH_ROOKIE,
    SMOOTH_TALKER,
    POKER_FACE,
    UNSHAKABLE,
    TRUTH_MASTER
}

object TruthPresentation {
    fun breakdown(analysis: VoiceAnalysis): TruthBreakdown {
        val stability = (100.0 - (analysis.jitter / 0.12 * 100.0)).roundToInt().coerceIn(0, 100)
        val confidence = (100.0 - (analysis.shimmer / 0.55 * 100.0)).roundToInt().coerceIn(0, 100)
        val hesitation = ((1.0 - analysis.pauseRatio) * 100.0).roundToInt().coerceIn(0, 100)
        val energy = (analysis.rms / 0.06 * 100.0).roundToInt().coerceIn(0, 100)
        val flow = ((stability + confidence + hesitation + energy) / 4.0).roundToInt().coerceIn(0, 100)
        return TruthBreakdown(stability, confidence, hesitation, energy, flow)
    }

    fun badge(score: Int): ResultBadge = when {
        score >= 90 -> ResultBadge.TRUTH_MASTER
        score >= 80 -> ResultBadge.UNSHAKABLE
        score >= 65 -> ResultBadge.SMOOTH_TALKER
        score >= 45 -> ResultBadge.POKER_FACE
        else -> ResultBadge.TRUTH_ROOKIE
    }
}
