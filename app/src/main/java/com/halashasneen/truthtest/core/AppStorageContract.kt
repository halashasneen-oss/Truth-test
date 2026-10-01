package com.halashasneen.truthtest.core

/**
 * Stable storage names used by the already-published app.
 *
 * Existing values are part of the Play Store upgrade contract and must never be renamed.
 * Phase 3 only adds new local stores; it does not rewrite legacy user data.
 */
object AppStorageContract {
    const val PREFS_SETTINGS = "truth_test_settings"
    const val PREFS_QUESTIONS = "truth_test_questions"
    const val PREFS_HISTORY = "truth_test_history"
    const val PREFS_ACHIEVEMENTS = "truth_test_achievements"
    const val PREFS_SHARE = "truth_test_share"

    const val PREFS_PROFILES = "truth_test_profiles"
    const val PREFS_SOCIAL = "truth_test_social"

    const val KEY_LANGUAGE = "language"
    const val KEY_LIGHT_THEME = "light_theme"
    const val KEY_ONBOARDING_SEEN = "onboarding_seen"
    const val KEY_NOTIFICATION_ASKED = "notification_asked"

    const val KEY_DAILY_NOTIFICATIONS_ENABLED = "daily_notifications_enabled"
    const val KEY_DAILY_NOTIFICATION_HOUR = "daily_notification_hour"
    const val KEY_DAILY_NOTIFICATION_MINUTE = "daily_notification_minute"
    const val KEY_STREAK_REMINDER_ENABLED = "streak_reminder_enabled"

    const val KEY_HISTORY_RESULTS = "results"
    const val KEY_ACHIEVEMENTS_UNLOCKED = "unlocked"
    const val KEY_QUESTION_INTENSITY = "preferred_intensity"
    const val KEY_SHARE_THEME = "share_theme"
    const val KEY_SHARE_SOUND = "share_sound"
    const val KEY_SHARE_HISTORY = "share_history"
    const val KEY_SHARE_TEMPLATE = "share_template"
    const val KEY_SHARE_FORMAT = "share_format"

    const val KEY_PLAYER_PROFILES = "player_profiles"
    const val KEY_ACTIVE_PROFILE_ID = "active_profile_id"
    const val KEY_ACTIVE_SOCIAL_SESSION = "active_social_session"
    const val KEY_COMPLETED_SOCIAL_SESSIONS = "completed_social_sessions"

    const val KEY_DATA_SCHEMA_VERSION = "data_schema_version"
    const val CURRENT_DATA_SCHEMA_VERSION = 3
}
