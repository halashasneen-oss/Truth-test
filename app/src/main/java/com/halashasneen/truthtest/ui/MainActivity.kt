package com.halashasneen.truthtest.ui

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.databinding.ActivityMainBinding
import com.halashasneen.truthtest.ui.history.HistoryFragment
import com.halashasneen.truthtest.ui.home.HomeFragment
import com.halashasneen.truthtest.ui.more.MoreFragment
import com.halashasneen.truthtest.ui.statistics.StatisticsFragment
import com.halashasneen.truthtest.monetization.MonetizationCoordinator

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SafeArea.apply(this, binding.root)
        MonetizationCoordinator.startAds(this) {
            (supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? HomeFragment)
                ?.refreshAdsAfterConsent()
        }

        binding.bottomNav.setOnItemSelectedListener { item ->
            val fragment = when (item.itemId) {
                R.id.nav_history -> HistoryFragment()
                R.id.nav_statistics -> StatisticsFragment()
                R.id.nav_more -> MoreFragment()
                else -> HomeFragment()
            }
            openRoot(fragment)
            true
        }

        if (savedInstanceState == null) {
            binding.bottomNav.selectedItemId = R.id.nav_home
        }
    }

    fun selectTab(itemId: Int) {
        binding.bottomNav.selectedItemId = itemId
    }

    fun showSecondary(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("secondary")
            .commit()
    }

    private fun openRoot(fragment: Fragment) {
        supportFragmentManager.popBackStack(
            null,
            androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE
        )
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(AppStorageContract.PREFS_SETTINGS, MODE_PRIVATE)
        if (
            Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean(AppStorageContract.KEY_NOTIFICATION_ASKED, false)
        ) {
            prefs.edit().putBoolean(AppStorageContract.KEY_NOTIFICATION_ASKED, true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
