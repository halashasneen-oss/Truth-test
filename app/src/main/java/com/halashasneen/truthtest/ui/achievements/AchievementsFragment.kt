package com.halashasneen.truthtest.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.AchievementKey
import com.halashasneen.truthtest.data.AchievementRepository
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.PlayerProfileRepository
import com.halashasneen.truthtest.data.SocialSessionRepository
import com.halashasneen.truthtest.databinding.FragmentAchievementsBinding

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
        val profiles = PlayerProfileRepository(requireContext())
        val sessions = SocialSessionRepository(requireContext())
        val results = history.getAll()
        val unlocked = AchievementRepository(requireContext()).sync(
            results = results,
            totalXp = profiles.totalXp(),
            completedSessions = sessions.completedCount()
        )
        val streak = history.currentStreak()

        binding.streakSummary.text = getString(R.string.current_streak_format, streak)
        binding.progressSummary.text = getString(
            R.string.achievement_progress_format,
            unlocked.size,
            AchievementKey.entries.size
        )

        bind(binding.firstBadge, AchievementKey.FIRST in unlocked, R.string.achievement_first, "◎", R.string.achievement_req_first)
        bind(binding.tenBadge, AchievementKey.TEN in unlocked, R.string.achievement_ten, "10", R.string.achievement_req_ten)
        bind(binding.fiftyBadge, AchievementKey.FIFTY in unlocked, R.string.achievement_fifty, "⚡", R.string.achievement_req_fifty)
        bind(binding.hundredBadge, AchievementKey.HUNDRED in unlocked, R.string.achievement_hundred, "100", R.string.achievement_req_hundred)
        bind(binding.highBadge, AchievementKey.HIGH in unlocked, R.string.achievement_high, "★", R.string.achievement_req_high)
        bind(binding.streakBadge, AchievementKey.STREAK_3 in unlocked, R.string.achievement_streak, "🔥", R.string.achievement_req_streak3)
        bind(binding.streakSevenBadge, AchievementKey.STREAK_7 in unlocked, R.string.achievement_streak_seven, "◆", R.string.achievement_req_streak7)
        bind(binding.duelBadge, AchievementKey.DUEL in unlocked, R.string.achievement_duel, "⚔", R.string.achievement_req_duel)
        bind(binding.groupBadge, AchievementKey.GROUP in unlocked, R.string.achievement_group, "◈", R.string.achievement_req_group)
        bind(binding.customBadge, AchievementKey.CUSTOM in unlocked, R.string.achievement_custom, "✎", R.string.achievement_req_custom)

        bind(binding.couplesBadge, AchievementKey.COUPLES in unlocked, R.string.achievement_couples, "♡", R.string.achievement_req_couples)
        bind(binding.friendsBadge, AchievementKey.FRIENDS in unlocked, R.string.achievement_friends, "◇", R.string.achievement_req_friends)
        bind(binding.partyBadge, AchievementKey.PARTY in unlocked, R.string.achievement_party, "◈", R.string.achievement_req_party)
        bind(binding.dailyBadge, AchievementKey.DAILY in unlocked, R.string.achievement_daily, "◉", R.string.achievement_req_daily)
        bind(binding.challengeBadge, AchievementKey.CHALLENGE in unlocked, R.string.achievement_challenge, "↯", R.string.achievement_req_challenge)
        bind(binding.xpBadge, AchievementKey.XP_500 in unlocked, R.string.achievement_xp500, "↑", R.string.achievement_req_xp500)
        bind(binding.sessionsBadge, AchievementKey.SESSIONS_5 in unlocked, R.string.achievement_sessions5, "5", R.string.achievement_req_sessions5)
    }

    private fun bind(
        view: TextView,
        unlocked: Boolean,
        labelRes: Int,
        icon: String,
        requirementRes: Int
    ) {
        val status = getString(
            if (unlocked) R.string.achievement_unlocked else R.string.achievement_locked
        )
        val displayIcon = if (unlocked) icon else "◇"
        view.text = getString(
            R.string.achievement_card_format,
            displayIcon,
            getString(labelRes),
            getString(requirementRes),
            status
        )
        view.alpha = if (unlocked) 1f else 0.72f
        view.setTextColor(requireContext().getColor(R.color.p2_text_primary))
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
