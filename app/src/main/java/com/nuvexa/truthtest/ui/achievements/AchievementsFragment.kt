package com.nuvexa.truthtest.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.data.AchievementKey
import com.nuvexa.truthtest.data.AchievementRepository
import com.nuvexa.truthtest.data.HistoryRepository
import com.nuvexa.truthtest.databinding.FragmentAchievementsBinding

class AchievementsFragment : Fragment() {
    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentAchievementsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun render() {
        val history = HistoryRepository(requireContext())
        val results = history.getAll()
        val unlocked = AchievementRepository(requireContext()).sync(results)
        val streak = history.currentStreak()

        binding.streakSummary.text = getString(R.string.current_streak_format, streak)
        binding.progressSummary.text = getString(
            R.string.achievement_progress_format,
            unlocked.size,
            AchievementKey.entries.size
        )

        bind(binding.firstBadge, AchievementKey.FIRST in unlocked, R.string.achievement_first, "🎯")
        bind(binding.tenBadge, AchievementKey.TEN in unlocked, R.string.achievement_ten, "🔟")
        bind(binding.fiftyBadge, AchievementKey.FIFTY in unlocked, R.string.achievement_fifty, "⚡")
        bind(binding.hundredBadge, AchievementKey.HUNDRED in unlocked, R.string.achievement_hundred, "💯")
        bind(binding.highBadge, AchievementKey.HIGH in unlocked, R.string.achievement_high, "👑")
        bind(binding.streakBadge, AchievementKey.STREAK_3 in unlocked, R.string.achievement_streak, "🔥")
        bind(binding.streakSevenBadge, AchievementKey.STREAK_7 in unlocked, R.string.achievement_streak_seven, "🚀")
        bind(binding.duelBadge, AchievementKey.DUEL in unlocked, R.string.achievement_duel, "⚔️")
        bind(binding.groupBadge, AchievementKey.GROUP in unlocked, R.string.achievement_group, "👥")
        bind(binding.customBadge, AchievementKey.CUSTOM in unlocked, R.string.achievement_custom, "✍️")
    }

    private fun bind(view: TextView, unlocked: Boolean, labelRes: Int, icon: String) {
        val label = getString(labelRes)
        view.text = if (unlocked) "$icon  $label" else "🔒  $label"
        view.alpha = if (unlocked) 1f else 0.42f
        view.setTextColor(requireContext().getColor(R.color.app_text_primary))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
