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
| Surfaces  | `hy-card` · `hy-grille` · `hy-jack` · `hy-terminal`                       |

#### Surface finishes

The house assigns materials by meaning, so each finish answers a different
question about the layer you are looking at:

| Finish       | What that layer is                                                    |
| ------------ | --------------------------------------------------------------------- |
| **glass**    | the layer you read *through* — `hy-pane`, frosted over a living Field  |
| **grille**   | the panel you *touch* — perforated, acoustically honest               |
| **jack**     | the panel you *connect to* — a machined socket                         |
| **terminal** | the layer a machine *writes to while you watch* — `hy-terminal`        |

`terminal` is cut **into** the surface rather than laid on it, which is why its
ground sits below `field.raised` and it carries an inner shadow rather than a
drop shadow, and why it is square: a terminal has no corner radius.

Two things it deliberately is *not*: there is no phosphor green and there are no
scanlines by default. A terminal that cosplays a VT100 is *saying* "this is
technical" in language, which is the exact move the core law forbids. What makes
it read as a terminal is the fixed advance, the well, and the cursor, each of
which does work. Scanlines are opt-in, for the one case where a CRT is literally
the subject.

State is carried entirely by the cursor: `idle` holds and blinks, `working`
breathes, `failed` stops and takes the danger hue. No status word, no spinner.
Because the cursor's *motion* changes as well as its colour, the state survives
greyscale and colour-vision deficiency (WCAG 1.4.1); under
`prefers-reduced-motion` every state holds solid and the cursor stays the state
channel without moving. The prompt glyph takes the provenance hue, so whether
the work is happening on this device or elsewhere is legible from the prompt
alone.

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
| `assets/primitives/`      | Raw third-party icon/logo reference pack — not wired into components or Storybook. See `assets/primitives/LICENSE-NOTE.txt`. |
| `assets/baliga-portfolio-assets/` | Scaffold for your own logos — mdhv.xyz, the asystemofcells brand, and per-product folders. Empty until real assets are added; see its own `README.md`. |

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
