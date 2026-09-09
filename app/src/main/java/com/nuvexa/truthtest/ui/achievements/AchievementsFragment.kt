package com.nuvexa.truthtest.ui.achievements

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.data.HistoryRepository
import com.nuvexa.truthtest.databinding.FragmentAchievementsBinding

class AchievementsFragment : Fragment() {
    private var _binding: FragmentAchievementsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View { _binding = FragmentAchievementsBinding.inflate(inflater, container, false); return binding.root }
    override fun onResume() { super.onResume(); val r = HistoryRepository(requireContext()).getAll(); bind(binding.firstBadge, r.isNotEmpty(), getString(R.string.achievement_first)); bind(binding.tenBadge, r.size >= 10, getString(R.string.achievement_ten)); bind(binding.highBadge, r.any { it.score >= 95 }, getString(R.string.achievement_high)) }
    private fun bind(view: android.widget.TextView, unlocked: Boolean, label: String) { view.text = if (unlocked) "🏅  $label" else "🔒  $label"; view.alpha = if (unlocked) 1f else 0.4f; view.setTextColor(requireContext().getColor(com.nuvexa.truthtest.R.color.app_text_primary)) }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
