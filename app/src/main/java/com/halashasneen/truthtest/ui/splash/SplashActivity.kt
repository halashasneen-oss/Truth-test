package com.halashasneen.truthtest.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.monetization.AppOpenAds
import com.halashasneen.truthtest.monetization.MonetizationCoordinator
import com.halashasneen.truthtest.databinding.ActivitySplashBinding
import com.halashasneen.truthtest.ui.MainActivity
import com.halashasneen.truthtest.ui.onboarding.OnboardingActivity
import kotlin.math.sin

class SplashActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySplashBinding
    private var phase = 0.0

    private val pulse = object : Runnable {
        override fun run() {
            if (isFinishing) return
            val amplitude = (0.08 + (sin(phase) + 1.0) * 0.045).toFloat()
            binding.splashWaveform.addAmplitude(amplitude)
            phase += 0.55
            binding.splashWaveform.postDelayed(this, 65)
        }
    }

    private var navigated = false
    private var showingOpenAd = false

    private fun openApp() {
        if (navigated || isFinishing || isDestroyed) return
        navigated = true
        binding.root.removeCallbacks(timeout)
        val seen = getSharedPreferences(AppStorageContract.PREFS_SETTINGS, MODE_PRIVATE)
            .getBoolean(AppStorageContract.KEY_ONBOARDING_SEEN, false)
        startActivity(Intent(this, if (seen) MainActivity::class.java else OnboardingActivity::class.java))
        finish()
    }

    // Keep the ad on the splash screen and skip gracefully when consent/network loading is slow.
    private val timeout = Runnable {
        if (!showingOpenAd) openApp()
    }

    private val start = Runnable {
        val seen = getSharedPreferences(AppStorageContract.PREFS_SETTINGS, MODE_PRIVATE)
            .getBoolean(AppStorageContract.KEY_ONBOARDING_SEEN, false)
        if (!seen) {
            openApp() // Never interrupt onboarding with an app-open ad.
        } else {
            MonetizationCoordinator.startAds(this) {
                if (!navigated && !isFinishing && !isDestroyed) {
                    AppOpenAds.preload(applicationContext) {
                        if (!navigated && !isFinishing && !isDestroyed) {
                            showingOpenAd = AppOpenAds.showIfAvailable(this) {
                                showingOpenAd = false
                                openApp()
                            }
                            if (showingOpenAd) binding.root.removeCallbacks(timeout)
                            else openApp()
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.splashWaveform.post(pulse)
        binding.root.postDelayed(start, 700)
        binding.root.postDelayed(timeout, 7_000)
    }

    override fun onDestroy() {
        binding.splashWaveform.removeCallbacks(pulse)
        binding.root.removeCallbacks(start)
        binding.root.removeCallbacks(timeout)
        super.onDestroy()
    }
}
