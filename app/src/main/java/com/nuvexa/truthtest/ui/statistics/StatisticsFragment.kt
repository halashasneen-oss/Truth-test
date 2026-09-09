package com.nuvexa.truthtest.ui.statistics

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.data.HistoryRepository
import com.nuvexa.truthtest.data.StatisticsCalculator
import com.nuvexa.truthtest.databinding.FragmentStatisticsBinding

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)
        render()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) render()
    }

    private fun render() {
        val summary = StatisticsCalculator.calculate(HistoryRepository(requireContext()).getAll())

        binding.totalTestsValue.text = summary.totalResults.toString()
        binding.testsTodayValue.text = summary.testsToday.toString()
        binding.averageValue.text = getString(R.string.stats_percent_format, summary.averageScore)
        binding.bestScoreValue.text = getString(R.string.stats_percent_format, summary.bestScore)
        binding.duelValue.text = summary.duelSessions.toString()
        binding.currentStreakValue.text = getString(R.string.stats_days_format, summary.currentStreak)
        binding.bestStreakValue.text = getString(R.string.stats_days_format, summary.bestStreak)

        if (summary.favoriteCategory == null) {
            binding.favoriteCategoryValue.text = getString(R.string.stats_no_data_short)
            binding.favoriteCategoryMeta.text = getString(R.string.stats_no_data)
        } else {
            binding.favoriteCategoryValue.text = categoryName(summary.favoriteCategory)
            binding.favoriteCategoryMeta.text = getString(
                R.string.stats_category_usage_format,
                summary.favoriteCategoryCount
            )
        }

        binding.trendView.setScores(summary.recentScores)
        binding.emptyState.visibility = if (summary.totalResults == 0) View.VISIBLE else View.GONE
        binding.trendView.visibility = if (summary.recentScores.isEmpty()) View.GONE else View.VISIBLE
        binding.trendEmpty.visibility = if (summary.recentScores.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun categoryName(category: String): String = when (category) {
        "embarrassing" -> getString(R.string.embarrassing)
        "funny" -> getString(R.string.funny)
        "bold" -> getString(R.string.bold)
        "romantic" -> getString(R.string.romantic)
        "friendship" -> getString(R.string.friendship)
        "family" -> getString(R.string.family)
        "custom" -> getString(R.string.custom_question)
        else -> category.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
