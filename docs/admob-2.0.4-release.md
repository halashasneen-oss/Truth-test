# Truth Test 2.0.4 — AdMob IDs & App Open release

Branch: truth-test-admob-v204 (based on custom nav approved HEAD 81f7a4178cfc516c47e2c9cbb3d22d665d1e326e).

- Preserve Google Play applicationId com.halahasneen.truthtest, advance versionCode to 6 and versionName to 2.0.4.
- Production app ID: ca-app-pub-5961173995415325~9450642239.
- Production banner: ca-app-pub-5961173995415325/6736848981.
- Production interstitial: ca-app-pub-5961173995415325/3452299387.
- Production rewarded: ca-app-pub-5961173995415325/9826136046.
- Production app-open: ca-app-pub-5961173995415325/5080066660.
- Debug uses only official Google test IDs, including app-open 9257395921.
- App-open displays exclusively over the SplashActivity after UMP approval and loaded ad, with a 7-second startup timeout. Skipped during onboarding, ad-free reward, or within 30 minutes of prior app-open and 2 minutes after any fullscreen ad; never interrupts app content.
- Interstitial after every two completed natural breaks, subject to 60-second interstitial interval and 2-minute cooldown after a *different* fullscreen ad. Triggers at solo result -> next/home and at completed social session -> replay/home. Never during microphone recording, analysis or active social turns.
- Rewarded ad-free hour suppresses all formats and existing setting is retained.
- SDK/consent/network/no-fill still control actual production delivery. Test-unit emulator success cannot verify new production AdMob IDs or account approval.
- GitHub CI builds lint, unit tests, connected emulator (Google test units), signed APK and signed Play AAB using existing upload secrets. Check Play Console's highest versionCode, upload-key fingerprint, AdMob account/app verification, app-ads.txt and Data Safety before rolling out. No Play Store publication is automatic.
