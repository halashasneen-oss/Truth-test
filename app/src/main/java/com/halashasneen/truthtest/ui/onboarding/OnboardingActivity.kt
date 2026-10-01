package com.halashasneen.truthtest.ui.onboarding

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.ui.SafeArea
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.databinding.ActivityOnboardingBinding
import com.halashasneen.truthtest.ui.MainActivity

class OnboardingActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOnboardingBinding
    private var page = 0

    private val titles = intArrayOf(
        R.string.onboarding_new_1_title,
        R.string.onboarding_new_2_title,
        R.string.onboarding_new_3_title
    )
    private val bodies = intArrayOf(
        R.string.onboarding_new_1_body,
        R.string.onboarding_new_2_body,
        R.string.onboarding_new_3_body
    )
    private val icons = listOf("?", "〰", "◈")

    override fun onCreate(state: Bundle?) {
        super.onCreate(state)
        binding = ActivityOnboardingBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SafeArea.apply(this, binding.root)

        binding.skipButton.setOnClickListener { finishOnboarding() }
        binding.nextButton.setOnClickListener {
            if (page < 2) {
                page++
                render()
            } else {
                finishOnboarding()
            }
        }
        render()
    }

    private fun render() {
        binding.onboardingTitle.setText(titles[page])
        binding.onboardingBody.setText(bodies[page])
        binding.onboardingIcon.text = icons[page]
        binding.progressText.text = getString(R.string.onboarding_progress_format, page + 1)
        binding.progressIndicator.setProgressCompat(page + 1, true)
        binding.noticeText.visibility = if (page == 2) View.VISIBLE else View.GONE
        binding.skipButton.visibility = if (page == 2) View.INVISIBLE else View.VISIBLE
        binding.nextButton.setText(if (page == 2) R.string.get_started else R.string.continue_label)
    }

    private fun finishOnboarding() {
        getSharedPreferences(AppStorageContract.PREFS_SETTINGS, MODE_PRIVATE)
            .edit()
            .putBoolean(AppStorageContract.KEY_ONBOARDING_SEEN, true)
            .apply()
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
