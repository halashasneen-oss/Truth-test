package com.halashasneen.truthtest.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.ui.MainActivity

class StreakReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        if (!NotificationSettings(applicationContext).streakReminderEnabled) return Result.success()
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val history = HistoryRepository(applicationContext)
        if (history.hasResultToday()) return Result.success()
        val streak = history.currentStreak()
        if (streak <= 0) return Result.success()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL,
                    applicationContext.getString(R.string.reminders_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                )
            )
        }

        val pending = PendingIntent.getActivity(
            applicationContext,
            11,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(applicationContext.getString(R.string.streak_reminder_title))
            .setContentText(applicationContext.getString(R.string.streak_reminder_text, streak))
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    applicationContext.getString(R.string.streak_reminder_text, streak)
                )
            )
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(1002, notification)
        return Result.success()
    }

    companion object {
        private const val CHANNEL = "truth_test_reminders"
    }
}
