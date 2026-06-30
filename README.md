# Hyle Design System

A **cross-platform** design system. A single set of design tokens is the source
of truth and compiles to native artifacts for **web, Android, and iOS**, while a
set of framework-agnostic **Lit web components** consumes those tokens on the web.

> Status: scaffold. The token values and the three starter components
> (`Button`, `Card`, `Input`) are sensible defaults — point me at your real
> HTML/CSS/designs and I'll swap in your colors, type, spacing, and components.
> The build pipeline below stays the same.

## Architecture

```
tokens/*.json          Style Dictionary           build/
(W3C DTCG format)  ──►  scripts/build-tokens.js ──►  web/      tokens.css · _tokens.scss · tokens.js · tokens.json
   color                                             android/  colors.xml · dimens.xml
   typography                                        ios/      Tokens.swift
   dimension
   shadow

src/components/*  ──►  Lit web components  ──►  <hy-button> · <hy-card> · <hy-input>
                       (consume CSS vars)        drop into React, Vue, Angular, or plain HTML

.storybook/  +  stories/  ──►  Storybook docs (foundations + component playground)
```

**Why token-first?** Designers and engineers agree on a name —
`color.action.primary` — not a hex value. Re-theming is a token edit, never a
component rewrite, and every platform stays in sync because each output is
*generated*, never hand-copied.

## Getting started

```bash
npm install
npm run tokens:build   # compile tokens → build/{web,android,ios}
npm run storybook      # browse components + foundations at http://localhost:6006
```

## Project layout

| Path                     | What it is                                                        |
| ------------------------ | ---------------------------------------------------------------- |
| `tokens/`                | Source-of-truth design tokens (W3C DTCG `$value`/`$type` JSON).   |
| `scripts/build-tokens.js`| Style Dictionary pipeline + custom transforms/formats.           |
| `build/`                 | **Generated** token artifacts (git-ignored; run `tokens:build`). |
| `src/components/`         | Lit web components, one folder per component, with stories.      |
| `stories/`               | Foundations docs (colors, spacing, radius, type) + intro.        |
| `.storybook/`            | Storybook config; `preview.ts` loads `tokens.css`.               |

## Design tokens

Tokens use the [W3C Design Tokens (DTCG)](https://www.designtokens.org/) format.
Dimensions are stored **unitless** so each platform applies its own unit:
`px` on web, `dp`/`sp` on Android, `CGFloat` points on iOS.

```jsonc
// tokens/color.json
{
  "color": {
    "palette": { "brand": { "500": { "$value": "#3a51e8", "$type": "color" } } },
    "action":  { "primary": { "$value": "{color.palette.brand.500}", "$type": "color" } }
  }
}
```

Edit a token, run `npm run tokens:build`, and the change flows to every platform.

### Outputs per platform

| Platform | Files                              | Consume with                              |
| -------- | ---------------------------------- | ----------------------------------------- |
| Web      | `tokens.css`, `_tokens.scss`, `tokens.js` | `var(--color-action-primary)` / `import` |
| Android  | `colors.xml`, `dimens.xml`         | `@color/color_action_primary`, `@dimen/spacing_4` |
| iOS      | `Tokens.swift`                     | `HyleTokens.Color.colorActionPrimary`     |

Adding a platform (Jetpack Compose, Flutter, React Native) is a new `platforms`
entry in `scripts/build-tokens.js` — the token source never changes.

## Using the web components

```js
import 'hyle-design-system/tokens.css'; // load the token layer once, globally
import 'hyle-design-system';            // register the <hy-*> custom elements
```

```html
<hy-button variant="primary">Save</hy-button>
<hy-input label="Email" type="email" required></hy-input>
<hy-card elevation="md"><p>Tokens all the way down.</p></hy-card>
```

Because they're standard custom elements, they work unchanged in React, Vue,
Angular, Svelte, or a plain HTML page.

## Adding a component

1. Create `src/components/<name>/hy-<name>.ts` (extend `LitElement`, style with
   `var(--token-name)` so it inherits theming for free).
2. Add a co-located `hy-<name>.stories.ts`.
3. Export it from `src/components/index.ts`.

## Scripts

| Script                    | Does                                              |
| ------------------------- | ------------------------------------------------- |
| `npm run tokens:build`    | Compile tokens for all platforms.                 |
| `npm run storybook`       | Run Storybook (rebuilds tokens first).            |
| `npm run build-storybook` | Static Storybook build into `storybook-static/`.  |
| `npm run format`          | Prettier across the repo.                         |
