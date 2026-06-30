# Architecture

## Rendering model

Form-World is a single full-screen fragment shader running an SDF raymarcher. One triangle covers the viewport (`-1,-1 / 3,-1 / -1,3`); every pixel of the image is computed in the fragment shader. The target is WebGL1 / GLSL ES 1.00, with `precision highp` guarded behind `#ifdef GL_FRAGMENT_PRECISION_HIGH` and a `mediump` fallback. `devicePixelRatio` is capped at 2.

Per frame, the JavaScript does three things: it steps the CPU sprite simulation (`updateSprites(dt)`), which packs results into the `uSpr[8]` and `uSprD[8]` uniform arrays; it pushes all uniforms and draws the triangle (`draw()`); and it schedules the next frame via `requestAnimationFrame`. The loop pauses when the canvas scrolls offscreen (an `IntersectionObserver` flips a `running` flag) and renders only a single static frame when `prefers-reduced-motion` is set.

Per pixel, the fragment shader builds the camera ray, resolves the light state, paints the background, marches the scene, shades the hit, and accumulates sprite glow. Those stages are described below.

## Scene system

`map(vec3 pos)` returns the signed distance to the nearest surface and branches on `uScene` (0–5): Bowl, Helix, Towers, Arch, Ruins, Planet. The scenes are mutually exclusive — only one branch runs.

Bowl is concentric Z-tori receding toward a glowing throat, each ring slowly precessing. Helix is a tall coil that scrolls and rotates. Towers are columns that plunge from y ≈ −7 up to y ≈ 13 and twist with height. Arch is a monumental gateway whose legs plunge to y ≈ −7 with the opening overhead. Ruins is the most composed scene (see below). Planet is two large spheres merged with a smooth-minimum.

The Once / Recursive split is driven by `uRecursive`: it switches each scene between a bounded form and an unbounded one. Towers and Ruins build their infinite versions with domain repetition (`mod`), and a per-cell hash (`h21` of the integer cell id) drives the height, width and twist of each cell so no two repeat. Domain-repeated marching uses a step cap (`cellMax = 1.1`) so the marcher cannot overshoot across a cell boundary and tunnel through a tower it should have hit. Arch's recursive form is an explicit corridor of receding arches.

Ruins-Once is the answer to "single colossal form versus scattered field": a stone platform and a column colonnade (columns laid out by clamped domain repetition), a faceted broken boulder-mass sitting on top, four debris shards that slowly bob and tumble nearby, and three pairs of thin poles receding to give scale and depth. Ruins-Recursive is the scattered field — an infinite tiled grid of hashed broken monoliths.

## Colour model

Three independent colours are the core of the brutalist read. `uMat` (Stone) is the surface albedo; `uFog` (Smog) is the atmosphere and fog; `uLight` (Light) drives the key light, specular, inner glow and the sun disc. Shading composes them as an ambient fill (a reddish-grey mix biased toward the smog, so shadowed faces pick up atmospheric bounce), plus the key light tinted by `uLight`, plus specular and fresnel and inner-glow terms, and finally a fog blend from the lit form toward the background smog.

Keeping these three separate is what makes the image read as brutalist. An early version tinted the forms the same colour as the fog, and the form-versus-atmosphere contrast collapsed into mush — that failure is the reason the model is structured this way now.

One deliberate decoupling: the background haze brightness does **not** depend on the Glow (light intensity) control. That means lowering Glow darkens the forms into silhouettes while the haze stays luminous — the Blade Runner / Gris "dark subject against bright dust" read. If haze brightness rode on Glow, dimming the light would dim the whole frame and that look would be impossible.

## Surface and displacement

There are two distinct layers, often conflated but controlled separately.

The first is **geometric displacement**, added inside `map()` by `soil(p)` and masked to a thin shell near surfaces with `smoothstep(.42, 0., abs(d))`. Because the displacement perturbs the distance field, `calcN()`'s central differences pick up the micro-relief automatically and it gets real lit highlights and shadowed valleys. Two styles share this path: the organic style (Soil/Hatch/Warp) sums value noise `n3` at 11/27/60× frequency — the top frequency is capped at 60 rather than higher to limit aliasing at point-blank range — with Grit as amplitude and Coarse as a frequency multiplier; the Facet style quantises an fBm field into discrete steps to produce flat plates, and `cellEdge()` darkens the plate boundaries to draw crack-seams.

The second layer is **image-level film grain**, produced by `surfTex()` in screen space and scaled by the Grain and Grain-size controls. It is added to the surface colour, and is skipped in Facet mode where the crack-seams take its place.

