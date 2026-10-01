# Truth Test 2.0 Foundation

Phase 1 establishes guardrails for a major Play Store update without changing the core user experience yet.

## Release identity

- Published application ID: `com.halahasneen.truthtest`
- Kotlin/Android namespace: `com.halashasneen.truthtest`
- The application ID is intentionally different from the namespace and must not be "corrected".
- Phase 1 starts the 2.0 line at version code 2 / version name 2.0.0.

## Local-first boundary

Core product data remains on-device:

- voice samples are analyzed in memory and are not persisted;
- history, achievements, settings, question state and share preferences use SharedPreferences;
- no account or backend is required;
- Android backup/data-transfer rules exclude app data so the privacy promise remains local;
- future network access is reserved for optional external services such as AdMob and opening Play/social apps.

## Stable storage contract

The preference file names used by the published version are centralized in `AppStorageContract`.
Future changes must use non-destructive migrations through `AppUpgradeManager`.

Never rename or remove existing preference files or keys without an explicit migration.

## Truth Test 2.0 section vocabulary

1. Truth Test
2. Party
3. Couples
4. Friends
5. Daily Truth
6. Challenges
7. Share Studio
8. Insights
9. Achievements

`ExperienceCatalog` is the canonical ordered list. Phase 2 will render these concepts into the new Home/navigation experience.

## Design foundation

`dimens.xml` and semantic `tt_*` color aliases are the first design-system tokens.
Phase 2 can replace hard-coded spacing and visual values incrementally without breaking legacy screens.

## Quality gate

Every `truth-test-v2-*` branch must pass:

- repository hygiene check;
- Android lint;
- unit tests;
- debug APK compilation;
- release AAB compilation.

Feature branches compile but do not publish APK/AAB artifacts or use production signing keys.
Production signing remains restricted to `main`.
