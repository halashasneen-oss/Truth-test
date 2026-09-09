package com.nuvexa.truthtest.ui.home

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.nuvexa.truthtest.data.QuestionRepository
import com.nuvexa.truthtest.databinding.FragmentHomeBinding
import com.nuvexa.truthtest.ui.test.TestActivity
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
        binding.duelCard.setOnClickListener { launch(TestActivity.MODE_DUEL) }
        binding.customCard.setOnClickListener { launch(TestActivity.MODE_CUSTOM) }
        binding.dailyStart.setOnClickListener { launch(TestActivity.MODE_SOLO, true) }
        binding.ambientWaveform.post(animator)
    }

    private fun launch(mode: String, daily: Boolean = false) {
        startActivity(Intent(requireContext(), TestActivity::class.java).apply {
            putExtra(TestActivity.EXTRA_MODE, mode)
            putExtra(TestActivity.EXTRA_DAILY, daily)
        })
    }

    override fun onDestroyView() {
        binding.ambientWaveform.removeCallbacks(animator)
        _binding = null
        super.onDestroyView()
    }
}
