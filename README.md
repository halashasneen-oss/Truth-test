# Truth Test

Offline-first Android entertainment app built with Kotlin + XML Views. It analyzes voice consistency (pitch, jitter, shimmer, pauses and energy) to create a **playful score** that is presented as a "Truth Test" result.

> Truth Test is not a scientific lie detector. Voice patterns cannot determine whether a person is telling the truth, and results must never be used for consequential decisions.

## Current MVP

- Solo test with six local question categories.
- Duel mode for two players on the same phone.
- Custom questions.
- 120 local questions across Arabic and English, with immediate-repeat avoidance per category.
- Real-time microphone waveform using `AudioRecord`.
- Local FFT-based pitch estimation plus jitter/shimmer/pause/energy features.
- Recording quality gate: answers shorter than two seconds or too quiet are rejected instead of generating nonsense results.
- Raw audio is held only in memory during analysis and is not persisted.
- Local JSON history via Gson/SharedPreferences (no Room, no server).
- Local achievements including a three-day streak calculated from history.
- Deterministic daily challenge + local WorkManager notification.
- Arabic and English resources with RTL support and in-app language switching.
- Dark theme by default with optional light theme.
- Shareable PNG result card rendered locally with Canvas and FileProvider.
- Optional 7-second vertical MP4/H.264 result video rendered fully on-device using Android `MediaCodec` + `MediaMuxer` + EGL/OpenGL ES. No server or FFmpeg binary is required.
- Video sharing animates the real recorded waveform, score reveal, question and final result; duel exports show both player scores.
- Four selectable share themes shared by PNG and MP4 export: Neon Purple, Cyber Cyan, Romantic Pink and Gold Challenge. The last selected style is remembered locally.
- GitHub Actions debug APK build using an installed Gradle version; no Gradle Wrapper JAR is committed.

## Architecture

`data/` repositories + models → `audio/` recording/DSP → XML/ViewBinding UI → local share/notification engines.

The app deliberately keeps all user test data on-device. Share media is generated only inside the app cache and handed to Android's share sheet through `FileProvider`.

## Video export choice

The original FFmpegKit project was retired and its historical Android binaries were removed. To keep clean CI builds reliable and avoid a large native dependency, the MVP video exporter uses Android platform APIs (`MediaCodec`, `MediaMuxer`, EGL/OpenGL ES) instead. The output is a silent 720×1280 H.264 MP4 intended for reels/stories and general social sharing.

## Planned next steps

1. Device-test video encoding on a wider range of Android chipsets and add codec fallback if needed.
2. Add optional sound/music that does not require uploading user audio.
3. Add group mode for up to four players.
4. Add monthly playful PDF report.
5. Add release signing and AAB workflow when Play Console publishing starts.
