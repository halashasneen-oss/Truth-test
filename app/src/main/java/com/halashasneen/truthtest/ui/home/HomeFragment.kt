package com.halashasneen.truthtest.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.databinding.FragmentHomeBinding
import com.halashasneen.truthtest.ui.MainActivity
import com.halashasneen.truthtest.ui.achievements.AchievementsFragment
import com.halashasneen.truthtest.ui.preview.FeaturePreviewFragment
import com.halashasneen.truthtest.ui.test.TestActivity
import kotlin.math.sin

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var phase = 0.0

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
        binding.startTruthButton.setOnClickListener { launch(TestActivity.MODE_SOLO) }
        binding.truthExperienceCard.setOnClickListener { launch(TestActivity.MODE_SOLO) }
        binding.duelButton.setOnClickListener { launch(TestActivity.MODE_DUEL, playerCount = 2) }
        binding.partyButton.setOnClickListener { showGroupSizePicker() }
        binding.partyExperienceCard.setOnClickListener { showGroupSizePicker() }
        binding.customButton.setOnClickListener { launch(TestActivity.MODE_CUSTOM) }

        binding.couplesExperienceCard.setOnClickListener { showPreview(ExperienceSection.COUPLES) }
        binding.friendsExperienceCard.setOnClickListener { showPreview(ExperienceSection.FRIENDS) }
        binding.challengesExperienceCard.setOnClickListener { showPreview(ExperienceSection.CHALLENGES) }
        binding.shareExperienceCard.setOnClickListener { showPreview(ExperienceSection.SHARE_STUDIO) }

        binding.dailyExperienceCard.setOnClickListener { launch(TestActivity.MODE_SOLO, daily = true) }
        binding.dailyStart.setOnClickListener { launch(TestActivity.MODE_SOLO, daily = true) }

        binding.insightsExperienceCard.setOnClickListener {
            (activity as? MainActivity)?.selectTab(R.id.nav_statistics)
        }
        binding.achievementsExperienceCard.setOnClickListener {
            (activity as? MainActivity)?.showSecondary(AchievementsFragment())
        }

        binding.ambientWaveform.post(animator)
        refreshDaily()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) refreshDaily()
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

    private fun showPreview(section: ExperienceSection) {
        (activity as? MainActivity)?.showSecondary(FeaturePreviewFragment.newInstance(section))
    }

    private fun showGroupSizePicker() {
        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.choose_group_size)
            .setItems(arrayOf(getString(R.string.three_players), getString(R.string.four_players))) { _, which ->
                launch(TestActivity.MODE_GROUP, playerCount = if (which == 0) 3 else 4)
            }
            .show()
    }

    private fun launch(mode: String, daily: Boolean = false, playerCount: Int = 1) {
        startActivity(Intent(requireContext(), TestActivity::class.java).apply {
            putExtra(TestActivity.EXTRA_MODE, mode)
            putExtra(TestActivity.EXTRA_DAILY, daily)
            putExtra(TestActivity.EXTRA_PLAYER_COUNT, playerCount)
        })
    }

    override fun onDestroyView() {
        binding.ambientWaveform.removeCallbacks(animator)
        _binding = null
        super.onDestroyView()
    }
}
