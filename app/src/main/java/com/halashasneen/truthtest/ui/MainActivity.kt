package com.halashasneen.truthtest.ui

import android.Manifest
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.halashasneen.truthtest.R
import com.halashasneen.truthtest.core.AppStorageContract
import com.halashasneen.truthtest.databinding.ActivityMainBinding
import com.halashasneen.truthtest.monetization.MonetizationCoordinator
import com.halashasneen.truthtest.ui.history.HistoryFragment
import com.halashasneen.truthtest.ui.home.HomeFragment
import com.halashasneen.truthtest.ui.more.MoreFragment
import com.halashasneen.truthtest.ui.statistics.StatisticsFragment

/**
 * OEM-independent bottom bar: four equal cells, with the icon and the label in
 * separate measured views. Keeps tab selection across recreation/secondary pages.
 */
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private var selectedTab = R.id.nav_home
    private val notificationPermission =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        SafeArea.apply(this, binding.root)
        bindNavigation()
        selectedTab = savedInstanceState?.getInt(STATE_SELECTED_TAB, R.id.nav_home)
            ?: R.id.nav_home
        renderNavigation()
        MonetizationCoordinator.startAds(this) {
            (supportFragmentManager.findFragmentById(R.id.fragmentContainer) as? HomeFragment)
                ?.refreshAdsAfterConsent()
        }
        if (savedInstanceState == null) openRoot(HomeFragment())
    }

    private fun bindNavigation() {
        binding.navHomeItem.setOnClickListener { selectTab(R.id.nav_home) }
        binding.navHistoryItem.setOnClickListener { selectTab(R.id.nav_history) }
        binding.navStatsItem.setOnClickListener { selectTab(R.id.nav_statistics) }
        binding.navMoreItem.setOnClickListener { selectTab(R.id.nav_more) }
    }

    fun selectTab(itemId: Int) {
        if (itemId !in NAV_IDS) return
        val isSameRoot = selectedTab == itemId &&
            supportFragmentManager.backStackEntryCount == 0
        selectedTab = itemId
        renderNavigation()
        if (!isSameRoot) {
            openRoot(when (itemId) {
                R.id.nav_history -> HistoryFragment()
                R.id.nav_statistics -> StatisticsFragment()
                R.id.nav_more -> MoreFragment()
                else -> HomeFragment()
            })
        }
    }

    private fun renderNavigation() {
        val activeColor = ContextCompat.getColor(this, R.color.p2_purple)
        val idleColor = ContextCompat.getColor(this, R.color.p2_text_muted)
        val groups = listOf(
            NavViews(R.id.nav_home, binding.navHomeItem,
                binding.navHomeIconHolder, binding.navHomeIcon, binding.navHomeLabel),
            NavViews(R.id.nav_history, binding.navHistoryItem,
                binding.navHistoryIconHolder, binding.navHistoryIcon, binding.navHistoryLabel),
            NavViews(R.id.nav_statistics, binding.navStatsItem,
                binding.navStatsIconHolder, binding.navStatsIcon, binding.navStatsLabel),
            NavViews(R.id.nav_more, binding.navMoreItem,
                binding.navMoreIconHolder, binding.navMoreIcon, binding.navMoreLabel)
        )
        groups.forEach { cell ->
            val active = selectedTab == cell.id
            cell.item.isSelected = active
            cell.item.isActivated = active
            cell.icon.imageTintList = ColorStateList.valueOf(if (active) activeColor else idleColor)
            cell.label.setTextColor(if (active) activeColor else idleColor)
            cell.iconHolder.background = if (active) {
                ContextCompat.getDrawable(this, R.drawable.bg_custom_nav_selected)
            } else null
        }
    }

    fun showSecondary(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .addToBackStack("secondary")
            .commit()
    }

    private fun openRoot(fragment: Fragment) {
        supportFragmentManager.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE)
        supportFragmentManager.beginTransaction()
            .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
            .replace(R.id.fragmentContainer, fragment)
            .commit()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt(STATE_SELECTED_TAB, selectedTab)
        super.onSaveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        val prefs = getSharedPreferences(AppStorageContract.PREFS_SETTINGS, MODE_PRIVATE)
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED &&
            !prefs.getBoolean(AppStorageContract.KEY_NOTIFICATION_ASKED, false)
        ) {
            prefs.edit().putBoolean(AppStorageContract.KEY_NOTIFICATION_ASKED, true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private data class NavViews(
        val id: Int,
        val item: View,
        val iconHolder: FrameLayout,
        val icon: ImageView,
        val label: TextView
    )

    companion object {
        private const val STATE_SELECTED_TAB = "selected_tab"
        private val NAV_IDS = setOf(R.id.nav_home, R.id.nav_history,
            R.id.nav_statistics, R.id.nav_more)
    }
}