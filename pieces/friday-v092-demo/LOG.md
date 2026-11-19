# LOG - friday-v092-demo

## Plan
- FRIDAY v0.9.2 demo reel per FRIDAY_shotlist.md: 52.0s, 9:16 (1080x1920), 30fps, 1560 frames, 120 BPM (8th = 6 frames), A minor. Eleven beats, exactly two structural full-frame moments: the HUD hard cut (22.0s / frame 660) and the end card. Phone-first premium product film built only from real app UI captures plus camera motion and drawn overlays; no invented chrome.

## Build
- Assets: real product captures in `assets/shots/` (24 embedded), probed into the capture-space geometry table `G` (src/scenes.js, 1080x2400 capture space).
- Pure deterministic f(t) frames (seeded RNG only); exactly two typefaces (Orbitron display, Roboto UI); one accent #38BDF8; grounds #09090C / #0E0E13 / #15151C; no glow on UI chrome, no corner labels, no frame borders, no flash-per-cut.
- Audio: seeded procedural score + SFX (src/score.js) -> renders/audio.wav plus stem-music.wav / stem-sfx.wav. Near-silence 20-22s, loudest hit at 22.0s, scripted loud resolve at 48.0s, fade by ~51.6s; master -17 dBFS RMS with -1 dBFS limiter.
- Hero metrics ~300px screen; telemetry as hero; captions Roboto 600 60px, 3-5 words, >= 1.25s hold; safe top 250 / bottom 420 / right 140.

## Review
- Critique loop (FRIDAY_critique.md): 3 rounds, stopped per the rule once every axis scored >= 8 (R3: 9,9,9,9,9,9,8,8,9,9).
  - R1 fixes: HUD decode plate relayout (label above digits, cyan unit beside digits), captions to y 1434 with scrims where content slides beneath, beat-8 ticks screen-anchored, ILLUSTRATIVE tag flush-draw, beat-9 stacked strips, band rect (0,96,1080,135), patch top 1908, v092 reveal at 42.4, view() clamp removal (whips are real pans).
  - R2 fixes (pixel-verified): beat-1 caption 1434 -> 1180 (clears the baked "Create new chat" pill; verified gap capture 1248..1356); beat-2 chips wrapped 2+1 at 24px (single row cannot fit inside safe-right 940); beat-3 cam rebuilt (glide 8 -> 9.0 into hold (610,1750,1.45) 9.0-10.3), carried line retargeted to the LIVE capsule-top edge via C2S (capture 2185, the plan's hardcoded 1554.4 was the wrong row), ignite start 8.35 -> 8.30.
  - R3: confirmation pass on a fresh 25-frame contact sheet + cut regression + double-render determinism; no fixes needed.
- review.mjs on the final build: all 10 cuts on grid PASS (cut 10 soft match, delta 0.1 frame at f1378); text PASS (0 cut-off/overlap, 3 safe-zone WARNs, accepted); SOUND PASS (-15.5 LUFS, true peak -1.0 dBFS, loudest 100ms window 22.0s at -7.5 dBFS, 2.0s silence before it); story-arc FAIL accepted (heuristic conflicts with the scripted loud resolve at 48.0); dead-beats WARN accepted (sanctioned end-card hold).

## Deviations (all documented, shotlist overrides where they conflict)
- Caption default y 1434 (safe band); beat-1 caption additionally at y 1180.
- Beat-2 model chips 2+1 wrap, 24px, rows cy 1190/1250.
- Beat-3 cam keys as above; carried line live C2S target; ignite at 8.30.
- Beat-6 tok/s unit exit fade: dissolve keyed to the unit's LIVE screen right edge (C2S distance key alpha = (W-24-edge)/120, gated TT > 26.5 so the opening macro keeps full alpha), completing 24px before the first sliced frame; added optional alpha to bigUnit().
- Beat-8 ticks screen-anchored; beat-9 stacked strips; ILLUSTRATIVE tag flush-draw; band rect (0,96,1080,135); patch top 1908; v092 reveal at 42.4; HUD plate relayout.
- view() clamp removed (whips are real pans; capture content is bounded by the cam transform).

## Notes
- Timing grid: 8th = 6 frames; morph centres and the single cut placed on 8ths.
- No unverified perf numbers on screen; ILLUSTRATIVE tag on the beats 5/9 counters only.
- Frame intermediates under frames/ are regenerable via export.mjs (deterministic) and were removed after mux, per the pass convention in renders/qc_report.md.
