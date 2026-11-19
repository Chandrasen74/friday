# FRIDAY v0.9.2 demo reel - critique log

Format: one round per section. Each round: evidence gathered, scores 1-10 (10 = ship),
the three worst fixes applied, what is deferred. Stop only when every axis scores >= 8.

Piece: pieces/friday-v092-demo, 52s / 1560f @30fps / 9:16, score A minor 120 BPM.
Shotlist: FRIDAY_shotlist.md (hero metrics ~300px; safe top 250 / bottom 420 / right 140).

## Round 1 - 2026-11-18

Evidence: contact sheet 21 frames (tile.mjs, deterministic: true), half-size phone sheet
of the HUD reveal at 22.5s, transition pairs at every cut boundary (full-frame diff +
targeted pixel checks), audio RMS windows on renders/audio.wav (ffmpeg astats).

Scores:

| # | axis | score | evidence |
|---|------|-------|----------|
| 1 | Hook 0-4s | 8 | f0 reads as an intentional breath before the facets; f75 lockup + 4 suggestion chips lands; the "Create new chat" chip transiently crosses the headline mid-flight (2.5s tile), settles after. |
| 2 | Product truth | 9 | All 21 sampled frames are the app's own screens (home, sessions, compose, stream, footer, HUD, tip card, settings, relaunch, about); no invented chrome; FIDELITY/PROVENANCE docs present; ILLUSTRATIVE tag only on beats 5/9 hero counters. The beat-6 hero plate redraw is the sanctioned plate treatment. |
| 3 | Motion quality | 8 | Every sampled shot has camera motion; anticipation/overshoot on pops (tip card 28.4, button 30.9, ticks 33/34/35, v092 42.4); deduction: 8.0-9.0s compose opening reads static and sparse (8.5s tile is nearly empty above the composer). |
| 4 | Continuity / carries | 9 | Amber dot both sides of 28.0 (1263/1269 px); band + button both sides of 32.0 (64669/65370 px); compression front 1372..1374 at 35.8 lands on seam 1249..1250 at 36.2; hairline 830 both sides of 42.0 (829/825); facets/logo/lockup 46-52; exactly one hard cut, 68.3% of pixels change at 659 vs 661; tile pass reports deterministic: true. |
| 5 | Typography | 7 | Exactly two faces; 40px labels; 60px tracked captions; hero digits ~300px screen per shotlist. DEFECT: at 22.5s the 12.6 digits cover the baked DECODE SPEED label (patch rows 1812..1880 vs digits 1792..1900 capture). |
| 6 | Color discipline | 9 | One cyan accent; amber dot is the product's own state color; grounds consistent; no glow on chrome. |
| 7 | Structure / pace | 7 | Two structural moments only (hard cut 68.3%, end card); DEFECT: beat 3 opening 8.0-9.0 has only the carried line glide + border ignite in a sparse frame. |
| 8 | Audio sync / loudness arc | 8 | RMS windows: 0.0-0.1 hit (-8.2), riser 19.5-20 (-12.3), near-silence 20.2-21.8 (-47.2), loudest hit 21.95-22.25 (-7.8, peak -1.0), resolve 47.9-48.3 (-8.8), tail gone by 51.9-52 (-82.5); full track peak -1.01 dB / RMS -17.7 dB (spec -1 / -17); cues scheduled from TIMELINE.cues (sample-accurate). Full-band listen pending final export. |
| 9 | Captions | 6 | All captions 3-5 words, holds 1.3-2.3s. DEFECT: beat 7 caption at y 1560 and beat 8 at y 1600 extend into the bottom 420 unsafe zone (content must end above y 1500). |
| 10 | Safe areas / composition | 7 | Hero + unit end x ~910 < 940; captions left-aligned x 90. DEFECTS: the caption zone issue above, and beat 5's 240px hero counters sit directly over the email body text with no scrim (18.6-22.0, white on white). |

Three worst fixes this round:

1. Captions into the safe zone (axes 9, 10): move beat 7 caption 1560 -> 1445 (void region
   below the card starts at screen 1304, so 1445..1505 is clean void); move beat 8 caption
   1600 -> 1445 and add an opt-in scrim to cap() (void patch behind the text) because the
   dolly keeps list rows sliding under that region.
2. HUD decode plate collision (axis 5): measure the baked label/value geometry from
   06_hud.png, enlarge the plate patch to cover label + value with margin, drop the hero
   center so the digits sit fully on the patch below the label, and anchor the unit to the
   measured digit width instead of the magic +137.
3. Beat 5 hero counters over card text (axis 10): draw a void scrim panel (rounded, 0.78)
   behind the counters inside the same over callback so the numbers read clean over the card.

