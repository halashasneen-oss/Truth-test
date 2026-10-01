package com.halashasneen.truthtest.notifications

import android.content.Context
import com.halashasneen.truthtest.core.AppStorageContract

class NotificationSettings(context: Context) {
    private val prefs = context.getSharedPreferences(AppStorageContract.PREFS_SETTINGS, Context.MODE_PRIVATE)

    val dailyEnabled: Boolean
        get() = prefs.getBoolean(AppStorageContract.KEY_DAILY_NOTIFICATIONS_ENABLED, true)

    val dailyHour: Int
        get() = prefs.getInt(AppStorageContract.KEY_DAILY_NOTIFICATION_HOUR, 19).coerceIn(0, 23)

    val dailyMinute: Int
        get() = prefs.getInt(AppStorageContract.KEY_DAILY_NOTIFICATION_MINUTE, 0).coerceIn(0, 59)

    val streakReminderEnabled: Boolean
        get() = prefs.getBoolean(AppStorageContract.KEY_STREAK_REMINDER_ENABLED, true)

    fun setDailyEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(AppStorageContract.KEY_DAILY_NOTIFICATIONS_ENABLED, enabled).apply()
    }

    fun setDailyTime(hour: Int, minute: Int) {
        prefs.edit()
            .putInt(AppStorageContract.KEY_DAILY_NOTIFICATION_HOUR, hour.coerceIn(0, 23))
            .putInt(AppStorageContract.KEY_DAILY_NOTIFICATION_MINUTE, minute.coerceIn(0, 59))
            .apply()
    }

    fun setStreakReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(AppStorageContract.KEY_STREAK_REMINDER_ENABLED, enabled).apply()
    }
}
