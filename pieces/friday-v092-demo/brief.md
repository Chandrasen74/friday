# FRIDAY v0.9.2 demo - brief

## Goal
45-60s 9:16, music-only animated feature demo for FRIDAY (`com.friday.assistant`) v0.9.2 (Chandrasen74/friday), isometric no-hero look, showing the shipped luxury-motion/telemetry work.

## One-line idea
A calm on-device assistant "lights up" on cold boot, runs a local chat, captures generation telemetry, and opens the diagnostics HUD - all as a precise, layered isometric UI.

## Format
F2 chronology/morph chain (proven). 52.0s @ 120 BPM, 24 fps, 1080x1920. Eras/scenes joined by shape morph bridges on the 8th grid; a single hard cut only on the Telemetry HUD reveal (the payoff turn) for emphasis.

## Style
`isometric` kit. White ground, hairline slabs, one dark accent per focus, spring settles, on-1s. No hero character (UI focus). Colours sampled to FRIDAY Obsidian/cyan identity (Obsidian void `#09090C`/`#0E0E13`, Stark cyan accent) expressed in the isometric medium.

## Beats (storyboard - 11)
| # | t range | beat | bridge to next | sound |
|---|---|---|---|---|
| 1 | 0.0-4.5 | Cold boot on Obsidian void: emblem pops in as HUD reticle locks (invisible OS splash handoff) | emblem -> prompt card | soft pad, low ticks |
| 2 | 4.5-9.0 | "Local ready" home: prompt cards rise in, cyan highlights | card -> composer capsule | light plucks per card |
| 3 | 9.0-13.5 | Composer capsule clipped/raised, message sent | send glyph -> user bubble | tap + send blip |
| 4 | 13.5-19.0 | Streaming assistant bubble: typing dots then tokens, kinetic plasma glow along edge | bubble -> telemetry footer | rhythmic token ticks, rising pad |
| 5 | 19.0-23.0 | Generation completes: glow dissolves, telemetry footer fades in (tok/s • time) | footer pill -> HUD stat card | confirm chord |
| 6 | 23.0-27.0 | Tap footer: Telemetry HUD sheet slides up (decode speed, latency, backend dot, output) | (HARD CUT on reveal) | payoff hit + chord |
| 7 | 27.0-31.5 | HUD detail: model/quant tag, CPU perf tip with "Open Settings" | settings glyph -> settings surface | soft pad |
| 8 | 31.5-36.0 | Clean top bar + settings clarity (status in sidebar, 26dp composer preserved) | panel -> cache icon | gentle ticks |
| 9 | 36.0-41.0 | Cold-boot persistence: app relaunch -> metrics footer still present from cache | cache block -> feature slate | warm motif returns |
| 10 | 41.0-47.0 | Payoff slate: v0.9.2 highlights (cache, plasma glow, haptics, HUD) + stability/fixes | slate -> logo | fullest chord, loudest hit |
| 11 | 47.0-52.0 | End card: FRIDAY wordmark/logo lockup on void, hold and fade | loop/hold | decay to room tone |

Silence/drop: brief dip before beat 6 reveal (HUD payoff). Loudest: beat 6 hit, sustained warmth in beat 10.

## Claims (on-screen) and sources
- "v0.9.2" release: https://github.com/Chandrasen74/friday/releases/tag/v0.9.2 (tag/release verified in-session)
- Features shown (cache, plasma glow, haptics, Telemetry HUD, loading-bubble fix, invisible splash handoff): release notes and commits `dc582da`, `1f61eb7`, `4efe5e6`, `77c08d6`, `d9b909c`, `c515258` on Chandrasen74/friday
- Package/app: `com.friday.assistant`, FRIDAY (from repo/manifest)

No unverified performance claims on screen (no specific tok/s asserted); telemetry values shown as illustrative UI.

## Assets / voice / track
None. All drawn in code in the isometric kit; music-only synthesized score.