Displacement aliasing was a recurring problem worth understanding before touching this code. Bump displacement makes the SDF non-Lipschitz (its gradient can exceed 1), so the marcher can step past a surface and produce sparkle. The mitigations currently in place are: bounded displacement amplitude biased slightly negative (so it tends to carve in, which is safe for marching), the 60× frequency cap, an under-relaxed march step of 0.55, and the thin-shell mask. A distance-based attenuation of the displacement was tried and reverted — it flattened the texture and introduced its own instability.

## Wisp sprites

The sprites are simulated on the CPU in JavaScript, up to eight at once, each running a small state machine: Drift (random walk) transitions to Seek (glide to a target near a form) which transitions to Ember (attach to the surface, glow bright, then release back to Drift). Each sprite is packed into two `vec4`s — `uSpr` carries position in `xyz` and brightness in `w` (with `w ≥ 1` meaning it is an ember), and `uSprD` carries the normalised velocity direction in `xyz` and the wisp length in `w`.

In the shader, `wispGlow()` samples two points along the velocity axis — a dim tail and a bright head — to render an elongated, directional glow. This was an explicit requirement: the sprites must read as wisps, never as spheres, so a single radial falloff was not acceptable. Embers (`w ≥ 1`) additionally inject a local warm point light onto the nearest lit surface, which is what makes a form "light up from within" where a wisp merges into it.

## Camera

Each scene defines a base camera (`ro`, `ta`, `focal`). On top of that sits a slow autonomous motion layer: an orbit (rotation of the camera-relative vector in xz), a pitch (rotation in yz), and a dolly (scaling of that vector), each a sine of `ct = uTime * uSpeed * lifeFactor`, where the life factor is 0.25 in Passive and 1.0 in Active. Cycles run around 40 seconds in Passive and 10 in Active. Per-scene amplitudes are tuned to the subject — Towers get a lateral parallax dolly, Arch gets a push toward and through the opening, Helix orbits the coil.

User input is layered after the autonomous motion: drag adds yaw and tilt, and zoom dollies the camera toward the target. Because every autonomous term is a sine of `ct`, setting Speed to 0 makes `ct = 0`, every offset collapses to zero, and the whole scene — camera and animation — freezes cleanly to a still frame.

## Fog

Fog combines two terms as `1 - (1 - fDist) * (1 - fHeight)`. The distance term is a clear near-zone (no fog for `t < 3`) followed by `1 - exp(-(t-3) * k)`, with `k` from the Atmos slider. The height term, applied only in the ground scenes, is `smoothstep(3, -5, y)` — dense low and thinning upward — so the plunging bases of towers and the arch dissolve into the smog with no visible end.

There is deliberately **no ground-plane geometry**. A raymarched infinite ground plane was tried and removed: grazing-angle rays crawl along a flat surface taking hundreds of tiny steps (a major slowdown), and the displaced plane's hit point jittered frame-to-frame, which made the surface noise visibly crawl in tiled patterns. The bright low haze band in the background reads as the floor instead, and height-fog supplies the base dissolve.

## Sun disc

`sceneBg()` draws a soft sun disc, a broad halo, and a thin delicate ring along a low-horizon direction whose azimuth comes from the Aim slider. Forms silhouette against it. The disc elevation is currently fixed; Aim rotates it around the horizon but does not raise or lower it.

## Build and validation

The HTML is assembled by `form-world.build.py`. CSS, the HTML body, and the JavaScript are Python strings. The GLSL fragment shader is a Python list, `FRAG_LINES`, with one string per line; it is joined with newlines and then escaped (backslash, single-quote, newline) into a JavaScript single-quoted string literal that is injected into the JS.

The reason the project is generated rather than hand-written is practical: the file is large enough that writing it through shell heredocs truncated it mid-write more than once. Assembling line-lists in Python is reliable, keeps the shader readable and diffable, and lets the build run validation before anyone loads the page.

After every build, validate — the build environment has no GPU, so this is the only safety net short of testing on a device. Parse the JavaScript by feeding the script body to `new Function(...)` in Node, which catches a dropped quote that would otherwise blank the page. Check that braces and parentheses balance in the FRAG string. Grep for the feature tokens you expect to be present. At runtime the in-page shader compiler logs the full compile error to the console and prints the first error line into the on-screen `#fallback` element, because the mobile target has no console to inspect.

## Performance

The cost drivers, per marching step, are the soil displacement, ambient occlusion, the soft shadow, and the wisp evaluation. The fixed loop bounds are `MAXS = 96` march steps, `AOS = 5` AO samples, `SHS = 12` shadow steps. Ruins-Once is the most expensive scene — the colonnade, boulder, four debris shards and six poles add up to roughly a dozen SDF operations per sample. If frame rate drops on a device, reduce sprite Count and Inner first, then scene Count; Facet is cheaper than it looks, and Ruins-Once is the one to be wary of.
