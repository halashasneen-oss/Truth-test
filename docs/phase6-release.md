# Truth Test 2.0 — Phase 6 Final Release Candidate

Baseline Phase 5 HEAD: `36789034c9941b28298d6b9586f22467057b29d5`

This phase freezes product scope and prepares the final 2.0 release candidate without merging to `main` or publishing to Google Play.

## Final product contract

- Google Play application ID remains `com.halahasneen.truthtest`.
- Version remains `versionCode 2 / versionName 2.0.0`, preserving the planned update identity.
- No paid purchase, subscription, BillingClient, premium entitlement, or `remove_ads` path exists.
- AdMob monetization is Banner + Interstitial + opt-in Rewarded only.
- One completed Rewarded ad grants exactly 60 minutes without Banner or Interstitial ads.
- Ad-free periods do not accumulate interstitial counters and stale interstitials are discarded when the reward is granted.
- Debug builds are locked to Google's demo ad units; Release builds contain the supplied production AdMob IDs.
- UMP remains the only consent/privacy-choice layer for ad requests.

## Release hardening

- Removed obsolete Feature Preview fragment/layout and stale “coming later” strings now that those experiences are live.
- Banner host is completely hidden when ads are suppressed, unavailable, or fail to load.
- Interstitial counters no longer advance during an active rewarded ad-free window.
- No destructive storage migration is introduced; schema remains version 4.
- FileProvider stays non-exported, cleartext traffic stays disabled, camera hardware remains optional, and the challenge deep-link parser remains constrained to the Truth Test payload format.

## CI release contract

The Phase 6 workflow now verifies before building:

- exact application ID and version;
- cleartext traffic disabled;
- no Billing source/dependency/legacy purchase identifiers;
- Google demo ad IDs in Debug configuration;
- supplied production Banner/Interstitial/Rewarded IDs in Release configuration.

The quality gate then runs:

- `lintDebug`
- `testDebugUnitTest`
- `assembleDebug`
- `assembleRelease`
- `bundleRelease`

For `truth-test-phase6`, CI also uploads release APK/AAB build outputs with SHA-256 checksums. These branch release outputs are release-candidate build artifacts; Google Play signing remains restricted to the established `main` signing workflow.

## Play Console handoff

Before production rollout in Google Play Console:

- declare that the app contains ads;
- do not declare in-app purchases, because the final build has none;
- use the published privacy-policy URL already updated for Google Mobile Ads and UMP;
- review Data Safety disclosures for data processed by Google Mobile Ads/UMP separately from the app's local voice/history data;
- review content rating for Bold/Couples question packs;
- App Access requires no login credentials;
- preserve the existing Play App Signing/upload-key identity.

## Release notes

### English
Truth Test 2.0 introduces a premium new design, social Party/Couples/Friends/Challenge modes, profiles and XP, achievements, Share Studio, local QR challenges, richer history and insights, and privacy-aware AdMob monetization. Voice-pattern scores remain entertainment-only and raw microphone audio is processed locally.

### العربية
يقدم Truth Test 2.0 تصميمًا جديدًا وتجارب الحفلة والأزواج والأصدقاء والتحديات، والملفات الشخصية وXP والإنجازات، واستوديو المشاركة وتحديات QR المحلية وسجلًا وإحصائيات أوسع، مع إعلانات AdMob تراعي خيارات الخصوصية. نتائج أنماط الصوت للترفيه فقط، والصوت الخام تتم معالجته محليًا.
