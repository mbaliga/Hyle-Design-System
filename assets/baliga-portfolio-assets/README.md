# baliga-portfolio-assets

Home for your own logos/brand marks — the personal portfolio site, the
`asystemofcells` studio brand, and the individual products — kept separate
from `assets/primitives/` (the third-party stock icon/logo reference pack).

This is currently a **scaffold only**: no logo files exist here yet, in this
repo or anywhere else in the constellation. The subfolders below were seeded
from product names cross-referenced across `hyle-design-system` and
`shared-libraries-asoc` (READMEs, `MIGRATION.md`, code comments) — add real
assets as they're created, and delete/rename any folder that doesn't match
reality.

- `mdhv.xyz/` — personal portfolio site
- `asystemofcells/` — the studio/brand itself (see also the procedural
  **Cells Logo generator**: `assets/../public/generators/cells-logo.html`,
  Storybook → Generators → Cells Logo — built in line with this brand but
  not yet used to produce a committed mark)
- `products/` — one folder per product app in the constellation:
  - `BOS_launcher/`
  - `Form-analyser/`
  - `Music_Player/`
  - `Foto-Xplorr/`
  - `Fyl-Manager/`
  - `hnm_playground/`
  - `Animalcules/`
  - `Horizkeeb/`
  - `Clackpad/`
  - `Android-IDE-core/`
  - `Android-IDE-Studio/`
  - `Personal-Tracker/`

Like `assets/primitives/`, this folder is static reference material only —
not part of `package.json`'s `files`, not wired into any component, build,
or Storybook output.
