package com.halashasneen.truthtest.monetization

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.google.android.gms.ads.AdError
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
import com.halashasneen.truthtest.R
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Only loads ads after UMP permits requests AND MobileAds initialization completes.
 * Production no-fill is an AdMob inventory decision, not a UI or retry-policy defect.
 */
object AdsManager {
    enum class RewardedState {
        WAITING_FOR_CONSENT, INITIALIZING, LOADING, READY,
        NO_FILL, NETWORK_ERROR, CONFIGURATION_ERROR, ERROR, AD_FREE_ACTIVE
    }

    private const val TAG = "TruthTestAds"
    private const val MAX_AUTOMATIC_RETRIES = 4
    private val main = Handler(Looper.getMainLooper())
    private val initializing = AtomicBoolean(false)
    private var sdkReady = false
    private var appContext: Context? = null
    private val initCallbacks = mutableListOf<() -> Unit>()
    private val rewardedObservers = linkedSetOf<(RewardedState) -> Unit>()
    private var bannerDiagnostic = "not requested"
    private var interstitialDiagnostic = "not requested"
    private var rewardedDiagnostic = "not requested"
    private var initializationDiagnostic = "not initialized"
    private var initializedAtElapsed = 0L

    var rewardedState = RewardedState.WAITING_FOR_CONSENT
        private set

    private var interstitial: InterstitialAd? = null
    private var interstitialLoading = false
    private var interstitialFailures = 0
    private var nextInterstitialAttemptAt = 0L
    private var interstitialRetry: Runnable? = null

    private var rewarded: RewardedAd? = null
    private var rewardedLoading = false
    private var rewardedShowing = false
    private var rewardedFailures = 0
    private var nextRewardedAttemptAt = 0L
    private var rewardedRetry: Runnable? = null

    fun registerRewardedObserver(observer: (RewardedState) -> Unit) {
        rewardedObservers += observer
        observer(rewardedState)
    }

    fun unregisterRewardedObserver(observer: (RewardedState) -> Unit) {
        rewardedObservers -= observer
    }

    fun isReadyForAds(): Boolean = sdkReady && ConsentManager.canRequestAds()

    fun reportConsentUnavailable() {
        main.post {
            initializationDiagnostic = "UMP blocked ad requests"
            interstitial = null
            rewarded = null
            interstitialRetry?.let(main::removeCallbacks)
            rewardedRetry?.let(main::removeCallbacks)
            interstitialRetry = null
            rewardedRetry = null
            setRewardedState(RewardedState.WAITING_FOR_CONSENT)
            Log.i(TAG, "Ads paused: UMP does not currently permit ad requests")
        }
    }

    fun initialize(context: Context, onReady: (() -> Unit)? = null) {
        val app = context.applicationContext
        appContext = app
        if (!ConsentManager.canRequestAds()) {
            reportConsentUnavailable()
            onReady?.invoke()
            return
        }
        if (sdkReady) {
            loadInterstitial(app)
            loadRewarded(app)
            onReady?.invoke()
            return
        }
        if (onReady != null) initCallbacks += onReady
        if (!initializing.compareAndSet(false, true)) return
        setRewardedState(RewardedState.INITIALIZING)
        initializationDiagnostic = "initializing"
        initializedAtElapsed = SystemClock.elapsedRealtime()
        Log.i(TAG, "Initializing Google Mobile Ads after UMP approval")
        MobileAds.initialize(app) {
            main.post {
                sdkReady = true
                initializationDiagnostic = "ready, elapsedMs=" +
                    (SystemClock.elapsedRealtime() - initializedAtElapsed)
                Log.i(TAG, "Google Mobile Ads SDK initialization completed")
                loadInterstitial(app)
                loadRewarded(app)
                val callbacks = initCallbacks.toList()
                initCallbacks.clear()
                callbacks.forEach { callback -> callback() }
            }
        }
    }

    private fun setRewardedState(next: RewardedState) {
        if (Looper.myLooper() != Looper.getMainLooper()) {
            main.post { setRewardedState(next) }
            return
        }
        if (rewardedState == next) return
        rewardedState = next
        rewardedObservers.toList().forEach { it(next) }
    }

