# JARVIS — Android Personal AI Assistant

A standalone Kotlin Android application inspired by the Iron Man AI assistant.  
Local-first memory, speech recognition & TTS, optional online AI (OpenAI-compatible APIs), wake-word foreground service, and a futuristic dark UI.

**This is a real Android project** — not a website mock or chatbot demo. You build and install an APK on your phone.

---

## Local AI (runs on your phone — no cloud required)

This project is **local-first**.

### What works immediately after install (no model download)

- Time / date
- Greetings and JARVIS personality
- Long-term memory save & recall ("remember that…", "what do you remember")
- Simple math
- Open Settings / YouTube / web search
- Speech recognition + spoken replies
- Full offline conversation history

### Optional: real on-device generative model (MediaPipe)

For free-form conversation **without any API key**:

1. Download a supported Gemma model (e.g. Gemma-3 1B int4 `.task` file) from Google / Hugging Face LiteRT community.
2. On the phone, copy the file into the app’s private models folder. Easiest paths the app checks:
   - `Android/data/com.jarvis.assistant/files/models/model.task`
   - or use a file manager after first launch to place `model.task` under the app’s files directory.
3. Restart JARVIS. Status will show the model is loaded.
4. Speak normally — replies come from the on-device model.

**Requirements:** 64-bit ARM phone, roughly 4 GB+ RAM recommended. Model size is typically 0.5–2 GB. The APK itself stays small; the model is user-supplied.

Online API keys remain optional for stronger models when you want them.

---

## Features implemented in this release

| Area | Status | Notes |
|------|--------|-------|
| Kotlin + Jetpack Compose UI | ✅ | Dark navy / cyan theme, animated voice orb |
| Speech-to-text (system) | ✅ | Push-to-talk + continuous mode for wake word |
| Text-to-speech | ✅ | Prefers `en-IN` then `en-US`; rate & pitch controls |
| Wake word "Hey JARVIS" | ✅ | Foreground service + visible notification |
| Local Room database | ✅ | Conversations, messages, memories, reminders |
| Long-term memory UI | ✅ | View / add / delete; used in AI context |
| Online AI (OpenAI-compatible) | ✅ | Configurable base URL, model, encrypted API key |
| Local fallback replies | ✅ | Time, greetings, basic intents when offline / no key |
| Phone intents | ✅ | Settings, browser search, YouTube, etc. |
| Secure API key storage | ✅ | EncryptedSharedPreferences |
| GitHub Actions APK build | ✅ | Workflow included |
| Gmail / Contacts / Calendar | 🔶 Structure | Permissions + documented OAuth setup; full clients need your Google Cloud credentials |
| Cloud sync backend | ❌ Not included | Local-only by design; flag prepared for future encrypted backend |
| Custom neural wake-word model | ❌ | Uses system SpeechRecognizer (higher battery use) |

Be honest: continuous system speech recognition is **not** a low-power always-on solution. For production always-listening you would integrate a dedicated on-device wake-word engine (e.g. open-source Porcupine / custom TFLite). This app uses the practical Android approach with a foreground notification.

---

## Requirements

- Android 8.0 (API 26) or higher  
- Microphone permission  
- (Optional) Internet + API key for advanced answers  
- For building: JDK 17, Android SDK 35, Gradle 8.9+

---

## Build the APK (recommended: GitHub Actions from your phone)

1. Create a new GitHub repository.
2. Upload **all** files from this project (or push via git).
3. On GitHub → **Actions** → **Build JARVIS APK** → **Run workflow**.
4. When finished, download the artifact `jarvis-debug-apk`.
5. Transfer the APK to your phone and install (enable “Install from unknown sources” / “Install unknown apps” for your file manager).

### Local build (on a computer)

```bash
# Generate the Gradle wrapper (first time)
gradle wrapper --gradle-version 8.9

# Build
./gradlew assembleDebug

# APK path:
# app/build/outputs/apk/debug/app-debug.apk
```

