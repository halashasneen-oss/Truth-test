package com.halashasneen.truthtest.ui.preview

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.databinding.FragmentFeaturePreviewBinding
import com.halashasneen.truthtest.ui.home.ExperienceSection

class FeaturePreviewFragment : Fragment(R.layout.fragment_feature_preview) {
    private var _binding: FragmentFeaturePreviewBinding? = null
    private val binding get() = _binding!!

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        _binding = FragmentFeaturePreviewBinding.bind(view)

        val section = ExperienceSection.entries.firstOrNull {
            it.stableId == arguments?.getString(ARG_SECTION)
        } ?: ExperienceSection.CHALLENGES

        val content = when (section) {
            ExperienceSection.COUPLES -> PreviewContent("♡", R.string.feature_couples_title, R.string.feature_couples_body)
            ExperienceSection.FRIENDS -> PreviewContent("◇", R.string.feature_friends_title, R.string.feature_friends_body)
            ExperienceSection.SHARE_STUDIO -> PreviewContent("↗", R.string.feature_share_title, R.string.feature_share_body)
            else -> PreviewContent("↯", R.string.feature_challenges_title, R.string.feature_challenges_body)
        }

        binding.previewIcon.text = content.icon
        binding.previewTitle.setText(content.titleRes)
        binding.previewBody.setText(content.bodyRes)
        binding.backButton.setOnClickListener { parentFragmentManager.popBackStack() }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private data class PreviewContent(val icon: String, val titleRes: Int, val bodyRes: Int)

    companion object {
        private const val ARG_SECTION = "section"

        fun newInstance(section: ExperienceSection) = FeaturePreviewFragment().apply {
            arguments = Bundle().apply { putString(ARG_SECTION, section.stableId) }
        }
    }
}