    fun rewardStatusText(state: RewardedState): Int = when (state) {
        RewardedState.WAITING_FOR_CONSENT -> R.string.hotfix_reward_waiting
        RewardedState.INITIALIZING -> R.string.hotfix_reward_initializing
        RewardedState.LOADING -> R.string.hotfix_reward_loading
        RewardedState.READY -> R.string.hotfix_reward_ready
        RewardedState.NO_FILL -> R.string.hotfix_reward_no_fill
        RewardedState.NETWORK_ERROR -> R.string.hotfix_reward_network
        RewardedState.CONFIGURATION_ERROR -> R.string.hotfix_reward_config
        RewardedState.ERROR -> R.string.hotfix_reward_error
        RewardedState.AD_FREE_ACTIVE -> R.string.p5_reward_granted
    }

    fun attachBanner(
        activity: Activity,
        container: FrameLayout,
        onLoaded: (() -> Unit)? = null,
        onLoadFailure: ((AdRetryPolicy.Failure) -> Unit)? = null
    ): AdView? {
        val prefs = MonetizationPreferences(activity)
        val host = container.parent as? View
        if (!isReadyForAds() || prefs.adsSuppressed()) {
            container.removeAllViews()
            container.visibility = View.GONE
            host?.visibility = View.GONE
            Log.d(TAG, "Banner skipped: SDK, consent or ad-free state")
            return null
        }
        val horizontalMargins =
            activity.resources.getDimensionPixelSize(R.dimen.tt_screen_horizontal) * 2
        val availablePx = container.width.takeIf { it > 0 }
            ?: (activity.resources.displayMetrics.widthPixels - horizontalMargins -
                container.paddingLeft - container.paddingRight)
        val widthDp =
            (availablePx.coerceAtLeast(1) / activity.resources.displayMetrics.density)
                .toInt().coerceAtLeast(1)
        val adView = AdView(activity).apply {
            adUnitId = BuildConfig.ADMOB_BANNER_ID
            setAdSize(AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp))
            adListener = object : AdListener() {
                override fun onAdLoaded() {
                    val allowed = isReadyForAds() &&
                        !MonetizationPreferences(activity).adsSuppressed()
                    host?.visibility = if (allowed) View.VISIBLE else View.GONE
                    container.visibility = if (allowed) View.VISIBLE else View.GONE
                    bannerDiagnostic = "loaded, visible=$allowed"
                    Log.i(TAG, "Banner onAdLoaded; visible=$allowed")
                    if (allowed) onLoaded?.invoke()
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    container.visibility = View.GONE
                    host?.visibility = View.GONE
                    val failure = AdRetryPolicy.classify(error.code)
                    bannerDiagnostic = "failed code=${error.code} domain=${error.domain} type=$failure"
                    Log.w(TAG, "Banner $bannerDiagnostic")
                    onLoadFailure?.invoke(failure)
                }
            }
        }
        container.removeAllViews()
        container.addView(
            adView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            )
        )
        container.visibility = View.INVISIBLE
        host?.visibility = View.INVISIBLE
        bannerDiagnostic = "requesting anchored banner, widthDp=$widthDp"
        Log.d(TAG, bannerDiagnostic)
        adView.loadAd(AdRequest.Builder().build())
        return adView
    }

    fun maybeShowInterstitial(activity: Activity, onComplete: () -> Unit) {
        val prefs = MonetizationPreferences(activity)
        if (!isReadyForAds() || prefs.adsSuppressed()) {
            onComplete()
            return
        }
        val count = prefs.recordNaturalBreak()
        val now = System.currentTimeMillis()
        val eligible = MonetizationPolicy.interstitialEligible(
            adFreeUntil = prefs.adFreeUntil,
            eventCount = count,
            lastShownAt = prefs.lastInterstitialAt,
            now = now
        )
        if (!eligible) {
            Log.d(TAG, "Interstitial frequency cap: completedBreaks=$count")
            onComplete()
            return
        }
        val ad = interstitial
        if (ad == null) {
            Log.d(TAG, "Interstitial eligible but ad not loaded; continue immediately")
            loadInterstitial(activity.applicationContext)
            onComplete()
            return
        }
        interstitial = null
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                prefs.markInterstitialShown()
                Log.i(TAG, "Interstitial dismissed after display")
                loadInterstitial(activity.applicationContext)
                onComplete()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "Interstitial show failed: code=${adError.code}")
                loadInterstitial(activity.applicationContext)
                onComplete()
            }
        }
        Log.i(TAG, "Showing interstitial at a completed-result transition")
        ad.show(activity)
    }

    /** Debug UI only; no diagnostics entry point is exposed in Release. */
    fun debugSnapshot(context: Context): String {
        if (!BuildConfig.DEBUG) return "Diagnostics unavailable in Release"
        val remaining = MonetizationPreferences(context).remainingAdFreeMs()
        return buildString {
            append("UMP: ").append(ConsentManager.debugStatus()).append('\n')
            append("SDK: ").append(initializationDiagnostic).append('\n')
            append("Rewarded: ").append(rewardedDiagnostic).append('\n')
            append("Interstitial: ").append(interstitialDiagnostic).append('\n')
            append("Banner: ").append(bannerDiagnostic).append('\n')
            append("Ad-free minutes: ").append(remaining / 60_000L)
        }
    }

    fun launchDebugAdInspector(activity: Activity, onResult: (String?) -> Unit) {
        if (!BuildConfig.DEBUG) return
        if (!sdkReady) {
            onResult("SDK not ready")
            return
        }
        MobileAds.openAdInspector(activity) { error ->
            onResult(error?.let { "Ad Inspector code=${it.code}, domain=${it.domain}" })
        }
    }

    fun debugRetryAll(context: Context) {
        if (!BuildConfig.DEBUG || !isReadyForAds()) return
        val app = context.applicationContext
        if (!rewardedLoading && rewarded == null) {
            rewardedRetry?.let(main::removeCallbacks)
            rewardedRetry = null
            rewardedFailures = 0
            nextRewardedAttemptAt = 0L
            loadRewarded(app)
        }
        if (!interstitialLoading && interstitial == null) {
            interstitialRetry?.let(main::removeCallbacks)
            interstitialRetry = null
            interstitialFailures = 0
            nextInterstitialAttemptAt = 0L
            loadInterstitial(app)
        }
    }

    fun preloadRewarded(context: Context) {
        if (isReadyForAds()) loadRewarded(context.applicationContext)
    }

    fun showRewarded(
        activity: Activity,
        onReward: (Long) -> Unit,
        onUnavailable: () -> Unit
    ) {
        val ad = rewarded
        if (!isReadyForAds() || ad == null ||
            MonetizationPreferences(activity).adsSuppressed()
        ) {
            preloadRewarded(activity.applicationContext)
            onUnavailable()
            return
        }
        rewarded = null
        rewardedShowing = true
        setRewardedState(RewardedState.LOADING)
        var granted = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedShowing = false
                if (!granted) {
                    Log.d(TAG, "Rewarded ad dismissed without an earned reward")
                    loadRewarded(activity.applicationContext)
                } else {
                    setRewardedState(RewardedState.AD_FREE_ACTIVE)
                }
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewardedShowing = false
                Log.w(TAG, "Rewarded show failed: code=${error.code}")
                setRewardedState(RewardedState.ERROR)
                loadRewarded(activity.applicationContext)
                if (!granted) onUnavailable()
            }
        }
        ad.show(activity) {
            if (!granted) {
                granted = true
                val expiry = MonetizationPreferences(activity).grantRewardedAdFree()
                interstitial = null
                interstitialRetry?.let(main::removeCallbacks)
                interstitialRetry = null
                setRewardedState(RewardedState.AD_FREE_ACTIVE)
                Log.i(TAG, "Rewarded hour granted after onUserEarnedReward")
                onReward(expiry)
            }
        }
    }

    private fun loadRewarded(context: Context) {
        if (!isReadyForAds() || rewardedLoading || rewardedShowing || rewarded != null) return
        if (MonetizationPreferences(context).adsSuppressed()) {
            setRewardedState(RewardedState.AD_FREE_ACTIVE)
            return
        }
        if (SystemClock.elapsedRealtime() < nextRewardedAttemptAt) return
        rewardedRetry?.let(main::removeCallbacks)
        rewardedRetry = null
        rewardedLoading = true
        setRewardedState(RewardedState.LOADING)
        rewardedDiagnostic = "requesting"
        Log.d(TAG, "Loading Rewarded Ad from AdMob")
        RewardedAd.load(
            context, BuildConfig.ADMOB_REWARDED_ID, AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedLoading = false
                    rewarded = ad
                    rewardedFailures = 0
                    nextRewardedAttemptAt = 0L
                    rewardedDiagnostic = "loaded"
                    Log.i(TAG, "Rewarded onAdLoaded")
                    setRewardedState(RewardedState.READY)
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedLoading = false
                    rewarded = null
                    val kind = AdRetryPolicy.classify(error.code)
                    rewardedDiagnostic = "failed code=${error.code} domain=${error.domain} type=$kind"
                    Log.w(TAG, "Rewarded $rewardedDiagnostic")
                    rewardedFailures++
                    scheduleRewardedRetry(context.applicationContext, kind)
                    setRewardedState(
                        when (kind) {
                            AdRetryPolicy.Failure.NO_FILL -> RewardedState.NO_FILL
                            AdRetryPolicy.Failure.NETWORK -> RewardedState.NETWORK_ERROR
                            AdRetryPolicy.Failure.CONFIGURATION -> RewardedState.CONFIGURATION_ERROR
                            else -> RewardedState.ERROR
                        }
                    )
                }
            }
        )
    }

    private fun scheduleRewardedRetry(context: Context, kind: AdRetryPolicy.Failure) {
        val delay = AdRetryPolicy.retryDelayMs(rewardedFailures, kind)
        if (kind == AdRetryPolicy.Failure.CONFIGURATION) {
            nextRewardedAttemptAt = Long.MAX_VALUE
            return
        }
        nextRewardedAttemptAt = SystemClock.elapsedRealtime() + delay
        if (rewardedFailures > MAX_AUTOMATIC_RETRIES) {
            Log.i(TAG, "Rewarded retries paused until a later app session")
            return
        }
        val retry = Runnable {
            rewardedRetry = null
            if (ConsentManager.canRequestAds()) loadRewarded(context)
        }
        rewardedRetry = retry
        main.postDelayed(retry, delay)
    }

    private fun loadInterstitial(context: Context) {
        if (!isReadyForAds() || interstitialLoading || interstitial != null) return
        if (MonetizationPreferences(context).adsSuppressed()) return
        if (SystemClock.elapsedRealtime() < nextInterstitialAttemptAt) return
        interstitialRetry?.let(main::removeCallbacks)
        interstitialRetry = null
        interstitialLoading = true
        interstitialDiagnostic = "requesting"
        Log.d(TAG, "Loading Interstitial Ad")
        InterstitialAd.load(
            context, BuildConfig.ADMOB_INTERSTITIAL_ID, AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialLoading = false
                    interstitial =
                        ad.takeUnless { MonetizationPreferences(context).adsSuppressed() }
                    interstitialFailures = 0
                    nextInterstitialAttemptAt = 0L
                    interstitialDiagnostic = "loaded"
                    Log.i(TAG, "Interstitial onAdLoaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialLoading = false
                    interstitial = null
                    val kind = AdRetryPolicy.classify(error.code)
                    interstitialFailures++
                    interstitialDiagnostic = "failed code=${error.code} domain=${error.domain} type=$kind"
                    Log.w(TAG, "Interstitial $interstitialDiagnostic")
                    val delay = AdRetryPolicy.retryDelayMs(interstitialFailures, kind)
                    nextInterstitialAttemptAt =
                        if (kind == AdRetryPolicy.Failure.CONFIGURATION) Long.MAX_VALUE
                        else SystemClock.elapsedRealtime() + delay
                    if (kind == AdRetryPolicy.Failure.CONFIGURATION ||
                        interstitialFailures > MAX_AUTOMATIC_RETRIES
                    ) return
                    val retry = Runnable {
                        interstitialRetry = null
                        loadInterstitial(context.applicationContext)
                    }
                    interstitialRetry = retry
                    main.postDelayed(retry, delay)
                }
            }
        )
    }
}
