package com.halashasneen.truthtest.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

object DailyChallengeScheduler {
    private const val DAILY_WORK = "truth-test-daily-challenge"
    private const val STREAK_WORK = "truth-test-streak-reminder"
    private const val STREAK_HOUR = 21
    private const val STREAK_MINUTE = 0

    fun schedule(context: Context) {
        val appContext = context.applicationContext
        val settings = NotificationSettings(appContext)
        val workManager = WorkManager.getInstance(appContext)

        if (settings.dailyEnabled) {
            val request = PeriodicWorkRequestBuilder<DailyChallengeWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(
                    nextDelayMillis(settings.dailyHour, settings.dailyMinute),
                    TimeUnit.MILLISECONDS
                )
                .build()
            workManager.enqueueUniquePeriodicWork(
                DAILY_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(DAILY_WORK)
        }

        if (settings.streakReminderEnabled) {
            val request = PeriodicWorkRequestBuilder<StreakReminderWorker>(24, TimeUnit.HOURS)
                .setInitialDelay(
                    nextDelayMillis(STREAK_HOUR, STREAK_MINUTE),
                    TimeUnit.MILLISECONDS
                )
                .build()
            workManager.enqueueUniquePeriodicWork(
                STREAK_WORK,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        } else {
            workManager.cancelUniqueWork(STREAK_WORK)
        }
    }

    fun nextDelayMillis(
        hour: Int,
        minute: Int,
        now: ZonedDateTime = ZonedDateTime.now()
    ): Long {
        var next = now
            .withHour(hour.coerceIn(0, 23))
            .withMinute(minute.coerceIn(0, 59))
            .withSecond(0)
            .withNano(0)
        if (!next.isAfter(now)) next = next.plusDays(1)
        return Duration.between(now, next).toMillis().coerceAtLeast(0L)
    }
}
