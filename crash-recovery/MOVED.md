# `dev.aarso:crash-recovery` has moved

**New home:** [`mbaliga/Shared-Libraries-asoc`](https://github.com/mbaliga/Shared-Libraries-asoc)
**Coordinate:** unchanged — still `dev.aarso:crash-recovery`
**Version:** `1.1.0` (here) → `1.5.0` (there)

What remains in this directory is a **tombstone**: API-shaped stubs whose only job is to fail
your build with a message pointing here, instead of leaving you with an unresolved dependency.

## Why it moved

The utility was built with **zero dependency on `:hyle`** on purpose — plain `android.widget`
views, no Compose, no Material, colours as plain `@ColorInt Int` — specifically so that apps
forbidden from depending on Hyle (Personal-Tracker `DECISIONS.md` **D-L**: Animalcules, Clackpad)
could still use it.

But keeping it inside the *design system* repo meant exactly those apps had to add the entire
Hyle-Design-System submodule to reach a module that has nothing to do with Hyle. **D-O** placed it
here when there was nowhere else to put it; Shared-Libraries-asoc is the neutral home that
resolves the contradiction (Personal-Tracker **D-V**, superseding D-O).

## How to migrate

**1. `settings.gradle.kts`**

```kotlin
includeBuild("shared-libraries")
```

Keep `includeBuild("hyle-design-system")` **only if you also use `dev.aarso:hyle`**. If your
submodule existed solely for crash-recovery — true for BOS_launcher, Clackpad and hnm_playground —
drop it:

```bash
git submodule deinit -f hyle-design-system
git rm -f hyle-design-system
git submodule add https://github.com/mbaliga/Shared-Libraries-asoc.git shared-libraries
```

If you keep **both** composites (e.g. you use `dev.aarso:hyle` from here as well as
`dev.aarso:crash-recovery` from shared-libraries), you need an explicit `dependencySubstitution`
on each `includeBuild` — one naming `:hyle` here, one naming `:crash-recovery` there — because
with this tombstone still declaring the same coordinate, two composites offer it and Gradle fails
with `Module version 'dev.aarso:crash-recovery' is not unique in composite`. Declaring any
explicit substitution for an included build disables *automatic* substitution for that build, so
naming `:hyle` here removes this repo as a crash-recovery candidate. See
`mbaliga/Android-IDE-core`'s `settings.gradle.kts` for a worked example.

**2. Your module's `build.gradle.kts`**

```diff
-implementation("dev.aarso:crash-recovery:1.1.0")
+implementation("dev.aarso:crash-recovery:1.5.0")
```

**3. Nothing else for the API this stub mirrors.** The package is still
`dev.aarso.crashrecovery` and every member this repo's `main` ever shipped is source-compatible —
no import changes, no call-site changes.

**4. CI** — if your checkout step names submodule paths explicitly, add the new one.

## Why the failure looks the way it does

The stub keeps the `dev.aarso:crash-recovery` coordinate alive so Gradle's composite-build
substitution still matches. Had the module simply been deleted, substitution would stop matching,
Gradle would fall through to Maven Central, and you would get:

```
Could not find dev.aarso:crash-recovery:1.1.0
```

— which tells you nothing about where it went.

The stubs are `@Deprecated(level = DeprecationLevel.ERROR)` rather than a configuration-time
`throw`, because Gradle configures **every** project in an `includeBuild`. A configuration failure
here would break consumers that use `dev.aarso:hyle` and never touched crash-recovery. A
compile-time error reaches only actual users of the API.

## Heads-up if you are on an old pin

`1.5.0` is not simply `1.1.0` renamed. Three histories had to be reconciled:

| | `c586f8f`+ (this repo's `main`) | `33b0faa` (never merged) | shared-libs#7 (never merged) |
|---|---|---|---|
| Declared version | 1.1.0 | 1.0.0 | based on 1.4.0 |
| Evolved recovery Activity, crash-mark drawables | ✅ | ❌ | ✅ (inherited) |
| `previewIntent` / `samplePreview` | ❌ | ✅ | ✅ (inherited) |
| "Continue" actually relaunches the app | ❌ (bug) | ❌ (bug) | ✅ (fix) |
| Non-destructive quarantine/salvage reset | ❌ | ❌ | ✅ |

Android-IDE-core pinned `33b0faa` — an unmerged branch commit — and calls
`CrashRecovery.previewIntent` from `SettingsRoom.kt`. It compiled, but only against work that
never landed, while missing every later improvement on `main`.

`1.2.0` first ended that split: `main`'s implementation with `previewIntent` merged forward and
rebuilt against the richer `CrashReport.Decoded`. `1.5.0` folds in a second stranded branch on
top of that — shared-libraries' own never-merged PR #7 — which fixed a real "Continue" relaunch
bug and replaced the destructive `clearApplicationUserData()` reset with a non-destructive
quarantine (data moved aside, not deleted) plus an optional host-supplied off-device backup
bridge. None of that later API is mirrored in this repo's stub: nothing here ever called it, so
there is no existing call site to preserve.
