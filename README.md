# Hyle — Design System

**Hyle** is the render-side design system for the Phonebrew / Aarso constellation: the
**tokens** (`Finish`, `Pulse`, `RadiantHues`) and the contract the renderer obeys. It is
**open**, publishable as `dev.aarso:hyle:0.1.0`, and consumed by the open core
(`android-ide-core`) and the other apps purely through the token contract — surfaces
reference tokens, never literals, so they "get dressed" when Hyle's Compose atoms land.

- **`:hyle`** — pure token data (JVM-tested); the publishable AAR.
- **`:hyle-probe`** — an on-device render harness app for Hyle.

> Extracted from the Aarso monorepo (`mbaliga/Android-IDE-Studio`) per its
> `docs/EXTRACTION_PLAN.md`. The monorepo retains the original commit history; this repo
> begins Hyle's independent life (it was always staged as a self-contained module for
> exactly this graduation).

## Build
```
./gradlew :hyle:test                 # JVM token tests
./gradlew :hyle:publishToMavenLocal  # prove it stands alone as dev.aarso:hyle:0.1.0
```
Requires an Android SDK (`local.properties` → `sdk.dir`), JDK 17.
