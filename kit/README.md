# Tactile Kit

Hyle's **physical-control language** — the "soft brutalism" layer from the ethos:
monumental honesty of material, warmed by tactility (*controls you want to
touch*). A single self-contained, themeable HTML surface.

`tactile-kit.html` is the runnable artifact (also served to Storybook from
`public/kit/`). Open it directly, or browse it under **Material / Tactile Kit**
in Storybook.

## What's in it

Sixteen sections: Display, Digital Displays, Mini Displays, a full **Colour
picker**, Voice Settings, Folders, Knobs, Sliders, Faders, Toggles, Transport,
Crater Buttons, Dial & Joystick, Meters (level + VU), Surface Buttons, Surfaces
(grille, jacks).

## The theming system

A sticky control-zone at the top drives five runtime axes via `data-*` on the
root, each remapping CSS variables:

| Axis        | Attribute     | Options                                                        |
| ----------- | ------------- | ------------------------------------------------------------- |
| **Surface** | `data-theme`  | jet · gunmetal · silver · white · champagne                   |
| **Accent**  | `data-acc`    | none · orange · cyan · green · **violet (`#8e7bff`)**          |
| **Finish**  | `data-ctex`   | machined · brushed · glass · sand · sandstone · leather · concrete |
| **Texture** | `data-btex`   | flat · leather · concrete · sand · sandstone · brushed · glass |
| **Label**   | `data-lbl`    | bright · bold · max · accent                                  |

Materials are real procedural textures (SVG `feTurbulence`/`feDiffuseLighting`
data-URIs for sand, sandstone, leather, concrete, brush, plastic; gradients for
glass; a radial pattern for machined metal) — finishes are *assigned by meaning*,
not decoration, per the core law.

## Bridges to the design system

These already line up with `tokens/`, and are the natural seams for unifying the
kit with the rest of Hyle:

- **Easing** — the kit's `--ease: cubic-bezier(.4, 0, .2, 1)` is identical to the
  `easing.standard` token.
- **Accent** — the kit's violet `#8e7bff` (`--acc`) is `color.palette.accent.violet`.
- **Finish / Texture vocabulary** — the material library (leather, concrete,
  glass, sand, sandstone, machined, brushed) is the physical counterpart to the
  Field's stone/smog/light and is a candidate for a future `tokens/texture.json`.

> Like the Form-World engine, this file is a large authored artifact. It is
> currently integrated as a **reference surface** (iframe). Porting individual
> controls (knob, fader, toggle, crater button, VU meter) into `<hy-*>` Lit
> components is the next step if we want them as first-class, composable parts.
