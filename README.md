# React

**A modern, modular and high-performance AI powered anti-cheat built for Paper and Folia servers.**

[![Java](https://img.shields.io/badge/Java-21+-orange)](...)
[![Paper](https://img.shields.io/badge/Paper-1.21.11-blue)](...)
[![Folia](https://img.shields.io/badge/Folia-Supported-green)](...)
[![License](https://img.shields.io/github/license/g4vrk/React)](LICENSE)

---

## Overview

React is a next-generation Minecraft AI anti-cheat focused on detection accuracy, scalability and maintainability.

Unlike traditional anti-cheats that rely heavily on static thresholds, React combines packet analysis, behavioral analysis and machine learning to identify illegitimate player actions while minimizing false positives.

Designed from the ground up with a modular architecture, React is suitable for both small private servers and large production networks.

---

## Highlights

- ⚡ High-performance packet analysis
- 🧠 Machine Learning assisted detections
- 🎯 Accurate Aim & Rotation analysis
- ⚔️ Combat detection framework
- 📦 Modular architecture
- 📡 Async processing
- 🔧 Fully configurable checks
- 📢 Flexible alert system
- 📊 Detailed violation information
- 📚 Developer-friendly API
- 🧩 Paper & Folia support
- 🚀 Built for Java 21

---

## Platform Support

| Platform | Version | Java         |
|----------|---------|--------------|
| Paper | 1.16.5+ | 17 and higher |
| Folia | 1.20.6+ | 21 and higher |

---

## Project Structure

| Module | Description                                               |
|---------|-----------------------------------------------------------|
| `common` | Shared anti-cheat logic, checks, processors and utilities |
| `paper` | Paper implementation                                      |
| `folia` | Folia implementation                                      |

---

## Detection Engine

React is built around independent detection modules.

Current detection categories include:

- Rotation
- Combat
- Machine Learning

Every check is isolated from the others and provides its own:

- configuration
- violation system
- decay strategy
- debugging information
- alert formatting

This makes new checks easy to develop and existing ones simple to maintain.

---

## Machine Learning

React includes a machine learning pipeline that assists traditional heuristic checks.

Instead of replacing conventional detections, the ML system acts as an additional confidence layer, allowing React to improve detection accuracy while reducing false positives.

---

## Alert System

The alert system supports:

- Adventure Components
- Rich formatting
- Console listeners
- Permission filtering
- Async publishing
- Custom preprocessors

---

## Performance

Performance has been one of the primary goals since the beginning of the project.

React minimizes:

- object allocations
- synchronous work
- packet overhead
- unnecessary computations

Heavy calculations are executed asynchronously whenever possible.

---

## Building

Requirements

- Java 21+
- Gradle 9+

```bash
./gradlew build
```

---

## Contributing

Contributions, bug reports and feature requests are always welcome.

Please open an Issue before making significant architectural changes.

---

## License

React is distributed under the license included in this repository.