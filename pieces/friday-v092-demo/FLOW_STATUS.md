# Flow Demo Pack Status

Updated: 2026-11-25
Branch: `main` (repo), pack lives in `.audits/friday-test-1/scratch/flow-demo-pack/` (gitignored scratch)

## Plan

Fully Flow-generated Friday v0.9.2 demo, 9:16, 52s target, 11-beat story arc adapted from `pieces/friday-v092-demo/FRIDAY_shotlist.md`.

Current adjustment: Shot 3 and Shot 4 are combined into one 8s clip (send + assistant reply) to improve retention.

## Shot progress

| Shot | Beat | Planned duration | Status | File / note |
|---|---|---:|---|---|
| 1 | Instant | 4s | Done (take saved) | `renders/shot01_instant.mp4` (2,134,862 bytes). Omni 1.2 Flash, 4s, 9:16. Gemini watermark present. |
| 2 | Ready locally | 4s | Done (take saved) | `renders/shot02_ready_locally.mp4` (1,997,569 bytes). Last frame extracted for continuity: `reference-screenshots/shot02_lastframe.png`. |
| 3+4 | One clean send + Alive while thinking (combined) | 8s | Blocked / needs retake | `renders/shot03_04_take1.mp4` (1,732,101 bytes). Veo 3.1 Lite take was bad: hand holding phone, gibberish UI, giant white FRIDAY bubble, large blue Send button, garbled reply, Veo watermark. Corrected prompt ready; Flow reports 24 credits needed. Recommended retake: Omni 1.2 Flash or Veo 3.1 Lite with corrected prompt and 3 references. |
| 5 | Every run, measured | 4s | Not started | Prompt ready in `SHOT_PROMPTS.md`. |
| 6 | Open the HUD | 6s | Not started | Prompt ready in `SHOT_PROMPTS.md`. |
| 7 | Tuned for this device | 4s | Not started | Prompt ready in `SHOT_PROMPTS.md`. |
| 8 | Clarity at every layer | 4s | Not started | Prompt ready in `SHOT_PROMPTS.md`. |
| 9 | Metrics persist | 6s | Not started | Prompt ready in `SHOT_PROMPTS.md`. |
| 10 | Crafted in v0.9.2 | 4s | Not started | Prompt ready in `SHOT_PROMPTS.md`. |
| 11 | Lockup | 6s | Not started | Prompt ready in `SHOT_PROMPTS.md`. |

## Current blocker

Flow generation is blocked by credits/quota: combined Shot 3+4 retake needs 24 credits.

## Ready for next run

Combined Shot 3+4 retake inputs:

- Model: Omni 1.2 Flash preferred for retake, 8s, 9:16 (or Veo 3.1 Lite if desired).
- References: `reference-screenshots/shot02_lastframe.png`, `reference-screenshots/03_compose_typed.png`, `reference-screenshots/04_stream_c.png`
- Corrected prompt: removes hands/fingers, giant overlays, large blue Send button, invented text; starts from Shot 2 last frame.

## Files in this pack

- `SHOT_PROMPTS.md`: full shot plan, global style bible, negative prompt, per-shot prompts.
- `STATUS.md`: this file.
- `reference-screenshots/`: 14 app screenshots, 2 logos, `shot02_lastframe.png`.
- `renders/`: saved generated takes.
- `frames/`: extracted analysis frames for bad take.

## Next steps

1. When credits are available, regenerate combined Shot 3+4 with the corrected prompt.
2. If AI generation keeps inventing UI/text, use compositing fallback: clean phone/background video plus real Friday screenshots animated in editor.
3. Continue with Shot 5 onward after 3+4 is acceptable.
4. Add post captions, music, and final lockup text in editor; handle Gemini/Veo watermark per Flow plan/export terms.
