# The Lens — substrate, lens, controls

> Derived from the law in the root README (*state is shown by material behavior,
> never said by language*) and the three sub-laws in `docs/PHILOSOPHY.md`:
> **light is only spent where thinking happens**, **the surface stays fixed while
> depth changes**, **motion occurs only when there is real state**.

This document answers what "Dialog", "Sheet" and "Surface" are in Hyle. The
answer is that they are **not three components**. They are one material at three
extents, and the middle sub-law above is what makes that true.

## The three strata

There are exactly three layers, in a fixed order. Nothing ever reorders them,
and nothing ever appears *between* them.

| Stratum | Material | Interactive | Carries |
|---|---|---|---|
| **Substrate** | grainy, matte, rough — sand | never | *background* activity, through motion |
| **Lens** | smooth, glossy, blurs what's beneath | never directly | *foreground* state, through depth |
| **Controls** | the cells | always, and only here | the user's intent |

The substrate is the Form-World ground (`field/`) and, where that is too
expensive, the flat grain of `Modifier.hyleTexture`. Its two noise layers are
already specified in `field/ARCHITECTURE.md` — geometric displacement that takes
real lighting, plus screen-space film grain.

The lens is what this document adds.

## Why the lens is one thing

"The surface stays fixed while depth changes" is the load-bearing sentence. A
lens does not move, does not tint, does not animate its own edge to signal
something. What changes is **how deeply it blurs the substrate beneath it**.

That single degree of freedom is enough to express everything the three Material
components were being used for:

| Was | Is | Extent | Blur beneath |
|---|---|---|---|
| `Surface` | **lens at rest** | its own content | shallow |
| `ModalBottomSheet` | **lens grown from an edge** | edge → partial | deep |
| `AlertDialog` | **lens grown from the control that summoned it** | control → centred | deepest |

Same material, three extents. There is no separate dialog component to design,
and no scrim: **the blur is the scrim**. A dimming overlay would be language
("this is disabled") laid over material that is already saying it.

## The single interaction surface

At any moment **exactly one lens is the interaction surface**. This is the rule
that makes the model behave rather than merely look right.

- In a settings room, the interaction surface is a lens carrying many controls —
  sliders, switches, knobs.
- In a chat room, it is a lens carrying one control — the input field.

A dialog therefore **never stacks on top of the interaction surface**. The
interaction surface *becomes* the dialog: it grows from wherever it already is,
takes the question, is answered, and returns. The input box transforming into the
dialog is not an animation flourish — it is the literal claim that there is only
one place the user can act, and it did not move.

Consequences worth stating, because they are easy to violate:

1. **Never two lenses at once.** A sheet over a dialog is incoherent — there is
   only one surface, so it cannot be in two places.
2. **A lens never appears from nothing.** It grows from the control that owns the
   question. If no control owns it, the question is misplaced.
3. **Dismissal returns the surface**, it does not destroy it. The lens shrinks
   back to the control it came from.

## What motion means

Motion beneath the lens means **the machine is doing something you did not just
ask for**. That is the only thing it may mean.

- Substrate still → nothing is happening.
- Substrate in motion → background activity: a model loading, a git write, a
  generation streaming.
- The lens itself never animates to indicate activity. It only changes extent,
  and only in response to the user.

Rate is governed by `Pulse` — *"heartbeat, not weather"*. Slow, regular,
low-amplitude. `Pulse.WATCHED` (2.4 s, 42–78%) for a live connection;
`Pulse.STILL` is the default, "when there is nothing to say".

## A contradiction in the current code

`HyleBlockingOverlay` (`cells/HyleGlass.kt`) is the one existing glass component,
and it violates the root law twice: it renders a `CircularProgressIndicator` — a
spinner — and a text label. The README's first paragraph says *"No status words,
no spinners."*

Under this model a blocking operation is shown, not said:

- the blur beneath the lens **deepens** (the substrate recedes — you cannot reach it),
- substrate motion **continues** (computation is genuinely happening),
- the controls on the lens go **inert** (Reflective, not Radiant — no emission).

No spinner, no label. The `label` parameter stays, but only as the accessibility
`contentDescription`: a screen reader must still be told in words, because a
blind user cannot read material. That is the correct place for language — the
accessibility channel, never the visual one.

## Why this is Fonebrew's register, not everyone's

Hyle is horses for courses. The lens is glass-and-grain because Fonebrew is a
**digital** artifact and should feel like one. The same three strata hold across
the constellation with different materials:

| App | Substrate | Lens |
|---|---|---|
| **Fonebrew** | grain / Form-World | glass |
| **Nooz** | paper tooth | vellum, waxed |
| **Animalcules** | specimen field | a microscope's cover slip |
| **Bocal** | leather, tooled | lacquer over brass |

The *strata* are the system. The *materials* are the app. Do not port Fonebrew's
glass into Nooz — port the model, then ask what a lens is made of on paper.
