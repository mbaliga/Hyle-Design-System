# Hyle Design System

> *Hyle* (ὕλη) — Aristotle's word for matter: pure potentiality awaiting form.

A **cross-platform** design system governed by one law:

> **State is SHOWN by material behavior, never SAID by language.**

No status words, no spinners. Hyle tells the truth about *computation* through
material. A single set of design tokens is the source of truth and compiles to
**Android, web, and iOS** from `tokens/*.json`.

## One monorepo, two render sides

```
tokens/*.json   ──►  scripts/build-tokens.js (Style Dictionary)  ──►  every platform
(W3C DTCG)
   │
   ├─► Android   hyle/src/main/java/dev/aarso/hyle/tokens/HyleTokens.kt   (Kotlin, Argb)
   │             hyle/src/main/res/values/hyle_tokens_*.xml               (colors / dimens)
   ├─► Web       build/web/tokens.css · _tokens.scss · tokens.js
   └─► iOS       build/ios/Tokens.swift
```

### Android side — `:hyle` (the native render contract)

The publishable Kotlin library (`dev.aarso:hyle:0.1.0`): the hand-authored
contract — `Finish` (Reflective / Radiant), `Pulse` ("heartbeat, not weather"),
`RadiantHues` — **plus** the generated `HyleTokens` object compiled from the
shared token source. `:hyle-probe` is the on-device render harness, and
`:wallpaper` ([Hyle Worlds](wallpaper/)) is a Brutalist **live wallpaper** that
runs the Form-World raymarcher (the procedural worlds) as an OpenGL ES 2.0
`WallpaperService`, themed from the same tokens.

```bash
./gradlew :hyle:test                 # JVM token tests
./gradlew :hyle:publishToMavenLocal  # prove it stands alone as dev.aarso:hyle:0.1.0
```
Requires an Android SDK (`local.properties` → `sdk.dir`), JDK 17.

### `:crash-recovery` — a shared reliability utility, NOT part of Hyle

