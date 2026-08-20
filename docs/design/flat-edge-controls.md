# Flat edge controls — the notch, the slant toggle, the tab field

A flat, light control language, distinct from the dark Tactile Kit (`src/components/*`).
Where the Tactile Kit is skeuomorphic hardware — grooves, LEDs, thunk — this is a flat
UI kit built around one idea: **surfaces relate to an edge, and controls lean.** It is
the language of the owner's photo-app mockups and is implemented first in the
Foto-Xplorr Android client (`com.fotoxplorr.app.hyle.*`); this document is the source of
truth for the geometry and palette so any second implementation matches the first.

The three motifs share two primitives:

- **The cove** — where a black surface meets a screen edge it does not end in a corner but
  in a concave, inverted-radius flare that blends into the edge, so the surface reads as
  *cut into* the edge rather than laid on top of it.
- **The rivet-and-bracket** — a small registration mark (a rounded-corner bracket plus a
  dot) sits at a control's leading corner, the shrunk echo of the cove that ties the
  small controls to the big notches.

Colours are the shared palette (aligned to `tokens/` where an equivalent exists):

| Role | Value | Note |
|---|---|---|
| Control track | `#D7D7D9` | light neutral the knob rides |
| Control ink / idle wedge | `#0B0B0D` / `#3A3A44` | the black knob; the idle field wedge |
| Field fill | `#F1F1F2` | the flat field surface |
| Accent (focus) | `#8E7BFF` | `color.palette.accent.violet` — wedge + cursor when focused |
| Focus hairline | `#6E97E8` | the field's border when selected |
| Danger | `#E5564B` | `color.palette.signal.danger` — invalid wedge, `!`, `*`, border |
| Registration mark | `#FFFFFF` | the rivet-and-bracket, and the `!` |

## 1. Edge notch (selection chrome)

Three black surfaces anchored to screen edges, each coving into its edge. Ported verbatim
from the mockup's 440×956 export; coordinates are that artboard's dp.

**Top action bar** — hangs from the top edge, coves in at both top corners, narrows to a
flat-bottomed trapezoid. Native size 209.08 × 50.79, left inset 8.

```
M217.076 0 H8 H15.6056 C22.1794 0 28.0842 4.02094 30.4929 10.1376
L42.5088 40.6515 C44.9175 46.7682 50.8223 50.7891 57.3961 50.7891
H169.308 C175.854 50.7891 181.739 46.8025 184.167 40.7241
L196.413 10.065 C198.841 3.98662 204.726 0 211.272 0 H217.076 Z
```

**Count pill** — a 16-radius rounded bar whose right end is a cove *cap* (bulges out above
centre, slants back in toward the bottom), the mate of the trash notch. Rounded rect
`x15 y909 w297 h44 r16` unioned with:

```
M172 925 C172 916.163 179.163 909 188 909 H314.663
C326.168 909 333.913 920.78 329.352 931.343 L324.17 943.343
C321.639 949.204 315.866 953 309.481 953 H188
C179.163 953 172 945.837 172 937 V925 Z
```

**Trash notch** — rises from the bottom edge, coves in at both bottom corners, narrows to a
rounded top. Shaped unlike the pill on purpose: the one destructive control is legible by
silhouette. Native size 134 × 47.05, right inset 3.

```
M338.86 918.741 C341.37 912.835 347.167 909 353.585 909 L386.42 909
C392.836 909 398.631 912.832 401.143 918.735 L412.869 946.293
C415.375 952.182 421.15 956.011 427.55 956.028 L437 956.053 L303 956
L312.385 956.025 C318.818 956.042 324.636 952.204 327.152 946.284 L338.86 918.741 Z
```

Each shape carries the mockup's own shadow (`0 4px 8px` under the bar, `0 -4px 4px` above
the trash). The pill and trash render a touch narrower than their native width so the cap
peak and notch width land on the reference PNG's extents (≈303 and ≈110 dp respectively).

## 2. Slant toggle

A `#D7D7D9` track, radius 9, default 64 × 32. A `#0B0B0D` parallelogram knob, width 34,
horizontal skew 6, slides across it; the knob is drawn hard-edged and **clipped to the
track**, so the three edges it shares with the track inherit the rounding while the
leading slant stays crisp. The knob carries the rivet-and-bracket at its top corner and
travels with it. Optional per-side glyphs (e.g. `#` idle / `*` on) sit at the quarter
points — white over the knob, ink over the track.

The slant is the state signal: the knob leans into its travel, so "which way is on" is
legible from the shape, not only from which side the knob sits on. Disabled desaturates
track and knob to `#E6E6E8` / `#BFBFC2`.

## 3. Tab field

A `#F1F1F2` field, radius 14, height 52, wearing a leaning **wedge tab** at its top-left
that pokes above the field and colours by state:

| State | Wedge | Border | Extras |
|---|---|---|---|
| Not selected | ink `#3A3A44` | none | |
| Selected | violet `#8E7BFF` | blue `#6E97E8` hairline | |
| Selected & mandatory | violet | blue hairline | trailing `*` violet |
| Invalid (not selected) | danger `#E5564B` + `!` | faint danger | |
| Invalid (selected) | danger + `!` | danger hairline | |
| Invalid & mandatory | danger + `!` | danger hairline | trailing `*` danger |
| Disabled | `#C7C7CC` | none | faded ink |

The wedge is a ~15 × 36 rounded parallelogram (skew 5) — the same lean as the toggle knob.
The `!` is drawn (a white stroke + dot), not typed, so it keeps weight at this size. The
editable variant hosts a single-line field with a violet caret; leading/trailing slots
carry a search glyph or a clear button.

---

_Reference implementation: `Foto-Xplorr/app/src/main/java/com/fotoxplorr/app/hyle/`
(`HyleNotch.kt`, `HyleToggle.kt`, `HyleField.kt`), rendered and diffed against the mockups
through that app's Roborazzi harness._
