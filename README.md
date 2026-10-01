# Truth Test

Truth Test is a local-first Android social entertainment app built with Kotlin + XML Views.
It analyzes voice consistency features such as pitch variation, jitter, shimmer, pauses and energy to create a **playful score**.

> Truth Test is not a scientific lie detector. Voice patterns cannot determine whether a person is telling the truth, and results must never be used for consequential decisions.

## Current app

- Solo test, duel mode and group mode for 3–4 players.
- Custom questions and a deterministic daily challenge.
- 150 Arabic + 150 English local questions across six categories.
- Light / Medium / Bold question intensity.
- Real-time microphone waveform using `AudioRecord`.
- Local FFT-based voice analysis with a recording-quality gate.
- Raw audio is analyzed in memory and is not persisted.
- Local history, streaks, statistics and achievements.
- Arabic/English resources, RTL support and in-app language switching.
- Dark theme by default with optional light theme.
- Daily challenge and streak reminder notifications.
- Shareable local PNG result cards.
- 7-second vertical H.264 MP4 sharing with locally generated optional AAC sound.
- Four share themes.
- Monthly PDF report generated on-device.
- No account or backend is required for core functionality.

## Architecture

`data/` repositories + models → `audio/` recording/DSP → XML/ViewBinding UI → local share/report/notification engines.

The app keeps core user data on-device. Share media is created in app cache and exposed only through Android `FileProvider`.

## Truth Test 2.0

The 2.0 work is being built in controlled phases. Phase 1 establishes:

- Play Store application-ID protection;
- non-destructive local data migration contracts;
- strict local-data backup rules;
- a product-level section catalog for the future multi-section Home;
- shared design-system tokens;
- a stronger CI quality gate for every 2.0 branch.

See `docs/TRUTH_TEST_2_FOUNDATION.md`.
