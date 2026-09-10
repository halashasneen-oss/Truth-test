package com.halashasneen.truthtest

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.halashasneen.truthtest.notifications.DailyChallengeScheduler

class TruthTestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val prefs = getSharedPreferences("truth_test_settings", MODE_PRIVATE)
        val language = prefs.getString("language", null)
        if (!language.isNullOrBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        }
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.getBoolean("light_theme", false)) AppCompatDelegate.MODE_NIGHT_NO
            else AppCompatDelegate.MODE_NIGHT_YES
        )
        DailyChallengeScheduler.schedule(this)
    }
}
