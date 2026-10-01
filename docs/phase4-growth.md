# Truth Test 2.0 — Phase 4 Viral Growth & Sharing

Baseline Phase 3 HEAD: `a546f4d20f15aee7d41c58720c420a965a456e14`

Phase 4 activates the offline-first sharing and challenge loop without adding ads, billing, accounts, analytics or a backend.

## Implemented scope

- Share Studio is a real product section instead of a preview.
- Recent saved results and completed social sessions can be selected without replaying.
- Six visual templates: Minimal, Neon, Party, Couples, Challenge and Dark Premium.
- Three image formats: 1:1 square, 4:5 feed and 9:16 story.
- Preview is rendered before export.
- Optional player/mode/QR details and three localized CTA choices.
- Existing video renderer is reused and extended with Studio template colors, sound choice and custom CTA.
- Session summaries can be shared using top score plus final ranking averages.
- Lightweight share history stores metadata only; exported media remains cache files.
- Share cache cleanup removes stale files after 24 hours.
- Versioned Truth Test challenge payload with strict field validation, maximum sizes and SHA-256 integrity checksum.
- QR challenge cards are generated completely on-device.
- In-app QR scanning accepts only verified Truth Test challenge links.
- Custom `truthtest://challenge` deep links open a dedicated verification screen.
- Imported challenge mode, pack, challenge type and optional first question feed into the existing Phase 3 local Session Setup.
- No arbitrary URL is opened from QR content.
- Arabic and English Studio strings are included; bitmap text uses Android StaticLayout for bidi/RTL shaping.

## Privacy and security

- No media, result, profile, audio or challenge is uploaded.
- QR scanning requests camera access only when the scanner is used.
- No facial analysis, lie detection from camera, tracking, Advertising ID or fingerprinting is introduced.
- Reaction Cam remains intentionally out of scope because it was optional in the Phase 4 plan; no face/video capture pipeline is added.
- Raw microphone audio remains local and is not persisted.
- Share files are provided through the existing non-exported FileProvider.

## Compatibility

- Application ID remains `com.halahasneen.truthtest`.
- Existing history/settings/achievements/profiles/sessions remain untouched.
- Schema marker advances to 3 without destructive rewrites.
- New Share Studio metadata uses additive keys in the existing `truth_test_share` preferences.
- Existing Phase 2 result image/video share paths remain source-compatible through default parameters.

## Quality gate

Phase 4 is complete only after Android Lint, unit tests, Debug APK, Release APK and Release AAB all pass on `truth-test-phase4`.
