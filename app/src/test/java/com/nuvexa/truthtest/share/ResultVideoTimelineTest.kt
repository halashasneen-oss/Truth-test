package com.nuvexa.truthtest.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ResultVideoTimelineTest {
    @Test
    fun sevenSecondTimelineUsesExpectedFrameCount() {
        assertEquals(105, ResultVideoTimeline.frameCount)
        assertEquals(0f, ResultVideoTimeline.progress(0), 0.0001f)
        assertEquals(1f, ResultVideoTimeline.progress(ResultVideoTimeline.frameCount - 1), 0.0001f)
    }

    @Test
    fun scoreAnimationIsMonotonicAndEndsAtOne() {
        var previous = 0f
        for (frame in 0 until ResultVideoTimeline.frameCount) {
            val value = ResultVideoTimeline.scoreProgress(ResultVideoTimeline.progress(frame))
            assertTrue(value + 0.0001f >= previous)
            previous = value
        }
        assertEquals(1f, previous, 0.0001f)
    }

    @Test
    fun waveformRevealCompletesBeforeResultOutro() {
        assertTrue(ResultVideoTimeline.waveformReveal(0.50f) >= 0.99f)
        assertTrue(ResultVideoTimeline.resultAlpha(0.60f) == 0f)
        assertTrue(ResultVideoTimeline.resultAlpha(0.90f) >= 0.99f)
    }
}
