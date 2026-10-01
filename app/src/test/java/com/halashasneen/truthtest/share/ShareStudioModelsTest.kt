package com.halashasneen.truthtest.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareStudioModelsTest {
    @Test
    fun formatsMatchExpectedSocialRatios() {
        assertEquals(1080, ShareFormat.SQUARE.width)
        assertEquals(1080, ShareFormat.SQUARE.height)
        assertEquals(1350, ShareFormat.FEED.height)
        assertEquals(1920, ShareFormat.STORY.height)
        assertTrue(ShareFormat.entries.all { it.width == 1080 })
    }

    @Test
    fun sixDistinctTemplatesAreAvailable() {
        assertEquals(6, ShareTemplate.entries.size)
        assertEquals(6, ShareTemplate.entries.map { it.storageKey }.toSet().size)
    }
}
