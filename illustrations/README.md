# Hyle Design System illustrations

This is a reference-driven illustration set for loading, empty, error, maintenance, and easter-egg states.

The set intentionally includes insects, animals, fungi, and unusual organisms. Each supplied subject is represented once; duplicate views were consolidated. The art is original hand-inked watercolor/gouache natural-history comic work with playful situations and scientifically readable silhouettes. No captions, Reddit UI, logos, or watermarks are baked into the assets.

## Format

- Still masters: square `384 × 384` WebP.
- Video posters: square `384 × 384` WebP.
- Loops: square `300 × 300` H.264 MP4, 12 fps, no audio.
- Loop lengths: sea otter 4 seconds, bat 5 seconds, gecko 4 seconds.
- Composition: the primary subject and situation sit inside a centered circular safe area with generous margin. The surrounding field is plain black or warm white so the assets can be cropped into any aspect ratio.
- Captions and scientific names belong in product copy or the manifest, not in the artwork.

## Folders

- `stills/` — static illustrations.
- `posters/` — poster frames for motion assets.
- `loops/` — short looping MP4s. Set `loop` in the consuming component.
- `manifest.json` — subject, situation, source reference, tone, and suggested UI usage.

## Suggested use

- Funny/empty: `rabbit-hay`, `rooster-kittens`, `angular-armored-insect`, `aquatic-stack`.
- Mystical/easter egg: `blue-glass-mushrooms`, `orange-ciliated-organism`, `bubble-organism-and-larva`, `yellow-black-sphere-fungi`.
- Error/maintenance: `hunched-fly-open`, `jaguar-fish`, `jaguar-in-thicket`, `harvestman-leaf`.
- Loading/interstitial: `sea-otter-sleep-loop`, `long-eared-bat-wake-loop`, `green-gecko-climb-loop`.

The suggested UI mappings are starting points. The `situation` field is the intended educational or comedic hook; verified scientific names can be added to `manifest.json` before captions are authored.