Deferred to round 2: beat 3 opening composition (sparse 8-9.0s hold), re-verify HUD plate
and beat 5 scrim at full res, full-band audio listen on the final export, LOG.md updates.

## Round 2 - 2026-11-18

Evidence: contact sheet 23 frames incl f285 (9.5s) and f405 (13.5s) (tile.mjs,
deterministic: true), cut-boundary regression identical to round 1 (gridDiffs
2454 / 8517 / 6636 / 10028; amber 1263/1269; cyan 64669/65370), and pixel probes
on 01_home.png / 03_compose.png plus 8 fresh stills (s02_5, s03_5, s06_8, s08_3,
s08_5, s08_6, s09_0, plus the round-1 verify set).

Scores (state after the round-1 fixes, as seen in this evidence):

| # | axis | score | evidence |
|---|------|-------|----------|
| 1 | Hook 0-4s | 8 | Facets/lockup/scan wipe unchanged; DEFECT: the beat-1 caption at y 1434 maps onto the baked "Create new chat" pill (capture rows 1460..1556, pill text 1360..1392) at 2.5s: the headline sits on the chip, not above it. |
| 2 | Product truth | 9 | All frames remain the app's own screens; chips carry the real model names (QWEN3-0.6B, GRANITE 4.0 350M INT8/FP16); no invented chrome. |
| 3 | Motion quality | 8 | Cam glides + pops verified in the contact sheet; DEFECT: beat 3's opening (f255 8.5s, f285 9.5s) reads static and mostly void: the cam parks at (610,1565,1.15-1.16) until 9.6 and 03_compose has content only at capture rows 16..168, 296..384 and the capsule region. |
| 4 | Continuity / carries | 9 | Every carry number identical to round 1 (gridDiffs 2454/8517/6636/10028; amber 1263/1269; cyan 64669/65370); determinism holds on the new sheet. |
| 5 | Typography | 9 | HUD decode plate fix verified: baked label rows 868..908 vs digits 927..1120 (clear), cyan unit sits right of the digits; 24/27s tiles read clean; 60px tracked captions; hero digits ~300px screen per shotlist. |
| 6 | Color discipline | 9 | One cyan accent; amber only as the product's own state color; grounds consistent; no glow added. |
| 7 | Structure / pace | 7 | DEFECT: beat 3's 8.0-9.6 window holds a wide, sparse frame (only the line glide + ignite in a mostly-empty screen); the rest of the reel keeps its 2s cadence. |
| 8 | Audio sync / loudness arc | 8 | Unchanged from round 1 (audio not touched this round): hit windows -8.2/-47.2/-7.8/-8.8/-82.5 dB RMS; peak -1.01 dB, RMS -17.7 dB; cues sample-accurate. Full-band listen stays deferred to export. |
| 9 | Captions | 9 | All captions verified in the safe band: beats 7/8/9/10 at y 1434 (s33_0 ink rows 1436..1478), beat 8 scrim n=48543, beat 5 counters on their own void panel (n=87082), beat 4 at 1100 in the card gap, beat 6 at 330 below the top safe line. |
| 10 | Safe areas / composition | 7 | DEFECT: the beat-2 model chips at 6.8s span chip-pred x 139..960 (cyan text ink x 159..940, ~781px of text) so the chip boxes reach ~1005, past the safe-right 940; centered 24px chips cannot fit (text alone leaves a 19px budget). Beat-1 caption collision from axis 1 also counts here. |

Three worst fixes this round:

1. Beat-1 caption out of the pill (axes 1, 10): row-profile of 01_home shows content
   bands 1360..1392 (pill text) and 1460..1556 (pill body) with a clean 1248..1356
   band above; caption moved 1434 -> 1180, whose ink maps to capture ~1259..1336 at
   both cam ends (t=2.35 cy 1066.9/z 1.083, t=4.35 cy 1085/z 1.10). Verified:
   s02_5 ink 1183..1227 (alpha 0.625 fade, loose pred), s03_5 ink 1181..1225, gap
   rows 1226..1276 EMPTY at 2.5s (baked pill text enters at >=1262 at 3.5s, no overlap).
2. Beat-2 chips wrap 2+1 (axis 10): 24px text, pad 36, gap 20, h 44, rows at cy
   1190/1250 (row 1: QWEN3-0.6B + GRANITE 4.0 350M INT8; row 2: GRANITE 4.0 350M
   FP16). Verified: row 1 chip box (292,1167)-(787,1212), row 2 (377,1227)-(682,1272),
   probe x 941..1080 EMPTY. Clear of the last drawer row (screen bottom ~1092 at 6.8s).
