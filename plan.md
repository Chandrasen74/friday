# FRIDAY Improvement Plan

Ideas compared against the feature set described in `easy-developer1/jarvis-phone` (that repo is README-only; no source). FRIDAY already covers local LiteRT-LM inference with bundled models, remote OpenAI-compatible API, speech input, TTS replies, sessions/export/import, markdown streaming, live metrics, model manager, diagnostics, and persona/memory.

## 1. First-launch bundled model copy progress (in progress)

**Problem:** v0.9.0 bundles ~1.4 GB of models in the APK. On first launch the app copies ~1.1 GB (Qwen3 + Granite int8/fp16) from assets to device storage before models are available. This can take several minutes and previously looked frozen/unresponsive.

**Fix implemented:**
- `LiteRtLmService.ensureBundledModels` now reports copy progress (file index/name, bytes copied/total, ~4 MB update interval) via callback.
- `ChatViewModel` exposes `bundledCopyProgress` during startup and clears it when copy completes or fails.
- UI shows a dedicated "Preparing F.R.I.D.A.Y" screen with per-file progress while bundled models are being copied (first launch only).

**Follow-ups:**
- Pre-check free storage before copy and show a clear error if insufficient (~1.1 GB needed in app storage).
- Consider showing overall progress across all pending files in addition to per-file progress.
- Verify behavior when the app is backgrounded/killed mid-copy (tmp files are cleaned; copy resumes for incomplete files on next launch).

## 2. TTS controls (rate, pitch, voice/language)

- Add Settings controls for speech rate, pitch, and preferred voice/language (e.g. prefer en-IN/en-US like jarvis-phone, but user-selectable).
- Persist preferences and apply in `TtsService`.
- Keep per-message speaker play/stop behavior.

## 3. Quick intents and deterministic shortcuts

- Handle common commands locally for speed and offline reliability: current time/date, open Settings/YouTube/URL, web search, simple math.
- Natural memory commands (e.g. "remember that...") that write to existing memory store without needing the LLM.
- Route: try deterministic intent parser first; fall back to model for everything else.

## 4. Encrypted API key storage

- Move Remote API key/secret config from plain prefs to `EncryptedSharedPreferences` or Keystore-backed storage.
- Keep non-secret config (base URL, model) portable as appropriate; document migration.

## 5. Wake word and hands-free mode ("Hey FRIDAY")

- Foreground service with visible notification and mic indicator while listening.
- Practical first step: continuous `SpeechRecognizer` restart loop (battery-heavy; document "Unrestricted battery" guidance).
- Longer-term: dedicated on-device wake-word engine (e.g. Porcupine/custom TFLite) for lower power and reliability.
- Requires careful permission/onboarding UX (microphone, notifications, battery optimization).

## 6. Dedicated voice mode UI

- Focused listening/speaking screen with animated orb/state indicator (idle, listening, thinking, speaking).
- Complements existing mic input and TTS reply toggles.

## 7. Reminders

- Local reminder storage and scheduled notifications.
- Simple deterministic parsing first ("remind me at 6 pm to..."); optional model-assisted parsing for natural language.
- UI to list/edit/delete reminders.

## 8. CI build workflow

- GitHub Actions workflow to build APK on push/tag.
- Note: bundled-model APK is very large (~1.5 GB debug), so CI build time and artifact storage/transfer need special handling (caching, or a variant without bundled large models for CI).

## Out of scope for now

- Gmail/Contacts/Calendar integrations (OAuth, sensitive scopes, account setup).
- Cloud sync backend.
- Custom neural wake-word model (evaluate after basic wake-word service).
