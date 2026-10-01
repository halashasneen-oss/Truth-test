package com.halashasneen.truthtest.ui

import android.view.View
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.halashasneen.truthtest.R
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.math.abs

@RunWith(AndroidJUnit4::class)
class CustomNavigationDeviceTest {
    @Test fun fourEqualWidthItemsKeepLabelsBelowTheirIcons() {
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            scenario.onActivity { activity ->
                val roots = listOf(R.id.navHomeItem, R.id.navHistoryItem,
                    R.id.navStatsItem, R.id.navMoreItem)
                val icons = listOf(R.id.navHomeIcon, R.id.navHistoryIcon,
                    R.id.navStatsIcon, R.id.navMoreIcon)
                val labels = listOf(R.id.navHomeLabel, R.id.navHistoryLabel,
                    R.id.navStatsLabel, R.id.navMoreLabel)
                val widths = roots.map { activity.findViewById<View>(it).width }
                assertTrue("Items should be measured", widths.all { it > 0 })
                assertTrue("All items must have equal width",
                    widths.maxOrNull()!! - widths.minOrNull()!! <= 2)
                for (i in roots.indices) {
                    val icon = activity.findViewById<View>(icons[i])
                    val label = activity.findViewById<View>(labels[i])
                    val iconPos = IntArray(2)
                    val labelPos = IntArray(2)
                    icon.getLocationOnScreen(iconPos)
                    label.getLocationOnScreen(labelPos)
                    assertTrue("The label must sit BELOW the icon",
                        labelPos[1] >= iconPos[1] + icon.height)
                    assertTrue("Label visible within its item", label.height > 0)
                }
            }
        }
    }
}
