package com.nuvexa.truthtest.ui.settings

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View { _binding = FragmentSettingsBinding.inflate(inflater, container, false); return binding.root }
    override fun onViewCreated(view: View, state: Bundle?) {
        val prefs = requireContext().getSharedPreferences("truth_test_settings", Context.MODE_PRIVATE)
        binding.aboutText.text = getString(R.string.entertainment_notice) + "\n\n" + getString(R.string.privacy_audio)
        binding.arabicButton.setOnClickListener { prefs.edit().putString("language", "ar").apply(); AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar")) }
        binding.englishButton.setOnClickListener { prefs.edit().putString("language", "en").apply(); AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en")) }
        binding.darkButton.setOnClickListener { prefs.edit().putBoolean("light_theme", false).apply(); AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES) }
        binding.lightButton.setOnClickListener { prefs.edit().putBoolean("light_theme", true).apply(); AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO) }
    }
    override fun onDestroyView() { _binding = null; super.onDestroyView() }
}
