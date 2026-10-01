package com.halashasneen.truthtest.core

import android.content.Context

/**
 * Central place for non-destructive migrations between Play Store releases.
 *
 * Phase 6 keeps schema version 4. The release candidate adds no destructive migration.
 * Legacy settings, history, achievements, profiles, sessions, sharing and monetization
 * timestamps remain untouched across the final 2.0 upgrade.
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
