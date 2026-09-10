package com.halashasneen.truthtest.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
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
            binding.splashWaveform.postDelayed(this, 70)
        }
    }

    private val openApp = Runnable {
        val seen = getSharedPreferences("truth_test_settings", MODE_PRIVATE)
            .getBoolean("onboarding_seen", false)
        startActivity(Intent(this, if (seen) MainActivity::class.java else OnboardingActivity::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.splashWaveform.post(pulse)
        binding.root.postDelayed(openApp, 1250)
    }

    override fun onDestroy() {
        binding.splashWaveform.removeCallbacks(pulse)
        binding.root.removeCallbacks(openApp)
        super.onDestroy()
    }
}
