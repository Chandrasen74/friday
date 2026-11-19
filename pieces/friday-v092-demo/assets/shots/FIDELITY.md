# FIDELITY.md: Step 6 fidelity log (3 rounds)

Authority: the Compose source. Every comparison cites file:line; the extracted facts live
in `assets/design_tokens.json` + `assets/screen_map.md`. Shots were recaptured after every
fix round (`node shots_src/capture.mjs`). Evidence sheets kept in `assets/shots/`:
`_contact.png` (round 1), `_round2_crops.png`, `_round3_crops.png`.

## Round 1: full-frame contact sheet (all 14 shots)
Issues found and fixed:
1. s04_stream_a/b/c carried an invented dimmed regenerate icon during streaming. The
   source never renders a dimmed regenerate (TelemetryHeader.kt:139 shows it only when
   enabled). Removed.
2. Greeting bubbles in s02/s03/s03_compose_typed were missing the copy icon shown on
   every non-blank assistant message (ChatScreen.kt:997). Added.
3. Status-pill dot was cyan; the default ModelStatus branch maps to
   onSurfaceVariant #A1A1AA (ChatScreen.kt:610). Corrected in s02 + s08.
4. Drawer bottom padding was nav+8; corrected to nav+16 (ChatScreen.kt:487-489:
   navigationBarsPadding + vertical 16dp).
Recaptured, re-tiled, re-reviewed: clean.

## Round 2: 2x zoom crops (home top / settings card / HUD sheet / footer zone)
Issues found and fixed:
1. HudStatCard label letter-spacing 1.4 -> 1.2sp (TelemetryHudSheet.kt:270-272).
2. Backend status dot moved from the label row into the value row with a 6dp gap
   (TelemetryHudSheet.kt:277-281); `.val-row` added to components.css.
3. ObsidianSurface fill gradient re-angled 135 -> 158deg to match the Compose brush
   geometry (start (0, 0.08h), end (0.4w, h)); border gradient stays 135deg per source.
4. SettingsTextField label moved OUTSIDE the field box (label, 6dp gap, then field) per
   SettingsScreen.kt:718-745; `.field-label` class added.
Recaptured; round-3 crops built from the fixed captures.

## Round 3: verification crops (typed composer / streaming bubble / tip card / settings card)
No new issues. Verified: caret + focused cyan border + send pill morph
(ChatScreen.kt:1210-1221, 1322-1331, 1404), three 6dp typing dots at 6dp gaps
(TypingDots.kt:55-82), plasma border 1.4dp stroke + 4dp halo frame
(PlasmaStreamBorder.kt:73-122), tip card amber border + PERFORMANCE TIP label +
Open Settings CTA (TelemetryHudSheet.kt:204-244), settings chips/active line/status
pill/field label placement. s09_relaunch_before/after footer lines verified byte-identical
(the persistence beat's requirement).

## Per-shot scores (1-10 fidelity)
| Shot | Score | One-line reason |
|---|---|---|
| 01_home | 9 | Every string/style from source; BreathingAura frozen at one static frame (documented approximation). |
| 02_models | 9 | Drawer geometry, pill, session rows, selected bar all match; status text + count are placeholders. |
| 03_compose | 10 | Nothing invented; greeting, header, capsule, mic pill all cited to source. |
| 03_compose_typed | 10 | Typed text is a real prompt-card string; caret, focused border, SEND morph per source. |
| 04_stream_a | 9 | Typing dots + plasma + STOP pill correct; plasma frozen at one pulse frame (documented). |
| 04_stream_b | 9 | Partial reply text is a placeholder; everything structural is real. |
| 04_stream_c | 9 | Near-full reply text is a placeholder; everything structural is real. |
| 05_footer | 9 | Footer format real ("X.X tok/s  •  X.XXs", chevron, 40dp inset); values placeholders. |
| 06_hud | 9 | Card grid, label ls 1.2, dot-in-value-row, quant chip match after round 2; values placeholders. |
| 07_tip | 9 | Gate honored (2.8 < 3.0 AND LOCAL // CPU), amber card + CTA exact; values placeholders. |
| 08_settings | 9 | Chips, active line, status pill, field label placement match after round 2; model filenames placeholders. |
| 09_relaunch_before | 9 | Frame identical to 05_footer by construction; persistence source cited. |
| 09_relaunch_after | 9 | Footer values identical to before (verified in source + render). |
| 10_about | 9 | Diagnostics line FORMAT real (SettingsScreen.kt:820-832); every value a placeholder. |

STOP-rule check: every screen derived cleanly from source. Nothing triggered the
"ask for a real capture" rule; no composable was missing.

## Documented approximations (platform/animation states, not fidelity defects)
1. Status bar (clock 10:08, wifi/signal/battery icons) and gesture nav pill: platform UI
   the app draws under (edge-to-edge, statusBarsPadding); rendered Pixel-style.
2. Modal scrims: drawer 32% black, HUD sheet 50% black (M3 default approximations).
3. M3 Switch off-state colors: default dark-scheme approximation (source uses the
   unthemed Switch composable).
4. PlasmaStreamBorder frozen at one frame of its 1400ms pulse (1.4dp stroke @0.50 +
   4dp halo @0.14); the film animates this procedurally as pure f(t).
5. BreathingAura frozen at one frame of its 3400ms breathe.
6. TypingDots resting frame (reduced-motion phase 0.25, TypingDots.kt:39-41).
7. Fonts: Roboto stands in for platform SansSerif; the app's FontFamily.Monospace
   telemetry renders in Roboto with tabular-nums (two-typeface rule). Glyph coverage
   for the lockup, bullet, chevron and "//" verified in `_glyph_report.json`.

**Result: PASS. No shot below 9.**
