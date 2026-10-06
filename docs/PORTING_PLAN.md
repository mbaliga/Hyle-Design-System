# Hyle Design System — multi-platform porting plan

> Part of the constellation-wide porting program (`Personal-Tracker/PORTING_PROGRAM.md`, 2026-10-06).
> Status: **PLAN — nothing in this document has been built.** Every claim about a target platform is
> labelled with its evidence class (§0). This file is owned by the lead planning session; a platform
> track updates only its own §4 row and appends to the progress ledger in §10 (this repo has no STATE
> or PROGRESS file; `README.md` is its index and carries the pointer to this file).
> Repo facts were read from the checkout (`main` at 07bb1ca, 2026-09-16) on 2026-10-06. The open pull
> requests were read through the GitHub API, and PR #16's branch through a read-only clone, the same day.

## 0. Evidence labels (never dropped)

asom's set, unchanged: `LAB` · `CI (hosted VM) evidence` · `EMULATOR EVIDENCE` · `SIMULATOR` ·
`CI-APPROX — NOT DEVICE EVIDENCE` · `SIMULATED — NOT DEVICE EVIDENCE` · `VIRTUALIZED — NOT DEVICE EVIDENCE` ·
`SYNTHETIC` · `CI-ONLY / NOT RUN` · `NEEDS-DEVICE-VALIDATION` (NDV) · `NEEDS-OWNER-VALIDATION` (NOV).
The program's additions: `PLAN` · `NOT-APPLICABLE (<reason>)` · `CONTAINER-BUILD-ONLY` (this container: JVM
x86_64 compile and tests, nothing else) · `BROWSER-HEADLESS`.

## 1. What this repo is, in porting terms

**Product.** The constellation's shared design system, governed by one law: state is shown by material
behaviour, never said by language. One W3C DTCG token source (`tokens/*.json`) compiles through Style
Dictionary to Android XML, a Compose-free Kotlin `HyleTokens`, web CSS/SCSS/JS and an iOS Swift file. On top
sit the Android library `dev.aarso:hyle` (`Finish`, `Pulse`, `RadiantHues`, `Provenance`, `Glyph`), a Lit 3
web-component kit with Storybook, the Form-World WebGL raymarcher, six OFL font families, two Android apps
(Hyle Probe, Hyle Worlds) and a legacy copy of `dev.aarso:crash-recovery`.

**State** (`README.md`, `git log`, `.github/workflows/`). Active: 47 commits from 2026-06-30 to 2026-09-16.
`ci.yml` runs `:hyle:test`, `:crash-recovery:test` and `:wallpaper:assembleDebug` on `ubuntu-latest`;
`build-apk.yml` publishes a rolling `hyle-worlds-latest` prerelease; `storybook.yml` deploys
https://mbaliga.github.io/Hyle-Design-System/; `cleanup-artifacts.yml` exists because Actions storage is
exhausted. The repo is public (GitHub API, 2026-10-06). Consumers of `dev.aarso:hyle` found by
`grep -rl 'dev.aarso:hyle' --include=*.kts` over the local checkouts: Android-IDE-core (`core-engine`,
`hyle-probe`; submodule pin 33b0faa), Fyl-Manager and Foto-Xplorr (pin c586f8f; each with an explicit
`substitute(module("dev.aarso:hyle")).using(project(":hyle"))` rule in `settings.gradle.kts`). Android-IDE-Studio
carries its own vendored `:hyle` (`hyleVersion = "0.1.0"`) and is outside this plan. Whether Foto-Xplorr and
Fyl-Manager are *officially* Hyle consumers is open (PT:D-W, OQ-29), although both build against it today.

**Open pull requests that change the porting picture** (GitHub API, 2026-10-06; none is merged):

| PR | State | What it changes | Why it matters here |
|---|---|---|---|
| #16 `claude/fonebrew-development-clzu43` | draft, reported mergeable | Adds a Compose form layer to `:hyle`: `cells/`, `component/`, `theme/` (29 Kotlin files, 6,631 LOC; 12 test files, 1,207 LOC, one of them a Robolectric render test), `api(...)` androidx Compose BOM/ui/material3, `kotlin.plugin.compose`, version 0.2.1; also tombstones crash-recovery | Decides what "convert `:hyle` to KMP" means: 308 LOC of pure data (`main` today) or about 6.9k LOC including Android Compose. Measured on the PR branch (`grep '^import android\.'`): 7 non-androidx `android.*` imports in 3 files: `HyleSubstrate` (Bitmap, BitmapShader, Paint, Shader), `HyleHaptics` (View, HapticFeedbackConstants), `Aeon` (BitmapFactory). The rest is plain androidx Compose |
| #10 | draft, reported conflicting | Also adds `cells/` and `theme/` to `:hyle` (33 files) | Overlaps #16's paths (#16 says it merges the stranded work of closed #13), so expect conflicts if both land |
| #7 | draft | Removes `:wallpaper`, `build-apk.yml` and the wallpaper CI step; the app moves to `mbaliga/portfolio` | If it lands, every Hyle Worlds reframe below belongs to portfolio's plan |
| #11 | draft | Adds `hy-terminal`; edits `tokens/` and the generated Kotlin and XML | Touches the generated files L1 moves; regenerate, never hand-merge |
| #15 | draft | Tombstones `:crash-recovery` (names Shared-Libraries-asoc 1.2.0; #16's copy says 1.5.0) | Not a Hyle port concern; changes what `ci.yml` tests |
| #19 | open | Docs only: `docs/design/flat-edge-controls.md`, a second, flat control language implemented in Foto-Xplorr | Another visual language the atoms step must not conflate with the Tactile Kit or the cells layer |

