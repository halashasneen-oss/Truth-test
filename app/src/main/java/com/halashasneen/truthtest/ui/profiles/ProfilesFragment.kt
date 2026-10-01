package com.halashasneen.truthtest.ui.profiles

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.card.MaterialCardView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.PlayerProfileRepository
import com.halashasneen.truthtest.data.XpEngine
import com.halashasneen.truthtest.data.model.PlayerProfile
import com.halashasneen.truthtest.databinding.DialogProfileBinding
import com.halashasneen.truthtest.databinding.FragmentProfilesBinding

class ProfilesFragment : Fragment() {
    private var _binding: FragmentProfilesBinding? = null
    private val binding get() = _binding!!
    private val repository by lazy { PlayerProfileRepository(requireContext()) }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        _binding = FragmentProfilesBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        binding.addProfileButton.setOnClickListener { showProfileDialog(null) }
        renderProfiles()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null) renderProfiles()
    }

    private fun renderProfiles() {
        val container = binding.profilesContainer
        container.removeAllViews()
        val activeId = repository.active().id

        repository.getAll().forEach { profile ->
            container.addView(profileCard(profile, profile.id == activeId))
        }
    }

    private fun profileCard(profile: PlayerProfile, active: Boolean): View {
        val context = requireContext()
        val card = MaterialCardView(context).apply {
            radius = dp(22).toFloat()
            cardElevation = 0f
            strokeWidth = dp(if (active) 2 else 1)
            strokeColor = context.getColor(if (active) R.color.p2_purple else R.color.p2_border)
            setCardBackgroundColor(context.getColor(R.color.p2_surface))
            val margin = dp(6)
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = margin
                bottomMargin = margin
            }
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.CENTER_VERTICAL
            setPadding(dp(16), dp(16), dp(12), dp(16))
        }

        val avatar = TextView(context).apply {
            text = profile.avatar
            gravity = android.view.Gravity.CENTER
            textSize = 25f
            setTextColor(context.getColor(R.color.p2_purple))
            background = context.getDrawable(R.drawable.bg_score_phase2)
            layoutParams = LinearLayout.LayoutParams(dp(56), dp(56))
        }

        val info = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply {
                marginStart = dp(14)
            }
        }
        val name = TextView(context).apply {
            text = if (active) profile.name + "  •  " + getString(R.string.p3_active) else profile.name
            textSize = 17f
            setTextColor(context.getColor(R.color.p2_text_primary))
            setTypeface(typeface, android.graphics.Typeface.BOLD)
        }
        val stats = TextView(context).apply {
            text = getString(
                R.string.p3_level_format,
                XpEngine.levelForXp(profile.xp)
            ) + "  •  " + getString(R.string.p3_xp_format, profile.xp) +
                "  •  " + getString(R.string.p3_games_format, profile.games) +
                "  •  " + getString(R.string.p3_average_format, profile.averageScore)
            textSize = 12f
            setTextColor(context.getColor(R.color.p2_text_secondary))
        }
        info.addView(name)
        info.addView(stats)

        val edit = MaterialButton(context).apply {
            text = "✎"
            minWidth = dp(48)
            minimumWidth = dp(48)
            layoutParams = LinearLayout.LayoutParams(dp(52), dp(48))
            backgroundTintList = ColorStateList.valueOf(context.getColor(R.color.p2_surface_soft))
            setTextColor(context.getColor(R.color.p2_text_primary))
            setOnClickListener { showProfileDialog(profile) }
        }

        row.addView(avatar)
        row.addView(info)
        row.addView(edit)
        card.addView(row)

        card.setOnClickListener {
            repository.setActive(profile.id)
            renderProfiles()
        }
        return card
    }

    private fun showProfileDialog(profile: PlayerProfile?) {
        val dialogBinding = DialogProfileBinding.inflate(layoutInflater)
        dialogBinding.profileNameInput.setText(profile?.name.orEmpty())
        selectAvatar(dialogBinding, profile?.avatar ?: PlayerProfileRepository.DEFAULT_AVATAR)

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setTitle(if (profile == null) R.string.p3_add_profile else R.string.p3_edit_profile)
            .setView(dialogBinding.root)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok, null)
            .apply {
                if (profile != null) {
                    setNeutralButton(R.string.p3_delete_profile) { _, _ ->
                        if (!repository.delete(profile.id)) {
                            Toast.makeText(
                                requireContext(),
                                R.string.p3_keep_one_profile,
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                        renderProfiles()
                    }
                }
            }
            .create()

        dialog.setOnShowListener {
            dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.profileNameInput.text?.toString().orEmpty()
                val avatar = selectedAvatar(dialogBinding)
                val ok = if (profile == null) {
                    repository.add(name, avatar) != null
                } else {
                    repository.update(profile.id, name, avatar)
                }
                if (ok) {
                    dialog.dismiss()
                    renderProfiles()
                }
            }
        }
        dialog.show()
    }

    private fun selectedAvatar(binding: DialogProfileBinding): String = when (
        binding.avatarGroup.checkedChipId
    ) {
        R.id.avatarTwo -> "◆"
        R.id.avatarThree -> "★"
        R.id.avatarFour -> "◈"
        R.id.avatarFive -> "⚡"
        else -> "◎"
    }

    private fun selectAvatar(binding: DialogProfileBinding, avatar: String) {
        val id = when (avatar) {
            "◆" -> R.id.avatarTwo
            "★" -> R.id.avatarThree
            "◈" -> R.id.avatarFour
            "⚡" -> R.id.avatarFive
            else -> R.id.avatarOne
        }
        binding.avatarGroup.check(id)
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }
}
