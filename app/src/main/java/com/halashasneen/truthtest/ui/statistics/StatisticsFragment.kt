package com.halashasneen.truthtest.ui.statistics

import android.content.ClipData
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.HistoryRepository
import com.halashasneen.truthtest.data.StatisticsCalculator
import com.halashasneen.truthtest.databinding.FragmentStatisticsBinding
import com.halashasneen.truthtest.report.MonthlyReportPdfRenderer
import java.time.Instant
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import kotlinx.coroutines.launch

class StatisticsFragment : Fragment(R.layout.fragment_statistics) {
    private var _binding: FragmentStatisticsBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentStatisticsBinding.bind(view)
        binding.monthlyReportButton.setOnClickListener { showMonthlyReportPicker() }
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

    private fun showMonthlyReportPicker() {
        val results = HistoryRepository(requireContext()).getAll()
        if (results.isEmpty()) {
            Toast.makeText(requireContext(), R.string.monthly_report_no_data, Toast.LENGTH_SHORT).show()
            return
        }
        val zone = ZoneId.systemDefault()
        val months = results.map {
            YearMonth.from(Instant.ofEpochMilli(it.timestamp).atZone(zone).toLocalDate())
        }.distinct().sortedDescending()
        val locale = resources.configuration.locales[0]
        val formatter = DateTimeFormatter.ofPattern("MMMM yyyy", locale)
        val labels = months.map { it.format(formatter) }.toTypedArray()

        MaterialAlertDialogBuilder(requireContext())
            .setTitle(R.string.monthly_report_choose_month)
            .setItems(labels) { _, which -> createMonthlyReport(results, months[which]) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private fun createMonthlyReport(results: List<com.halashasneen.truthtest.data.model.TestResult>, month: YearMonth) {
        binding.monthlyReportButton.isEnabled = false
        binding.monthlyReportButton.setText(R.string.monthly_report_creating)
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val uri = MonthlyReportPdfRenderer.render(requireContext(), results, month)
                startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).apply {
                    type = "application/pdf"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    clipData = ClipData.newUri(requireContext().contentResolver, getString(R.string.monthly_report_title), uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }, getString(R.string.monthly_report_share)))
            } catch (_: Throwable) {
                Toast.makeText(requireContext(), R.string.monthly_report_failed, Toast.LENGTH_LONG).show()
            } finally {
                if (_binding != null) {
                    binding.monthlyReportButton.isEnabled = true
                    binding.monthlyReportButton.setText(R.string.monthly_report_export)
                }
            }
        }
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
