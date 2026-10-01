package com.halashasneen.truthtest.core

import android.content.Context

/**
 * Central place for non-destructive migrations between Play Store releases.
 *
 * Version 1 intentionally performs no data rewrite: it establishes a schema marker
 * around the storage format already used by the published app.
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
