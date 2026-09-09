# Truth Test

Offline-first Android entertainment app built with Kotlin + XML Views. It analyzes voice consistency (pitch, jitter, shimmer, pauses and energy) to create a **playful score** that is presented as a "Truth Test" result.

> Truth Test is not a scientific lie detector. Voice patterns cannot determine whether a person is telling the truth, and results must never be used for consequential decisions.

## Current MVP

- Solo test with six local question categories.
- Duel mode for two players on the same phone.
- Custom questions.
- Real-time microphone waveform using `AudioRecord`.
- Local FFT-based pitch estimation plus jitter/shimmer/pause/energy features.
- Recording quality gate: answers shorter than two seconds or too quiet are rejected instead of generating nonsense results.
- Raw audio is held only in memory during analysis and is not persisted.
- Local JSON history via Gson/SharedPreferences (no Room, no server).
- Local achievements.
- Deterministic daily challenge + local WorkManager notification.
- Arabic and English resources with RTL support and in-app language switching.
- Dark theme by default with optional light theme.
- Shareable PNG result card rendered locally with Canvas and FileProvider.
- GitHub Actions debug APK build using an installed Gradle version; no Gradle Wrapper JAR is committed.

## Architecture

`data/` repositories + models → `audio/` recording/DSP → XML/ViewBinding UI → local share/notification engines.

The app deliberately keeps all user test data on-device.

## Planned next steps

1. Expand both question banks and add content-quality review.
2. Add animated result reveal and more polished neon/pulse motion.
3. Add optional 6–10 second MP4 share export after choosing a maintained FFmpeg integration/fork that does not compromise build reliability.
4. Add group mode, share themes and monthly playful PDF report.
5. Add release signing and AAB workflow when Play Console publishing starts.
