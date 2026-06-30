# Form-World

A single-file WebGL generative-art engine. Monumental, grainy, atmospheric forms standing in coloured haze — a calm-technology aesthetic, deliberately *not* clean-vector / Tron. Grey stone, coloured smog, coloured light, kept as three separate things so the forms read against the atmosphere rather than dissolving into it.

It runs as one self-contained HTML file: no build step at runtime, no server, no assets, no network. Mobile-first; the development target is a RedMagic phone (Adreno 740) viewed in the Claude app.

## Run

Open `form-world.html` in any browser with WebGL1. Tap the canvas to show or hide the control panel.

If you get a blank screen with a line of monospaced text, the GLSL shader failed to compile and the first error line is printed on-screen — there is no console on mobile, so the error is surfaced in the page itself.

## Files

`form-world.html` is the runnable artifact. It is **generated, not hand-edited** — do not edit it directly. `form-world.build.py` is the generator that produces it; edit that and re-run it (see ARCHITECTURE.md for why the project is built this way and how to validate a build). `ARCHITECTURE.md` explains how the engine works internally, and `ROADMAP.md` records the known limitations, the tradeoffs behind current decisions, and what is still pending.

## Build

```
python3 form-world.build.py        # writes form-world.html
```

Always validate after building. The build environment has no GPU and cannot run WebGL, so a passing build is necessary but not sufficient — real verification happens on a device. The validation steps (JS parse, brace/paren balance, feature greps) are described in ARCHITECTURE.md.

## Controls

The panel is grouped. Defaults below are the current shipping values.

**Scene** (mutually exclusive): Bowl, Helix, Towers, Arch, Ruins, Planet. Default Bowl. Each scene has its own camera framing and its own slow autonomous motion.

**Mode**: Passive / Active (global animation intensity — Passive ≈ 0.15×, Active = 1.0×) and Once / Recursive (bounded vs infinite version of the scene). Defaults Passive, Once.

**Sprites** ("wisps"): behaviour Drift / Seek / Ember, and a Count slider 0–8 (default 4). Drift is a slow random walk; Seek glides each wisp to a point near a form; Ember attaches it to a surface where it glows bright and lights the surface locally, then releases.

**Colour** — three independent pickers:

| Picker | Role | Default |
|---|---|---|
| Stone | surface albedo (the material) | `#9b958c` |
| Smog | fog / atmosphere colour | `#cc1a0a` |
| Light | key light, specular, glow, sun disc | `#ffd2b0` |

plus a Pattern selector (Even / Flow / Aurora / Bloom / Bands) that modulates the material.

**Surface** — texture character Soil / Hatch / Warp / Facet, and four sliders:

| Slider | Effect | Range / default |
|---|---|---|
| Grit | depth of the 3D surface displacement | 0–1.5 / 1.0 |
| Coarse | scale of that displacement (fine sand ↔ chunky) | 0–1 / 0.5 |
| Grain | film-grain amount (image-level noise) | 0–1 / 0.5 |
| Grain size | scale of the film grain | 0–1 / 0.5 |

**Light** — toggles Breathe / Flicker / Drift / Pulse (defaults: Breathe on, Drift on, Flicker off, Pulse off), plus Aim (light/sun azimuth, 0–2π, default 2.2), Glow (intensity, 0.3–2.0, default 1.2), Inner (inner-glow / translucency, 0–1.5, default 0.3).

**Camera / atmosphere** sliders: Zoom (0–1), Count (scene element count — rings/towers/arches, 1–7, default 5), Look X (yaw, ±1.4), Look Y (tilt, ±1.0), Atmos (fog density, 0–1, default 0.5), Speed (global tempo, 0–1, default 0.28).

**Gestures**: one-finger drag looks around (yaw/tilt); two-finger pinch zooms; a tap with no drag toggles the panel. The ↻ button reseeds (new random seed + re-initialised sprites); ▽ hides the panel. **Speed = 0 freezes everything** — camera and scene — for a still frame.

## Look recipes

The control space is large and the good combinations are not obvious, so a few starting points:

**Blade Runner 2049 (monochrome dust).** Set Smog to a pale orange, drop Glow to ≈ 0.4, push Atmos up. The forms collapse into dark silhouettes while the haze stays luminous (haze brightness is intentionally independent of Glow), and distance washes everything toward the bright dust. Towers or Ruins suit this best.

**Gris (faceted, painterly).** Switch Surface to Facet — displacement becomes stepped plates with thin dark crack-seams; Coarse sets the plate size, Grit how pronounced they are. Aim the sun disc (Aim slider) behind the form for the soft disc + halo + thin ring. Soften the palette toward pastels if you want the watercolour register.

**Single colossal monument.** Scene Ruins, mode Once: a faceted broken mass on a colonnade with floating debris and receding poles. Switch to Recursive for an endless field of broken forms instead.

**The bowl / glowing throat.** Defaults already land here — concentric rings receding into a lit core.
