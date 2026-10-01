package com.halashasneen.truthtest.core

import android.content.Context

/**
 * Central place for non-destructive migrations between Play Store releases.
 *
 * Phase 4 advances the local schema marker to version 3 only. Share Studio metadata
 * is additive inside the existing share preferences, while profiles/social sessions
 * remain independent. Legacy settings, history, achievements and questions are not rewritten.
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
