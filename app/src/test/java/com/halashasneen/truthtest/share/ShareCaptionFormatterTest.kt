package com.halashasneen.truthtest.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareCaptionFormatterTest {
    @Test fun copyToneChangesWithTheResult() {
        assertEquals(ShareCaptionFormatter.Tone.LOW, ShareCaptionFormatter.tone(30))
        assertEquals(ShareCaptionFormatter.Tone.MEDIUM, ShareCaptionFormatter.tone(50))
        assertEquals(ShareCaptionFormatter.Tone.HIGH, ShareCaptionFormatter.tone(80))
    }
    @Test fun storeLinkUsesThePublishedApplicationId() {
        assertTrue(ShareCaptionFormatter.PLAY_URL.endsWith("id=com.halahasneen.truthtest"))
    }
}
