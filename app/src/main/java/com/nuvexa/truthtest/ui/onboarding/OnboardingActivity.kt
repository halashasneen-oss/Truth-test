package com.nuvexa.truthtest.ui.onboarding

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.databinding.ActivityOnboardingBinding

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private var page = 0
    private val titles = intArrayOf(R.string.onboarding_1_title, R.string.onboarding_2_title, R.string.onboarding_3_title)
    private val bodies = intArrayOf(R.string.onboarding_1_body, R.string.onboarding_2_body, R.string.onboarding_3_body)
    override fun onCreate(state: Bundle?) { super.onCreate(state); binding = ActivityOnboardingBinding.inflate(layoutInflater); setContentView(binding.root); render(); binding.nextButton.setOnClickListener { if (page < 2) { page++; render() } else { getSharedPreferences("truth_test_settings", MODE_PRIVATE).edit().putBoolean("onboarding_seen", true).apply(); finish() } } }
    private fun render() { binding.onboardingTitle.setText(titles[page]); binding.onboardingBody.setText(bodies[page]); binding.onboardingIcon.text = listOf("〰️", "💯", "⚔️")[page]; binding.nextButton.setText(if (page == 2) R.string.get_started else R.string.continue_label) }
}
