# PLACEHOLDERS.md: every value in assets/shots that is NOT derivable from source

Rule: values marked `data-placeholder` in `shots_src/*.html` are placeholders. They are NOT
real benchmarks. In the film they may only appear labeled ILLUSTRATIVE or with no numeric
claim, unless replaced by real values below.

## Global (every shot)
| Item | Value used | Why placeholder |
|---|---|---|
| Status bar clock + signal icons | 10:08, generic wifi/signal/battery | Platform UI; not defined by app source. Documented approximation. |
| Gesture nav pill | generic 108x4dp pill | Platform UI approximation. |

## 02_models (drawer)
| Item | Value used | Needs |
|---|---|---|
| Model status pill state | "No local model loaded" (the real default string) with dot #A1A1AA | Real state on device at capture time (ready/loading/error changes dot + text). |
| Session row message count | "0 messages" | Real session count. |
| Session title | "New session" (real fallback string) | Real first-session title. |

## 04_stream_a/b/c (streaming reply)
| Item | Value used | Needs |
|---|---|---|
| Reply text (one response at 3 lengths) | "Here is a concise draft you can send as is. / Subject: Quick follow-up on our roadmap / ..." | ONE real response text from a real run on device. Same text must back a/b/c. |

## 05_footer + 09_relaunch_before/after (metrics footer)
| Item | Value used | Needs |
|---|---|---|
| Footer values | "12.6 tok/s  •  2.41s" | Real tok/s + duration from a real generation. Before/after must stay identical. |

## 06_hud (telemetry HUD)
| Item | Value used | Needs |
|---|---|---|
| DECODE SPEED | 12.6 tok/s | Real value (>= 3.0 so the tip stays hidden). |
| TOTAL LATENCY | 2.41s | Real total ms. |
| COMPUTE BACKEND | LOCAL // CPU (amber dot per rule) | Real backendLabel. |
| OUTPUT VOLUME | ~214 tokens | Real estimatedTokens. |
| ACTIVE MODEL | granite-4.0-350m-fp16.litertlm + FP16 tag | Real model filename (quant tag follows the real rule). |

## 07_tip (performance tip card)
| Item | Value used | Needs |
|---|---|---|
| DECODE SPEED | 2.8 tok/s (drives the < 3.0 tip gate) | Real value from a CPU-bound generation. |
| Other HUD values | same placeholders as 06 | Real values. |
| Tip text + "Open Settings" | exact source strings (real) | n/a |

## 08_settings (NEURAL ENGINE)
| Item | Value used | Needs |
|---|---|---|
| Model status pill | "No local model loaded", grey dot | Real state. |
| Model row filenames + paths | qwen3-0.6b.litertlm, granite-4.0-350m-int8.litertlm, granite-4.0-350m-fp16.litertlm + /data/user/0/com.friday.assistant/files/models/... | Real runtime filenames (path.substringAfterLast('/')). |
| Selection state | qwen3 selected | Real selected model. |

## 10_about (diagnostics)
| Item | Value used | Needs |
|---|---|---|
| Diagnostics block | Device: Pixel 8 / OS: 15 / ABI: arm64-v8a / App: v0.9.2 / Mode: LOCAL / Model: granite-4.0-350m-fp16.litertlm / Last model load: 1834 ms / Last generation: 2410 ms / Sessions: 3 (12 messages) / TTS: ready | Every line comes from DiagnosticsInfo on a real device (SettingsScreen.kt:820-832). |
