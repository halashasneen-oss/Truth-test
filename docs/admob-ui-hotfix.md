# Truth Test 2.0.2 — Bottom navigation and AdMob hotfix

Baseline: `truth-test-final-hotfix` at `b054229bb63cbe272a01099e90ae133dc55810fa`.

## Corrected root causes

- The 68dp Material BottomNavigationView and its default active indicator crowded icon and label on certain devices/locales. The 88dp bar now fixes icon size, symmetrical item padding, disabled horizontal translation, equal active/inactive label text, and uses selected-state tint without an indicator obscuring either icon or label.
- Prior Rewarded button was enabled even when Rewarded Ad hadn't loaded. After a failed SDK request, neither Rewarded nor Interstitial had bounded automatic retries or a meaningful error state. The new AdsManager initializes only after UMP permits requests and MobileAds initialization completes, reports separate loading/ready/no-fill/network/configuration states, and performs bounded retries without continuous requests.
- Banner attaches only when SDK and consent are ready; while loading, the host is INVISIBLE (measurable), then either VISIBLE on load or GONE on failure. The Home fragment retries a failed Banner at most three times with policy-specific backoff.
- Interstitial remains capped at two completed natural breaks and at least 90 seconds since the previous shown ad. Ads are reloaded when allowed and don't block a result transition if unavailable. The rewarded hour suppresses banners and interstitials, and granting it invalidates the cached interstitial.
- Home and Settings disable the Rewarded button until the SDK's onAdLoaded callback actually fires, show a localized status, and update when its state changes. UMP and Ads SDK log error code and domain only, without user identifiers, raw voice, or ad click automation.

## Limitations / external conditions

No-fill, ad serving restrictions, invalid Release placement IDs, account review, limited demand, and AdMob approval cannot be repaired by making the client retry more aggressively. Debug builds use Google's test IDs so their successful ad loading validates integration, not real-world release fill. Inspect the Logcat tags `TruthTestAds` and `TruthTestConsent` on the device, and compare with AdMob dashboard diagnostics before interpreting a release No Fill as a programming error.

## Release safeguards

- Application ID: `com.halahasneen.truthtest` (unchanged).
- Version: 2.0.2 / versionCode 4 (check against the highest code already uploaded to Play Console before deployment).
- No Billing, subscriptions, permanent purchase, or destructive preference migrations.
- GitHub Actions contract checks, lint, unit tests, Debug APK, Release APK, Release AAB, signing with the existing GitHub Secrets upload key, and signature verification.
- Do not merge into main or publish to Play automatically.

## Manual device QA remaining

Compare RTL labels under icons in Arabic, English, gesture navigation and three-button navigation, large fonts, Samsung/TECNO/Infinix. Use test ads in Debug to verify Banner/Interstitial/Rewarded callbacks; inspect production ad availability separately on an eligible device or via AdMob diagnostics. Compare the APK signing certificate against the Play Console **app signing certificate** before claiming the direct APK will update a store-installed copy.
