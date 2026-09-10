package com.halashasneen.truthtest.share

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareThemeTest {
    @Test
    fun unknownStoredThemeFallsBackToNeonPurple() {
        assertEquals(ShareTheme.NEON_PURPLE, ShareTheme.fromStorage("missing-theme"))
        assertEquals(ShareTheme.NEON_PURPLE, ShareTheme.fromStorage(null))
    }

    @Test
    fun themeStorageKeysAndPalettesAreDistinct() {
        val themes = ShareTheme.entries
        assertEquals(themes.size, themes.map { it.storageKey }.distinct().size)
        assertEquals(themes.size, themes.map { it.palette }.distinct().size)
        assertTrue(themes.all { it.storageKey.isNotBlank() })
        assertNotEquals(ShareTheme.NEON_PURPLE.palette, ShareTheme.GOLD_CHALLENGE.palette)
    }
}
