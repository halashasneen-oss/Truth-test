# Truth Test 2.0.1 — Final Hotfix / Release Candidate

Parent: approved Phase 6 `b4b8a6793e0db81fdf3e1755066cc7cfa2af822e`. Branch: `truth-test-final-hotfix`.

## User-tested defects addressed

- Edge-to-edge and navigation-bar system insets are applied to Main, Test, Social Session, QR Import and Onboarding roots, keeping CTA and navigation clear of gesture / 3-button navigation areas.
- Home banner was using unbounded **inline adaptive** dimensions inside a scroll view; now uses **anchored adaptive** sizing based on available width. Hide the full ad host on no fill, active reward or loading failure.
- Rewarded one-hour ad-free card is now prominent on Home, persists expiry, updates countdown, and restores banners only on expiry. The original Settings entry remains available.
- Interstitial attempts are eligible on natural transitions after two completed results with a 90-second minimum interval since the last shown ad. Loading/no-fill/consent failure never blocks navigation. None appear during voice recording, analysis or social turns, or during an active rewarded hour.
- Single-player Truth Test now offers 1/5/10 questions; users see progress and Next Question after each score, can finish early, and see average/best/completed summary on final result. Daily/Custom/Duel/Group and imported social turns remain their prior single-question/turn designs.
- Next-question selection excludes already-used question IDs, first preferring current category/intensity, then category, then another unused question if needed. Scores are committed once at completion rather than again when navigating.
- Localized Arabic and English share captions dynamically describe the question/score with light-hearted outcome-dependent wording, encourage a challenge, and contain the exact published Play link. Result image/video Share Sheets and Share Studio result/session/QR Share Sheets use these captions. Some destination apps may omit image/video captions by platform design; no claim of cross-app enforcement is made.
- Fresh hotfix version: applicationId stays `com.halahasneen.truthtest`, versionCode 3, versionName 2.0.1. Check the *highest existing Play Console versionCode* before uploading; this information is not accessible from project source alone.

## Quality and signing

GitHub Actions runs contract audit, lint, unit tests, Debug APK, Release APK and Release AAB. On `truth-test-final-hotfix`, the workflow uses the existing protected `UPLOAD_*` secrets from the historically successful main signing workflow (run #42) to generate signed APK and AAB; verifies them with apksigner/jarsigner and publishes separate artifact archives plus checksums. No keystore or password is added to Git.

**Important:** a Google Play App Signing *upload key* is not always the same as the Play **app signing key**. The signed APK may not directly update an APK downloaded from Google Play if Play re-signs distributed APKs. Confirm the Play Console app-signing fingerprint and do a real update-over-installed test before claiming compatibility.

## Test coverage / limitations

Automated: non-repeating selection, average score, tone and Play URL contract, ad threshold/cooldown, rewarded suppression, current and legacy core unit tests, lint and all three Gradle builds.

Still requires physical-device/manual QA: Samsung/TECNO/Infinix gesture and 3-button navigation, constrained screens, actual ad fill and UMP prompts, mic/camera permissions, sharing recipients, signed APK install/update compatibility, and a test-track Play delivery.

## Additional improvements proposed, not auto-added

- Optional share-as-text action for receivers that ignore media captions.
- Accessibility / TalkBack pass on OEM devices with maximum font scaling.
- Instrumented device farm screenshot tests for every locale, theme and navigation mode.
