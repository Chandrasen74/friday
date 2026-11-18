# Screen Map: composable -> ViewModel state -> layout (SHOT FORGING Step 2)

Repo root prefix: `app/src/main/java/com/friday/assistant/`.
All strings/colors/styles verified against source with line refs; tokens in `assets/design_tokens.json`.
Font situation: only `orbitron_bold.ttf` exists as a file. UI text = `FontFamily.SansSerif`,
telemetry = `FontFamily.Monospace` (platform defaults, no files). Decision needed before Step 3.

## 1. Home / empty state (shots: 01_home)
- Composable: `ChatScreen` empty branch (session == null), `ui/ChatScreen.kt:700-778`; prompt card `PromptCard` `ui/ChatScreen.kt:1485-1553`.
- VM state: `UiState.input`, `UiState.isGenerating`, `modelStatus` (`ui/ChatViewModel.kt` UiState block); empty session means no ComposerBar (`ui/ChatScreen.kt:712-723`).
- Layout: Scaffold(topBar=TelemetryHeader) > Column(fillMaxSize, padding(top/bottom bars) + padding h/v 24) centered:
  96dp logo box (BreathingAura + friday_logo_blue 64dp) -> "F.R.I.D.A.Y" headlineMedium (TextPrimary) -> 6dp -> "READY FOR LOCAL INFERENCE" labelSmall (TextTertiary, mono) -> 28dp -> 4 PromptCards (ObsidianSurface r16, Row pad 16x14, Send icon 14dp cyan + labelLarge text) 10dp apart -> 8dp -> ActionButton "Create new chat" (primary).
- Styles: headlineMedium orbitron bold 24sp; kicker labelSmall mono 10.5sp ls1.4; card text labelLarge sans semibold 14sp TextPrimary.
- Strings: prompts `ui/ChatScreen.kt:727-730`, kicker `:759`, CTA `:777`.

## 2. Model list / drawer (shots: 02_models)
- Composable: `AppDrawer` (name at definition site; content `ui/ChatScreen.kt:481-553`), rows `SessionRow` `:556-602`, `StatusPill` `:605+`.
- VM state: `UiState.sessions`, `activeSessionId`, `modelStatus`.
- Layout: Column(maxWidth 320dp, bg surface, statusBars+navigationBars+displayCutout padding, pad 12x16):
  Row(logo 28dp + "F.R.I.D.A.Y" titleMedium orbitron cyan-primary + Add icon) -> 8dp -> StatusPill (999dp pill, Elevated bg, dot by status color) -> 12dp -> divider -> 8dp -> "Sessions" labelLarge onSurfaceVariant -> LazyColumn session rows (r10, selected = StarkCyanMuted bg + 3x28dp cyan bar, title bodyMedium + "N messages" labelSmall, DeleteOutline icon) -> divider -> Settings row (r10, Settings icon + "Settings" bodyMedium).
- Strings: `ui/ChatScreen.kt:504,521,550,589`; status default `data/ChatModels.kt:54`.
- NOTE: real filenames of available models for the "model chip names" come from the Settings model manager list (see 3): `Qwen3-0.6B`, `Granite 4.0 350M int8`, `Granite 4.0 350M fp16` (from `ui/screens/SettingsScreen.kt:356` preloaded copy).

## 3. Model picker (inside Settings) (supports 02_models)
- Composable: `SettingsScreen` NEURAL ENGINE section, `ui/screens/SettingsScreen.kt:268-446`; model rows `:374-410`.
- VM state: `availableModels` (paths), `modelPathInput`, `modelStatus`, `useGpu`, `inferenceMode`.
- Layout: LazyColumn > ObsidianSurface card containing: ModeChips row ("Local (LiteRT-LM)" | "Remote API", selected = cyan gradient pill) -> active line bodySmall -> "Remote API endpoint" field (12dp field r12) -> ActionButton row "Load model"/"Unload"/"Refresh" -> "Model manager" labelLarge -> preloaded copy bodySmall TextSecondary -> "Available models" labelLarge -> model rows (r10 Elevated bg, name bodySmall TextPrimary, path labelSmall mono TextTertiary ellipsized, right label "Selected" cyan / "Use" TextSecondary) -> "Use GPU backend" row + Switch -> "Loaded: <path>" mono line.
- Strings: `ui/screens/SettingsScreen.kt:280-288,341-356,362-369,403,418-433`.

