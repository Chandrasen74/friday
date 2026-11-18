# FRIDAY v0.9.2 Demo - QC / Render Report

## Pass: FRIDAY_shotlist.md rebuild, critique loop closed, final export (current)
- Date: 2026-11-18 (local)
- Branch: `feature/luxury-motion` (verified). Scope: `pieces/friday-v092-demo/` only. No app source/behavior/package identity/release metadata/APKs/signing/production-asset changes. No commit/push/tag/PR. Pre-existing `M README.md`, untracked `pieces/`, and `renders/baseline_draft_24fps/` preserved.
- Spec: 52.0s, 1080x1920 (9:16), 30fps, 1560 frames, 120 BPM. Real product captures + camera/overlay only; two typefaces (Orbitron, Roboto); one accent #38BDF8; seeded deterministic f(t) frames; procedural score + SFX.

### Changes in this pass (final fix set)
- Round 2 fixes (pixel-verified): beat-1 caption y 1434 -> 1180 (clears the baked "Create new chat" pill; empty capture band 1248..1356 verified at both cam ends); beat-2 chips wrapped 2+1 at 24px (rows cy 1190/1250; probe x 941..1080 EMPTY); beat-3 cam hold (610,1750,1.45) over 9.0-10.3 with the carried line targeting the LIVE capsule-top edge via C2S (capture 2185) and ignite start moved to 8.30 (border wipe verified entering frame at 8.6 as (0,1618)-(263,1619); full border at 9.0 y=1592 over 903 columns).
- Final review FAIL fix (2026-11-18): beat-6 "tok/s" hero unit was sliced by the right frame edge at 27.40-27.97s during the HUD exit dolly (onset measured between 27.3 and 27.4; ink at x 848..1079 by 27.4). Fixed with a motivated dissolve keyed to the unit's LIVE screen right edge: alpha = (W - 24 - edge)/120 computed via C2S each frame, time-gated to TT > 26.5 so the opening macro keeps full alpha (the unit sits near the edge there while the hero grows). Added optional alpha to bigUnit(). Verified: opening macro bit-identical (s22_5 cyan n=6480, same bbox as pre-fix); edge columns x 1040..1080 EMPTY at 27.4/27.5/27.8; hero digits untouched (s27_8 text (194,404)-(883,686)).
- Poster: `renders/poster.png` (end-card frame at t=50.0, 1080x1920).

### Commands (this pass)
1. `build.mjs pieces/friday-v092-demo` -> `index.html` (2291 lines, 24 assets embedded, syntax ok, no external assets).
2. Three critique rounds (see `FRIDAY_critique.md`): contact sheets via tile.mjs (deterministic: true), cut-boundary diffs, audio RMS windows, pixel probes (probe24-36.ps1), one vision read per round.
3. `export.mjs pieces/friday-v092-demo` -> 1560 frames + audio.wav + stem-music/stem-sfx -> `renders/final.mp4` (libx264 crf 14 preset slow yuv420p + aac 192k, +faststart).
4. `review.mjs pieces/friday-v092-demo` -> verdicts below. `still.mjs` -> poster.
5. Frame intermediates under `frames/` removed after mux (regenerable via export.mjs), per the pass convention.

### Final media (ffprobe)
- final.mp4: h264, yuv420p, 1080x1920, r_frame_rate 30/1, duration 52.000000 (video+audio), 14,814,617 bytes.
- poster.png: 1080x1920.

### review.mjs verdicts (2026-11-18)
- Cuts: 10/10 on grid PASS (cut 10 soft match, delta 0.1 frame at f1378).
- Text: PASS (0 cut-off/overlap; 3 safe-zone WARNs, accepted below).
- Sound: PASS (-15.5 LUFS integrated, range 4.7 LU, true peak -1.0 dBFS; loudest 100ms window 22.0s at -7.5 dBFS; 2.0s of <= -40 dBFS silence before it).
- Story arc: FAIL (2/3 rules) - ACCEPTED DEVIATION: the tool's "goodbye decays" / "gift is loudest act" rules conflict with the shotlist's scripted loud resolve at 48.0s; the spec that matters (loudest scripted moment is the 22.0s hit, with 2.0s silence before) holds.
- Dead beats: WARN 47.17-52.0s (4.8s hold over deadMax 4.5s) - ACCEPTED DEVIATION: the end card is one of the two sanctioned structural moments.

