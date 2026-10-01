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

    @Test
    fun phaseThreeAddsIndependentLocalStoresWithoutRenamingLegacyData() {
        assertEquals("truth_test_profiles", AppStorageContract.PREFS_PROFILES)
        assertEquals("truth_test_social", AppStorageContract.PREFS_SOCIAL)
        assertEquals("player_profiles", AppStorageContract.KEY_PLAYER_PROFILES)
        assertEquals("active_social_session", AppStorageContract.KEY_ACTIVE_SOCIAL_SESSION)
        assertEquals("completed_social_sessions", AppStorageContract.KEY_COMPLETED_SOCIAL_SESSIONS)
        assertEquals("share_history", AppStorageContract.KEY_SHARE_HISTORY)
        assertEquals("share_template", AppStorageContract.KEY_SHARE_TEMPLATE)
        assertEquals("share_format", AppStorageContract.KEY_SHARE_FORMAT)
        assertEquals("truth_test_monetization", AppStorageContract.PREFS_MONETIZATION)
        assertEquals("ad_free_until", AppStorageContract.KEY_AD_FREE_UNTIL)
        assertEquals("interstitial_event_count", AppStorageContract.KEY_INTERSTITIAL_EVENT_COUNT)
        assertEquals("last_interstitial_at", AppStorageContract.KEY_LAST_INTERSTITIAL_AT)
        assertEquals(4, AppStorageContract.CURRENT_DATA_SCHEMA_VERSION)
    }
}