**Observed drift, not edited by this plan** (docs convention: pointer edit only). `README.md` says
`dev.aarso:hyle:0.1.0` and crash-recovery `1.0.0`, while `hyle/build.gradle.kts` is `0.2.0` and
`crash-recovery/build.gradle.kts` is `1.1.0`; the `settings.gradle.kts` comment also says `0.1.0`. `README.md`
says the tokens "compile to Android, web, and iOS", but the web and iOS outputs go to a gitignored `build/` and
nothing consumes them. `package.json` says `"license": "MIT"` while `LICENSE` is Apache-2.0. The crash-recovery
tombstone that Fyl-Manager's and Foto-Xplorr's settings comments describe (`crash-recovery/MOVED.md`) exists
only on unmerged branches (`claude/fonebrew-development-clzu43`, `claude/repo-status-overview-2c0rgm`), not on
`main` or at either pin.

**Stack.** Kotlin 2.1.0 (`gradle/libs.versions.toml`); TypeScript 5.7 and Lit 3.2 (`src/`); Node 20 build
scripts (`scripts/`); GLSL ES 1.00 (`wallpaper/src/main/res/raw/world_frag.glsl`); a Python 3 generator
(`field/form-world.build.py`); single-file HTML (`kit/tactile-kit.html`). UI: androidx Compose BOM 2025.05.01 +
Material3 in `hyle-probe` and the wallpaper settings screen; plain `android.widget` in `crash-recovery`; Lit 3 +
Storybook 8.6 on the web. Not Compose Multiplatform: `org.jetbrains.compose` appears nowhere on `main`. Build:
Gradle 8.14.3, AGP 8.9.1, JDK 17 toolchain, npm (style-dictionary 4.3, vite 5.4). minSdk 31, compileSdk 36.
Native dependencies: none.

**Size** (measured 2026-10-06 on `main`). `git ls-files | wc -l` = 3,119 tracked files: 2,583 under `assets/`
(a third-party reference pack, wired into nothing), 378 under `fonts/`, 158 elsewhere. Kotlin by
`git ls-files <dir> | grep '\.kt$' | xargs cat | wc -l`: `hyle` 308, `hyle-probe` 1,444, `wallpaper` 572,
`crash-recovery` 1,539 (3,863 total). `src/` TypeScript 5,511 lines; `scripts/` 396; `tokens/` 180. Tests: 18
`@Test` in 3 files, all JVM-side (`hyle` 8, `crash-recovery` 10). Nothing tests the web kit, the shaders or the
token outputs.

## 2. Portable core vs platform-bound layers

| Module / dir | Role | Portability | Approx LOC | Notes |
|---|---|---|---|---|
| `tokens/` + `scripts/build-tokens.js` | The single source and its Style Dictionary pipeline (6 platforms) | portable (Node) | 180 + 396 | Kotlin and Android XML outputs are committed; web and iOS go to gitignored `build/`. The iOS format hard-codes `import UIKit` |
| `hyle/` (`:hyle`) | The contract: `Finish`, `Pulse`, `RadiantHues`, `Provenance`, `Glyph` (`Hyle.kt`, 117 lines) + generated `HyleTokens.kt` (108) + generated XML | pure Kotlin, zero `android.*` imports, built as an AAR | 308 | Mechanical to convert on `main`; on #16's branch it gains 29 Compose files |
| `hyle-probe/` | Android app, five tabs; three use AGSL `RuntimeShader` | android-bound | 1,444 | Compose code is otherwise source-compatible with Compose Multiplatform |
| `wallpaper/` | Hyle Worlds: `WallpaperService`, EGL14/GLES2, `SharedPreferences` | android-bound; the shader is portable | 572 + 246 GLSL | Product category absent off-Android; #7 proposes moving it out |
| `crash-recovery/` | Legacy 1.1.0 copy | android-bound (only `CrashReport.kt` is pure JVM) | 1,539 | Out of scope: canonical home is Shared-Libraries-asoc (F2) |
| `src/` | Lit kit: 26 component folders, `theme.ts`, `kit-runtime.ts` | web, any modern WebView | 5,511 | WebGL1, Web Audio, `navigator.vibrate`; no fetch or CDN loads |
| `field/` | Form-World engine (generated HTML, GLSL source of truth) | web / GLES2 | 1,519 (646 generator) | Dynamic uniform-array indexing is implementation-defined in WebGL1 (`field/ROADMAP.md`) |
| `kit/`, `public/` | Tactile Kit source and served copies | web | 1,948 / 5,479 | Components are extracted verbatim from `kit/tactile-kit.html` |
| `stories/`, `.storybook/` | Storybook IA | web | 702 | The cross-platform reference gallery, not a target |
| `fonts/` | Six OFL 1.1 families, 25 TTFs | portable data | none | a `grep` for the four family-name prefixes over source, style, HTML, JSON and Markdown files finds no use outside `fonts/`; `tokens/typography.json` names Archivo Variable and JetBrains Mono (npm `@fontsource-variable`), and the Android probe uses `FontFamily.Monospace` |
| `assets/` | Third-party icon and logo reference pack | not part of any build | none | Excluded from every package this plan produces |

| Platform-bound API | Where | Porting impact |
|---|---|---|
| `com.android.library` + `kotlin.android`, no `multiplatform`; zero `expect`/`actual` | `hyle/build.gradle.kts` | The one gate for every Compose or KMP consumer: today `dev.aarso:hyle` resolves only from an Android module |
| `android.graphics.RuntimeShader` (AGSL, API 33+) + `ShaderBrush`/`drawWithCache` | `hyle-probe/.../{RadiantGlowProbe,GlassSandProbe,FerrofluidProbe}.kt` | Android-only API. That the shader text is SkSL-compatible is an assumption (AGSL derives from SkSL) until compiled with `RuntimeEffect` |
| `WallpaperService`, `WallpaperManager`, `EGL14`, `GLES20` | `wallpaper/.../*.kt` | Live wallpaper is an Android category; the GLSL ES 1.00 shader ports, the host is reframed or dropped |
| `import UIKit` + `color/UIColorSwift` transform | `scripts/build-tokens.js` | Generated Swift cannot compile for AppKit or a SwiftUI-only package; nothing consumes it today |
| WebGL1, Web Audio, `navigator.vibrate`, `postMessage` bridges | `src/components/field/hy-field.ts`, `src/kit/kit-runtime.ts` | Present in every WebView host; `vibrate` is a no-op on desktop |
| `ubuntu-latest` only (`android-actions/setup-android`) | `.github/workflows/*.yml` | No macOS or Windows lane exists; the repo is public, so hosted minutes are free |