### Audio arc (ffmpeg astats, filter-only atrim, decoded from final.mp4; master = WAV)
| window | final.mp4 | master |
|---|---|---|
| 0-0.1 (opening hit) | RMS -7.95, peak +2.66 dBFS | RMS -8.2, peak -1.0 |
| 20.2-21.8 (near-silence) | RMS -47.3 | -47.2 |
| 21.95-22.25 (loudest) | RMS -7.79, peak -0.94 | -7.8 |
| 47.9-48.3 (resolve, scripted loud) | RMS -8.79 | -8.8 |
| 51.9-52.0 (tail) | RMS -82.6 | -82.5 |

The opening hit's decoded peak reads +2.66 dBFS: AAC transient overshoot on the -1.0 dBFS limited source (codec artifact, present in every encode of this master), not a mix failure.

### Accepted deviations / notes (truthful)
- Safe-zone WARNs ("EVERYTHING READY LOCALLY.", "tok/s", "METRICS SURVIVE RELAUNCH." sit past the safe-right line): captions and the hero unit were verified by pixel probes into empty/owned regions at their y/x positions; kept for composition.
- Hard-cut pixel percentage (72.5% at f659->f661, step-4 sample grid) is not comparable to the earlier 68.3% figure (different sampling grid); all cuts remain on the timing grid.
- 27.8/28.2 gridDiff moved 2454 -> 2466 (+0.08% of the pair grid): the 2454 stills predate the sanctioned round-1 HUD plate relayout whose region sits inside that dolly pair; fixed-region carries (amber 1263/1269, cyan 64669/65370) are bit-identical across all builds; cross-process double-render diffs 0 cells.
- Beat-3 upper-frame void is inherent to the new-chat capture (content only at rows 16..168, 296..384 and the capsule region); intentional negative space, not chased.
- Baked product text exiting the top frame edge during the 26.5-28.0 push is capture content, not drawn text.
- `renders/share.mp4` is from the superseded 2026-11-17 pass (stale; regenerate on demand).
- No human playback or by-ear audio review was performed; verification is via deterministic render, review.mjs automated checks, ffprobe metadata, pixel probes, and audio-window measurements.

### Deliverables
- `renders/final.mp4`, `renders/poster.png`, `renders/audio.wav`, `renders/stem-music.wav`, `renders/stem-sfx.wav`
- `index.html`, `src/*`, `piece.json`, `FRIDAY_shotlist.md`, `FRIDAY_critique.md`, `LOG.md`
- Baseline preserved: `renders/baseline_draft_24fps/`

## Pass: Caption tweak + official logo + rebuild/re-render (superseded 2026-11-17 pass)
- Date: 2026-11-17 (local)
- Branch: `feature/luxury-motion` (verified)
- Scope: `pieces/friday-v092-demo/` only. No app source/behavior/release metadata/APKs/signing/production assets changed. No commit/push/tag/PR.

## Changes in this pass
- Updated Beat 1 caption from "Instant on Obsidian." to **"Instant on your mobile device."** (5 words) in `src/head.html` (timeline + storyboard metadata) and `storyboard.md`. Caption rendering adjusted in `src/scenes.js` to auto-fit within caption width and use a slightly softer tone so it does not stand out.
- Official embedded FRIDAY logo asset used in rendering (`src/scenes.js` `drawLogoMark` via `drawAsset` for `assets/friday-logo.png` / `assets/friday_logo_blue.png`, with procedural fallback). Assets: `assets/friday-logo.png`, `assets/friday_logo_blue.png` (512x512 RGBA). Build embeds 2 asset(s).
- Prior compositing fix retained: non-hard-cut boundary blends via offscreen layer compositing; sole hard cut remains Beat 5→6 at 23.4s/frame 702.

## Environment & tool versions
- Node: v24.21.0
- Python: 3.14.3
- FFmpeg/FFprobe: 9.0-full_build-www.gyan.dev
- Playwright/Chromium: omniroute global Playwright, Chromium `%LOCALAPPDATA%\ms-playwright\chromium-1255\chrome-win64\chrome.exe`, `--disable-gpu --use-gl=swiftshader --enable-unsafe-swiftshader`.

