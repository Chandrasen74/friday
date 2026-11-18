# FRIDAY v0.9.2: shot list (proposal, awaiting OK)
52 s · 120 BPM · 104 beats = 26 bars (1 bar = 2.0 s) · format: 9x16 1080x1920 @30 fps (renders at 60 + blur)
Music: original procedural score, A minor, 120 BPM, offset 0 (grid is exact because we synthesize it) · VO: none
Ground: the app's own dark tokens (#09090C void, #0E0E13, #15151C). One accent: #38BDF8 (the app's cyan).
Logo: assets/friday-logo.png (official, facets reused for the reassemble motif). Safe area: top 250, bottom 420, right 140.

| # | bars (beats) | time | shot | on screen (asset) | motion | out (what carries) | sound |
|---|--------------|------|------|-------------------|--------|--------------------|-------|
| 1 | 0-2 (0-8) | 0-4 | Instant | logo facets already mid-flight on frame 0, lock on beat 1; scan line wipes the real home capture in (`shots/01_home.png`) | facets snap (spring, tight), 8% push | scan line becomes the model-card edge | impact @0, tick-lock @1 |
| 2 | 2-4 (8-16) | 4-8 | Ready locally | real home/model capture cut into layers; model chip names set in type (`shots/02_models.png`) | layers separate in z, 12% dolly | active card edge extends into the composer | glass pings on each card |
| 3 | 4-6 (16-24) | 8-12 | One clean send | real composer, real typed prompt (`shots/03_compose.png`) | caret blink, send press, bubble flies up the thread | bubble lands, thread scrolls up | soft click @ press, whoosh |
| 4 | 6-9 (24-36) | 12-18 | Alive while thinking | 3 real streaming captures (`shots/04_stream_a/b/c.png`) | push-in on streaming cursor, per-word stagger | reply bottom edge drops into the footer | shimmer pad swell |
| 5 | 9-11 (36-44) | 18-22 | Every run, measured | macro crop of the real footer (`shots/05_footer.png`) | real numbers count up, camera locks | music cuts to near silence bars 10-11 | riser, then hold |
| 6 | 11-14 (44-56) | 22-28 | Open the HUD | HARD CUT on downbeat 22.0 to the real HUD (`shots/06_hud.png`) | hero metrics scale to ~300 px, cut-out cards parallax | CPU row expands | loudest hit of the film @22.0 |
| 7 | 14-16 (56-64) | 28-32 | Tuned for this device | real CPU tip card + Open Settings (`shots/07_tip.png`) | tap, button rect scales to header | match cut into settings | warm confirm |
| 8 | 16-18 (64-72) | 32-36 | Clarity at every layer | real settings list (`shots/08_settings.png`) | dolly down list, row-by-row accent tick | list collapses into relaunch seam | sparse pitched ticks |
| 9 | 18-21 (72-84) | 36-42 | Metrics persist | before/after real captures side by side (`shots/09_relaunch.png`) | footer rows align, a hairline links identical numbers | footer line lifts | opening motif returns |
| 10 | 21-23 (84-92) | 42-46 | Crafted in v0.9.2 | real version/diagnostics crop (`shots/10_about.png`) | v0.9.2 in display type, slow push | everything contracts to the logo | harmonic lift |
| 11 | 23-26 (92-104) | 46-52 | Lockup | official logo facets reassemble (callback to #1); "F.R.I.D.A.Y" + "on-device, by design." | lock on bar 24 (48.0 s), then holds 4 s | none | resolve motif, fade |

Longest gap without a new event: 1 bar (2.0 s). Longest still hold: end card, 4.0 s (>= 1.5 s).

## Needed from you (nothing is invented without these)
1. Real captures (PNG or screen recording, ideally 1080x2400): home/model list, composer with a real prompt, 3 moments of a streaming reply, footer after a run, HUD open, CPU tip card, settings, the same footer after relaunch, version/diagnostics. Airplane-mode on in one capture would prove "offline" with real status-bar icons.
2. The app's real typefaces (Compose Typography) as font files, plus any wordmark file. We bundle two faces instead of system stacks.
3. OK on one accent (#38BDF8) next to the logo's steel blue.
4. OK on the 11 time slots above (shots 4, 6, 9, 11 get 3 bars; the rest 2).
