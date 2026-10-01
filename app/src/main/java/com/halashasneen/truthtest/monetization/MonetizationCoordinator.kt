package com.halashasneen.truthtest.monetization

import android.app.Activity

object MonetizationCoordinator {
    fun startAds(activity: Activity, onReady: (() -> Unit)? = null) {
        ConsentManager.gather(activity) { canRequestAds ->
            if (canRequestAds) {
                AdsManager.initialize(activity.applicationContext)
            }
            onReady?.invoke()
        }
    }
}
