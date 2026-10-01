package com.halashasneen.truthtest.monetization

import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.halashasneen.truthtest.BuildConfig
import com.halashasneen.truthtest.ui.MainActivity
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.Collections

/** Requests only official Google TEST units; does not show or click ads. */
@RunWith(AndroidJUnit4::class)
class GoogleDemoAdLoadDeviceTest {
    @Test fun bannerInterstitialAndRewardedReturnSuccessfulCallbacks() {
        assertTrue("Device smoke must never request production ads", BuildConfig.DEBUG)
        val initialized = CountDownLatch(1)
        val instrument = InstrumentationRegistry.getInstrumentation()
        val context = instrument.targetContext.applicationContext
        instrument.runOnMainSync { MobileAds.initialize(context) { initialized.countDown() } }
        assertTrue("Google Mobile Ads SDK init timed out",
            initialized.await(60, TimeUnit.SECONDS))

        val completed = CountDownLatch(3)
        val failures = Collections.synchronizedList(mutableListOf<String>())
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                val banner = AdView(activity)
                banner.adUnitId = BuildConfig.ADMOB_BANNER_ID
                banner.setAdSize(AdSize.BANNER)
                banner.adListener = object : AdListener() {
                    override fun onAdLoaded() { completed.countDown() }
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        failures += "Banner code=${error.code}"
                        completed.countDown()
                    }
                }
                activity.findViewById<ViewGroup>(android.R.id.content)
                    .addView(banner, FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT))
                banner.loadAd(AdRequest.Builder().build())

                InterstitialAd.load(activity, BuildConfig.ADMOB_INTERSTITIAL_ID,
                    AdRequest.Builder().build(), object : InterstitialAdLoadCallback() {
                        override fun onAdLoaded(ad: InterstitialAd) { completed.countDown() }
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            failures += "Interstitial code=${error.code}"
                            completed.countDown()
                        }
                    })
                RewardedAd.load(activity, BuildConfig.ADMOB_REWARDED_ID,
                    AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
                        override fun onAdLoaded(ad: RewardedAd) { completed.countDown() }
                        override fun onAdFailedToLoad(error: LoadAdError) {
                            failures += "Rewarded code=${error.code}"
                            completed.countDown()
                        }
                    })
            }
            assertTrue("Not all Google test ad requests returned callbacks",
                completed.await(75, TimeUnit.SECONDS))
            assertTrue("Test ad request failed: $failures", failures.isEmpty())
        }
    }
}
