# Truth Test 2.0.3: final Custom Navigation and AdMob diagnostics

Source: approved `truth-test-admob-ui-fix` HEAD `ed58907cf63a6968e7d3536a28024283ba4eb07b`.
Working branch: `truth-test-custom-nav-admob-fix`. Never merge into main or publish automatically.

## Real-device navigation regression

The Material BottomNavigationView icon/label inner measurements differ across some OEMs and font configurations.
It is fully replaced with an explicit four-column horizontal bar; each equally weighted item contains
a 54x36dp icon holder, 24dp icon, separate label below with 7dp gap and a min-height allowing
large system fonts and two-line Arabic labels. RTL order is derived from locale; the root remains
inside SafeArea so gesture-navigation and three-button system bar insets cannot cover controls.
Active state uses a subtle tint and icon-only rounded highlight (not an overlay above text).
Selected tab survives Activity recreation and secondary screens.

## Confirmed source configuration

Release app ID: `ca-app-pub-5961173995415325~9185872171`.
Release banner/interstitial/rewarded IDs: `3434852042`, `7149812330`, `5836730662`
with common `ca-app-pub-5961173995415325/` prefix.
Debug uses Google test app and ad unit IDs. Manifest declares `com.google.android.gms.ads.APPLICATION_ID`
through the build variant's `admob_app_id` string resource. No Billing or premium purchases are added.

The Google Play public developer website points at `halashasneen-oss.github.io`;
the website's public repository contains `app-ads.txt` with
`google.com, pub-5961173995415325, DIRECT, f08c47fec0942fa0`.
The existence of this file does not establish that AdMob has approved the app or
that inventory is available. Confirm the account-side verification status in AdMob.

## Diagnostics and testing

- Debug-only section in Settings shows consent state, SDK initialization,
  latest Banner / Interstitial / Rewarded request outcome, and allows refresh.
- Google's official `MobileAds.openAdInspector` entry point is wired only for Debug;
  the device may need to be registered as an AdMob test device.
- Logcat tags: `TruthTestAds` and `TruthTestConsent`. Report code and domain
  without audio, user content, ad-click simulation, or identifiers.
- Rewarded is available only once `onAdLoaded` fires, and the reward is granted
  only from `onUserEarnedReward`; the one-hour timer remains local and persistent.
- Interstitial after two completed natural breaks with 90 seconds between shown ads;
  no interruptions during recording or social sessions; failure doesn't block navigation.
- Banner uses anchored adaptive sizing and stays hidden if no request is allowed or no
  successful response arrives. Bounded retries, no request flooding.

## Manual QA gates

Use Debug build to test official Google test IDs on a networked Google Play Services device,
including Banner, Interstitial after two results, and Rewarded. Open Ad Inspector and
capture its adapter and response reports. A Debug test ad load does not prove Release
fill: compare device logs with AdMob account verification, serving restrictions, requests,
match rates and app-ads.txt status. The public Play listing's Data Safety declaration
must be re-reviewed before release because it currently states no collection or sharing;
Google Mobile Ads data handling may require updated disclosure.

Validate navigation visually on both pictured phones, portrait Arabic/English,
small display sizes, large font scale, gesture and three-button navigation.

## Release

Proposed `versionCode = 5`, `versionName = 2.0.3`.
The highest version code in Play Console must be checked before publishing; current
public listing does not expose it. Sign APK + AAB using *existing* GitHub Secrets,
verify AAB with jarsigner and APK with apksigner, record artifact checksums, and
compare upload-certificate fingerprint with previous releases.
An upload-key-signed APK is not necessarily install-compatible with Google Play
App Signing's distributed APK. No automatic Play rollout.
