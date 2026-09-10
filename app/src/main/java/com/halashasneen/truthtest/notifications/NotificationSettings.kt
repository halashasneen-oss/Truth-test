package com.halashasneen.truthtest.notifications

import android.content.Context

class NotificationSettings(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    val dailyEnabled: Boolean
        get() = prefs.getBoolean(KEY_DAILY_ENABLED, true)

    val dailyHour: Int
        get() = prefs.getInt(KEY_DAILY_HOUR, 19).coerceIn(0, 23)

    val dailyMinute: Int
        get() = prefs.getInt(KEY_DAILY_MINUTE, 0).coerceIn(0, 59)

    val streakReminderEnabled: Boolean
        get() = prefs.getBoolean(KEY_STREAK_ENABLED, true)

    fun setDailyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_DAILY_ENABLED, enabled).apply()
    }

    fun setDailyTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(KEY_DAILY_HOUR, hour.coerceIn(0, 23))
            .putInt(KEY_DAILY_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun setStreakReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_STREAK_ENABLED, enabled).apply()
    }

    companion object {
        private const val PREFS = "truth_test_settings"
        private const val KEY_DAILY_ENABLED = "daily_notifications_enabled"
        private const val KEY_DAILY_HOUR = "daily_notification_hour"
        private const val KEY_DAILY_MINUTE = "daily_notification_minute"
        private const val KEY_STREAK_ENABLED = "streak_reminder_enabled"
    }
}