## 3. Binding rules this port must not break

1. **The core law** (`README.md`, `docs/PHILOSOPHY.md`): no status words or spinners as the primary state channel. PT:D-W (2026-08-03) clarifies, not relaxes: it governs real-time legibility; helper copy, labels and on-demand detail are fine. A port never substitutes a platform spinner or status string for the material signal.
2. **Colour is never the sole carrier of meaning** (WCAG 1.4.1; `README.md` hard gates, `docs/PHILOSOPHY.md` §5e, `tokens/color.json` descriptions on `provenance.*`, enforced for provenance by `hyle/src/test/.../ProvenanceTest.kt`). The provenance hue always pairs with `Glyph`. Constellation corollary I-3: red and green never carry meaning (the owner is colour-blind).
3. **Accessibility gates** (`README.md`): UI surfaces at `#121212`-class (the Field canvas may be `#000000`); 4.5:1 text and 3:1 non-text contrast per theme, verified by real testing, not by eye.
4. **One violet accent, user-selectable** (`src/theme/theme.ts`, `tokens/color.json`): semantic tokens reference the primitive accent so retheming works. A native port rethemes from one accent; it does not freeze a palette.
5. **`tokens/*.json` is the only source.** `HyleTokens.kt` and the XML say "Do not edit by hand"; every new platform output is a Style Dictionary format in `scripts/build-tokens.js`, never a hand copy.
6. **`field/` owns the shader.** `field/form-world.html` is generated by `field/form-world.build.py` and not hand-edited; `wallpaper/src/main/res/raw/*.glsl` are re-extracted from it (`wallpaper/README.md`). There is no GPU in the build environment: a passing build is necessary, not sufficient, and nothing may claim a shader renders without device verification (`field/README.md`, `Hyle.kt` KDoc).
7. **`kit/tactile-kit.html` is the source for the control language** (`kit/README.md`, `src/kit/kit-runtime.ts`): components are extracted verbatim. A native control is a reference-faithful re-render, not a reinterpretation.
8. **Coordinates.** `dev.aarso:hyle:0.1.0` is permanently burned (`CONTRIBUTING.md`, `hyle/build.gradle.kts`, PT:D-A). Project-level `group = "dev.aarso"`, the version and the project path `:hyle` must survive, because `includeBuild` substitution matches on `group:name` and Foto-Xplorr and Fyl-Manager name `project(":hyle")` explicitly. Breaking changes are flagged in the PR (`CONTRIBUTING.md`).
9. **Sharing and pins.** PT:D-A: submodule + `includeBuild`, Maven publishing a later owner-gated upgrade. PT:D-Q: every composite consumer's AGP equals Hyle's exactly (now 8.9.1, wrapper 8.14.3). Read in full on 2026-10-06: neither names an artifact shape, so neither forbids an AAR-to-KMP change; both constrain how it reaches consumers. OQ-17 rules the pins.
10. **D-L.** Apps with their own visual language (Animalcules and Horizkeeb, which `NAMES.md` records as the Clackpad repo) never become Hyle consumers: every consumer rehearsal and every "adopt the atoms" step below excludes them. `crash-recovery` has zero dependency on `:hyle` (D-O) and has moved to Shared-Libraries-asoc (`MIGRATION.md` there); it is not extended here.
11. **Licensing** (`LICENSE`, `NOTICE`, `TRADEMARKS.md`, `CONTRIBUTING.md`): code Apache-2.0 with DCO sign-off on every commit; fonts OFL 1.1 with Reserved Font Names; `assets/primitives/` is reference only; the Hyle, Aarso and `dev.aarso` marks are the owner's. `NOTICE` admits no third-party licence scan exists, so every new dependency (Compose Multiplatform, Skiko, Qt, XcodeGen, WiX) is recorded as it is added. This repo has no SPDX-header convention; F5's opt-in headers stay off unless the owner opts in.
12. **No telemetry, no network** (I-1; `.storybook/main.ts` sets `core.disableTelemetry: true`; no CDN loads; Form-World is "no server, no assets, no network"). Ports add no analytics SDK, font CDN or update check.
13. **The radiant hue is an open owner decision** (`RadiantHues` KDoc): `RADIUM` and `COLD_CYAN` both ship. Every output (Kotlin, QML, Swift) carries both; no port picks.
14. **Environment honesty** (I-4; `Hyle.kt` KDoc): no phone, emulator or GPU in the build container; render, gesture and shader behaviour is owner-verified. The desktop harness below is `CI-APPROX — NOT DEVICE EVIDENCE` at best.
15. **Program rules.** R2: `:hyle:test`, `:crash-recovery:test`, `:wallpaper:assembleDebug` and the Storybook build stay green. R3: new workflow files only, SHA-pinned (`ci.yml` pins by tag and is not edited). R6: no artifact uploads on PRs. R11 and R12 apply to any identifier and any reframe.
16. **Where proposals go.** This repo has no decision log (`field/ROADMAP.md`'s is Form-World-only); sharing-mechanism decisions live in Personal-Tracker `DECISIONS.md`. Proposals PH-1 to PH-5 (§5) are therefore written here and left for the owner to file there. This plan edits nothing in Personal-Tracker.

## 4. Target matrix (owner's order)

| Target | Feasibility | Approach | Blockers | Effort (eng-weeks, estimate) | Evidence today |
|---|---|---|---|---|---|
| Ubuntu Touch | reframe. Nearest shape: tokens + fonts + a QML face + a webapp-container reference gallery. Hyle Worlds has no native shape (Lomiri has no live-wallpaper API) | A `qml` Style Dictionary platform emitting `Tokens.qml`; fonts as click assets; the Storybook build in a webapp-container click as the zero-rewrite reference; optionally the Form-World shader in a QML `ShaderEffect` as an "ambient app"; a minimal QML atom set (pane, pulse, chip, button, provenance badge). No JVM and no Compose in the Lomiri app path | No UT device (OQ-1), so every device gate is NDV; whether the UT webview engine runs Lit 3 and WebGL1 is unknown; dynamic uniform indexing may fail on the Qt GLES path; Lomiri.Components theming is Qt 5 only until the Qt 6 migration; click identifiers wait for a NAMES.md row (R11, OQ-25) | 4 (excludes a full QML mirror of the 26-folder kit, +6 to 8, not recommended) | PLAN |
| Linux desktop | straight | Convert `:hyle` to `kotlin("multiplatform")` + `com.android.library` (`androidTarget()` + `jvm()`), pure contract first; lift the Compose layer into `commonMain` behind an `expect`/`actual` shader seam (AGSL vs Skia `RuntimeEffect`); a desktop render harness in a separate Gradle build; jpackage app-image for the harness only | OQ-17 pins; disposition of PRs #16/#10 (§1); how the KMP variants resolve through four consumers' `includeBuild` is unverified; no Android SDK or GPU in this container; publish location (OQ-24) | 3 (assumes `main`'s 308 LOC plus a minimal atom subset; lifting all 29 files of #16 is not covered and not estimated) | PLAN |
| iOS / iPadOS | straight | Two paths from one source. Swift consumers: a generated Swift token package (UIKit, AppKit and SwiftUI branches under `canImport`). Kotlin consumers: `iosArm64` + `iosSimulatorArm64` on `:hyle`, an XCFramework, and the Compose layer on Compose Multiplatform iOS with the `RuntimeEffect` actual. Form-World through a WKWebView, or an SkSL rewrite later. Hyle Worlds: `NOT-APPLICABLE (iOS has no third-party live wallpapers)` | No macOS lane exists yet; Apple Developer Program and delivery route for a device (OQ-2); OpenGL ES is deprecated; no GPU to validate either host; the hue decision is open | 3 | PLAN |
| macOS | native-fit | The same JVM artifact is planned to run unchanged; the Swift package gains its AppKit branch; the harness packaged as an unsigned `.dmg`. No `macosArm64` Kotlin/Native target unless a non-JVM consumer appears. A `.saver` reframe of Hyle Worlds is optional | No Mac on record (OQ-5), so device gates are NOV; Developer ID and notarisation (OQ-3); OpenGL is deprecated, so a `.saver` needs Metal, Skia or a WKWebView | 1.5 (excludes the optional `.saver`, +2 to 3) | PLAN |
| Windows | native-fit | The same JVM artifact; the harness packaged as an unsigned MSI by jpackage on `windows-2025`; fonts bundled in the jar. No native-Windows token format until a consumer exists. A `.scr` or Lively reframe is optional | Signing route (OQ-3; Azure Artifact Signing is unavailable to the owner); no Windows machine except the Dell while it is still Windows (OQ-5); Gradle-on-Windows quirks untested in the constellation (the path lint, §6, is clean today) | 1 (excludes the optional `.scr`, +2) | PLAN |

The five rows total 12.5 engineer-weeks, an estimate. Evidence reachable if the plan runs: Ubuntu Touch
`CI (hosted VM)` for generator drift and QML lint, everything on a device NDV; Linux `CI (hosted VM)`,
`CONTAINER-BUILD-ONLY` for a JVM-only build and `createDistributable`, `CI-APPROX — NOT DEVICE EVIDENCE` for
harness pixels; iOS `CI (hosted VM)` and `SIMULATOR`; macOS and Windows `CI (hosted VM)` with device gates NOV
until OQ-5 changes. No row can say a port "works" until its device gate is run by the owner.

## 5. Tier and sequencing

**Tier G: gate** (matches `Personal-Tracker/PORTING_PROGRAM.md` §5). Hyle is not a high-value end-user product
on any of the five targets; its apps are C or D at best. It is the shared dependency gate: no Compose app in
the constellation can build a non-Android target from `dev.aarso:hyle` until it exists as a KMP artifact, and
it is the cheapest conversion in the program (308 LOC of pure Kotlin on `main`, a token pipeline that already
emits web and Swift). Gate before any wave: OQ-17 ruled (PT:D-Q lockstep), a new PT:DECISIONS entry for the KMP
conversion, `0.1.0` never republished, and OQ-29 for the undecided consumers (F1's own gate).

| Wave | What Hyle contributes | Repo-local gate before it starts |
|---|---|---|
| **P-0 Foundation** | F1 homed here. L1 (KMP contract, role test, token-path retarget) and the generator work for `qml` and Swift. The master lists the effort under the Linux column, but L1 is P-0 work | OQ-17 ruled; PH-1 filed and ruled; the open PRs in §1 dispositioned; `main` CI green |
| **P-UT a** (parallel with P-LX) | U1 `Tokens.qml`; U2 gallery recipe | F7's webapp template; a UT device or an explicit CI-only waiver (OQ-1); a NAMES.md row before any manifest |
| **P-UT b** | `Tokens.qml` consumed by the JVM-cored QML clicks through F7; U4 atoms | The consumer's own P-LX core green; S-UT1 passed or waived (it concerns that click's JVM core, not Hyle) |
| **P-LX** | L1 to L3; every other P-LX item that uses Hyle takes L1 as build-entry | OQ-17; consumer rehearsals green; `DEVICE_CHECKLIST_LINUX.md` (F11) |
| **P-iOS** | I1 to I3 | F1's iOS targets; the CMP-iOS recipe proven on Clavis in the simulator; `macos-latest` lanes; OQ-2 for any device |
| **P-mac** | M1, M2 | P-LX binaries exist; OQ-3 for signing; no Mac, so NOV (OQ-5) |
| **P-win** | W1, W2 | P-LX binaries exist; a signing route (OQ-3) or accepted unsigned; the Dell only while Windows (OQ-5) |

**Proposals for the owner** (ids unassigned; to be filed in Personal-Tracker `DECISIONS.md` beside D-A and D-Q).
Nothing below is ruled.

- **PH-1.** Convert `:hyle` to `kotlin("multiplatform")` + `com.android.library` with `androidTarget()` and `jvm()`; add iOS targets in I2 and nothing else until a consumer asks. Project path `:hyle`, group `dev.aarso` and artifact `hyle` stay; the next unburned version (proposed 0.3.0) is used and `0.1.0` is never reused. D-A stays: delivery is submodule + `includeBuild`, Maven publishing stays owner-gated. D-Q's AGP lockstep is unchanged at the current pins; it moves only if OQ-17 picks Option A (Option C would release binary consumers from it).
- **PH-2.** One hand-authored role manifest in `commonMain` (each meaning-colour role paired with its non-colour channel, as `Provenance` pairs hue with `Glyph`) and a `commonTest` that fails when a role lacks a distinct channel. It enforces I-3 for the roles Hyle declares; it cannot police a consumer's own screens.
- **PH-3.** The Compose layer reaches `commonMain` of `:hyle` after the pure contract is converted (the master's F1 shape; also the shape #16 drafts), with the android-bound files behind `expect`/`actual`. The contract lands first with no Compose dependency, so a split into a sibling coordinate stays possible until L2 adds that dependency. A split would need a second `substitute(...)` line in Foto-Xplorr and Fyl-Manager.
- **PH-4.** The desktop harness is a new, separate Gradle build under `desktop/` that includes `..`; `:hyle-probe` is left untouched until a separate PR that the owner device-verifies. Reason: Gradle configures every project of an `includeBuild`, so a Compose Desktop module in the root `settings.gradle.kts` would load its plugins in every consumer's composite.
- **PH-5.** Generated QML and Swift outputs are committed (`ubuntu-touch/qml/`, `apple/HyleTokens/`), as the generated Kotlin and XML already are, with a drift check that regenerates and runs `git diff --exit-code`. Delivery of web tokens (committed, or an npm publish) follows OQ-24.

## 6. Work breakdown

**Placement and conventions (R1 to R4).** KMP source sets live inside `hyle/` (`commonMain`, `androidMain`, `jvmMain`, `commonTest`, later `iosMain`). Platform tracks add only `desktop/` (a separate Gradle build with its own `settings.gradle.kts`), `apple/` and `ubuntu-touch/`. Root `settings.gradle.kts` and `ci.yml` are not edited by a port. New workflows, SHA-pinned, one per platform: `desktop-linux.yml` (which also hosts the foundation lane, the KMP contract's JVM and Android jobs, because it is the first new lane), `desktop-macos.yml`, `desktop-windows.yml`, `ios.yml`, `ubuntu-touch.yml` (until F9's `kmp-matrix.yml` exists). Package and sign lanes run on `main` and tags only; PRs compile only; nothing is uploaded as an Actions artifact (R6); every binary is `UNSIGNED — not for release`. The pure core is converted and tested before any UI step (R4). Step weeks add up to each row of §4. This container (JVM x86_64 only: no Android SDK, Swift, Clickable, Docker daemon or flatpak-builder) can verify none of the Android, Ubuntu Touch, Apple or Windows steps; each evidence ceiling below names what hosted CI can show, and anything on a device stays NDV or NOV.

### 6.0 Owner rulings (no repo change)

**L0.** Rule OQ-17, PH-1 to PH-5, the fate of PRs #16, #10, #7, #11, #15 (§8 Q1 and Q2), and OQ-29. Until then no step below starts. Conflict note: #11 and #16 modify the files L1 moves; land or close them first, or resolve by regenerating the token outputs.

### 6.1 Foundation and Linux desktop (3 weeks)

**L1 (1.0 week). Pure contract to KMP.** `hyle/build.gradle.kts` becomes `multiplatform` + `com.android.library`.
`Hyle.kt` and the generated `HyleTokens.kt` move to `hyle/src/commonMain/kotlin/dev/aarso/hyle/`; the generated XML
to `hyle/src/androidMain/res/values/`; the 8 tests are re-expressed with `kotlin.test` in `hyle/src/commonTest/`
(JUnit 4's `assertThrows` is JVM-only; no assertion is weakened). `scripts/build-tokens.js` retargets only the
`kotlin` and `android` `buildPath`s. `androidTarget()` and the `com.android.library` plugin are intended to be applied only when an Android SDK is found
(the `androidSdkAvailable()` pattern F5 names), so that JVM lanes and this container can configure `:hyle`; whether that
gating works inside `hyle/build.gradle.kts` without a root `settings.gradle.kts` edit is unverified (S-H1). A `test` aggregate
task keeps `./gradlew :hyle:test` (README, CONTRIBUTING and `ci.yml` all call it) running `jvmTest` plus the Android
unit tests; if Gradle refuses the alias, a one-line `ci.yml` edit becomes an owner question (§8 Q15). PH-2's role
manifest and test land here. If #16 landed first, its 29 Compose files move verbatim to `androidMain` (their `api(...)`
dependencies to that source set, the Robolectric render test to `androidUnitTest`): a path move, not a lift, and
whether the Robolectric native-graphics setup survives the KMP Android target is unverified. The first action is an
**S-H1 spike** on a scratch branch with four unknowns: do Android-IDE-core (flavors `full`/`play`) and the two
explicit-substitution consumers still resolve `dev.aarso:hyle` to a usable Android variant; does a differing Kotlin
Gradle plugin version across the composite matter; does the `test` alias hold; can `com.android.library` be applied
conditionally from inside `hyle/build.gradle.kts` so an SDK-less JVM configuration succeeds.
*Done when:* `desktop-linux.yml` (ubuntu-latest) is green on `:hyle:jvmTest`, the Android unit tests,
`:hyle-probe:assembleDebug` (not built by `ci.yml` today) and `:wallpaper:assembleDebug` (while #7 is unmerged);
`ci.yml` is green and unedited; draft PRs bumping the submodule pin in Android-IDE-core, Fyl-Manager and Foto-Xplorr
pass those repos' own gates (their PRs, their rules; D-L apps excluded); `publishToMavenLocal` lists the expected
publications. *Evidence ceiling:* `CI (hosted VM)`. This container has no Android SDK, so only the SDK-less JVM
configuration can be `CONTAINER-BUILD-ONLY`, and only if S-H1 shows that configuration is possible.

**L2 (1.0 week). Compose layer subset and shader seam.** Apply the Compose Multiplatform plugin to `:hyle` at the
version OQ-17 pairs with Kotlin. Atoms go in `hyle/src/commonMain/kotlin/dev/aarso/hyle/` for the subset the owner
names (plan assumption: provenance badge, pulse, pane, chip, button). `HyleShader` is an `expect`/`actual`: the
Android actual uses `RuntimeShader` + `ShaderBrush` as the three probe files do, the `jvmMain` actual uses
`org.jetbrains.skia.RuntimeEffect`. If #16 landed, `HyleSubstrate` (Bitmap, BitmapShader, Paint), `HyleHaptics`
(View constants) and `Aeon`'s `BitmapFactory` use move behind seams; the compiler already rejects `android.*` in
`commonMain`. **Sizing spike S-H2 first:** compile the whole `cells/`, `theme/` and `component/` layer in a scratch
`jvm()` source set and count the errors; only then is a number for lifting all 29 files defensible.
*Done when:* the atoms compile for `androidTarget` and `jvm()`; every atom that encodes meaning in colour has its
non-colour channel asserted in a test; the `RuntimeEffect` actual is exercised in L3. *Evidence ceiling:*
`CI (hosted VM)` compile and tests; shader compatibility is an assumption until L3; device behaviour NDV (owner).

**L3 (1.0 week). Desktop render harness and packaging.** `desktop/` is a separate Gradle build (its own
`settings.gradle.kts` with `includeBuild("..")` and `include(":hyle-harness")`); `:hyle-harness` is a Compose Desktop
`application` showing the probe tabs in order, Radiant glow first. The AGSL
strings become shared constants in `:hyle`, and `:hyle-probe` adopts them only in its own owner-verified PR. An
offscreen render (`ImageComposeScene`, recalled and unverified) asserts that each shader compiled and that the
pixels are not uniform; no screenshot is uploaded. `createDistributable` yields a jpackage app-image, shipped as a
tarball with `install.sh` and an AppImage (the master lists Hyle among the AppImage repos). No Flatpak: a harness is
not a product, and its README says "desktop render harness", never a port of Hyle Probe (R12).
*Done when:* `desktop-linux.yml` renders the harness offscreen on a hosted VM and builds the app-image on
`main`/tags. *Evidence ceiling:* `CI-APPROX — NOT DEVICE EVIDENCE` for software-raster pixels; `createDistributable`
plausibly `CONTAINER-BUILD-ONLY`; Wayland, HiDPI, GPU and Steam Deck behaviour NDV.

**L4 (optional, not in the 3.0).** A Plasma 6 wallpaper reframe of Hyle Worlds through F10's web-wallpaper kit
around `field/form-world-bg.html` (OQ-6). If #7 lands it belongs to portfolio. Evidence `NDV`.

### 6.2 Ubuntu Touch (4 weeks; all device gates NDV, OQ-1)

| Step | What and where | Done when | Evidence ceiling |
|---|---|---|---|
| **U1** (1.0) `Tokens.qml` and fonts | A `qml` platform in `scripts/build-tokens.js` writes `ubuntu-touch/qml/Tokens.qml` (a singleton, colours as `#AARRGGBB`, integer dimensions and milliseconds) and a `qmldir`; a Lomiri palette mapping file (the exact Lomiri theming mechanism is unverified). No TTF is copied here: the font list and its OFL notice are documented for each click's template to bundle. Which families (Archivo and JetBrains Mono, or the Hyle* set) is §8 Q9 | `ubuntu-touch.yml` regenerates and `git diff --exit-code ubuntu-touch/` passes; `qmllint` runs if the runner can install a Qt 5 build (availability unverified) | `CI (hosted VM)`; rendering NDV |
| **U2** (0.75) Reference gallery click | `ubuntu-touch/gallery/` holds the recipe to serve the Storybook static build (already produced by `storybook.yml`) from F7's webapp-container template. No `manifest.json` and no `clickable.yaml` identifier is written until a NAMES.md row exists (R11, OQ-25) | Static-build recipe checked in CI; the manifest step is marked `BLOCKED(OQ-25)` | `CI (hosted VM)` for the build; whether Lit 3 and WebGL1 run in the UT engine is unknown, NDV |
| **U3** (1.25) Form-World ambient app, optional (OQ-6) | `ubuntu-touch/ambient/`: a QML `ShaderEffect` using the sprites-disabled fragment shader, extracted from `field/` by a generator step (never hand-copied, §3 rule 6); labelled "ambient app" (R12) | Lint only; the Qt 6 migration will need a shader rebuild | `NDV`; the uniform-array risk (`field/ROADMAP.md`) is why the sprites-disabled variant is used |
| **U4** (1.0) Minimal QML atoms | `ubuntu-touch/qml/components/`: provenance badge (disc or ring plus hue, never hue alone), pulse, pane, chip, button, hand-written by reference to the kit (§3 rule 7) | Lint; a QML test that the badge exposes a non-colour channel | `NDV` |

### 6.3 iOS and iPadOS (3 weeks; iPadOS first, OQ-2)

| Step | What and where | Done when | Evidence ceiling |
|---|---|---|---|
| **I1** (1.0) Swift token package | A fixed Swift format in `scripts/build-tokens.js` writes `apple/HyleTokens/` (`Package.swift`, `Sources/`, `Tests/`): plain RGBA values with `canImport(SwiftUI)`, `canImport(UIKit)` and `canImport(AppKit)` accessors, `CGFloat` dimensions, integer-millisecond durations. The old `build/ios/Tokens.swift` output is left in place. Consumed by local path through the submodule (I-6) | `ios.yml` (`macos-latest`): `swift build`, `swift test`, drift check | `CI (hosted VM)`; no Swift in this container |
| **I2** (0.5) iOS targets for the contract | `iosArm64` and `iosSimulatorArm64` (no Intel simulator) added to `:hyle`; an unsigned XCFramework assembled and not uploaded | `iosSimulatorArm64Test` passes the same commonTest on `macos-latest` | `SIMULATOR` |
| **I3** (1.5) Compose layer on iOS | iOS targets for the L2 atoms; the `RuntimeEffect` actual over Skia and Metal; `apple/harness/` a SwiftUI shell from the F9/F10 XcodeGen template, built with `CODE_SIGNING_ALLOWED=NO`. Its bundle id is `BLOCKED(OQ-25)` (R11), so I1 and I2 do not wait on it. Form-World: a WKWebView hosting `field/form-world.html` (unverified), not OpenGL ES | Simulator build and tests pass; no device claim | `SIMULATOR`; device NDV until OQ-2 |

### 6.4 macOS (1.5 weeks) and 6.5 Windows (1 week)

| Step | What and where | Done when | Evidence ceiling |
|---|---|---|---|
| **M1** (0.5) JVM and AppKit lane | `desktop-macos.yml` (`macos-latest`): `:hyle:jvmTest` and the harness build; the Swift package's `canImport(AppKit)` branch compiles under I1's `swift build` | Both pass | `CI (hosted VM)`; device NOV |
| **M2** (1.0) Harness `.dmg`, unsigned | `packageDmg` on `main`/tags only; Developer ID, notarisation and entitlements stay disabled templates from F10 until OQ-3, never jpackage's default `sandbox.plist` | An unsigned `.dmg` builds; nothing is notarised | `CI (hosted VM)`; Gatekeeper behaviour NOV |
| **W1** (0.5) Path lint and JVM lane | `desktop-windows.yml` runs R3's path lint first. Measured today on 3,119 tracked files: 0 names with `:<>|?*"` or control characters, 0 reserved names, 0 trailing dots or spaces, 0 case collisions, longest path 99 characters; no `.gitattributes` (`git ls-files --eol`: 1 CRLF file, `gradlew.bat`; 242 binary). Then `:hyle:jvmTest` on `windows-2025`. The drift check stays Linux-only; a `.gitattributes` is added only if a CRLF failure appears | Lint and tests pass | `CI (hosted VM)` |
| **W2** (0.5) Harness MSI, unsigned | `packageMsi` (WiX) on `main`/tags only; the winget manifest stays a disabled template; signing route per OQ-3 | An unsigned MSI builds | `CI (hosted VM)`; SmartScreen behaviour NOV |

Not planned, with reasons: a `wasmJs` target (no Hyle consumer needs one; add it when one does); Kotlin/Native
`linuxArm64`, `linuxX64`, `mingwX64` or `macosArm64` targets (no non-JVM consumer; Foto-Xplorr's UT shell calls a
C API, not Hyle); a full QML or SwiftUI mirror of the 26-folder kit; native crash-recovery (F2, Shared-Libraries-asoc);
converting `:hyle-probe` in place; WinUI tokens (no consumer); Hyle Worlds as a product on iOS
(`NOT-APPLICABLE`).

## 7. Shared foundation this repo consumes or provides

**Provides.** All of F1 (hyle-kmp): the KMP `:hyle` (`androidTarget` + `jvm()`, iOS in I2); the `qml` platform and
`Tokens.qml` that F7 and every QML click consume; the Kotlin token output retargeted to `commonMain` (the master's
`kotlin-common`); the I-3 role test (PH-2); the Swift token package (F1's iOS codegen). Also: web tokens for
asystemofcells-figma, whose hand mirror F1 is meant to replace (delivery form: PH-5, OQ-24); and a desktop render
harness that other Compose ports can reuse, since no repo can verify rendering in CI today (a repo-local deliverable,
not an F item). Who needs it: Android-IDE-core, Fyl-Manager and Foto-Xplorr today; Shared-Libraries-asoc, whose
catalog is "pinned to match Hyle exactly"; Foto-Xplorr Phase 9 and Fonebrew core on the Compose side; the UT clicks
for tokens; BOS, Crocodyl and the figma suite, which re-express provenance ad hoc.

**Consumes.** OQ-17 (the pin; D-Q lockstep). F9's `kmp-matrix.yml` once it exists (until then, inline SHA-pinned
jobs). F10 templates: jpackage, WiX, XcodeGen, entitlements and the web-wallpaper kit. F7's webapp-container
template for U2. F11's labels and `DEVICE_CHECKLIST_*.md`. **Not consumed:** F5's `kmp-conventions` plugin is not a
prerequisite. Hyle is the upstream of the catalog ("pinned to match Hyle"), and making Hyle depend on a plugin hosted
in Shared-Libraries-asoc would create a Hyle-to-Shared-Libraries `includeBuild` edge that does not exist today; L1
uses Hyle's own catalog and adopts F5 later if the owner wants. Also not consumed: F2, F6 and F8 (no crash capture,
no keys, no engines), and F3 (`cell-shell` is a navigation shell, unrelated to Hyle's `cells/` form layer).

## 8. Open questions for the owner

1. **Toolchain pins and the KMP conversion (OQ-17).** Pick Option A, B or C; confirm asom's "no KMP" is asom-local; approve PH-1 as a new DECISIONS entry. Blocks: L1 and everything after it; every consumer's rehearsal.
2. **The open PRs.** Which of #16 and #10 lands (they overlap), and in what shape (Compose inside `:hyle` as drafted, or later split, PH-3); does #7 move Hyle Worlds out; #11 and #15 order. Repo-local. Blocks: L1's shape, L2's scope and estimate, every wallpaper reframe, the CI steps L1 must keep green.
3. **Which KMP targets to publish.** The plan proposes `androidTarget` + `jvm()` now and iOS in I2. Add `wasmJs`, `macosArm64`, `linuxX64`, `mingwX64` only for a named consumer. Blocks: L1's target list and CI cost.
4. **Where artifacts are published** (OQ-24; OQ-17 Option C). Keep submodule + `includeBuild`, GitHub Packages or Maven Central under `dev.aarso`; the npm package `hyle-design-system` 0.1.0 (registry or not, and `license` MIT versus Apache-2.0). Blocks: PH-5's web delivery, Option C, any non-submodule consumer.
5. **Radiant hue.** `RADIUM` or `COLD_CYAN` (open in `Hyle.kt`); every output ships both until ruled. Blocks: nothing in this plan.
6. **Atom scope and who designs them.** Which atoms form the cross-platform subset; written in `commonMain` now, or designed and owner-verified on Android first (as #16 states); the plan assumes five. Blocks: L2, U4 and I3 scope.
7. **Do the `signal.*` tokens stay?** `tokens/color.json` defines `palette.signal.danger #E5564B`, `warning #E0941A` and `success #5BBF7A` (a green), aliased as `feedback.*`; `hy-input`, `hy-button` and `hy-screen` use the danger colour (found by `grep`; whether each use also has a non-colour channel was not audited, so it is unknown). Under I-3 PH-2's test must pair each with a shape or word, or the owner marks them non-meaning. Blocks: PH-2's role list.
8. **Hyle Worlds reframes** (OQ-6): Plasma wallpaper, `.saver`, `.scr`, a UT ambient app, or Android-only. Blocks: U3, L4 and the optional macOS and Windows steps.
9. **Fonts and licences.** Which families native atoms use (Archivo and JetBrains Mono as `tokens/typography.json` says, or the Hyle* set that nothing references today); recording CMP, Skiko, Qt and tool licences in `NOTICE` (no scan exists). Blocks: U1 font packaging, L2's dependency record.
10. **Ubuntu Touch shape.** A QML atom kit, the Morph gallery click, or both (the plan funds both, minimal); a UT device (OQ-1); whether Waydroid (OQ-21) makes the Android probe moot (Hyle's UT row is tokens and fonts, not an APK port). Blocks: U2 and U4 scope, every UT device gate.
11. **Apple, signing and hardware** (OQ-2, OQ-3, OQ-5): Developer Program and delivery route; Developer ID, notarisation and a Windows signing route; whether an iPad, Mac or Windows machine is available for the owner-run step. Blocks: I3 device gate, M2 and W2 signing, all NOV gates.
12. **Hyle-consumer status** of Foto-Xplorr, Fyl-Manager, csapp and Assay (OQ-29). The first two build against Hyle today; the others show no `dev.aarso:hyle` dependency in the local checkouts. Blocks: L1's consumer-rehearsal list.
13. **`crash-recovery` in this repo.** Delete, tombstone (#15 and #16 disagree on the Shared-Libraries version, 1.2.0 versus 1.5.0) or keep; Android-IDE-core still declares 1.0.0 against this repo's copy. Blocks: what `ci.yml` and L1's CI must still build.
14. **Harness shape.** Promote `:hyle-probe` into the cross-platform harness, or keep PH-4's separate `desktop/` build and leave the probe alone. Blocks: L3.
15. **`ci.yml` and the `test` alias.** If Gradle will not alias `:hyle:test` to the KMP test tasks, may `ci.yml` take a one-line change despite R3? Blocks: L1's done-when only in that case.
16. **Identifiers** (OQ-25) for any click, bundle or MSI id, and CI visibility (OQ-20: keep the repo public so macOS lanes stay free). Blocks: U2's manifest, I3's shell, M2 and W2 ids.

## 9. Sources read

In this repo (2026-10-06): `README.md`, `CONTRIBUTING.md`, `NOTICE`, `TRADEMARKS.md`, `LICENSE` (header),
`docs/PHILOSOPHY.md`, `settings.gradle.kts`, `build.gradle.kts`, `gradle.properties`, `gradle/libs.versions.toml`,
`gradle/wrapper/gradle-wrapper.properties`, `hyle/build.gradle.kts`, `hyle/src/main/java/dev/aarso/hyle/Hyle.kt`,
`hyle/src/main/java/dev/aarso/hyle/tokens/HyleTokens.kt`, `hyle/src/test/java/dev/aarso/hyle/{FinishTest,ProvenanceTest}.kt`,
`hyle-probe/build.gradle.kts` and `src/main/java/dev/aarso/hyleprobe/*.kt`, `wallpaper/build.gradle.kts`,
`wallpaper/README.md`, `wallpaper/src/main/java/dev/aarso/hyle/worlds/*.kt`, `crash-recovery/build.gradle.kts` and
`src/main/java/dev/aarso/crashrecovery/*.kt`, `scripts/build-tokens.js`, `scripts/build-color-picker.js` and
`scripts/build-texture-surface.js` (heads), `package.json`, `tsconfig.json`, `.gitignore`, `.storybook/main.ts`,
`src/components/index.ts`, `src/theme/theme.ts`, `src/kit/kit-runtime.ts`, `tokens/*.json`, `field/README.md`,
`field/ARCHITECTURE.md`, `field/ROADMAP.md`, `kit/README.md`, `stories/Introduction.mdx`,
`.github/workflows/{ci,build-apk,storybook,cleanup-artifacts}.yml`, `assets/primitives/LICENSE-NOTE.txt`,
`assets/baliga-portfolio-assets/README.md`, `fonts/HyleGroteskClassic/LICENSE-NOTE.txt`; `git log`, `git ls-files`,
`git ls-files --eol`.

Outside this repo: `Personal-Tracker/PORTING_PROGRAM.md` (§0 to §3, §4, this repo's §5 row, §6 to §8),
`Personal-Tracker/DECISIONS.md` (D-A, D-L, D-O, D-P, D-Q, D-W), `Personal-Tracker/NAMES.md`,
`Personal-Tracker/porting/platforms/*.md`; the consumers' `settings.gradle.kts`, `.gitmodules` and
`app/build.gradle.kts` in Android-IDE-core, Android-IDE-Studio, Fyl-Manager and Foto-Xplorr;
`Shared-Libraries-asoc/MIGRATION.md`, `settings.gradle.kts` and `PORTING_PLAN.md`; and, through the GitHub API on
2026-10-06, Hyle's open pull requests #6, #7, #10, #11, #15, #16, #18 and #19, the branch list, and a read-only clone of
`claude/fonebrew-development-clzu43` (`hyle/build.gradle.kts`, `cells/README.md`, the `hyle/src/main` Compose layer).

## 10. Progress ledger

None. Nothing in this plan has been built, run or verified. Entries append here as dated lines carrying their
evidence label, with real command output pasted and no fabricated logs (program rule R7).
