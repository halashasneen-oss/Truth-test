# Truth Test 2.0 — Phase 1 Foundation

Baseline: `fd8ebd6611716589aa018cc8582ab18c9c775807`

Phase 1 establishes the upgrade contract for the already-published Google Play app.

## Release invariants

- Google Play application ID remains `com.halahasneen.truthtest`.
- Existing SharedPreferences file names and important keys remain unchanged.
- Existing local history, achievements, settings, question preferences and share preferences are preserved.
- Core product sections are local-first and require no account.
- Raw audio remains an in-memory analysis input and is not persisted by the app.
- App backup/data extraction is disabled so private local activity is not copied to cloud backup.
- Cleartext network traffic is disabled.
- Signing is never performed on the development phase branch.

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

Phase 1 intentionally does not replace the current user-facing screens. The visual redesign is Phase 2.

## Quality gate

The phase branch must pass:

- Android Lint (debug)
- JVM unit tests
- Debug APK compilation
- Release APK compilation
- Release AAB compilation

The production upload key is only used on a direct push to `main`.