---

## First-run setup on the phone

1. Install the APK and open JARVIS.
2. Grant **Microphone** and **Notifications** when asked.
3. (Recommended) Android Settings → Apps → JARVIS → Battery → **Unrestricted** (so the wake service is not killed).
4. Open **Settings → AI Configuration** and enter an API key if you want online intelligence:
   - OpenAI, OpenRouter, a local LLM server that speaks the OpenAI Chat Completions format, etc.
5. Optionally enable **Wake Word**. A persistent notification will appear while listening.
6. Tap the orb or say **“Hey JARVIS”** and speak naturally.

---

## Personality & voice

- Addresses you as “boss”.
- Calm, professional, occasionally witty.
- Spoken responses via device TTS (English India preferred when installed).
- No fake custom voice is claimed; whatever voices your phone’s TTS engine provides are used.

---

## Memory

- Stored locally in Room.
- “Remember that …” commands and the Memory screen both save facts.
- Relevant memories are injected into the system prompt for online AI calls.
- You can view, edit (via delete + re-add), and clear history.

**Do not store passwords or highly sensitive secrets in ordinary memory notes.**

---

## Gmail / Google integrations (setup required)

To enable Gmail:

1. Create a project in [Google Cloud Console](https://console.cloud.google.com/).
2. Enable **Gmail API**.
3. Create an **OAuth 2.0 Client ID** of type **Android**.
4. Package name: `com.jarvis.assistant`
5. Add your signing certificate SHA-1 (debug keystore SHA-1 for testing).
6. Restrict scopes to what you need (`gmail.readonly`, `gmail.compose`, …).
7. Wire the client ID into the integration module (code structure is prepared; complete the Google Sign-In flow with the official libraries).

The app **never** asks for or stores your Google password.

---

## Privacy summary

- Microphone use is visible (notification when service is on; UI indicator when listening).
- No silent recording.
- No hardcoded API keys.
- Local database stays on device.
- Online AI: only the text you send (plus memory context) goes to the endpoint **you** configured.
- Cloud sync is **off** by default and has no backend in this release.

---

## Project structure (high level)

```
app/src/main/java/com/jarvis/assistant/
├── MainActivity.kt
├── JarvisApplication.kt
├── ai/                 # OpenAI-compatible client + local fallback
├── data/
│   ├── db/             # Room database + DAOs
│   ├── model/
│   └── repository/     # Memory + encrypted preferences
├── service/            # Foreground wake service, receivers
├── voice/              # SpeechRecognizer + TTS managers
├── ui/
│   ├── theme/
│   ├── navigation/
│   ├── screens/        # Home, Voice, History, Memory, Settings, …
│   └── viewmodel/
└── integration/        # Place for Gmail / Contacts / Calendar clients
```

---

## Known limitations (honest)

1. **Wake word** relies on continuous `SpeechRecognizer` restarts → higher battery use and occasional missed detections. Manufacturer aggressive battery savers can still kill the service.
2. **Gmail / full Calendar write** require you to finish OAuth client configuration; the UI documents the path.
3. **No cloud memory server** is shipped. Implementing one would mean a real backend + auth + encryption — out of scope for a pure client release.
4. Offline speech recognition quality depends on whether the user has installed offline language packs for the system recognizer.
5. Complex natural-language reminder scheduling is best when an online model is configured; local parsing is basic.

---

## License & responsibility

You are free to use and modify this project for personal use.  
You are responsible for:

- Any API costs from the keys you enter.
- Complying with Google API Terms if you enable Gmail.
- Not using the assistant for illegal or harmful purposes.

---

## Quick start phrase examples

- “Hey JARVIS” → “Yes, boss?”
- “What time is it?”
- “Remember that my birthday is March 15”
- “Search for quantum computing”
- “Open settings”
- “Open YouTube”

Enjoy your personal JARVIS, boss.
