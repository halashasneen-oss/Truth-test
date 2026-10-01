package com.halashasneen.truthtest

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.core.AppUpgradeManager
import com.halashasneen.truthtest.notifications.DailyChallengeScheduler

class TruthTestApp : Application() {
    override fun onCreate() {
        super.onCreate()
        AppUpgradeManager.migrate(this)

        val prefs = getSharedPreferences(AppStorageContract.PREFS_SETTINGS, MODE_PRIVATE)
        val language = prefs.getString(AppStorageContract.KEY_LANGUAGE, null)
        if (!language.isNullOrBlank()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(language))
        }
        AppCompatDelegate.setDefaultNightMode(
            if (prefs.getBoolean(AppStorageContract.KEY_LIGHT_THEME, false)) {
                AppCompatDelegate.MODE_NIGHT_NO
            } else {
                AppCompatDelegate.MODE_NIGHT_YES
            }
        )
        DailyChallengeScheduler.schedule(this)
    }
}
