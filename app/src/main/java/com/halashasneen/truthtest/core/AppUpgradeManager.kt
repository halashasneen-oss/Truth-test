package com.halashasneen.truthtest.core

import android.content.Context

/**
 * Central place for non-destructive migrations between Play Store releases.
 *
 * Phase 3 advances the local schema marker to version 2 only. The new profile and
 * social-session stores are independent, so legacy settings, history, achievements,
 * questions and share preferences do not need to be rewritten.
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
