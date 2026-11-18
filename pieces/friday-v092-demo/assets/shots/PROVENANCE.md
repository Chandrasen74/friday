# PROVENANCE.md: Step 7 shot provenance

Delivery rule: every frame in `assets/shots/` is an HTML/CSS rebuild of the F.R.I.D.A.Y
Compose source, rendered headless in Chromium at 1080x2400 (DPR 1). NONE of it is
screen-recorded footage. The finished reel must never be described as a screen capture,
and the delivery note must state that the shots are rebuilt from source.

Real assets used: the app logo (`app/src/main/res/drawable/friday_logo_blue.png`, copied
to `shots_src/assets/friday-logo.png`) and the app's bundled display font
(`app/src/main/res/font/orbitron_bold.ttf`).

## Font substitution (documented)
The app uses three families: FridayDisplayFont = bundled Orbitron (Type.kt:11-15),
FontFamily.SansSerif for UI text, and FontFamily.Monospace for telemetry (platform
defaults, no bundled files). Per the two-typeface film rule, Roboto (@fontsource 5.2.5,
Apache-2.0, latin woff2, 400/500/600/700) is bundled as the UI face, and every Monospace
string (metrics footer, HUD labels, model rows, diagnostics) renders in Roboto with
`font-variant-numeric: tabular-nums` + `font-feature-settings: "tnum"`. Orbitron usage is
verbatim from the app; the Roboto substitution applies to the two platform families only.

## Per-shot provenance
Source prefix: `app/src/main/java/com/friday/assistant/`.

| Shot | Rebuilt from | Placeholders |
|---|---|---|
| 01_home | ui/ChatScreen.kt:700-778 (empty branch), :1485-1553 (PromptCard), :759 (kicker), :727-730 (prompts), :777 (CTA); ui/components/TelemetryHeader.kt:65-149; ui/components/ObsidianSurface.kt:43; ui/screens/SettingsScreen.kt:769-818 (ActionButton) | status bar only |
| 02_models | ui/ChatScreen.kt:481-602 (drawer + session rows), :605-630 (StatusPill), :504/:521/:550/:589 (strings); data/ChatModels.kt:54 (default status text) | status text + dot state, message count |
| 03_compose | ui/ChatScreen.kt:700-870 (scaffold), :875-1113 (MessageRow), :1200-1425 (ComposerBar); ui/ChatViewModel.kt:459 (greeting) | status bar only |
| 03_compose_typed | + ui/ChatScreen.kt:1210-1221 (focused border), :1322-1331 (SEND state), :1275-1292 (field + caret) | status bar only |
| 04_stream_a | ui/ChatScreen.kt:1021-1068 (typing branch), :1057 (Streaming...); ui/components/TypingDots.kt:28-82; ui/components/PlasmaStreamBorder.kt:54-122 | reply text (one response backs a/b/c) |
| 04_stream_b | same sources as 04_stream_a | reply text (partial frame) |
| 04_stream_c | same sources as 04_stream_a | reply text (near-full frame) |
| 05_footer | ui/ChatScreen.kt:1071-1111 (footer row), :1172-1183 (formatFootprint) | footer tok/s + duration, reply text |
| 06_hud | ui/components/TelemetryHudSheet.kt:54-198 (sheet), :251-296 (HudStatCard); backend labels ui/ChatScreen.kt:809-813 | all four stat values, model filename + quant tag |
| 07_tip | ui/components/TelemetryHudSheet.kt:199-244 (tip card + gate) | decode speed driving the gate + all HUD values |
| 08_settings | ui/screens/SettingsScreen.kt:236-446 (NEURAL ENGINE), :539-576 (top bar), :641-659 (status pill), :674-703 (ModeChip), :707-768 (SettingsTextField) | model filenames + paths, selection state, pill text/state |
| 09_relaunch_before | 05_footer sources + ui/telemetry/GenerationTelemetryCache.kt (persistence) | footer values (identical to after) |
| 09_relaunch_after | same as 09_relaunch_before | footer values (identical to before) |
| 10_about | ui/screens/SettingsScreen.kt:483-528 (diagnostics card), :820-832 (formatDiagnostics) | every diagnostics value |

Full placeholder inventory (values used and what real data replaces them):
`assets/shots/PLACEHOLDERS.md`. Fidelity evidence: `assets/shots/FIDELITY.md`.

## Delivery-note wording (use verbatim in the final delivery)
"All 14 shots in this reel are HTML/CSS rebuilds of the F.R.I.D.A.Y Compose source
(shot-forging pipeline, per-shot citations in PROVENANCE.md); none is screen-recorded.
The app logo and the Orbitron display face are the app's real bundled assets. UI text
and telemetry render in Roboto standing in for the platform SansSerif/Monospace
families. Every measured figure shown (tok/s, latency, token counts, model filenames,
diagnostics values) is a placeholder marked ILLUSTRATIVE, pending real run data."
