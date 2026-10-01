package com.halashasneen.truthtest.core

import android.content.Context

/**
 * Central place for non-destructive migrations between Play Store releases.
 *
 * Phase 5 advances the local schema marker to version 4 only. Monetization preferences
 * live in a new independent store. Legacy settings, history, achievements, profiles,
 * sessions and sharing data are not rewritten or deleted.
 */
object AppUpgradeManager {
    fun migrate(context: Context) {
        val prefs = context.getSharedPreferences(
            AppStorageContract.PREFS_SETTINGS,
            Context.MODE_PRIVATE
        )
        val storedVersion = prefs.getInt(AppStorageContract.KEY_DATA_SCHEMA_VERSION, 0)

        if (storedVersion < AppStorageContract.CURRENT_DATA_SCHEMA_VERSION) {
            prefs.edit()
                .putInt(
                    AppStorageContract.KEY_DATA_SCHEMA_VERSION,
                    AppStorageContract.CURRENT_DATA_SCHEMA_VERSION
                )
                .apply()
        }
    }
}
