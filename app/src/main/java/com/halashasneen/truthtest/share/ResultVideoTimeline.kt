package com.halashasneen.truthtest.share

import kotlin.math.pow

object ResultVideoTimeline {
    const val DURATION_MS = 7_000L
    const val FPS = 15

    val frameCount: Int
        get() = (DURATION_MS * FPS / 1_000L).toInt()

    fun progress(frameIndex: Int): Float {
        if (frameCount <= 1) return 1f
        return (frameIndex.toFloat() / (frameCount - 1).toFloat()).coerceIn(0f, 1f)
    }

    fun questionAlpha(progress: Float): Float = window(progress, 0.02f, 0.18f)

    fun waveformReveal(progress: Float): Float = window(progress, 0.12f, 0.48f)

    fun scoreProgress(progress: Float): Float {
        val linear = window(progress, 0.28f, 0.70f)
        return 1f - (1f - linear).pow(3)
    }

    fun resultAlpha(progress: Float): Float = window(progress, 0.66f, 0.82f)

    /** Each group result row enters slightly after the previous one. */
    fun groupRowAlpha(progress: Float, rowIndex: Int): Float {
        val index = rowIndex.coerceIn(0, 3)
        val start = 0.60f + index * 0.045f
        return window(progress, start, start + 0.12f)
    }

    /** Winner/tie headline lands after the ranking rows are already readable. */
    fun groupWinnerAlpha(progress: Float): Float = window(progress, 0.76f, 0.91f)

    /** Small deterministic pop for the winner headline without changing video duration. */
    fun groupWinnerScale(progress: Float): Float {
        val reveal = groupWinnerAlpha(progress)
        val eased = 1f - (1f - reveal).pow(3)
        return 0.88f + eased * 0.12f
    }

    private fun window(value: Float, start: Float, end: Float): Float {
        if (end <= start) return if (value >= end) 1f else 0f
        return ((value - start) / (end - start)).coerceIn(0f, 1f)
    }
}
