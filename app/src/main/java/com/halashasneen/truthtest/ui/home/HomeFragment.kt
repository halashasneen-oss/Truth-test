package com.halashasneen.truthtest.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.data.QuestionRepository
import com.halashasneen.truthtest.databinding.FragmentHomeBinding
import com.halashasneen.truthtest.ui.test.TestActivity
import kotlin.math.sin

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var phase = 0.0
    private val animator = object : Runnable {
        override fun run() {
            if (_binding == null) return
            val value = (0.08 + (sin(phase) + 1.0) * 0.025).toFloat()
            binding.ambientWaveform.addAmplitude(value)
            phase += 0.45
            binding.ambientWaveform.postDelayed(this, 90)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        val daily = QuestionRepository(requireContext()).dailyQuestion()
        binding.dailyQuestion.text = daily?.text.orEmpty()
        binding.soloCard.setOnClickListener { launch(TestActivity.MODE_SOLO) }
        binding.duelCard.setOnClickListener { launch(TestActivity.MODE_DUEL, playerCount = 2) }
        binding.groupCard.setOnClickListener { showGroupSizePicker() }
        binding.customCard.setOnClickListener { launch(TestActivity.MODE_CUSTOM) }
        binding.dailyStart.setOnClickListener { launch(TestActivity.MODE_SOLO, daily = true) }
        binding.ambientWaveform.post(animator)
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
