# Truth Test 2.0 — Phase 5 AdMob Monetization

Baseline Phase 4 HEAD: `aebb087e5d28863d769da73ec5e710eb1e3b3137`

Phase 5 monetizes Truth Test through AdMob only. There are no paid in-app purchases, subscriptions, premium ownership states, or Google Play Billing flows.

## SDKs

- Google Mobile Ads SDK 25.5.0.
- Google User Messaging Platform 4.0.0.
- Kotlin 2.3.21 + Android Gradle Plugin 8.13.2 + Gradle 8.13.
- Debug builds use Google's demo ad units only.
- Release builds use the supplied Truth Test AdMob app/banner/interstitial/rewarded units.

## Ad behavior

- One inline adaptive banner location on Home only.
- No banner inside recording, analysis, results, social sessions, Share Studio or QR flows.
- Interstitial eligibility is recorded only at a natural break when leaving a completed result.
- Interstitial requires at least 3 completed natural breaks and at least 4 minutes since the previous interstitial.
- Social session turns never trigger interstitials because their TestActivity is marked as an external session.
- If an interstitial is not ready, navigation continues immediately.

## Rewarded ad-free behavior

- Rewarded ads are opt-in only.
- The reward is granted exclusively from Google's reward callback.
- One completed rewarded ad grants exactly 60 minutes without banner or interstitial ads.
- Expiry is persisted locally as a timestamp, so closing/reopening the app does not reset the hour.
- There is no permanent Remove Ads purchase and no paid entitlement.

## Consent and privacy

- UMP requests updated consent information at app launch and shows a form only if required.
- Ads are initialized only when UMP reports that ads can be requested.
- Settings exposes Privacy Options when UMP says the entry point is required.
- Raw microphone audio is never supplied to the ads SDK.
- No Firebase Analytics, custom analytics, fingerprinting, advertising-ID storage, Billing SDK, or paid purchase flow is added.

## Compatibility

- Application ID remains `com.halahasneen.truthtest`.
- Schema marker remains 4 using the independent `truth_test_monetization` preference store.
- No destructive migration touches history, profiles, XP, achievements, sessions, Share Studio or settings.

## Quality gate

Phase 5 is complete only after lint, unit tests, Debug APK, Release APK and Release AAB all pass on `truth-test-phase5`.
