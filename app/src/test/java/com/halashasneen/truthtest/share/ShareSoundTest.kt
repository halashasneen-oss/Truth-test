package com.halashasneen.truthtest.share

import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareSoundTest {
    @Test
    fun unknownStorageFallsBackToNeonBeat() {
        assertEquals(ShareSound.NEON_BEAT, ShareSound.fromStorage("missing"))
    }

    @Test
    fun offModeIsSilent() {
        val values = (0L until 44_100L step 441L).map {
            ProceduralSoundSynth.sample(ShareSound.OFF, it)
        }
        assertTrue(values.all { it == 0f })
    }

    @Test
    fun generatedSoundIsBoundedAndAudible() {
        val values = (0L until 44_100L step 147L).map {
            ProceduralSoundSynth.sample(ShareSound.NEON_BEAT, it)
        }
        assertTrue(values.any { abs(it) > 0.001f })
        assertTrue(values.all { abs(it) <= 0.82f })
    }
}
