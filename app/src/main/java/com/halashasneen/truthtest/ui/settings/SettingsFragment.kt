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
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.databinding.FragmentSettingsBinding
import com.halashasneen.truthtest.monetization.AdsManager
import com.halashasneen.truthtest.monetization.ConsentManager
import com.halashasneen.truthtest.monetization.MonetizationPreferences
import com.halashasneen.truthtest.notifications.DailyChallengeScheduler
import com.halashasneen.truthtest.notifications.NotificationSettings
import java.util.Calendar
import kotlin.math.ceil

class SettingsFragment : Fragment() {
    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var notificationSettings: NotificationSettings
    private lateinit var monetizationPrefs: MonetizationPreferences
    private val rewardStateObserver: (AdsManager.RewardedState) -> Unit = {
        if (_binding != null && isAdded) refreshMonetization()
    }

    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted && isAdded) {
            Toast.makeText(requireContext(), R.string.notification_permission_needed, Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, state: Bundle?): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, state: Bundle?) {
        val prefs = requireContext().getSharedPreferences(AppStorageContract.PREFS_SETTINGS, Context.MODE_PRIVATE)
        notificationSettings = NotificationSettings(requireContext())
        monetizationPrefs = MonetizationPreferences(requireContext())
        binding.aboutText.text = getString(R.string.entertainment_notice) + "\n\n" +
            getString(R.string.privacy_audio) + "\n\n" + getString(R.string.p5_ads_privacy_summary)

        binding.arabicButton.setOnClickListener {
            prefs.edit().putString(AppStorageContract.KEY_LANGUAGE, "ar").apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("ar"))
        }
        binding.englishButton.setOnClickListener {
            prefs.edit().putString(AppStorageContract.KEY_LANGUAGE, "en").apply()
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("en"))
        }
        binding.darkButton.setOnClickListener {
            prefs.edit().putBoolean(AppStorageContract.KEY_LIGHT_THEME, false).apply()
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        }
        binding.lightButton.setOnClickListener {
            prefs.edit().putBoolean(AppStorageContract.KEY_LIGHT_THEME, true).apply()
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

        binding.rewardedAdButton.setOnClickListener {
            AdsManager.showRewarded(
                requireActivity(),
                onReward = {
                    if (_binding != null) {
                        refreshMonetization()
                        Toast.makeText(requireContext(), R.string.p5_reward_granted, Toast.LENGTH_LONG).show()
                    }
                },
                onUnavailable = {
                    if (_binding != null) {
                        Toast.makeText(
                            requireContext(),
                            AdsManager.rewardStatusText(AdsManager.rewardedState),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            )
        }
        binding.privacyOptionsButton.setOnClickListener {
            ConsentManager.showPrivacyOptions(requireActivity()) { shown ->
                if (ConsentManager.canRequestAds()) {
                    AdsManager.initialize(requireContext().applicationContext)
                } else {
                    AdsManager.reportConsentUnavailable()
                }
                if (_binding != null) refreshMonetization()
                if (!shown && isAdded) {
                    Toast.makeText(requireContext(), R.string.p5_privacy_not_required, Toast.LENGTH_LONG).show()
                }
            }
        }
        AdsManager.registerRewardedObserver(rewardStateObserver)
        refreshMonetization()
    }

    override fun onResume() {
        super.onResume()
        if (_binding != null && ::monetizationPrefs.isInitialized) {
            AdsManager.preloadRewarded(requireContext())
            refreshMonetization()
        }
    }

    private fun refreshMonetization() {
        val remaining = monetizationPrefs.remainingAdFreeMs()
        binding.adStatusText.text = if (remaining > 0L) {
            getString(
                R.string.p5_ad_free_remaining,
                ceil(remaining / 60_000.0).toInt()
            )
        } else {
            getString(R.string.p5_ads_active)
        }
        val ready = AdsManager.rewardedState == AdsManager.RewardedState.READY
        binding.rewardedAdButton.isEnabled = remaining <= 0L && ready
        binding.rewardedAdButton.text = getString(
            if (ready) R.string.p5_watch_rewarded
            else R.string.hotfix_reward_loading_button
        )
        if (remaining <= 0L) {
            binding.adStatusText.append(
                "\n" + getString(AdsManager.rewardStatusText(AdsManager.rewardedState))
            )
        }
        binding.privacyOptionsButton.visibility =
            if (ConsentManager.privacyOptionsRequired()) View.VISIBLE else View.GONE
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
        binding.dailyTimeButton.text = getString(
            R.string.daily_notification_time_format,
            DateFormat.getTimeFormat(requireContext()).format(calendar.time)
        )
    }

    private fun ensureNotificationPermission() {
        if (Build.VERSION.SDK_INT < 33) return
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    override fun onDestroyView() {
        AdsManager.unregisterRewardedObserver(rewardStateObserver)
        _binding = null
        super.onDestroyView()
    }

    companion object {
        private const val PRIVACY_POLICY_URL = "https://halashasneen-oss.github.io/truth-test-privacy/"
    }
}