3. Beat-3 opening rebuilt (axes 3, 7): cam hold tightened to (610,1750,1.45) over
   9.0-10.3 (glide 8->9.0, was parking at (1565,1.15) until 9.6); carried line now
   targets the LIVE capsule-top edge (capture 2185 via C2S, was hardcoded screen
   1673 which was measured against the old cam); ignite starts 8.30 so the border
   wipe follows the line landing instead of starting 5 frames after it. Verified:
   s08_3 line bestRow y=1654 count=1080 (predicted 1656.5); wipe enters frame at
   s08_6 as (0,1618)-(263,1619) vs predicted (0..263.3, 1622); full border at s09_0
   y=1592, 903 columns (predicted 1590.75); typed prompt text present at 9.0
   (rows 1662..1709, n=5955); caret tick at (300..303, 1669..1739); caption gap
   above the border EMPTY (rows 1480..1585).

Deferred to round 3: fresh contact sheet + cut strips + 22.0 hard-cut diff on the
fixed build, one vision read, then export if every axis holds >= 8. Note: beat 3's
upper-frame void is inherent to the new-chat capture (content only at rows 16..168,
296..384, capsule); the composition anchors the action in the lower third with the
caption at 1434 bridging the empty band - documented, not a defect to chase.

## Round 3 - 2026-11-18 (confirmation pass)

Evidence: fresh contact sheet, 25 frames incl f75/f105 (beat-1 caption), f204 (chips),
f255/f270/f285 (beat-3 opening), f660 (hard cut), f1272 (v092) and f1500 (end card)
(tile.mjs, deterministic: true), one full vision read of the sheet, cut-boundary
regression re-rendered on the final build, cross-process determinism double-render,
22.0 hard-cut pair re-measured.

Scores (no fixes needed this round; every axis holds):

| # | axis | score | evidence |
|---|------|-------|----------|
| 1 | Hook 0-4s | 9 | f75/f105: the caption sits in the verified gap between the suggestion list and the "Create new chat" pill (ink 1183..1227 vs chip band ending 1152 and pill text entering >=1262); facets, scan wipe and lockup unchanged. |
| 2 | Product truth | 9 | Every tile is the app's own UI (home, drawer, compose, stream, footer, HUD, tip, settings, model manager, storage, about); real model names on the chips; ILLUSTRATIVE tag only on beats 5/9 counters. |
| 3 | Motion quality | 9 | Cam motion in every sampled shot; beat 3 now glides 8->9.0 into the (610,1750,1.45) hold, the border wipe enters frame at 8.53 (verified x 0..263 at y 1618) and the whip carries the bubble to the thread; pops keep anticipation/overshoot. |
| 4 | Continuity / carries | 9 | Amber dot 1263/1269 and band/button cyan 64669/65370 bit-identical across all three builds; gridDiffs 2466/8517/6636/10028 (8517/6636/10028 exactly match rounds 1-2; the 27.8 pair moved 2454->2466 because the 2454 stills predate the sanctioned round-1 plate relayout, whose region sits inside that dolly pair - the fixed-region carries are unchanged); double-render of s27_8 diffs 0 cells; hard cut 659 vs 661 changes 72.5% of a step-4 sample grid (round 1's 68.3% used a different sampling grid, not comparable). |
| 5 | Typography | 9 | Two faces only; HUD label/digit stack verified; chips 24px legible in two rows; 60px tracked captions; hero digits ~300px screen. |
| 6 | Color discipline | 9 | One cyan accent; amber only on the product's own state dot and tip card; grounds consistent; no glow on chrome. |
| 7 | Structure / pace | 8 | Two structural moments only (22.0 cut, end card); something new every 2s holds. Remaining deduction, documented not chased: beat 3's upper frame is inherently void (the new-chat capture has content only at rows 16..168, 296..384 and the capsule) and the wipe needs ~5 frames after the line lands before it enters frame (caret stays alive). |
| 8 | Audio sync / loudness arc | 8 | Score body untouched: hit windows -8.2 / -47.2 / -7.8 / -8.8 / -82.5 dB RMS, peak -1.01 dB, RMS -17.7 dB, cues sample-accurate. Final full-band listen happens on the exported file. |
| 9 | Captions | 9 | Every caption verified on pixel rows inside the safe band (1183..1227 for beat 1, 1435..1479 elsewhere, 1100 beat 4, 330 beat 6); beat 8 scrim and beat 5 panel hold. |
| 10 | Safe areas / composition | 9 | Chips end at x 787/682 (probe x 941..1080 EMPTY); hero+unit ~910; captions x 90; ping outlines at 6.8s extend off-frame right as product row geometry, documented. |

No three-worst list this round: all axes >= 8, so the loop stops here per the rule.
Deferred to export: full-band listen on final.mp4, LOG.md deviations, renders/qc_report.md.