Publishable as `dev.aarso:crash-recovery:1.0.0`. It lives in this repo (the
constellation's one sharing mechanism, D-A) but has **zero dependency on `:hyle`**
— no Compose, no Material, plain `android.widget` views only — so it is a
reliability utility, not a design-system dependency. That distinction matters:
apps with their own visual identity that must never depend on Hyle (Animalcules,
Horizkeeb — see Personal-Tracker DECISIONS.md D-L) can still take this one
dependency (see D-O).

Captures a device-only launch/runtime crash (CI never sees these — CI runs unit
tests, never launches the app) to the app's private files dir, then shows a
recovery screen on the next launch instead of the app's real content — headline
first, Share/Copy, Continue, a confirm-gated Reset, and the full trace collapsed
behind a "Technical details" toggle. Colours are plain `@ColorInt Int`s
(`CrashRecoveryStyle`) so each consumer themes it to its own palette without
taking on Hyle's tokens.

```kotlin
// Application.onCreate(), before constructing anything that could itself throw:
CrashRecovery.install(this, appLabel = "Runout")

// first thing in the launcher Activity's onCreate():
if (CrashRecovery.maybeShowRecovery(this, appLabel = "Runout")) return
```

```bash
./gradlew :crash-recovery:test                 # JVM tests (formatting/persistence, no Android SDK needed to run)
./gradlew :crash-recovery:publishToMavenLocal   # prove it stands alone as dev.aarso:crash-recovery:1.0.0
```

### Web side — tokens, Lit components & Storybook

Framework-agnostic **Lit web components** consume the same tokens. The two
material layers:

- **The Field** (`<hy-field>`) — the living layer *behind the pane to indicate
  movement*; hosts the **Form-World** WebGL engine ([`field/`](field/)).
- **The Pane** (`<hy-pane>`) — frosted glass over the Field, carrying readable
  controls (`<hy-chip>`, `<hy-button>`, `<hy-input>`).
- **The Tactile Kit** ([`kit/`](kit/)) — the physical-control language ("soft
  brutalism"): knobs, faders, toggles, crater buttons, meters in honest materials.

```bash
npm install
npm run tokens:build   # compile tokens → :hyle module + build/{web,ios}
npm run storybook      # browse the Field, Pane, tokens, components at :6006
```

#### Component inventory

| Group     | Elements                                                                 |
| --------- | ------------------------------------------------------------------------ |
| Material  | `hy-field` · `hy-pane`                                                    |
| Controls  | `hy-chip` · `hy-button` · `hy-input` · `hy-slider`                        |
| Tactile   | `hy-knob` · `hy-fader` · `hy-toggle` · `hy-key` · `hy-joystick` · `hy-dial` · `hy-transport` |
| Displays  | `hy-meter` · `hy-vu` · `hy-waveform` · `hy-screen`                        |
| Surfaces  | `hy-card` · `hy-grille` · `hy-jack`                                       |

Two larger, app-specific pieces from the Tactile Kit are intentionally **left
living in [`kit/`](kit/)** rather than reimplemented as components: the full HSL
**colour picker** (ring/slice/palette) and the **folders** browser. They're
better used as references or embedded whole than distilled into primitives.
Mock apps that compose the components live under **Storybook → Mock Apps**.

## Project layout

| Path                      | What it is                                                       |
| ------------------------- | ---------------------------------------------------------------- |
| `tokens/`                 | **Source of truth** — W3C DTCG tokens (field, ink, accent, motion). |
| `scripts/build-tokens.js` | Style Dictionary pipeline → Kotlin / Android res / web / iOS.     |
| `hyle/`                   | Android library: `Finish`/`Pulse`/`RadiantHues` + generated tokens. |
| `hyle-probe/`             | Android on-device render harness.                                |
| `wallpaper/`              | **Hyle Worlds** — Brutalist live wallpaper (Form-World via GLES2).|
| `src/components/`         | Lit web components, one folder per component, with stories.      |
| `field/`                  | The Form-World engine + its README / ARCHITECTURE / ROADMAP.     |
| `kit/`                    | The Tactile Kit (physical-control language) + its README.        |
| `public/`                 | Engine + kit served to Storybook (`<hy-field>`, Tactile Kit story). |
| `docs/PHILOSOPHY.md`      | Hyle — Ethos & Lineage (the theory behind the law).              |
| `stories/`                | Storybook Introduction + Foundations (token) docs.               |
| `build/`                  | **Generated** web/iOS token artifacts (git-ignored).             |

## Shared tokens, one source

Tokens use the [W3C DTCG](https://www.designtokens.org/) format. Colours carry
alpha; dimensions are stored **unitless** so each platform applies its own unit
(`dp`/`sp` Android · `px` web · `pt` iOS); durations compile to `ms` on web and
`Int` on Kotlin. Editing a token and running `npm run tokens:build` updates the
Kotlin `HyleTokens`, the Android resources, the web CSS, and the iOS Swift at once.

The provenance hues are aligned across platforms: `provenance.native`
(`#C7EF9E`) matches `RadiantHues.RADIUM`, `provenance.cloud` (`#35E0FF`) matches
`RadiantHues.COLD_CYAN`.

### Accessibility (hard gates from the ethos)

- Provenance must **never** carry meaning by colour alone — pair every hue with a
  second non-colour channel (form, motion, position, icon). WCAG 1.4.1.
- UI surfaces sit at `#121212`-class, never pure black, to reduce halation.
- Verify ≥4.5:1 text / ≥3:1 non-text contrast per theme with real testing.

## Scripts

| Script                    | Does                                              |
| ------------------------- | ------------------------------------------------- |
| `npm run tokens:build`    | Compile tokens for every platform.                |
| `npm run storybook`       | Run Storybook (rebuilds tokens first).            |
| `npm run build-storybook` | Static Storybook build into `storybook-static/`.  |
| `./gradlew :hyle:test`    | JVM token tests for the Android library.          |

> Both large authored artifacts (the Form-World engine and the Tactile Kit) are
> **generated/standalone HTML** — see their own READMEs under `field/` and `kit/`.
