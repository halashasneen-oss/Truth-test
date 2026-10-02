package com.halashasneen.truthtest.monetization

import android.content.Context
import com.halashasneen.truthtest.core.AppStorageContract

class MonetizationPreferences(context: Context) {
    private val prefs = context.getSharedPreferences(
        AppStorageContract.PREFS_MONETIZATION,
        Context.MODE_PRIVATE
    )

    val adFreeUntil: Long
        get() = prefs.getLong(AppStorageContract.KEY_AD_FREE_UNTIL, 0L)

    val interstitialEventCount: Int
        get() = prefs.getInt(AppStorageContract.KEY_INTERSTITIAL_EVENT_COUNT, 0)

    val lastInterstitialAt: Long
        get() = prefs.getLong(AppStorageContract.KEY_LAST_INTERSTITIAL_AT, 0L)

    val lastFullscreenAt: Long
        get() = prefs.getLong(AppStorageContract.KEY_LAST_FULLSCREEN_AD_AT, 0L)

    val lastAppOpenAt: Long
        get() = prefs.getLong(AppStorageContract.KEY_LAST_APP_OPEN_AT, 0L)

    fun adsSuppressed(now: Long = System.currentTimeMillis()): Boolean =
        MonetizationPolicy.adsSuppressed(adFreeUntil, now)

    fun grantRewardedAdFree(
        now: Long = System.currentTimeMillis()
    ): Long {
        val base = maxOf(now, adFreeUntil)
        val until = base + MonetizationPolicy.REWARDED_AD_FREE_MS
        prefs.edit()
            .putLong(AppStorageContract.KEY_AD_FREE_UNTIL, until)
            .apply()
        return until
    }

    fun recordNaturalBreak(): Int {
        val next = interstitialEventCount + 1
        prefs.edit()
            .putInt(AppStorageContract.KEY_INTERSTITIAL_EVENT_COUNT, next)
            .apply()
        return next
    }

    fun markInterstitialShown(now: Long = System.currentTimeMillis()) {
        prefs.edit()
            .putInt(AppStorageContract.KEY_INTERSTITIAL_EVENT_COUNT, 0)
            .putLong(AppStorageContract.KEY_LAST_INTERSTITIAL_AT, now)
            .putLong(AppStorageContract.KEY_LAST_FULLSCREEN_AD_AT, now)
            .apply()
    }

    fun markRewardedShown(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(AppStorageContract.KEY_LAST_FULLSCREEN_AD_AT, now).apply()
    }

    fun markAppOpenShown(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(AppStorageContract.KEY_LAST_APP_OPEN_AT, now)
            .putLong(AppStorageContract.KEY_LAST_FULLSCREEN_AD_AT, now)
            .apply()
    }

    fun remainingAdFreeMs(now: Long = System.currentTimeMillis()): Long =
        MonetizationPolicy.remainingAdFreeMs(adFreeUntil, now)
}
