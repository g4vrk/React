# React

**Aim-only anti-cheat for Paper and Folia, with an optional AI verdict layer.**

[![Java](https://img.shields.io/badge/Java-21+-orange)](.)
[![Paper](https://img.shields.io/badge/Paper-1.16.5+-blue)](.)
[![Folia](https://img.shields.io/badge/Folia-Supported-green)](.)
[![License](https://img.shields.io/github/license/g4vrk/React)](LICENSE)
[![AI Server](https://img.shields.io/badge/AI%20Server-optional-purple)](#ai-verdict-server)

---

> [Русская версия](README.md)

## What this is

React does one thing: it watches how players aim, and decides whether a human could have produced that data.

No block-break heuristics, no inventory checks, no fifteen unrelated modules bolted onto a "combat suite." Every check in this repo exists to answer one question — is this rotation pattern physically plausible — using acceleration deltas, GCD-based error scoring, and quantized rotation comparison instead of static thresholds that break the moment someone changes their sensitivity.

The plugin itself is free and MIT-licensed. Run it as-is and you get a solid heuristic pipeline out of the box. If you want a second opinion, point it at the **AI Verdict Server**: a separate service that ingests rotation samples, runs them through a model, and returns a confidence score. That part is a paid, hosted add-on — but it's also swappable. Bring your own model, your own inference endpoint, your own thresholds. React doesn't lock you into ours.

---

## How detection works

- **Heuristic layer (open source, local).** Every check runs on the server, in real time, with no external calls. GCD-based comparisons, acceleration delta tracking, and mode-averaged rotation quantization replace the old pairwise-GCD approach, which was prone to false positives whenever a player's mouse polling rate didn't line up neatly with in-game tick boundaries.
- **AI layer (hosted, optional).** Rotation history gets batched and sent to the verdict server, which returns a verdict plus a confidence value. Configurable batch size, sample window, and confidence thresholds mean you decide how much weight the AI opinion carries relative to the heuristic checks.
- **Verdict, not a ban hammer.** React reports a score and a reasoning trail. What you do with a flagged player — mute, kick, ban, silent-log — is your call, not baked into the plugin.

---

## Why not just more thresholds

Most GrimAC-style checks fail the same way: static cutoffs that work for one client, one sensitivity, one FPS cap, and fall apart everywhere else. React's heuristic layer was rebuilt around **mode-averaged quantum comparison** instead of chained pairwise GCD calculations, specifically to kill the instability, quantization false positives, and NaN propagation that plague naive GCD-based aim checks. The AI layer exists for the cases heuristics structurally can't catch — patterns that are individually legal but collectively inhuman.

---

## Free vs. paid

| | Core plugin | AI Verdict Server |
|---|---|---|
| License | MIT, open source | Paid hosted service |
| Runs | On your server | Cloud, or self-hosted with your own model |
| What it does | Heuristic aim checks, config, alerts | Rotation-pattern verdicts via ML |
| Required? | Yes | No — the plugin works fully standalone |

---

## Platform support

| Platform | Version | Java |
|----------|---------|------|
| Paper | 1.16.5+ | 17+ |
| Folia | 1.20.6+ | 21+ |

## Project structure

| Module | Description |
|--------|-------------|
| `common` | Shared check logic, processors, utilities |
| `paper` | Paper implementation |
| `folia` | Folia implementation |

## Features

- GCD-error and acceleration-delta based aim analysis
- Mode-averaged quantum rotation comparison (no pairwise GCD chaining)
- Streak-based violation buffering with decay
- Optional AI verdict layer — hosted or bring-your-own-model
- Fully async, low allocation, built for Java 21
- Configurable per-check thresholds, no recompiling to tune sensitivity
- Adventure-based alerts with permission filtering

## Building

Requirements: Java 21+, Gradle 9+

```bash
./gradlew build
```

## AI Verdict Server

The AI server is a separate component from this repository — see its own docs for setup. It exposes a simple ingestion endpoint for rotation samples and returns a verdict object (score, confidence, reasoning tags). You can point React at Anthropic's/your provider's hosted instance, or run your own model behind the same API contract.

## Contributing

Issues and PRs are welcome. Open an issue before starting on anything architectural — it saves both of us a rewrite.

## License

React (the plugin) is MIT-licensed — see [LICENSE](LICENSE). The AI Verdict Server is a separate paid service and is not covered by this license.