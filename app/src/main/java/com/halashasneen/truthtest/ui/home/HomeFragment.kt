package com.halashasneen.truthtest.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.PlayerProfileRepository
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.data.SocialSessionRepository
import com.halashasneen.truthtest.data.XpEngine
import com.halashasneen.truthtest.data.model.SocialMode
import com.halashasneen.truthtest.databinding.FragmentHomeBinding
import com.halashasneen.truthtest.ui.MainActivity
import com.halashasneen.truthtest.ui.achievements.AchievementsFragment
import com.halashasneen.truthtest.ui.profiles.ProfilesFragment
import com.halashasneen.truthtest.ui.social.SocialSessionActivity
import com.halashasneen.truthtest.ui.share.ShareStudioFragment
import com.halashasneen.truthtest.ui.test.TestActivity
import com.halashasneen.truthtest.monetization.AdsManager
import com.halashasneen.truthtest.monetization.MonetizationPreferences
import android.widget.Toast
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlin.math.ceil
import com.google.android.gms.ads.AdView
import kotlin.math.sin

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var phase = 0.0
    private var bannerAd: AdView? = null
    private val rewardExpiry = Runnable {
        if (_binding != null && isAdded) {
            val expired = MonetizationPreferences(requireContext()).remainingAdFreeMs() <= 0L
            refreshReward()
            if (expired) refreshAdsAfterConsent()
        }
    }

    private val animator = object : Runnable {
        override fun run() {
            if (_binding == null) return
            val value = (0.08 + (sin(phase) + 1.0) * 0.026).toFloat()
            binding.ambientWaveform.addAmplitude(value)
            phase += 0.42
            binding.ambientWaveform.postDelayed(this, 88)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.startTruthButton.setOnClickListener { chooseSoloLength() }
        binding.homeRewardButton.setOnClickListener {
            binding.homeRewardButton.isEnabled = false
            AdsManager.showRewarded(
                requireActivity(),
                onReward = {
                    if (_binding != null) {
                        bannerAd?.destroy()
                        bannerAd = null
                        binding.homeAdContainer.removeAllViews()
                        binding.homeAdSection.visibility = View.GONE
                        refreshReward()
                        Toast.makeText(requireContext(), R.string.p5_reward_granted, Toast.LENGTH_LONG).show()
                    }
                },
                onUnavailable = {
                    if (_binding != null) {
                        refreshReward()
                        Toast.makeText(requireContext(), R.string.p5_reward_unavailable, Toast.LENGTH_SHORT).show()
                    }
                }
            )
        }
        binding.truthExperienceCard.setOnClickListener { chooseSoloLength() }
        binding.duelButton.setOnClickListener { launch(TestActivity.MODE_DUEL, playerCount = 2) }
        binding.partyButton.setOnClickListener { launchSocial(SocialMode.PARTY) }
        binding.partyExperienceCard.setOnClickListener { launchSocial(SocialMode.PARTY) }
        binding.customButton.setOnClickListener { launch(TestActivity.MODE_CUSTOM) }

        binding.couplesExperienceCard.setOnClickListener { launchSocial(SocialMode.COUPLES) }
        binding.friendsExperienceCard.setOnClickListener { launchSocial(SocialMode.FRIENDS) }
        binding.challengesExperienceCard.setOnClickListener { launchSocial(SocialMode.CHALLENGE) }
        binding.shareExperienceCard.setOnClickListener {
            (activity as? MainActivity)?.showSecondary(ShareStudioFragment())
        }

        binding.dailyExperienceCard.setOnClickListener { launch(TestActivity.MODE_SOLO, daily = true) }
        binding.dailyStart.setOnClickListener { launch(TestActivity.MODE_SOLO, daily = true) }

        binding.insightsExperienceCard.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_statistics)
        }
        binding.achievementsExperienceCard.setOnClickListener {
            (activity as? MainActivity)?.showSecondary(AchievementsFragment())
        }
        binding.homeProgressCard.setOnClickListener {
            (activity as? MainActivity)?.showSecondary(ProfilesFragment())
        }
        binding.resumeSessionButton.setOnClickListener {
            startActivity(Intent(requireContext(), SocialSessionActivity::class.java).apply {
                putExtra(SocialSessionActivity.EXTRA_RESUME, true)
            })
        }

        binding.ambientWaveform.post(animator)
        refreshDaily()
        refreshProgress()
        refreshActiveSession()
        refreshReward()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) {
            refreshDaily()
            refreshProgress()
            refreshActiveSession()
            refreshReward()
            refreshAdsAfterConsent()
        }
    }

    fun refreshAdsAfterConsent() {
        if (_binding == null || !isAdded) return
        binding.homeAdContainer.post {
            if (_binding == null || !isAdded) return@post
            bannerAd?.destroy()
            bannerAd = AdsManager.attachBanner(requireActivity(), binding.homeAdContainer)
            refreshReward()
        }
    }

    private fun refreshReward() {
        if (_binding == null) return
        val remaining = MonetizationPreferences(requireContext()).remainingAdFreeMs()
        binding.homeRewardCard.removeCallbacks(rewardExpiry)
        if (remaining > 0L) binding.homeRewardCard.postDelayed(
            rewardExpiry, minOf(remaining + 100L, 60_000L)
        )
        binding.homeRewardStatus.text = if (remaining > 0L) {
            getString(R.string.p5_ad_free_remaining, ceil(remaining / 60000.0).toInt())
        } else {
            getString(R.string.hotfix_reward_description)
        }
        binding.homeRewardButton.isEnabled = remaining <= 0L
        binding.homeRewardButton.visibility = if (remaining <= 0L) View.VISIBLE else View.GONE
        if (remaining <= 0L) AdsManager.preloadRewarded(requireContext())
    }

    private fun chooseSoloLength() {
        val counts = intArrayOf(5, 10, 1)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.hotfix_choose_length)
            .setItems(arrayOf(
                getString(R.string.hotfix_five_questions),
                getString(R.string.hotfix_ten_questions),
                getString(R.string.hotfix_single_question)
            )) { _, index -> launch(TestActivity.MODE_SOLO, questionCount = counts[index]) }
            .show()
    }

    private fun refreshDaily() {
        val context = requireContext()
        val daily = QuestionRepository(context).dailyQuestion()
        val history = HistoryRepository(context)
        binding.dailyQuestion.text = daily?.text.orEmpty()
        binding.dailyStatus.setText(
            if (history.hasDailyResultToday()) R.string.daily_completed else R.string.daily_ready
        )
        binding.dailyStreak.text = getString(R.string.daily_streak_format, history.currentStreak())
    }

    private fun refreshProgress() {
        val profile = PlayerProfileRepository(requireContext()).active()
        binding.homeProfileAvatar.text = profile.avatar
        binding.homeProfileName.text = profile.name
        binding.homeProfileLevel.text =
            getString(R.string.p3_level_format, XpEngine.levelForXp(profile.xp)) +
                "  •  " + getString(R.string.p3_xp_format, profile.xp)
        binding.homeProfileProgress.max = XpEngine.levelSize
        binding.homeProfileProgress.progress = XpEngine.levelProgress(profile.xp)
    }

    private fun refreshActiveSession() {
        val session = SocialSessionRepository(requireContext()).active()
        binding.resumeSessionCard.visibility = if (session == null) View.GONE else View.VISIBLE
        if (session != null) {
            binding.resumeSessionMeta.text = getString(
                R.string.p3_resume_session_format,
                socialModeLabel(SocialMode.fromStorage(session.mode)),
                session.turns.size,
                session.totalTurns
            )
        }
    }

    private fun socialModeLabel(mode: SocialMode): String = getString(
        when (mode) {
            SocialMode.COUPLES -> R.string.p3_mode_couples
            SocialMode.FRIENDS -> R.string.p3_mode_friends
            SocialMode.PARTY -> R.string.p3_mode_party
            SocialMode.CHALLENGE -> R.string.p3_mode_challenge
        }
    )

    private fun launchSocial(mode: SocialMode) {
        startActivity(Intent(requireContext(), SocialSessionActivity::class.java).apply {
            putExtra(SocialSessionActivity.EXTRA_MODE, mode.storageKey)
        })
    }

    private fun launch(mode: String, daily: Boolean = false, playerCount: Int = 1, questionCount: Int = 1) {
        startActivity(Intent(requireContext(), TestActivity::class.java).apply {
            putExtra(TestActivity.EXTRA_MODE, mode)
            putExtra(TestActivity.EXTRA_DAILY, daily)
            putExtra(TestActivity.EXTRA_PLAYER_COUNT, playerCount)
            putExtra(TestActivity.EXTRA_QUESTION_COUNT, questionCount)
        })
    }

    override fun onDestroyView() {
        binding.ambientWaveform.removeCallbacks(animator)
        binding.homeRewardCard.removeCallbacks(rewardExpiry)
        bannerAd?.destroy()
        bannerAd = null
        _binding = null
        super.onDestroyView()
    }
}
