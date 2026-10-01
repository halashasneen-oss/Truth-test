package com.halashasneen.truthtest.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppStorageContractTest {
    @Test
    fun publishedPreferenceFilesKeepTheirOriginalNames() {
        assertEquals("truth_test_settings", AppStorageContract.PREFS_SETTINGS)
        assertEquals("truth_test_questions", AppStorageContract.PREFS_QUESTIONS)
        assertEquals("truth_test_history", AppStorageContract.PREFS_HISTORY)
        assertEquals("truth_test_achievements", AppStorageContract.PREFS_ACHIEVEMENTS)
        assertEquals("truth_test_share", AppStorageContract.PREFS_SHARE)
    }

    @Test
    fun preferenceFilesRemainUnique() {
        val names = listOf(
            AppStorageContract.PREFS_SETTINGS,
            AppStorageContract.PREFS_QUESTIONS,
            AppStorageContract.PREFS_HISTORY,
            AppStorageContract.PREFS_ACHIEVEMENTS,
            AppStorageContract.PREFS_SHARE
        )
        assertEquals(names.size, names.toSet().size)
        assertTrue(AppStorageContract.CURRENT_DATA_SCHEMA_VERSION >= 1)
    }
}
