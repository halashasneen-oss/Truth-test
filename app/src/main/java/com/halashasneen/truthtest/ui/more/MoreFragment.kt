package com.halashasneen.truthtest.ui.more

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.databinding.FragmentMoreBinding
import com.halashasneen.truthtest.ui.MainActivity
import com.halashasneen.truthtest.ui.achievements.AchievementsFragment
import com.halashasneen.truthtest.ui.settings.SettingsFragment

class MoreFragment : Fragment(R.layout.fragment_more) {
    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMoreBinding.bind(view)

        binding.achievementsCard.setOnClickListener {
            (activity as? MainActivity)?.showSecondary(AchievementsFragment())
        }
        binding.settingsCard.setOnClickListener {
            (activity as? MainActivity)?.showSecondary(SettingsFragment())
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
