# Hyle Design System

> *Hyle* (ὕλη) — Aristotle's word for matter: pure potentiality awaiting form.

A **cross-platform** design system governed by one law:

> **State is SHOWN by material behavior, never SAID by language.**

No status words, no spinners. Hyle tells the truth about *computation* through
material: light only where thinking happens, motion only when there is real
state. A single set of design tokens is the source of truth and compiles to
**web, Android, and iOS**; framework-agnostic **Lit web components** consume
those tokens on the web.

## The material stack

Hyle is built in two layers:

- **The Field** (`<hy-field>`) — the living layer of monumental forms in
  coloured haze that sits *behind the pane to indicate movement*. It hosts the
  **Form-World** WebGL engine (vendored under [`field/`](field/)): matte and
  near-still at rest, moving only on real computation.
- **The Pane** (`<hy-pane>`) — frosted glass floating over the Field, carrying
  the readable controls. Glass means legibility; it stays calm and still.

On the glass: **chips** (`<hy-chip>`), buttons, inputs — quiet hairline outlines
until pressed, when they invert to solid ink. The lone violet accent
(`#8E7BFF`) is the only colour spent freely.

## Architecture

```
tokens/*.json          Style Dictionary           build/
(W3C DTCG)        ──►   scripts/build-tokens.js ──►  web/      tokens.css · _tokens.scss · tokens.js
   color                                             android/  colors.xml · dimens.xml
   typography                                        ios/      Tokens.swift
   dimension · shadow · motion

src/components/*  ──►   Lit web components      ──►  <hy-field> <hy-pane> <hy-chip>
                                                     <hy-button> <hy-input> <hy-card>

field/            ──►   Form-World engine       ──►  the living material behind the pane
docs/PHILOSOPHY.md ─►   the full ethos & lineage
```

## Getting started

```bash
npm install
npm run tokens:build   # compile tokens → build/{web,android,ios}
npm run storybook      # browse the Field, Pane, tokens, components at :6006
```

## Project layout

| Path                      | What it is                                                       |
| ------------------------- | ---------------------------------------------------------------- |
| `tokens/`                 | Source-of-truth tokens (W3C DTCG). Field, ink, accent, motion.   |
| `scripts/build-tokens.js` | Style Dictionary pipeline + custom transforms/formats.           |
| `build/`                  | **Generated** token artifacts (git-ignored; run `tokens:build`). |
| `src/components/`         | Lit web components, one folder per component, with stories.      |
| `field/`                  | The Form-World engine + its README / ARCHITECTURE / ROADMAP.     |
| `public/field/`           | The runnable engine, served to Storybook for `<hy-field>`.       |
| `docs/PHILOSOPHY.md`      | Hyle — Ethos & Lineage (the theory behind the law).              |
| `stories/`                | Introduction + Foundations (token) docs.                         |

## Design tokens

Tokens use the [W3C Design Tokens (DTCG)](https://www.designtokens.org/) format.
Colours carry alpha (ink is a warm off-white spent sparingly); dimensions are
stored **unitless** so each platform applies its own unit (`px` / `dp`+`sp` /
`pt`); durations compile to `ms` on web; easings and letter-spacing pass through.

```jsonc
// tokens/color.json
"ink":    { "full": { "$value": "rgba(236, 232, 228, 0.92)", "$type": "color" } },
"accent": { "violet": { "$value": "#8e7bff", "$type": "color" } }
```

| Platform | Files                              | Consume with                              |
| -------- | ---------------------------------- | ----------------------------------------- |
| Web      | `tokens.css`, `_tokens.scss`, `tokens.js` | `var(--color-action-primary)`      |
| Android  | `colors.xml`, `dimens.xml`         | `@color/color_action_primary`, `@dimen/spacing_4` |
| iOS      | `Tokens.swift`                     | `HyleTokens.Color.colorActionPrimary`     |

### Accessibility (hard gates from the ethos)

- Provenance (radium-green = on-device, alien-cyan = cloud) must **never** carry
  meaning by colour alone — pair every hue with a second non-colour channel
  (form, motion, position, icon). WCAG 1.4.1.
- UI surfaces sit at `#121212`-class, never pure black, to reduce halation.
- Verify ≥4.5:1 text / ≥3:1 non-text contrast per theme with real testing.

## Using the web components

```js
import 'hyle-design-system/tokens.css'; // load the token layer once, globally
import 'hyle-design-system';            // register the <hy-*> elements
```

```html
<hy-field src="/field/form-world.html">
  <hy-pane dock="bottom" heading="Form · World">
    <hy-chip pressed>Bowl</hy-chip>
    <hy-chip>Helix</hy-chip>
  </hy-pane>
</hy-field>
```

## Scripts

| Script                    | Does                                              |
| ------------------------- | ------------------------------------------------- |
| `npm run tokens:build`    | Compile tokens for all platforms.                 |
| `npm run storybook`       | Run Storybook (rebuilds tokens first).            |
| `npm run build-storybook` | Static Storybook build into `storybook-static/`.  |
| `npm run format`          | Prettier across the repo.                         |

The Form-World engine is **generated, not hand-edited** — see
[`field/README.md`](field/README.md) and edit `field/form-world.build.py`.
