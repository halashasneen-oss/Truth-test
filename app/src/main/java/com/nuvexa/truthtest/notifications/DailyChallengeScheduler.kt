package com.nuvexa.truthtest.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object DailyChallengeScheduler {
    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<DailyChallengeWorker>(24, TimeUnit.HOURS).build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "truth-test-daily-challenge",
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
