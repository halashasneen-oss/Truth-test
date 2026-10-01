package com.halashasneen.truthtest.core

import org.junit.Assert.assertEquals
import org.junit.Test

class AppStorageContractTest {
    @Test
    fun legacyPreferenceFilesRemainStableForExistingInstalls() {
        assertEquals("truth_test_settings", AppStorageContract.PREFS_SETTINGS)
        assertEquals("truth_test_questions", AppStorageContract.PREFS_QUESTIONS)
        assertEquals("truth_test_history", AppStorageContract.PREFS_HISTORY)
        assertEquals("truth_test_achievements", AppStorageContract.PREFS_ACHIEVEMENTS)
        assertEquals("truth_test_share", AppStorageContract.PREFS_SHARE)
    }

    @Test
    fun legacyDataKeysRemainStable() {
        assertEquals("results", AppStorageContract.KEY_HISTORY_RESULTS)
        assertEquals("unlocked", AppStorageContract.KEY_ACHIEVEMENTS_UNLOCKED)
        assertEquals("preferred_intensity", AppStorageContract.KEY_QUESTION_INTENSITY)
        assertEquals("share_theme", AppStorageContract.KEY_SHARE_THEME)
        assertEquals("share_sound", AppStorageContract.KEY_SHARE_SOUND)
    }
}
