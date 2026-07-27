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
| **Nucleus** — the one marked point inside each cell | The marker riding the slant, the right-anchored 5-point mandatory asterisk, the dropdown caret, a chip's violet label |
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

## Where the code lives

**In `:hyle`, not here.** This folder is the design record; the components are a
real, compiled, published part of the library:

```
hyle/src/main/java/dev/aarso/hyle/cells/   <- the components
hyle/src/main/java/dev/aarso/hyle/theme/   <- the colour system they read
```

This folder briefly held `.kt` copies. That was a mistake and they have been
deleted: loose source files that nothing compiles and nothing tests are exactly
how `0.1.0` ended up shipping from three divergent copies. If you want to change
a component, change it in `:hyle` — there is no other copy, and Fonebrew now
consumes it as a dependency rather than keeping its own.

## What's in the cells register

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

`CellGeometry.kt` — the **exact** authored geometry, and `HyleWellToggle.kt` —
the two-state toggle. See below.

## Transcribed, not re-derived

The field silhouette was originally *measured* off the Figma exports and rebuilt
from a radius/slant formula (slope 0.25, 32dp box, bar inset 12.5%). That
approximation drifted visibly from the source and was rejected on review. It is
now a **control-point transcription** of the export — `CellGeometry.kt` carries
the authored path data verbatim, and nothing re-derives it.

The field canvas (`3040 × 320`) stretches horizontally, so it cannot be
uniformly scaled. The rule:

- left-edge features stay **left-anchored**, scaled by `h/320`
- right-edge features stay **right-anchored**, same factor
- only the straight top/bottom runs between them stretch

At the authored aspect the right-anchor mapping collapses to a plain uniform
scale, so the export is reproduced exactly. The toggle canvas (`644 × 320`) is
fixed-aspect, so one uniform scale applies.

Four things about the field are load-bearing and were each corrected during
review — don't "simplify" them back:

1. The ring is a **gradient**, not a flat stroke; the ramp is what makes the
   edge read as lit, and the colour encodes state.
2. The slant marker is present in **every** state — the default mutes it rather
   than dropping it. Error is the only state that splits the same silhouette
   into a literal exclamation (stem + dot).
3. The mandatory asterisk is **five-point** and **right-anchored** — not six-arm,
   not riding the slant. It is not repeated in the label row.
4. The text baseline sits low in the box; label and error caption sit tight to it.

## The well toggle

`HyleWellToggle.kt` — a recessed **well** holding a raised **chip**, the
two-state control in the cells register. Three details are load-bearing:

1. Depth comes from a **violet-tinted** inner shadow — not black, and not a
   stroke. That tint bouncing inside the recess is what reads as reflectivity.
   Alpha is owner-set at 35% below the export's value.
2. The chip's lit edge is a **gradient** stroke, dark at bottom-left running to
   violet at top-right. Flat kills it.
3. Selecting the other side rotates the chip a full **180°**, not a horizontal
   mirror — that keeps the slant leaning the same way on both sides. The glint
   deliberately does *not* rotate: the light source is fixed.

The chip sits **flush** to the well's edges (no padding) — the owner's chosen
variant of the two that were compared.

## The seam — how cells pack (owner direction 2026-07-24)

The slant edge is not a decoration on one control; it is a **seam** — the shared
wall adjacent cells pack along, with a thin strip of ground between them. This is
the compositional half of the Voronoi identity (cells tessellate), validated by
the same language in Cohere's brand (their segmented nav pills) and the owner's
own Global/Button (a split action: label cell `/` affordance cell) and
Global/Toggle (the active cell's slant edge *is* the divider).

`Segments.kt` carries the grammar:

- **`HyleSegmentShape(slantStart, slantEnd)`** — the generalised segment. EVERY
  seam edge leans the same `/` way at the field's slope (0.25): a slanted END
  has its **bottom** inset (top runs to full width), a slanted START has its
  **top** inset — so the two edges of any seam are **parallel** and the strip of
  ground between them is constant. Group ends keep square-rounded outer corners:
  `▐███/` · `/███/` · `/███▌`. Seam gap: 3dp of ground; because each box carries
  its own slant inset, rows pack with `spacedBy(gap − slant)` overlap.
  (Deliberately NOT the nav chips' mirrored pair, which point at opposite screen
  edges — seams pack, mirrors face. An earlier cut of this shape mirrored the
  end slant and produced V-shaped gaps; corrected 2026-07-24.)
- **`HyleSplitButton`** — one action as two packed cells: label + trailing
  affordance ("+" by default, optionally its own action). Fill states identical
  to `HyleButton`.
- **`HyleSegmentedToggle`** — one rounded container; the selected option is a
  filled cell whose slant edges are themselves the dividers (first selection
  slants only its end, middle both, last only its start). Selection carried by
  fill AND label colour, never hue alone.

**The owner's own designs are the canonical reference for the seam** (their
tab bar, Mobius breadcrumb, Global/Button, and Global/Toggle); Cohere's nav is
corroboration, not the source, and its palette is NOT adopted — cells stay on
the Fonebrew field/violet axis.

Applied so far: the app's shared `HyleTabBar` renders every tab as its own
cell, packed along parallel `/` seams with 3dp of ground between — no slash
dividers, no strip; the cells are the structure (owner refinement over an
earlier slash-divider cut). The selected cell uses the violet-on-dim-violet
selection register; the composer's mode picker inherits it. Still to apply:
breadcrumb trails (the Mobius reference — cells + `/` + count badges).

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
