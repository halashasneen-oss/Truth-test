package com.halashasneen.truthtest.monetization

import android.app.Activity
import android.util.Log
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform

object ConsentManager {
    private var consentInformation: ConsentInformation? = null

    fun gather(activity: Activity, onComplete: (Boolean) -> Unit) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation = info
        val params = ConsentRequestParameters.Builder().build()
        info.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w("TruthTestConsent", "UMP form: code=${formError.errorCode}")
                    }
                    onComplete(info.canRequestAds())
                }
            },
            { error ->
                Log.w("TruthTestConsent", "UMP update: code=${error.errorCode}")
                onComplete(info.canRequestAds())
            }
        )
    }

    fun canRequestAds(): Boolean = consentInformation?.canRequestAds() == true

    fun privacyOptionsRequired(): Boolean =
        consentInformation?.privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity, onComplete: (Boolean) -> Unit) {
        if (!privacyOptionsRequired()) {
            onComplete(false)
            return
        }
        UserMessagingPlatform.showPrivacyOptionsForm(activity) {
            onComplete(true)
        }
    }
}
