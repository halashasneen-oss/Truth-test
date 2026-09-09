package com.nuvexa.truthtest.notifications

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
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.data.QuestionRepository
import com.nuvexa.truthtest.ui.MainActivity

class DailyChallengeWorker(
    appContext: Context,
    params: WorkerParameters
) : Worker(appContext, params) {

    override fun doWork(): Result {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(
                applicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) return Result.success()

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL, applicationContext.getString(R.string.daily_challenge), NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val question = QuestionRepository(applicationContext).dailyQuestion()?.text
            ?: applicationContext.getString(R.string.daily_notification_fallback)
        val intent = Intent(applicationContext, MainActivity::class.java)
        val pending = PendingIntent.getActivity(
            applicationContext, 10, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentTitle(applicationContext.getString(R.string.daily_challenge))
            .setContentText(question)
            .setStyle(NotificationCompat.BigTextStyle().bigText(question))
            .setContentIntent(pending)
            .setAutoCancel(true)
            .build()
        manager.notify(1001, notification)
        return Result.success()
    }

    companion object { private const val CHANNEL = "daily_challenge" }
}
