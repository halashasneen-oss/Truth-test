package com.halashasneen.truthtest.ui.more

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.PlayerProfileRepository
import com.halashasneen.truthtest.data.XpEngine
import com.halashasneen.truthtest.databinding.FragmentMoreBinding
import com.halashasneen.truthtest.ui.MainActivity
import com.halashasneen.truthtest.ui.achievements.AchievementsFragment
import com.halashasneen.truthtest.ui.profiles.ProfilesFragment
import com.halashasneen.truthtest.ui.settings.SettingsFragment

class MoreFragment : Fragment(R.layout.fragment_more) {
    private var _binding: FragmentMoreBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentMoreBinding.bind(view)

        binding.profilesCard.setOnClickListener {
            (activity as? MainActivity)?.showSecondary(ProfilesFragment())
        }
        binding.achievementsCard.setOnClickListener {
            (activity as? MainActivity)?.showSecondary(AchievementsFragment())
        }
        binding.settingsCard.setOnClickListener {
            (activity as? MainActivity)?.showSecondary(SettingsFragment())
        }
        refreshProgress()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) refreshProgress()
    }

    private fun refreshProgress() {
        val profile = PlayerProfileRepository(requireContext()).active()
        binding.profileAvatar.text = profile.avatar
        binding.profileName.text = profile.name
        binding.profileLevel.text =
            getString(R.string.p3_level_format, XpEngine.levelForXp(profile.xp)) +
                "  •  " + getString(R.string.p3_xp_format, profile.xp) +
                "  •  " + getString(R.string.p3_best_format, profile.bestScore)
        binding.profileProgress.max = XpEngine.levelSize
        binding.profileProgress.progress = XpEngine.levelProgress(profile.xp)
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
