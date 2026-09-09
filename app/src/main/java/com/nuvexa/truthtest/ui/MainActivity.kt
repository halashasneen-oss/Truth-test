package com.nuvexa.truthtest.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.nuvexa.truthtest.R
import com.nuvexa.truthtest.databinding.ActivityMainBinding
import com.nuvexa.truthtest.ui.achievements.AchievementsFragment
import com.nuvexa.truthtest.ui.history.HistoryFragment
import com.nuvexa.truthtest.ui.home.HomeFragment
import com.nuvexa.truthtest.ui.onboarding.OnboardingActivity
import com.nuvexa.truthtest.ui.settings.SettingsFragment
import android.content.Intent

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val prefs = getSharedPreferences("truth_test_settings", MODE_PRIVATE)
        if (!prefs.getBoolean("onboarding_seen", false)) {
            startActivity(Intent(this, OnboardingActivity::class.java))
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment: Fragment = when (item.itemId) {
                R.id.nav_history -> HistoryFragment()
                R.id.nav_achievements -> AchievementsFragment()
                R.id.nav_settings -> SettingsFragment()
                else -> HomeFragment()
            }
            supportFragmentManager.beginTransaction().replace(R.id.fragmentContainer, fragment).commit()
            true
        }
        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home
        }
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences("truth_test_settings", MODE_PRIVATE)
        if (prefs.getBoolean("onboarding_seen", false) && Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean("notification_asked", false)
        ) {
            prefs.edit().putBoolean("notification_asked", true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
