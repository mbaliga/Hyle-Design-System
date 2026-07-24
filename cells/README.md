# Cells

Hyle's **form layer** — buttons, input fields, selectors, chips — extracted from
Fonebrew's shipping Aeon components and named for what they visually are:
**cells**.

## Why "cells"

Hyle's premise is a **silicon-based organism**. The asystemofcells identity makes
that literal: the ASOC logo is a Voronoi diagram — organic rounded cells packed
with a visible gap, each carrying a **nucleus** (a letter or a dot). Put the ASOC
logo next to Fonebrew's Aeon fields and buttons and the correspondence is direct,
not metaphorical:

| Cell anatomy | Aeon component anatomy |
|---|---|
| **Membrane** — the organic, unevenly-rounded cell outline | The field silhouette: rounded rect with a slanted left edge, bottom-left corner rounder than the other three (`HyleFieldShape`) |
| **Cytoplasm** — the cell's fill against the ground | The inset/raised fill on the dark field, no border — the cell reads as a lighter body on the ground |
| **Nucleus** — the one marked point inside each cell | The 2dp accent bar, the 5-point mandatory asterisk riding the slant, the dropdown caret, a chip's violet label |
| **Cell gap** — the ground showing between cells | Spacing between controls; the ground is always visible around each cell |

So the form layer isn't a bolt-on "professional widgets" set that fights the
organism premise — it *is* the organism, at the scale where it does serious,
somber work: forms, settings, data entry.

## The three registers of Hyle

| Register | Folder | When |
|---|---|---|
| **Cells** (this) | `cells/` | Forms, settings, inputs, actions — the calm, professional, somber register |
| **Tactile Kit** | `kit/` | Physical-control moments — knobs, faders, transport, craters — where an interface deliberately reads as hardware |
| **Field** | `field/` | Atmosphere — the generative ground the organism lives on |

The kit's skeuomorphic/neumorphic controls remain first-class; cells cover the
register the kit deliberately doesn't. (The existing `<hy-input>` in
`src/components/` is a generic token-styled rectangle with no cell identity —
superseding it with a cell-styled port is part of the integration step below.)

## What's here

`Cells.kt` — the components, **extracted verbatim** from Fonebrew
(`Android-IDE-core`, `app/src/main/java/dev/aarso/ui/aeon/Aeon.kt`), the same
provenance-preserving pattern the kit's `<hy-*>` extractions follow:

| Component | What it is |
|---|---|
| `HyleFieldShape` / `HyleRightSlantShape` | The cell-membrane silhouettes (slant left / slant right) |
| `Modifier.hyleContainer` | The shared cell body: membrane + fill + nucleus (accent bar / error `!` / 5-point asterisk) |
| `HyleField` | Text input — label above, cell body, error caption beneath; empty/filled/disabled/error states |
| `HyleDropdownField` | Selector — same cell body, caret nucleus, dropdown menu |
| `HyleButton` | Action — 6dp corners, 40dp; primary (violet fill, pressed steps the ramp) and secondary (raised + hairline) |
| `HyleChip` | Selection cell — violet-on-dim-violet when selected, hairline otherwise |
| `HyleNavChip` | Compact slant-edged navigation cell (both slant directions) |
| `Segments.kt` → `HyleSegmentShape` / `HyleSplitButton` / `HyleSegmentedToggle` | The seam grammar — cells packed along shared slant seams (next section) |

Geometry was measured off the owner's Figma selector-state SVGs and Buttons
sheet (slant slope 0.25, 32dp field box, bar inset 12.5% of height, the `!`
mark's stem at 21.9%–59.4%) — that measurement history lives in the code
comments and is part of what "verbatim" preserves.

## The seam — how cells pack (owner direction 2026-07-24)

The slant edge is not a decoration on one control; it is a **seam** — the shared
wall adjacent cells pack along, with a thin strip of ground between them. This is
the compositional half of the Voronoi identity (cells tessellate), validated by
the same language in Cohere's brand (their segmented nav pills) and the owner's
own Global/Button (a split action: label cell `/` affordance cell) and
Global/Toggle (the active cell's slant edge *is* the divider).

`Segments.kt` carries the grammar:

- **`HyleSegmentShape(slantStart, slantEnd)`** — the generalised segment. Every
  seam leans the same `/` way at the same slope (0.25, identical to
  `HyleFieldShape`), so any segment's slanted END tessellates against the next
  segment's slanted START. Group ends keep square-rounded outer corners:
  `▐███/` · `/███/` · `/███▌`. Seam gap: 3dp of ground.
- **`HyleSplitButton`** — one action as two packed cells: label + trailing
  affordance ("+" by default, optionally its own action). Fill states identical
  to `HyleButton`.
- **`HyleSegmentedToggle`** — one rounded container; the selected option is a
  filled cell whose slant edges are themselves the dividers (first selection
  slants only its end, middle both, last only its start). Selection carried by
  fill AND label colour, never hue alone.

The existing `HyleNavChip` (one slant, either direction) is this grammar's
single-cell degenerate case — `HyleFieldShape`'s left slant and
`HyleRightSlantShape`'s right slant were already parallel and tessellate as-is.
Next applications of the seam: the tab bar (active tab as a carved cell, `/`
seams as dividers — the owner's Summary/Graph View and Mobius breadcrumb
references), breadcrumb trails, and the composer's mode picker.

## What the cells consume (port contract)

The components deliberately take **no raw hex** — everything comes through a
colour scheme (`LocalHyleColors` in the app today), so cells re-theme with the
user's runtime dark/light + accent choice. Roles used: `violet`,
`violetPressed`, `violetDim`, `onViolet`, `inset`, `raised`, `hairline`,
`error`, `textHigh`, `textMid`, `textDisabled` — all of which have counterparts
in `tokens/` (`HyleTokens.Color`). Plus Material3 typography roles and the
app's `HyleHaptics` (tap on press).

## Status & integration path

This folder is the **source-of-truth artifact** (like `kit/tactile-kit.html`
before its controls were ported): it does not compile inside this repo yet, and
Fonebrew still compiles its own copy in `Aeon.kt`. The integration steps, in
order:

1. **Compose port** — enable Compose in the `hyle` module, move these into
   `dev.aarso.hyle.cells`, back them with a library-local `HyleCellColors`
   (fed from `HyleTokens`), and repoint Fonebrew's `Aeon.kt` at the library.
2. **Web port** — `<hy-cell-field>` / `<hy-cell-button>` / `<hy-cell-chip>` Lit
   components with the same membrane geometry (SVG path), superseding the
   generic `<hy-input>`.
3. **Tokens** — promote the membrane geometry (slant ratio, corner radii,
   nucleus bar metrics) into `tokens/` so both ports read one source.
