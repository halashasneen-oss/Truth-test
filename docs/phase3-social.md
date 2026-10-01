# Truth Test 2.0 — Phase 3 Social Game Systems

Baseline Phase 2 HEAD: `27c60a83e2c9efd33a73b5c29a6a1c1e6572f11d`

Phase 3 activates the social game systems planned after the global UI transformation.

## Scope

- Local Player Profiles with avatars, XP, levels, games, best and average score.
- Local resumable social sessions.
- Couples, Friends, Party and Challenges as real playable modes.
- Reuse of the existing microphone recorder, Truth Engine and VoiceAnalyzer for every social turn.
- Six local question-pack types: Classic, Funny, Bold, Deep, Couples and Friends.
- 60 additional English + 60 additional Arabic social questions with stable cross-language IDs.
- Session summaries with winner/tie, rankings, top moment, earned XP and newly unlocked achievement.
- XP progression for existing solo/daily play, social turns, session completion and newly unlocked achievements.
- Daily XP bonus only on the first Daily Truth completion of the day.
- Seven new achievements for Couples, Friends, Party, Daily, Challenges, 500 XP and five completed social sessions.
- Social modes added to History filters.
- Home progress card and resumable-session card.
- More screen profile progression and profile management.

## Compatibility

- Application ID remains `com.halahasneen.truthtest`.
- Legacy SharedPreferences names and keys remain unchanged.
- Phase 3 only adds `truth_test_profiles` and `truth_test_social`.
- Data schema marker advances to 2 without rewriting legacy history/settings/achievements/share data.
- No account, backend, cloud sync, AdMob, QR/referral system, reaction camera or Phase 4 viral system is added.
- Raw audio remains local and is not persisted.

## Quality gate

Phase 3 is complete only after Android Lint, JVM unit tests, Debug APK, Release APK and Release AAB all pass on `truth-test-phase3`.