## 4. Chat with composer (shots: 03_compose, 03_compose_typed)
- Composable: `ChatScreen` session branch `ui/ChatScreen.kt:700-870`, `MessageRow` `:875-1113`, `ComposerBar` `:1200-1305`, `ComposerActionButton` `:1315-1425`.
- VM state: `UiState.input`, `messages`, `isGenerating`.
- Layout: Scaffold(topBar=TelemetryHeader, bottomBar=ComposerBar) > LazyColumn(spacing 8dp, top spacer 8dp, row pad h12): user bubbles right (End), assistant bubbles left with 28dp r8 avatar box (SmartToy 16dp cyan, top pad 4dp, end pad 8dp).
  Bubble: Column(maxW 320dp, assistant minHeight 56dp, clip shape, bg gradient/border, pad 12x10).
  User: gradient #162836->#111C26, border CyanBorderGlow, shape 20/6/20/20, hint "Tap to edit and resend, long press to copy" labelSmall TextTertiary above text. Assistant: gradient Elevated->Surface, border SpecularHighlightSubtle, shape 6/20/20/20, header Row "F.R.I.D.A.Y" labelSmall orbitron bold cyan + Copy icon 14dp, 4dp, content bodyLarge TextPrimary.
  Composer: Column(bg background, navigationBars+ime padding) > Row(pad 12x10, align Bottom): capsule Box(weight 1f, r26, gradient #1A1A23->#131319, sheen top half, border 1.2dp SpecularHighlightSubtle idle / cyan 0.65 focused) with placeholder bodyMedium TextTertiary "Message F.R.I.D.A.Y..." (pad 18x14) + BasicTextField 14sp TextPrimary; 8dp; action 44dp target, pill 46dp r14: MIC (Elevated bg + Subtle border, Mic icon 20dp TextSecondary) when blank, SEND (cyan gradient, Send icon 20dp LegacyOnPrimaryDark) when text, STOP (crimson glass + 12dp square) when generating.
- Strings: composer `:1275`; bubbles `:990,1015`.

## 5. Streaming reply / thinking (shots: 04_stream_a/b/c)
- Composable: `MessageRow` showTyping branch (`ui/ChatScreen.kt:1021-1063`): AnimatedContent swaps bubble body to `TypingDots` (`ui/components/TypingDots.kt`) while streaming, plus "Streaming..." labelSmall TextSecondary below the bubble (`:1057`), and `PlasmaStreamBorder` glow on the bubble (`:1064-1068`, `ui/components/PlasmaStreamBorder.kt`).
- VM state: `message.isStreaming`, `state.isGenerating` (composer shows STOP, placeholder "Generating...").
- Content rule for a/b/c: one real reply text at three lengths (short/medium/full) from the same response; typed text must come from user-approved text (ask user) since no real reply text exists in captures yet.

## 6. Metrics footer (shot: 05_footer, also 09 relaunch)
- Composable: post-bubble metrics Row in `MessageRow`, `ui/ChatScreen.kt:1071-1111`.
- Layout: under assistant bubble, start pad 40dp, top 5dp: Row(r8 pill, clickable -> HUD): 4dp cyan dot (0.7 alpha) -> 6dp -> mono 10.5sp ls0.8 tnum TextTertiary "X.X tok/s  •  X.XXs" (formatFootprint `:1172-1183`) -> 5dp -> "›" 12sp TextTertiary.
- VM state: `SavedGenerationMetrics` from `GenerationTelemetryCache` (prefs `friday_generation_metrics`/`metrics_v1`); identical values before/after relaunch by design (proves persistence).

## 7. Telemetry HUD (shot: 06_hud)
- Composable: `TelemetryHudSheet` `ui/components/TelemetryHudSheet.kt:54-247`, `HudStatCard` `:251+`.
- VM state: `SavedGenerationMetrics` (tokensPerSecond, durationMs, backendLabel, estimatedTokens, modelName).
- Layout: ModalBottomSheet(container ObsidianBase, tonal 0, top corners 24, no dragHandle) > Column(pad h20, bottom 30): cyan handle 36x4 r2 @0.85 -> 16dp -> "INFERENCE TELEMETRY // DIAGNOSTICS" labelSmall mono ls1.4 TextTertiary -> 4dp -> "F.R.I.D.A.Y" headlineMedium orbitron TextPrimary -> 18dp -> 2x2 grid (rows spacedBy 10, cards r14 ObsidianSurface weight 1 each):
  DECODE SPEED (mono? value 16sp StarkCyanPrimary "%.1f tok/s"), TOTAL LATENCY (16sp TextPrimary formatDuration), COMPUTE BACKEND (12sp TextSecondary + 6dp dot Emerald/Amber), OUTPUT VOLUME (14sp TextPrimary "~N tokens") -> 10dp -> ACTIVE MODEL card (r14, pad 14x12): label labelSmall mono ls1.2 TextTertiary, 8dp, Row: model name mono 12sp TextSecondary ellipsized + quant chip (r6 Elevated bg Subtle border pad 8x3, mono 10sp ls0.8 StarkCyanPrimary).
- Strings/refs: `ui/components/TelemetryHudSheet.kt:90-195`; backend labels `ui/ChatScreen.kt:810-812`.

## 8. CPU performance tip card (shot: 07_tip)
- Composable: tip block inside `TelemetryHudSheet` `ui/components/TelemetryHudSheet.kt:199-244`.
- Gate: tokensPerSecond < 3.0f AND backendLabel == "LOCAL // CPU".
- Layout: 10dp below model card: Column(r14, Elevated bg, border Amber 0.35, pad 14): Row(6dp amber dot + "PERFORMANCE TIP" labelSmall mono ls1.2 Amber) -> 8dp -> body (bodySmall lh17 Default family TextSecondary, 2 lines) -> 12dp -> ActionButton "Open Settings" (primary cyan).
- String: `ui/components/TelemetryHudSheet.kt:228-229` (exact text in design_tokens.json).

## 9. Settings (shot: 08_settings)
- Composable: `SettingsScreen` MAIN page `ui/screens/SettingsScreen.kt:236-532`; top bar `SettingsTopBar` `:539-576`; chevron rows `:596-637`; rows/cards per tokens file.
- VM state: `inferenceMode`, `modelStatus`, `availableModels`, `speakReplies`, `diagnostics`, endpoint config.
- Layout: SettingsTopBar(SETTINGS: ArrowBack icon + titleMedium orbitron ls1.5 TextPrimary over ObsidianSurfaceGlass + 1dp SpecularHighlightStrong seam) > LazyColumn: [status pill] -> "NEURAL ENGINE" header (mono 11sp ls1.6 TextTertiary) -> engine card (ModeChips, model manager...) -> "VOICE & SPEECH" -> speak card (row pad 16, title bodyLarge + subtitle bodySmall TextSecondary + Switch) -> "MEMORY & PERSONA" -> chevron row "Persona & memory" -> "STORAGE & DIAGNOSTICS" -> chevron row "Chats & storage" -> Diagnostics card (titleSmall title + subtitle bodySmall + mono diagnostics block r10 Elevated + "Copy diagnostics" ActionButton secondary).
- Strings: `ui/screens/SettingsScreen.kt:258,269-497` (full list in tokens).

## 10. Diagnostics / about (shot: 10_about)
- Composable: Diagnostics card `ui/screens/SettingsScreen.kt:491-528`; format `formatDiagnostics` `:820-832`.
- VM state: `DiagnosticsInfo` (device, androidVersion, abi, appVersion, inferenceMode, modelFile, modelLoadMs, lastGenerationMs, lastGenerationError, sessionCount, messageCount, ttsReady) built in `ChatViewModel`.
- Layout: ObsidianSurface card > Column(pad 16): "Diagnostics" titleSmall TextPrimary -> 6dp -> subtitle bodySmall TextSecondary -> 10dp -> mono block (r10, Elevated, border Subtle, pad 12, bodySmall mono TextSecondary, 11 lines) -> 10dp -> "Copy diagnostics" ActionButton secondary.
- Values NOT derivable from source: device/OS/ABI/appVersion/session counts/last gen timings = PLATFORM DATA, placeholders unless user supplies a real capture.

## 11. Relaunch before/after (shots: 09_relaunch_before, 09_relaunch_after)
- Both are chat-with-footer states (see 4+6); identical `SavedGenerationMetrics` values in both frames (that is the persistence claim the beat makes).
- Source proof: `ui/telemetry/GenerationTelemetryCache.kt` (prefs-backed LRU, loaded on cold start).

## 12. Boot / logo (film asset, not a shot)
- `LogoIntro` `ui/intro/LogoIntro.kt`: 160dp emblem in 216dp orbital assembly, 288dp `hudReticle`, wordmark 28sp at y=184dp, telemetry strip labelSmall mono + 5dp emerald dot, "ON-DEVICE NEURAL CORE // READY" `:68`; dock target = header logo 28dp.

## Cannot be derived from source (needs real capture or user data)
1. Any real measured metric values (tok/s, latency, tokens, TTFT, load ms) -> placeholders until user supplies real run values or a capture.
2. Real reply text for streaming beats -> need user-provided text from a real run.
3. `DiagnosticsInfo` platform values (device/OS/ABI/appVersion/sessions/TTS) -> need real capture (10_about).
4. Exact Android status bar rendering (clock, icons) -> platform UI; will draw a faithful generic Pixel-style status bar unless a capture is supplied.
5. Available model filenames (`path.substringAfterLast('/')`) -> names like `qwen3-0.6b...litertlm` are built at runtime; the preloaded marketing names are in source (`:356`). Rows will use source-derived names from the app's model registry if present; otherwise ask.
