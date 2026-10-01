package com.halashasneen.truthtest.monetization

import android.app.Activity
import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.halashasneen.truthtest.BuildConfig
import java.util.concurrent.atomic.AtomicBoolean

object AdsManager {
    private val initialized = AtomicBoolean(false)
    private var interstitialAd: InterstitialAd? = null
    private var rewardedAd: RewardedAd? = null
    private var interstitialLoading = false
    private var rewardedLoading = false

    fun initialize(context: Context) {
        if (!ConsentManager.canRequestAds()) return
        if (initialized.compareAndSet(false, true)) {
            MobileAds.initialize(context.applicationContext)
        }
        loadInterstitial(context)
        loadRewarded(context)
    }

    fun attachBanner(activity: Activity, container: FrameLayout): AdView? {
        val prefs = MonetizationPreferences(activity)
        if (!ConsentManager.canRequestAds() || prefs.adsSuppressed()) {
            container.removeAllViews()
            container.visibility = View.GONE
            return null
        }
        val widthPx = container.width.takeIf { it > 0 }
            ?: activity.resources.displayMetrics.widthPixels
        val widthDp = (widthPx / activity.resources.displayMetrics.density).toInt().coerceAtLeast(320)
        val adView = AdView(activity).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize(activity, widthDp))
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    container.visibility = View.VISIBLE
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    container.visibility = View.GONE
                }
            }
        }
        container.removeAllViews()
        container.addView(
            adView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        container.visibility = View.GONE
        adView.loadAd(AdRequest.Builder().build())
        return adView
    }

    fun maybeShowInterstitial(activity: Activity, onComplete: () -> Unit) {
        val prefs = MonetizationPreferences(activity)
        val count = prefs.recordNaturalBreak()
        val now = System.currentTimeMillis()
        val eligible = MonetizationPolicy.interstitialEligible(
            adFreeUntil = prefs.adFreeUntil,
            eventCount = count,
            lastShownAt = prefs.lastInterstitialAt,
            now = now
        )
        val ad = interstitialAd
        if (!eligible || ad == null || !ConsentManager.canRequestAds()) {
            loadInterstitial(activity)
            onComplete()
            return
        }

        interstitialAd = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                prefs.markInterstitialShown()
                loadInterstitial(activity)
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                loadInterstitial(activity)
                onComplete()
            }
        }
        ad.show(activity)
    }

    fun showRewarded(
        activity: Activity,
        onReward: (Long) -> Unit,
        onUnavailable: () -> Unit
    ) {
        val ad = rewardedAd
        if (ad == null || !ConsentManager.canRequestAds()) {
            loadRewarded(activity)
            onUnavailable()
            return
        }
        rewardedAd = null
        var granted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                loadRewarded(activity)
            }

            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                loadRewarded(activity)
                if (!granted) onUnavailable()
            }
        }
        ad.show(activity) {
            if (!granted) {
                granted = true
                val until = MonetizationPreferences(activity).grantRewardedAdFree()
                onReward(until)
            }
        }
    }

    private fun loadInterstitial(context: Context) {
        if (!initialized.get() || interstitialLoading) return
        if (MonetizationPreferences(context).adsSuppressed()) return
        interstitialLoading = true
        InterstitialAd.load(
            context,
            BuildConfig.ADMOB_INTERSTITIAL_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitialAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitialAd = null
                }
            }
        )
    }

    private fun loadRewarded(context: Context) {
        if (!initialized.get() || rewardedLoading) return
        rewardedLoading = true
        RewardedAd.load(
            context,
            BuildConfig.ADMOB_REWARDED_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewardedAd = ad
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    rewardedAd = null
                }
            }
        )
    }
}