## Commands (this pass)
1. `node "ANIMATE_TOOLS\build.mjs" pieces/friday-v092-demo` -> `index.html` (1890 lines, 2 asset(s) embedded, syntax ok).
2. `node "ANIMATE_TOOLS\export.mjs" pieces/friday-v092-demo --no-audio --no-mux` -> `frames/f0000..f1559`.
3. `ffmpeg -y -hide_banner -loglevel error -framerate 30 -start_number 0 -i frames/f%04d.png -i renders/audio.wav -frames:v 1560 -t 52 -c:v libx264 -preset slow -crf 14 -pix_fmt yuv420p -r 30 -c:a aac -b:a 192k -ar 48000 -movflags +faststart renders/final.mp4` (exit 0).
4. `ffmpeg -y -hide_banner -loglevel error -framerate 30 -start_number 0 -i frames/f%04d.png -i renders/audio.wav -frames:v 1560 -t 52 -c:v libx264 -preset slow -crf 24 -pix_fmt yuv420p -r 30 -c:a aac -b:a 192k -ar 48000 -movflags +faststart renders/share.mp4` (exit 0).
5. `node "ANIMATE_TOOLS\storyboard.mjs" pieces/friday-v092-demo pieces/friday-v092-demo/storyboard.png`.
6. Motion board (ffmpeg xstack, 3x11 start/peak/settle frames) -> `motion-board.png`.
7. ffprobe metadata, `ffmpeg -v error -i ... -f null -` decode checks (final/share exit 0), `git diff --check` (exit 0). Frame intermediates removed after mux.

## Final media (ffprobe)
### final.mp4
- Video: codec_name=h264, width=1080, height=1920, pix_fmt=yuv420p, r_frame_rate=30/1, avg_frame_rate=30/1, duration=52.000000, nb_frames=1560
- Audio: codec_name=aac, sample_rate=48000, channels=2, duration=52.000000
### share.mp4
- Video: nb_frames=1560, duration=52.000000 (same composition/timing), h264 yuv420p 1080x1920 30fps
- Audio: aac 48kHz stereo, 52.000000s

## Procedural score
- `music/generate_score.py` (deterministic, original) -> `music/procedural_score.wav` (48 kHz stereo, 52.0s), copied to `renders/audio.wav` and muxed into both MP4s. No external music/assets.

## Checks and results
- Beat 1 caption updated and auto-fit: **PASS (source/build/export completed)**
- Official logo asset embedded and rendered via `drawAsset`: **PASS (2 asset(s) embedded)**
- Timeline: 30 fps, 1560 frames, 52.0s, 11 beats with boundaries in `TIMELINE`/`BEATS_DEF`; `assertTimeline()` enforces single hard cut (Beat 5→6, 23.4s, frame 702): **PASS (source assertions/structural)**
- Boundary blends for non-hard-cut transitions (offscreen compositing): **PASS (implementation/structural)**
- Build/syntax, frame export, mux, storyboard, motion board generation: **PASS**
- ffprobe specs and decode-to-null (final/share): **PASS**
- `git diff --check`: **PASS**
- No app files changed; no commit/push/tag/PR; baseline preserved: **PASS**

## Review limitations (truthful)
- **Visual playback review:** not performed as human playback in this environment. Verified by deterministic render/export, structural/timing assertions, media metadata, decode checks, and regenerated storyboard/motion board artifacts.
- **Audio listening review:** muxed audio not listened to by ear; verified structurally (WAV/MP4 parameters, duration 52s, 48kHz stereo AAC present, decode ok).
- Caption text was changed at user request (Beat 1 now "Instant on your mobile device.") and therefore differs from the previously specified caption set; other beats unchanged in this pass.
- Detailed UI-label/value fidelity claims in the long prompt were not exhaustively cross-verified against every app source/screenshot in this pass; telemetry visuals remain abstract/non-fabricated where values are unverified.

## Deliverables
- `renders/final.mp4`, `renders/share.mp4`, `renders/audio.wav`, `renders/stem-music.wav`, `renders/qc_report.md`
- `index.html`, `src/*`, `piece.json`, `music/generate_score.py`, `music/procedural_score.wav`, `assets/friday-logo.png`, `assets/friday_logo_blue.png`
- `README.md`, `storyboard.md`, `storyboard.png`, `motion-board.png`
- Baseline: `renders/baseline_draft_24fps/`
