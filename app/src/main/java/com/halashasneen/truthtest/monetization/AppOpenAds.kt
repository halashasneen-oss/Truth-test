package com.halashasneen.truthtest.monetization

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import com.halashasneen.truthtest.BuildConfig

/** Only displayed on the splash screen; never interrupted active app content. */
object AppOpenAds {
    private const val TAG = "TruthTestAppOpen"
    private const val MAX_AGE_MS = 4L * 60L * 60L * 1_000L
    private const val FAILED_RETRY_MS = 2L * 60L * 1_000L
    private var cached: AppOpenAd? = null
    private var loadedAt = 0L
    private var retryAt = 0L
    private var loading = false
    var isShowing = false
        private set
    private val pending = mutableListOf<() -> Unit>()

    fun clear() {
        cached = null
        pending.clear()
    }

    fun preload(context: Context, onComplete: (() -> Unit)? = null) {
        if (!AdsManager.isReadyForAds() || MonetizationPreferences(context).adsSuppressed()) {
            onComplete?.invoke()
            return
        }
        if (cached != null && SystemClock.elapsedRealtime() - loadedAt < MAX_AGE_MS) {
            onComplete?.invoke()
            return
        }
        if (loading) {
            if (onComplete != null) pending.add(onComplete)
            return
        }
        if (SystemClock.elapsedRealtime() < retryAt) {
            onComplete?.invoke()
            return
        }
        if (onComplete != null) pending.add(onComplete)
        cached = null
        loading = true
        AppOpenAd.load(
            context.applicationContext,
            BuildConfig.ADMOB_APP_OPEN_ID,
            AdRequest.Builder().build(),
            object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(ad: AppOpenAd) {
                    loading = false
                    cached = if (AdsManager.isReadyForAds()) ad else null
                    loadedAt = SystemClock.elapsedRealtime()
                    retryAt = 0L
                    Log.i(TAG, "App-open ad loaded")
                    finishPending()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    loading = false
                    cached = null
                    retryAt = SystemClock.elapsedRealtime() + FAILED_RETRY_MS
                    Log.w(TAG, "App-open load failed: code=" + error.code + " domain=" + error.domain)
                    finishPending()
                }
            }
        )
    }

    private fun finishPending() {
        val callbacks = pending.toList()
        pending.clear()
        callbacks.forEach { it() }
    }

    /** Returns true only if a preloaded ad is now shown over SplashActivity. */
    fun showIfAvailable(activity: Activity, onFinished: () -> Unit): Boolean {
        val prefs = MonetizationPreferences(activity)
        val now = System.currentTimeMillis()
        if (isShowing || AdsManager.isFullScreenShowing() || !AdsManager.isReadyForAds() ||
            activity.isFinishing || activity.isDestroyed ||
            cached == null || SystemClock.elapsedRealtime() - loadedAt >= MAX_AGE_MS ||
            !MonetizationPolicy.appOpenEligible(
                prefs.adFreeUntil, prefs.lastAppOpenAt, prefs.lastFullscreenAt, now
            )
        ) return false
        val ad = cached ?: return false
        cached = null
        isShowing = true
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                prefs.markAppOpenShown()
            }

            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                preload(activity.applicationContext)
                onFinished()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "App-open display failed: code=" + error.code)
                isShowing = false
                preload(activity.applicationContext)
                onFinished()
            }
        }
        ad.show(activity)
        return true
    }
}
