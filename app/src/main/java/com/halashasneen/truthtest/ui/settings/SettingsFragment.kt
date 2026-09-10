package com.halashasneen.truthtest.ui.settings

import android.Manifest
import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import androidx.core.os.LocaleListCompat
import androidx.fragment.app.Fragment
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.databinding.FragmentSettingsBinding
import com.halashasneen.truthtest.notifications.DailyChallengeScheduler
import com.halashasneen.truthtest.notifications.NotificationSettings
import java.util.Calendar

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationSettings: NotificationSettings

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted && isAdded) {
            Toast.makeText(requireContext(), R.string.notification_permission_needed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        state: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        val prefs = requireContext().getSharedPreferences("truth_test_settings", Context.MODE_PRIVATE)
        notificationSettings = NotificationSettings(requireContext())
        binding.aboutText.text = getString(R.string.entertainment_notice) + "\n\n" + getString(R.string.privacy_audio)

        binding.arabicButton.setOnClickListener {
            prefs.edit().putString("language", "ar").apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"))
        }
        binding.englishButton.setOnClickListener {
            prefs.edit().putString("language", "en").apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        binding.darkButton.setOnClickListener {
            prefs.edit().putBoolean("light_theme", false).apply()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        binding.lightButton.setOnClickListener {
            prefs.edit().putBoolean("light_theme", true).apply()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        }

        binding.dailyNotificationsSwitch.isChecked = notificationSettings.dailyEnabled
        binding.streakReminderSwitch.isChecked = notificationSettings.streakReminderEnabled
        updateDailyTimeLabel()

        binding.dailyNotificationsSwitch.setOnCheckedChangeListener { _, checked ->
            notificationSettings.setDailyEnabled(checked)
            if (checked) ensureNotificationPermission()
            DailyChallengeScheduler.schedule(requireContext())
        }
        binding.streakReminderSwitch.setOnCheckedChangeListener { _, checked ->
            notificationSettings.setStreakReminderEnabled(checked)
            if (checked) ensureNotificationPermission()
            DailyChallengeScheduler.schedule(requireContext())
        }
        binding.dailyTimeButton.setOnClickListener { showTimePicker() }
        binding.privacyPolicyButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(PRIVACY_POLICY_URL)))
        }
    }

    private fun showTimePicker() {
        TimePickerDialog(
            requireContext(),
            { _, hour, minute ->
                notificationSettings.setDailyTime(hour, minute)
                updateDailyTimeLabel()
                ensureNotificationPermission()
                DailyChallengeScheduler.schedule(requireContext())
            },
            notificationSettings.dailyHour,
            notificationSettings.dailyMinute,
            DateFormat.is24HourFormat(requireContext())
        ).show()
    }

    private fun updateDailyTimeLabel() {
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, notificationSettings.dailyHour)
            set(Calendar.MINUTE, notificationSettings.dailyMinute)
        }
        val formatted = DateFormat.getTimeFormat(requireContext()).format(calendar.time)
        binding.dailyTimeButton.text = getString(R.string.daily_notification_time_format, formatted)
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val PRIVACY_POLICY_URL = "https://halashasneen-oss.github.io/truth-test-privacy/"
    }
}
