package dev.aarso.hyle.cells

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.LocalHyleColors
import dev.aarso.hyle.theme.toHexRgb
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * **The real colour picker** — a from-scratch, honest-native-Compose port of the THREE.js
 * application in `kit/tactile-kit.html` (`#hyle-picker`, CSS ~L449-557, markup ~L644-677, app
 * logic ~L1172-1895), which is the actual source of truth this component is meant to resemble.
 * This file replaces an earlier pass whose 3D view was a bare wireframe cube behind a manual
 * toggle chip and whose layout was a big stacked ring+square, neither of which matched the
 * original. This pass reads the full IIFE (not a skim) and rebuilds the stage layout, the 3D
 * model, and the palette/region panel against it. **Canvas + gesture detectors only — no
 * WebView, no 3D library** (this repo's own engineering value; see the file history).
 *
 * ### What's a faithful port
 * - **Stage geometry**: hue ring (mid band, ~70% of the stage / ~15% inset), an SV slice
 *   centred *inside the ring's hole* (~38% / ~31% inset — not a separate square below the
 *   ring), and a small corner-anchored 3D model (~20% / ~78.5% inset) — the exact `.slot-*`
 *   percentages from the CSS. Tapping the corner element swaps it with the centred one
 *   (`swap()` in the source), animated like the source's own `.42s` CSS transition.
 * - **The 3D model's shape follows the active tab**, exactly like the source (`SPACES[space].model`):
 *   RGB → a cube, HSV → an open cylinder, Lab/HCL → the "blob" (the RGB cube's surface resampled
 *   into Lab space, `buildBlob()`). Each solid is drawn as a set of small flat-shaded, vertex-
 *   coloured facets in back-to-front (painter's-algorithm) order under a fixed orthographic
 *   camera whose eye direction is copied from the source's own `camFor()` per shape — then the
 *   real edge wireframe is drawn on top (cube: its 12 edges; cylinder: its top/bottom rim
 *   circles, matching what `EdgesGeometry(cylGeo, 30)` actually keeps once you account for its
 *   angle threshold — **the source has no wireframe for the blob at all** (`edges.blob = null`),
 *   so this port doesn't add one either).
 * - **A floating slice-plane + border**, per tab, showing where the 2D slice cuts the volume —
 *   ported from the source's `fillCubeQuad`/`fillCylRect`/`fillLDisc`/`fillBlobRect`, plus the
 *   translucent violet "sheet" halo and violet border outline it draws alongside the fill.
 * - **The palette engine is the source's real `genPalette`**: an 11-step-per-channel grid over
 *   the *current region* (the slice-position range sliders), farthest-point sampling in CIE Lab
 *   distance (optionally through the source's exact Machado-2009 deuteranopia matrix for the
 *   colour-blind-safe checkbox), not a re-invented simpler scheme.
 * - The RGB/HSV/Lab/HCL tabs, `srgbToLab`/`labToLch`, the hex readout + copy, and the hue-ring /
 *   SV-field drag maths are the pre-existing (already-correct) implementation, reused as-is.
 *
 * ### Deliberate scope cuts (read before assuming parity)
 * - **The ring + SV field always edit hue/saturation/value**, regardless of the selected tab.
 *   The source instead re-parameterises *what the ring/square edit* per family (an axis-cycle
 *   button lets you pick which native channel bands the ring for any of the 4 spaces — e.g. by
 *   default RGB bands on Blue, Lab bands on Lightness), which is a much bigger generic N-space
 *   slicing engine. Per this task's own item 4 ("keep the existing hue-ring/SV-field drag
 *   maths… don't rewrite for the sake of it"), that engine is **not ported** — only its
 *   *display* (tabs, 3D shape, slice-plane) follows the selected space; the interaction stays a
 *   fixed hue ring. For HSV and HCL this happens to match the source's own *default* band
 *   anyway (hue); for RGB (default: Blue) and Lab (default: Lightness) it doesn't.
 * - **Mode A (the per-channel arc-gauge stage) and Mode C (the orbit-draggable dense "cluster"
 *   view with a dots/cubes/spheres shape toggle) are not ported.** The source is a 3-mode
 *   picker (`mode: 'A'|'B'|'C'`, cycled by a button this port removes); this task's 5 items all
 *   describe Mode B's layout (ring + slice + corner model) as "the" picker, so that's what's
 *   built. Concretely this loses: the alternate per-channel gauge input surface, and Mode C's
 *   orbit-drag inspection of a dense (~2000-point) gamut sample. In its place, item 2's "small
 *   instanced markers plotting the palette's colours" is satisfied more cheaply: the *current
 *   (≤12) palette swatches* are plotted as small dots inside the 3D model, always, using the
 *   same per-shape position mapping the source uses for its single current-colour marker.
 * - **The task's framing of "decorative connector arcs… linking the ring/slice toward the
 *   corner model" doesn't match the source.** The only SVG overlay positioned above the stage
 *   (`#arcs`, z-index 5, pointer-events:none) is Mode A's set of per-channel gauge arcs — it is
 *   not a decoration connecting stage elements, and Mode A isn't ported (previous bullet), so no
 *   connector-arc decoration is added. What *is* real and ported: the ring's own drag thumb and
 *   the SV field's crosshair cursor.
 * - **The blob (Lab/HCL) surface is a 6×6-per-face grid (216 facets)**, not the source's 11×11
 *   (~600 triangles): Canvas has no GPU rasterizer, and redrawing ~600 individually-sorted,
 *   individually-filled `Path`s every recomposition is a real cost this pass trims proactively.
 *   Likewise the cube is a 4×4-per-face grid rather than one flat-coloured quad per face, so it
 *   still reads as a gradient once shaded (see next point).
 * - **Per-face Lambertian shading is an addition, not a literal port.** In the real source the
 *   gamut solids use `THREE.MeshBasicMaterial` — **unlit** in three.js; the scene's ambient +
 *   directional lights only actually affect Mode C's `MeshLambertMaterial` cluster meshes, which
 *   this pass doesn't port. Canvas also can't interpolate a colour smoothly across a filled
 *   polygon the way a GPU rasterizer interpolates per-vertex colours. So each small facet here
 *   is flat-filled with its 4 corners' averaged true gamut colour, *then* tinted by its own
 *   normal · light-direction dot product — a from-scratch substitute, done because the task
 *   explicitly asks the solid to "read as an actual 3D object," not because the source does it.
 * - **The hex field stays read-only-plus-copy** (unchanged from the prior pass), even though the
 *   source's `#hex` is a live-editable text input you can type a hex code into. Item 4 called
 *   the existing hex readout "already correct," so this pass didn't add text-edit parsing.
 *
 * ### Adversarial-audit fixes (this pass)
 * A follow-up audit against `kit/tactile-kit.html` found two real fidelity regressions in the
 * pass above, both now fixed:
 * - **The floating slice-plane fill was a single flat averaged swatch on the RGB/HSV/HCL tabs**
 *   (`rgbSlicePlane`/`hsvSlicePlane`/`hclSlicePlane`), not a gradient — the source's own `gQuad`
 *   feeds 4 per-corner colours into the GPU's vertex-colour interpolation (`tactile-kit.html`
 *   `fillCubeQuad`/`fillCylRect`/`fillBlobRect`, ~L1667-1703), producing a smooth surface these
 *   three functions were flattening into one `avg4(...)`. Fixed by routing them through the same
 *   [subdivideQuad] n×n-facet technique [buildCubeFaceFacets] already used for the solids, so the
 *   fill now reads as a gradient the same way. (`labSlicePlane`'s rim-fan construction already
 *   varied per facet and didn't need this.)
 * - **`genPalette`'s near-gray-candidate hue substitution used the ring's HSV-native hue even on
 *   the HCL tab**, where the source's own `hueCache` is native-LCH at that point (`sliceVals`'s
 *   HCL branch, `tactile-kit.html:1282`; re-synced per-family on every space/colour change,
 *   `:1355`). HSV-hue and LCH-hue disagree for the same RGB (sRGB red: 0deg vs ~40deg), so this
 *   mis-scoped which near-gray 11-step-grid candidates a narrowed Hue region would admit. Fixed
 *   in `roll()` by using `lch.h` (this composable's own native-LCH value) on the HCL tab instead.
 *   **Known residual approximation**: the source's `hueCache` persists a family's last real hue
 *   *through* achromatic dips (it's only overwritten when the current colour is non-achromatic in
 *   that family); this fix uses the current colour's live `lch.h` unconditionally, which can read
 *   as an arbitrary (but stable — `atan2(0,0) == 0`) hue at exact zero chroma rather than the last
 *   real one. Narrow blast radius unchanged from the audit's own framing (near-gray candidates,
 *   HCL tab only, narrowed Hue region only) — a full sticky-cache mirror of the source's semantics
 *   would need restructuring this composable's state model, which is out of scope for this fix.
 * Also fixed: the hue ring's tap/drag hit-region was the whole stage (`Modifier.fillMaxSize()`)
 * instead of the source's `#ring` div, a 70%-wide square inset 15% on each side
 * (`tactile-kit.html:470`) — the *drawn* ring was already correctly sized, only the interactive
 * area was oversized. See `Stage`'s ring hit-region comment.
 *
 * @param color the current colour (source of truth).
 * @param onColorChange called with the new opaque colour on each drag/tap/palette-select.
 * @param modifier layout modifier for the whole picker column.
 */
@Composable
fun HyleColorPicker3D(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptics = rememberHyleHaptics()
    val argb = color.toArgb()
    val incoming = remember(argb) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(argb, it) }
    }
    var hue by remember { mutableFloatStateOf(incoming[0]) }
    var sat by remember { mutableFloatStateOf(incoming[1]) }
    var value by remember { mutableFloatStateOf(incoming[2]) }

    // Re-sync from an externally-set colour (a preset/palette tap): preserve the local hue when
    // the incoming colour is achromatic, same rule the flat HyleColorPicker uses.
    LaunchedEffect(argb) {
        if (incoming[1] > 0.01f) hue = incoming[0]
        sat = incoming[1]
        value = incoming[2]
    }

    fun emit() {
        val out = android.graphics.Color.HSVToColor(
            floatArrayOf(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f)),
        )
        onColorChange(Color(out))
    }

    var tab by remember { mutableIntStateOf(1) } // default space = HSV, matching the source's `space = 'hsv'`
    var swapped by remember { mutableStateOf(false) }

    val currentArgb = remember(hue, sat, value) {
        android.graphics.Color.HSVToColor(floatArrayOf(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f)))
    }
    val rgbNow = remember(currentArgb) {
        floatArrayOf(
            android.graphics.Color.red(currentArgb) / 255f,
            android.graphics.Color.green(currentArgb) / 255f,
            android.graphics.Color.blue(currentArgb) / 255f,
        )
    }
    val lab = remember(currentArgb) {
        srgbToLab(android.graphics.Color.red(currentArgb), android.graphics.Color.green(currentArgb), android.graphics.Color.blue(currentArgb))
    }
    val lch = remember(lab) { labToLch(lab) }

    // ── region (the palette's sampling window) + palette state ──────────────────────────────
    var region by remember { mutableStateOf(famRanges(tab)) }
    LaunchedEffect(tab) { region = famRanges(tab) } // changeSpace() also calls resetRange()
    var palette by remember { mutableStateOf(listOf<FloatArray>()) }
    var palActive by remember { mutableIntStateOf(-1) }
    var palCount by remember { mutableIntStateOf(6) }
    var cvdSafe by remember { mutableStateOf(false) }
    var palOpen by remember { mutableStateOf(false) }

    fun roll() {
        // The near-gray/achromatic-candidate hue substitution inside `genPalette` (via
        // `sliceValsFor`/`inRegion`) must be expressed in the *active tab's own native hue
        // domain* — the source's own `hueCache` is always re-synced to the current family
        // whenever the colour or the space changes (`changeSpace`, `tactile-kit.html:1355`;
        // `sliceVals`'s HCL branch, `:1282`). HSV-hue and LCH-hue are numerically different for
        // the same RGB (sRGB red is H=0deg in HSV but ~40deg in LCH), so always passing the ring's
        // HSV `hue` state here mis-scoped the substitution on the HCL tab. RGB/Lab have no
        // angular axis at all (`famAxes().angular == -1` for both), so this value is unused for
        // those two tabs regardless; HSV already matches (the ring's own hue *is* HSV-native).
        val hueCache = if (tab == 3) lch.h.toFloat() else hue
        palette = genPalette(tab, region, palCount, cvdSafe, hueCache)
        palActive = -1
    }

    val mesh = when (tab) { 0 -> CUBE_MESH; 1 -> CYL_MESH; else -> BLOB_MESH }
    val slicePlane = remember(tab, hue, sat, value, lab, lch) {
        when (tab) {
            0 -> rgbSlicePlane(rgbNow)
            1 -> hsvSlicePlane(hue, sat, value)
            2 -> labSlicePlane(lab.l)
            else -> hclSlicePlane(lch.h)
        }
    }
    val markerPos = remember(tab, hue, sat, value, lab) {
        when (tab) { 0 -> cubeMarkerPos(rgbNow); 1 -> cylMarkerPos(hue, sat, value); else -> blobMarkerPos(lab) }
    }
    val paletteMarkers = remember(palette, tab) {
        palette.map { p ->
            val pos = when (tab) {
                0 -> cubeMarkerPos(p)
                1 -> {
                    val hsv = FloatArray(3)
                    android.graphics.Color.RGBToHSV((p[0] * 255).roundToInt().coerceIn(0, 255), (p[1] * 255).roundToInt().coerceIn(0, 255), (p[2] * 255).roundToInt().coerceIn(0, 255), hsv)
                    cylMarkerPos(hsv[0], hsv[1], hsv[2])
                }
                else -> blobMarkerPos(srgbToLab((p[0] * 255).roundToInt().coerceIn(0, 255), (p[1] * 255).roundToInt().coerceIn(0, 255), (p[2] * 255).roundToInt().coerceIn(0, 255)))
            }
            pos to Color(p[0], p[1], p[2])
        }
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HyleSegmentedToggle(
            options = listOf("RGB", "HSV", "Lab", "HCL"),
            selected = tab,
            onSelect = { tab = it },
            modifier = Modifier.fillMaxWidth(),
        )

        Stage(
            hue = hue, sat = sat, value = value,
            onHueChange = { hue = it; emit() },
            onSatValueChange = { s, v -> sat = s; value = v; emit() },
            swapped = swapped,
            onToggleSwap = { swapped = !swapped },
            mesh = mesh,
            slicePlane = slicePlane,
            markerPos = markerPos,
            markerColor = Color(currentArgb),
            paletteMarkers = paletteMarkers,
            haptics = haptics,
        )

        ChannelTable(tab = tab, argb = currentArgb, hue = hue, sat = sat, value = value)
        HexRow(color = Color(currentArgb), haptics = haptics)

        PaletteDisclosure(
            open = palOpen,
            onToggle = {
                palOpen = !palOpen
                if (palOpen && palette.isEmpty()) roll()
            },
        )
        if (palOpen) {
            PalettePanel(
                tab = tab,
                region = region,
                onRegionChange = { i, r -> region = region.toMutableList().also { it[i] = r } },
                onRegionReset = { region = famRanges(tab) },
                cvd = cvdSafe,
                onCvdChange = { cvdSafe = it; roll() },
                count = palCount,
                onCountChange = { palCount = it; roll() },
                palette = palette,
                active = palActive,
                onSelect = { i ->
                    palActive = i
                    val p = palette[i]
                    onColorChange(Color(p[0], p[1], p[2]))
                },
                onRoll = { roll() },
                onAdd = {
                    if (palette.size < 12) {
                        palette = palette + listOf(rgbNow.copyOf())
                        palActive = palette.lastIndex
                        palCount = palette.size
                    }
                },
                onReplace = {
                    if (palActive in palette.indices) {
                        palette = palette.toMutableList().also { it[palActive] = rgbNow.copyOf() }
                    }
                },
                onDelete = {
                    if (palActive in palette.indices) {
                        palette = palette.toMutableList().also { it.removeAt(palActive) }
                        palActive = palActive.coerceAtMost(palette.lastIndex)
                        // Unclamped, matching the source's own `palCount = palette.length`
                        // (`tactile-kit.html:1849`) — it can reach 0 after deleting the last
                        // swatch. Safe here too: [CountSlider]'s track math already clamps its
                        // drawn fraction to `0f..1f`, so a sub-range value never produces a
                        // negative-width fill.
                        palCount = palette.size
                    }
                },
            )
        }
    }
}

// ── the stage: hue ring + SV slice + 3D model, with the source's tap-to-swap ────────────────

/**
 * The square "stage" — the source's `#stage` (position:relative, `aspect-ratio:1/1`). Draws the
 * hue ring across the whole stage (visual radius ~0.33× the stage, matching `drawRing()`), then
 * absolutely positions the SV slice and the 3D model at the CSS's own `.slot-*` percentages,
 * animating between the default (slice centred/model corner) and swapped (model big/slice
 * corner) arrangements exactly like the source's `.42s` `.elem` transition.
 */
@Composable
private fun Stage(
    hue: Float,
    sat: Float,
    value: Float,
    onHueChange: (Float) -> Unit,
    onSatValueChange: (Float, Float) -> Unit,
    swapped: Boolean,
    onToggleSwap: () -> Unit,
    mesh: ShapeMesh,
    slicePlane: SlicePlane,
    markerPos: Vec3,
    markerColor: Color,
    paletteMarkers: List<Pair<Vec3, Color>>,
    haptics: HyleHaptics,
    modifier: Modifier = Modifier,
) {
    val hueColors = remember {
        (0..360 step 60).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f))) }
    }
    val pureHue = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))

    // `.slot-center` (31%/38%) <-> `.slot-corner` (78.5%/20%) for the slice; the model does the
    // complementary `.slot-corner` <-> `.slot-big` (23%/54%) swap — the source's `swap()`.
    val sliceFrac by animateFloatAsState(if (swapped) 0.20f else 0.38f, tween(420), label = "sliceFrac")
    val sliceInset by animateFloatAsState(if (swapped) 0.785f else 0.31f, tween(420), label = "sliceInset")
    val modelFrac by animateFloatAsState(if (swapped) 0.54f else 0.20f, tween(420), label = "modelFrac")
    val modelInset by animateFloatAsState(if (swapped) 0.23f else 0.785f, tween(420), label = "modelInset")

    BoxWithConstraints(modifier.fillMaxWidth().aspectRatio(1f)) {
        val stagePx = constraints.maxWidth.toFloat().coerceAtLeast(1f)

        // Ring visual: drawn across the whole stage — the drawn radius (0.33x stage) and stroke
        // are unaffected by the hit-region fix below.
        Canvas(Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val outerR = size.width * 0.33f
            val thick = max(size.width * 0.026f, 5.dp.toPx())
            drawCircle(Brush.sweepGradient(hueColors), outerR, Offset(cx, cy), style = Stroke(thick))
            val rad = hue * (PI.toFloat() / 180f)
            val tx = cx + outerR * cos(rad)
            val ty = cy + outerR * sin(rad)
            drawCircle(Color.Black.copy(alpha = 0.35f), thick * 0.34f, Offset(tx, ty + 2f))
            drawCircle(pureHue, thick * 0.34f, Offset(tx, ty))
            drawCircle(Color.White, thick * 0.34f, Offset(tx, ty), style = Stroke(width = 2.dp.toPx()))
        }

        // Ring hit-region: the source's `#ring` div that owns `pointerdown` is a 70%-wide square
        // inset 15% on each side (`top:15%;left:15%;width:70%;height:70%`, `tactile-kit.html:470`)
        // — not the full stage. Sized/positioned to match exactly; the slice/model boxes are
        // still composed after it (on top) and capture their own touches, so no manual "outside
        // the hole" band test is needed. `hueFromCenter` is passed this box's own size (not the
        // full `stagePx`) since the box is concentric with the stage (symmetric 15% inset), so
        // its local centre coincides with the stage's centre.
        val ringHitPx = 0.70f * stagePx
        val ringHitOffsetPx = (0.15f * stagePx).roundToInt()
        Box(
            Modifier
                .offset { IntOffset(ringHitOffsetPx, ringHitOffsetPx) }
                .size(with(LocalDensity.current) { ringHitPx.toDp() })
                .pointerInput(Unit) {
                    detectTapGestures { pos -> onHueChange(hueFromCenter(pos, ringHitPx)); haptics.settle() }
                }
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragEnd = { haptics.settle() },
                        onDrag = { change, _ -> onHueChange(hueFromCenter(change.position, ringHitPx)) },
                    )
                }
                .semantics { contentDescription = "Hue ring" },
        )

        val sliceSizePx = sliceFrac * stagePx
        val sliceOffset = (sliceInset * stagePx).roundToInt()
        Box(Modifier.offset { IntOffset(sliceOffset, sliceOffset) }.size(with(LocalDensity.current) { sliceSizePx.toDp() })) {
            SvSlice(
                hue = hue, sat = sat, value = value,
                onSatValueChange = onSatValueChange,
                isCorner = swapped,
                onCornerTap = onToggleSwap,
                haptics = haptics,
            )
        }

        val modelSizePx = modelFrac * stagePx
        val modelOffset = (modelInset * stagePx).roundToInt()
        Box(Modifier.offset { IntOffset(modelOffset, modelOffset) }.size(with(LocalDensity.current) { modelSizePx.toDp() })) {
            Model3D(
                mesh = mesh, slicePlane = slicePlane, markerPos = markerPos, markerColor = markerColor,
                paletteMarkers = paletteMarkers, onTap = onToggleSwap,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private fun hueFromCenter(pos: Offset, stagePx: Float): Float {
    val c = stagePx / 2f
    var deg = atan2(pos.y - c, pos.x - c) * (180f / PI.toFloat())
    if (deg < 0f) deg += 360f
    return deg
}

/**
 * Saturation/value field, adapted from [HyleColorPicker]'s linear drag maths to sit inside the
 * ring's hole. When [isCorner] (the swapped state — the source's `slice.slot-corner`), a tap
 * swaps back instead of setting a colour, matching `slice.addEventListener('pointerdown', …)`'s
 * own `if (slice.classList.contains('slot-corner')) { swap(); return; }` guard. The crosshair
 * cursor matches the source's `#cross` (a plain ring, not a filled colour thumb).
 */
@Composable
private fun SvSlice(
    hue: Float,
    sat: Float,
    value: Float,
    onSatValueChange: (Float, Float) -> Unit,
    isCorner: Boolean,
    onCornerTap: () -> Unit,
    haptics: HyleHaptics,
    modifier: Modifier = Modifier,
) {
    val pureHue = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    BoxWithConstraints(
        modifier
            .clip(RoundedCornerShape(3.dp))
            .background(Brush.horizontalGradient(listOf(Color.White, pureHue)))
            .semantics { contentDescription = "Saturation and brightness field" },
    ) {
        Box(
            Modifier.fillMaxSize()
                .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black))),
        )
        val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val hPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        fun setFromOffset(x: Float, y: Float) {
            val s = (x / wPx).coerceIn(0f, 1f)
            val v = (1f - y / hPx).coerceIn(0f, 1f)
            onSatValueChange(s, v)
        }
        Box(
            Modifier.fillMaxSize()
                .pointerInput(isCorner) {
                    detectTapGestures { pos ->
                        if (isCorner) { haptics.tap(); onCornerTap() } else { setFromOffset(pos.x, pos.y); haptics.settle() }
                    }
                }
                .pointerInput(isCorner) {
                    if (!isCorner) {
                        detectDragGestures(
                            onDragEnd = { haptics.settle() },
                            onDrag = { change, _ -> setFromOffset(change.position.x, change.position.y) },
                        )
                    }
                },
        )
        if (!isCorner) {
            val crossX = maxWidth * sat.coerceIn(0f, 1f)
            val crossY = maxHeight * (1f - value).coerceIn(0f, 1f)
            Box(
                Modifier
                    .offset { IntOffset((crossX - 9.dp).roundToPx(), (crossY - 9.dp).roundToPx()) }
                    .size(18.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
                    .border(1.dp, Color.Black.copy(alpha = 0.4f), CircleShape),
            )
        }
    }
}

// ── the 3D model: a lit-looking solid + wireframe + slice-plane + palette markers ───────────

/**
 * The corner (or, swapped, "big") 3D view. Draws, back-to-front: the translucent violet "sheet"
 * halo around the slice-plane, the shape's own solid facets (painter's-algorithm depth sort under
 * a fixed camera, each facet flat-shaded — see file KDoc on why that's a from-scratch addition
 * rather than a port of the source's lighting), the slice-plane's true-colour fill, its violet
 * border outline, the shape's wireframe edges (none for the blob — the source has none either),
 * the palette's swatches as small dots, and the current colour as a bigger glowing marker on top
 * — mirroring the source's solid → plane → border → edges → marker layering as closely as a 2D
 * Canvas painter's algorithm reasonably can.
 */
@Composable
private fun Model3D(
    mesh: ShapeMesh,
    slicePlane: SlicePlane,
    markerPos: Vec3,
    markerColor: Color,
    paletteMarkers: List<Pair<Vec3, Color>>,
    onTap: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    Canvas(
        modifier
            .pointerInput(Unit) { detectTapGestures { haptics.tap(); onTap() } }
            .semantics { contentDescription = "3D colour-space model, tap to expand" },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        // Camera half-width 1.1 world units (the source's fixed Mode-B `vw = 2.2`) maps to half
        // the canvas — same fixed framing for every shape, so shapes differ in apparent size
        // exactly like the un-auto-fit source does.
        val scale = min(size.width, size.height) / 2f / 1.1f

        drawFilledPolygon(slicePlane.sheet, mesh.basis, scale, cx, cy, c.violet.copy(alpha = 0.14f), shaded = false)
        drawFaces(mesh.faces, mesh.basis, scale, cx, cy, mesh.fillAlpha)
        slicePlane.fillFaces.forEach { f ->
            drawFilledPolygon(f.corners, mesh.basis, scale, cx, cy, f.color.copy(alpha = 0.9f), shaded = false)
        }
        drawBorderLoop(slicePlane.borderLoop, mesh.basis, scale, cx, cy, c.violet)
        drawEdges(mesh.edges, mesh.basis, scale, cx, cy, Color.White, mesh.edgeAlpha)
        paletteMarkers.forEach { (p, col) -> drawSmallMarker(p, mesh.basis, scale, cx, cy, col) }
        drawGlowMarker(markerPos, mesh.basis, scale, cx, cy, markerColor)
    }
}

private fun DrawScope.project(p: Vec3, basis: Basis, scale: Float, cx: Float, cy: Float): Offset =
    Offset(cx + p.dot(basis.right) * scale, cy - p.dot(basis.up) * scale)

private fun DrawScope.drawFaces(faces: List<Face3D>, basis: Basis, scale: Float, cx: Float, cy: Float, alpha: Float) {
    val sorted = faces.sortedBy { f -> f.corners.sumOf { it.dot(basis.depth).toDouble() } / f.corners.size }
    sorted.forEach { f ->
        val tinted = shade(f.color, faceNormal(f.corners)).copy(alpha = alpha)
        drawFilledPolygon(f.corners, basis, scale, cx, cy, tinted, shaded = false)
    }
}

private fun DrawScope.drawFilledPolygon(corners: List<Vec3>, basis: Basis, scale: Float, cx: Float, cy: Float, color: Color, shaded: Boolean) {
    if (corners.size < 3) return
    val fill = if (shaded) shade(color, faceNormal(corners)) else color
    val pts = corners.map { project(it, basis, scale, cx, cy) }
    val path = Path().apply {
        moveTo(pts[0].x, pts[0].y)
        for (i in 1 until pts.size) lineTo(pts[i].x, pts[i].y)
        close()
    }
    drawPath(path, color = fill)
}

private fun DrawScope.drawBorderLoop(loop: List<Vec3>, basis: Basis, scale: Float, cx: Float, cy: Float, color: Color) {
    if (loop.size < 2) return
    val pts = loop.map { project(it, basis, scale, cx, cy) }
    for (i in pts.indices) {
        drawLine(color.copy(alpha = 0.9f), pts[i], pts[(i + 1) % pts.size], strokeWidth = 1.4.dp.toPx())
    }
}

private fun DrawScope.drawEdges(edges: List<Edge3D>, basis: Basis, scale: Float, cx: Float, cy: Float, color: Color, alpha: Float) {
    edges.forEach { e ->
        drawLine(
            color.copy(alpha = alpha),
            project(e.a, basis, scale, cx, cy),
            project(e.b, basis, scale, cx, cy),
            strokeWidth = 1.2.dp.toPx(),
        )
    }
}

private fun DrawScope.drawGlowMarker(pos: Vec3, basis: Basis, scale: Float, cx: Float, cy: Float, color: Color) {
    val p = project(pos, basis, scale, cx, cy)
    val r = 5.dp.toPx()
    drawCircle(color.copy(alpha = 0.30f), r * 2.0f, p)
    drawCircle(color.copy(alpha = 0.55f), r * 1.4f, p)
    drawCircle(color, r, p)
    drawCircle(Color.White, r, p, style = Stroke(width = 1.6.dp.toPx()))
}

private fun DrawScope.drawSmallMarker(pos: Vec3, basis: Basis, scale: Float, cx: Float, cy: Float, color: Color) {
    val p = project(pos, basis, scale, cx, cy)
    val r = 2.6.dp.toPx()
    drawCircle(color, r, p)
    drawCircle(Color.Black.copy(alpha = 0.35f), r, p, style = Stroke(width = 1.dp.toPx()))
}

// ── 3D geometry primitives ───────────────────────────────────────────────────────────────────

private data class Vec3(val x: Float, val y: Float, val z: Float) {
    operator fun plus(o: Vec3) = Vec3(x + o.x, y + o.y, z + o.z)
    operator fun minus(o: Vec3) = Vec3(x - o.x, y - o.y, z - o.z)
    operator fun times(s: Float) = Vec3(x * s, y * s, z * s)
    fun dot(o: Vec3): Float = x * o.x + y * o.y + z * o.z
    fun cross(o: Vec3) = Vec3(y * o.z - z * o.y, z * o.x - x * o.z, x * o.y - y * o.x)
    fun length(): Float = sqrt(x * x + y * y + z * z)
    fun normalized(): Vec3 { val l = length(); return if (l > 1e-6f) Vec3(x / l, y / l, z / l) else this }
}

/** A fixed orthographic camera's orthonormal basis, built the same way `THREE.Camera.lookAt`
 *  does: [depth] is the (unit) eye direction the camera sits along (looking at the origin);
 *  [right]/[up] span the screen plane. No orbit/drag rotation — Mode B's camera is fixed per
 *  shape in the source (`camFor()`); only Mode C, which this pass doesn't port, lets you drag
 *  to orbit. */
private data class Basis(val right: Vec3, val up: Vec3, val depth: Vec3)

private fun cameraBasis(eyeDir: Vec3): Basis {
    val d = eyeDir.normalized()
    val worldUp = Vec3(0f, 1f, 0f)
    val right = worldUp.cross(d).normalized()
    val up = d.cross(right)
    return Basis(right, up, d)
}

/** One small flat-shaded polygon in object space, already reduced to a single fill colour (the
 *  true, unshaded average of its corners' gamut colours — [shade] tints it at draw time). */
private data class Face3D(val corners: List<Vec3>, val color: Color)
private data class Edge3D(val a: Vec3, val b: Vec3)

private data class ShapeMesh(
    val faces: List<Face3D>,
    val edges: List<Edge3D>,
    val fillAlpha: Float,
    val edgeAlpha: Float,
    val basis: Basis,
)

private val LIGHT_DIR = Vec3(0.5f, 1f, 0.85f).normalized()

/** See the file KDoc's "Deliberate scope cuts" — the source's gamut solids use an *unlit*
 *  `MeshBasicMaterial`, so this tint is a from-scratch substitute for per-pixel light/vertex-
 *  colour interpolation Canvas can't do, not a port of the (real, but otherwise-unused-here)
 *  scene lights. */
private fun shade(base: Color, normal: Vec3): Color {
    val n = normal.dot(LIGHT_DIR).coerceIn(-1f, 1f)
    val tint = 0.58f + 0.42f * max(0f, n)
    return Color(
        red = (base.red * tint).coerceIn(0f, 1f),
        green = (base.green * tint).coerceIn(0f, 1f),
        blue = (base.blue * tint).coerceIn(0f, 1f),
        alpha = base.alpha,
    )
}

private fun faceNormal(corners: List<Vec3>): Vec3 {
    if (corners.size < 3) return Vec3(0f, 0f, 1f)
    val n = (corners[1] - corners[0]).cross(corners[2] - corners[0])
    val centroid = corners.fold(Vec3(0f, 0f, 0f)) { acc, v -> acc + v } * (1f / corners.size)
    val normalized = n.normalized()
    // Flip to face away from the shape's own centre (the origin, for cube/cyl/blob alike) so
    // shading reads as an outward-facing surface regardless of winding order.
    return if (normalized.dot(centroid) < 0f) normalized * -1f else normalized
}

private fun avg4(a: Color, b: Color, c: Color, d: Color) = Color(
    red = (a.red + b.red + c.red + d.red) / 4f,
    green = (a.green + b.green + c.green + d.green) / 4f,
    blue = (a.blue + b.blue + c.blue + d.blue) / 4f,
)

private fun avg3(a: Color, b: Color, c: Color) = Color(
    red = (a.red + b.red + c.red) / 3f,
    green = (a.green + b.green + c.green) / 3f,
    blue = (a.blue + b.blue + c.blue) / 3f,
)

// ── the three gamut solids (built once — static geometry, exactly like the source's
// pre-built THREE.js geometries) ─────────────────────────────────────────────────────────────

/** Subdivide a planar quad — parameterised by (u,v) in [0,1]² for both position and colour —
 *  into an n×n grid of small flat facets, each filled with the average of its own 4 corners'
 *  *exact* colour-function values (not a re-average of the parent's already-averaged corners).
 *  This is what makes a smoothly-varying fill (linear/bilinear in u,v, same as every quad the
 *  source feeds into its GPU rasterizer's per-vertex colour interpolation — `gQuad` in
 *  `tactile-kit.html:1667`) read as a gradient once painted with flat-shaded facets, instead of
 *  one flat averaged swatch. Originally only used for the cube's faces; must-fix #1 in the
 *  adversarial audit found the floating slice-planes (`rgbSlicePlane`/`hsvSlicePlane`/
 *  `hclSlicePlane`) skipped this and used a single `Face3D` per plane instead — fixed by routing
 *  them through this same helper. */
private fun subdivideQuad(n: Int, pos: (u: Float, v: Float) -> Vec3, col: (u: Float, v: Float) -> Color): List<Face3D> {
    val faces = mutableListOf<Face3D>()
    for (i in 0 until n) for (j in 0 until n) {
        val u0 = i / n.toFloat(); val u1 = (i + 1) / n.toFloat()
        val v0 = j / n.toFloat(); val v1 = (j + 1) / n.toFloat()
        val corners = listOf(pos(u0, v0), pos(u1, v0), pos(u1, v1), pos(u0, v1))
        val fill = avg4(col(u0, v0), col(u1, v0), col(u1, v1), col(u0, v1))
        faces.add(Face3D(corners, fill))
    }
    return faces
}

/** One face of the unit RGB cube (±0.5 per axis), subdivided into an n×n grid of flat facets —
 *  a finer grid than one flat quad-per-face so, once shaded, it reads as the smooth gradient the
 *  source gets for free from per-vertex colour interpolation. Colour rule is the source's own:
 *  vertex colour = position + 0.5 per axis (`cubeGeo` in `tactile-kit.html`). */
private fun buildCubeFaceFacets(axis: Int, fixed: Float, n: Int): List<Face3D> {
    val o1 = (axis + 1) % 3
    val o2 = (axis + 2) % 3
    fun point(u: Float, v: Float): Vec3 {
        val p = FloatArray(3)
        p[axis] = fixed - 0.5f
        p[o1] = u - 0.5f
        p[o2] = v - 0.5f
        return Vec3(p[0], p[1], p[2])
    }
    fun color(u: Float, v: Float): Color {
        val c = FloatArray(3)
        c[axis] = fixed
        c[o1] = u
        c[o2] = v
        return Color(c[0], c[1], c[2])
    }
    return subdivideQuad(n, { u, v -> point(u, v) }, { u, v -> color(u, v) })
}

private val CUBE_CORNERS: List<Vec3> = (0..7).map { i ->
    Vec3(if (i and 1 != 0) 0.5f else -0.5f, if (i and 2 != 0) 0.5f else -0.5f, if (i and 4 != 0) 0.5f else -0.5f)
}
private val CUBE_EDGE_PAIRS: List<Pair<Int, Int>> = buildList {
    for (i in 0..7) for (j in i + 1..7) if ((i xor j).countOneBits() == 1) add(i to j)
}

private fun buildCubeMesh(): ShapeMesh {
    val n = 4
    val faces = mutableListOf<Face3D>()
    for (axis in 0..2) for (fixed in listOf(0f, 1f)) faces += buildCubeFaceFacets(axis, fixed, n)
    val edges = CUBE_EDGE_PAIRS.map { (a, b) -> Edge3D(CUBE_CORNERS[a], CUBE_CORNERS[b]) }
    return ShapeMesh(faces, edges, fillAlpha = 0.5f, edgeAlpha = 0.3f, basis = cameraBasis(Vec3(1.1f, 0.75f, 1.5f)))
}

/** The open (no caps) HSV cylinder wall — the source's `CylinderGeometry(0.5,0.5,1,40,1,false)`,
 *  coloured `hsv2rgb(angle, 1, y+0.5)` per vertex (radius is constant on the side wall, so
 *  saturation is always 1 there). 28 radial segments (vs the source's 40) — plenty smooth at
 *  picker scale, cheaper to depth-sort every frame. */
private fun buildCylinderMesh(): ShapeMesh {
    val n = 28
    val faces = mutableListOf<Face3D>()
    val edges = mutableListOf<Edge3D>()
    fun cornerPos(angleDeg: Float, y: Float): Vec3 {
        val rad = angleDeg * (PI.toFloat() / 180f)
        return Vec3(0.5f * cos(rad), y, 0.5f * sin(rad))
    }
    fun cornerColor(angleDeg: Float, v: Float): Color {
        val hue = ((angleDeg % 360f) + 360f) % 360f
        return Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, v.coerceIn(0f, 1f))))
    }
    for (i in 0 until n) {
        val a0 = i / n.toFloat() * 360f
        val a1 = (i + 1) / n.toFloat() * 360f
        val p00 = cornerPos(a0, -0.5f); val p10 = cornerPos(a1, -0.5f)
        val p11 = cornerPos(a1, 0.5f); val p01 = cornerPos(a0, 0.5f)
        val c00 = cornerColor(a0, 0f); val c10 = cornerColor(a1, 0f)
        val c11 = cornerColor(a1, 1f); val c01 = cornerColor(a0, 1f)
        faces.add(Face3D(listOf(p00, p10, p11, p01), avg4(c00, c10, c11, c01)))
        edges.add(Edge3D(cornerPos(a0, 0.5f), cornerPos(a1, 0.5f)))
        edges.add(Edge3D(cornerPos(a0, -0.5f), cornerPos(a1, -0.5f)))
    }
    return ShapeMesh(faces, edges, fillAlpha = 0.17f, edgeAlpha = 0.26f, basis = cameraBasis(Vec3(0.7f, 0.6f, -0.7f)))
}

/** The "blob": the RGB cube's 6 faces resampled into Lab space (`buildBlob()` in the source),
 *  n×n per face (source: 11×11 — reduced here, see file KDoc). **No wireframe**: the source's own
 *  `edges.blob` is `null`, so none is added here either. */
private fun buildBlobMesh(): ShapeMesh {
    val n = 6
    val faces = mutableListOf<Face3D>()
    for (axis in 0..2) for (fixed in listOf(0f, 1f)) {
        val o1 = (axis + 1) % 3
        val o2 = (axis + 2) % 3
        fun rgbAt(u: Float, v: Float): FloatArray {
            val c = FloatArray(3); c[axis] = fixed; c[o1] = u; c[o2] = v; return c
        }
        fun posAt(rgb: FloatArray): Vec3 {
            val lab = srgbToLab(
                (rgb[0] * 255f).roundToInt().coerceIn(0, 255),
                (rgb[1] * 255f).roundToInt().coerceIn(0, 255),
                (rgb[2] * 255f).roundToInt().coerceIn(0, 255),
            )
            return Vec3((lab.a / 130.0).toFloat(), ((lab.l - 50.0) / 130.0).toFloat(), (lab.b / 130.0).toFloat())
        }
        for (i in 0 until n) for (j in 0 until n) {
            val u0 = i / n.toFloat(); val u1 = (i + 1) / n.toFloat()
            val v0 = j / n.toFloat(); val v1 = (j + 1) / n.toFloat()
            val r00 = rgbAt(u0, v0); val r10 = rgbAt(u1, v0); val r11 = rgbAt(u1, v1); val r01 = rgbAt(u0, v1)
            val corners = listOf(posAt(r00), posAt(r10), posAt(r11), posAt(r01))
            val col = Color(
                (r00[0] + r10[0] + r11[0] + r01[0]) / 4f,
                (r00[1] + r10[1] + r11[1] + r01[1]) / 4f,
                (r00[2] + r10[2] + r11[2] + r01[2]) / 4f,
            )
            faces.add(Face3D(corners, col))
        }
    }
    return ShapeMesh(faces, emptyList(), fillAlpha = 0.6f, edgeAlpha = 0f, basis = cameraBasis(Vec3(0.9f, 0.55f, 1.3f)))
}

private val CUBE_MESH: ShapeMesh by lazy { buildCubeMesh() }
private val CYL_MESH: ShapeMesh by lazy { buildCylinderMesh() }
private val BLOB_MESH: ShapeMesh by lazy { buildBlobMesh() }

// ── marker positions per shape (the source's `markerPos()`) ─────────────────────────────────

private fun cubeMarkerPos(rgb: FloatArray) = Vec3(rgb[0] - 0.5f, rgb[1] - 0.5f, rgb[2] - 0.5f)

private fun cylMarkerPos(hueDeg: Float, sat: Float, value: Float): Vec3 {
    val rad = hueDeg * (PI.toFloat() / 180f)
    return Vec3(sat * 0.5f * cos(rad), value - 0.5f, sat * 0.5f * sin(rad))
}

private fun blobMarkerPos(lab: Lab) = Vec3((lab.a / 130.0).toFloat(), ((lab.l - 50.0) / 130.0).toFloat(), (lab.b / 130.0).toFloat())

// ── the floating slice-plane + violet "sheet" halo + border, per tab ────────────────────────
// Ports of fillCubeQuad/fillCylRect/fillLDisc/fillBlobRect + their sheetPerp/sheetRadial halo
// and the border loop the source traces from the same corners (`gQuad`'s `bSeg` calls).

/** Chroma-axis max for the Lab/HCL slices — the source's own `CMAX = 135` (`tactile-kit.html:1175`),
 *  shared by [hclSlicePlane], [maxChroma], and the HCL tab's region range so it can't drift out
 *  of sync between them (was a hardcoded `135f` local to `hclSlicePlane` alone). */
private const val CMAX: Float = 135f

/** Facet grid resolution for the floating slice-planes (`rgbSlicePlane`/`hsvSlicePlane`/
 *  `hclSlicePlane`) — subdivided so a smoothly-varying fill reads as a gradient once painted with
 *  flat-shaded facets, same technique as [buildCubeFaceFacets]. See [subdivideQuad]. */
private const val SLICE_GRID = 8

private data class SlicePlane(val fillFaces: List<Face3D>, val sheet: List<Vec3>, val borderLoop: List<Vec3>)

/** RGB tab: the source's default depth axis for the "ortho" family is Blue (`DEPTH.ortho = 2`)
 *  — an R×G square at the current Blue value. (The axis-cycle button that lets you pick a
 *  different depth channel isn't ported — see file KDoc.) */
private fun rgbSlicePlane(rgb: FloatArray, n: Int = SLICE_GRID): SlicePlane {
    val depth = rgb[2]
    fun pos(u: Float, v: Float) = Vec3(u - 0.5f, v - 0.5f, depth - 0.5f)
    fun col(u: Float, v: Float) = Color(u, v, depth)
    val corners = listOf(pos(0f, 0f), pos(1f, 0f), pos(1f, 1f), pos(0f, 1f))
    val fillFaces = subdivideQuad(n, { u, v -> pos(u, v) }, { u, v -> col(u, v) })
    val sheet = listOf(pos(-0.07f, -0.07f), pos(1.07f, -0.07f), pos(1.07f, 1.07f), pos(-0.07f, 1.07f))
    return SlicePlane(fillFaces, sheet, corners)
}

/** HSV tab: the radial saturation×value wedge at the current hue — this is exactly the same
 *  gradient the interactive SV field already shows, just wrapped onto the cylinder. */
private fun hsvSlicePlane(hueDeg: Float, sat: Float, value: Float, n: Int = SLICE_GRID): SlicePlane {
    val rad = hueDeg * (PI.toFloat() / 180f)
    val cx = cos(rad); val cz = sin(rad)
    fun pos(s: Float, v: Float) = Vec3(s * 0.5f * cx, v - 0.5f, s * 0.5f * cz)
    // The true HSV(H,S,V)->RGB formula, evaluated exactly at each grid corner. For fixed hue this
    // is bilinear in (S,V) — `RGB = V*(1-S) + V*S*hueRGB` — so it reproduces exactly the same
    // smooth surface the source's GPU gets "for free" from bilinearly interpolating the same 4
    // corner colours (black, black, pureHue, white) the source feeds `gQuad` (`fillCylRect`,
    // `tactile-kit.html:1687-1689`); computing the real formula per grid-corner is simpler than
    // re-deriving the bilinear blend by hand and gives an identical result.
    fun col(s: Float, v: Float) = Color(android.graphics.Color.HSVToColor(floatArrayOf(hueDeg, s.coerceIn(0f, 1f), v.coerceIn(0f, 1f))))
    val corners = listOf(pos(0f, 0f), pos(1f, 0f), pos(1f, 1f), pos(0f, 1f))
    val fillFaces = subdivideQuad(n, { u, v -> pos(u, v) }, { u, v -> col(u, v) })
    val sheet = listOf(
        Vec3(-0.12f * cx, -0.55f, -0.12f * cz), Vec3(0.6f * cx, -0.55f, 0.6f * cz),
        Vec3(0.6f * cx, 0.55f, 0.6f * cz), Vec3(-0.12f * cx, 0.55f, -0.12f * cz),
    )
    return SlicePlane(fillFaces, sheet, corners)
}

/** Lab tab: a constant-lightness gamut cross-section disc at the current L — `fillLDisc`, the
 *  source's default depth for Lab (`DEPTH.lab = 0` = Lightness). */
private fun labSlicePlane(lightness: Double, ringSteps: Int = 24): SlicePlane {
    val y = ((lightness - 50.0) / 130.0).toFloat()
    val center = Vec3(0f, y, 0f)
    val centerRgb = lab2rgb(lightness, 0.0, 0.0)
    val centerColor = Color(centerRgb[0].coerceIn(0f, 1f), centerRgb[1].coerceIn(0f, 1f), centerRgb[2].coerceIn(0f, 1f))
    val rim = (0 until ringSteps).map { i ->
        val h = i / ringSteps.toDouble() * 360.0
        val chroma = maxChroma(h, lightness)
        val lab = lch2lab(h, chroma, lightness)
        val rgb = lab2rgb(lab.l, lab.a, lab.b)
        val pos = Vec3((chroma * cos(Math.toRadians(h)) / 130.0).toFloat(), y, (chroma * sin(Math.toRadians(h)) / 130.0).toFloat())
        pos to Color(rgb[0].coerceIn(0f, 1f), rgb[1].coerceIn(0f, 1f), rgb[2].coerceIn(0f, 1f))
    }
    val faces = (0 until ringSteps).map { i ->
        val (p0, c0) = rim[i]
        val (p1, c1) = rim[(i + 1) % ringSteps]
        Face3D(listOf(center, p0, p1), avg3(centerColor, c0, c1))
    }
    val sheet = listOf(Vec3(-0.57f, y, -0.57f), Vec3(0.57f, y, -0.57f), Vec3(0.57f, y, 0.57f), Vec3(-0.57f, y, 0.57f))
    return SlicePlane(faces, sheet, rim.map { it.first })
}

/** HCL tab: the radial chroma×lightness wedge at the current hue — `fillBlobRect`, the source's
 *  default depth for HCL (`DEPTH.lch = 0` = Hue), which happens to already match this port's
 *  fixed hue-ring interaction (see file KDoc). */
private fun hclSlicePlane(hueDeg: Double, n: Int = SLICE_GRID): SlicePlane {
    val rad = Math.toRadians(hueDeg)
    val cx = cos(rad).toFloat(); val cz = sin(rad).toFloat()
    fun pos(c: Float, l: Float) = Vec3(c * cx / CMAX, (l - 50f) / 130f, c * cz / CMAX)
    // The source's own `fillBlobRect` feeds literal black/black/white/white corner colours into
    // `gQuad` here (`tactile-kit.html:1700-1703`) — i.e. a height-only (lightness) ramp,
    // independent of chroma and hue. That's reproduced verbatim below (colour depends only on
    // `v`, the lightness fraction), not "corrected" to the true per-point Lab->RGB colour, which
    // the source itself never computes for this particular fill.
    fun col(v: Float): Color { val t = v.coerceIn(0f, 1f); return Color(t, t, t) }
    val corners = listOf(pos(0f, 0f), pos(CMAX, 0f), pos(CMAX, 100f), pos(0f, 100f))
    val fillFaces = subdivideQuad(n, { u, v -> pos(u * CMAX, v * 100f) }, { _, v -> col(v) })
    val sheet = listOf(
        Vec3(-0.12f * cx, -0.55f, -0.12f * cz), Vec3(0.6f * cx, -0.55f, 0.6f * cz),
        Vec3(0.6f * cx, 0.55f, 0.6f * cz), Vec3(-0.12f * cx, 0.55f, -0.12f * cz),
    )
    return SlicePlane(fillFaces, sheet, corners)
}

// ── sRGB <-> CIE Lab <-> LCH (HCL), pure functions so they're easy to unit-test ──────────────

/** CIE L*a*b* (D65 white point). */
data class Lab(val l: Double, val a: Double, val b: Double)

/** Cylindrical form of [Lab] — lightness/chroma/hue, i.e. LCH (the kit's "HCL" tab). */
data class Lch(val l: Double, val c: Double, val h: Double)

private fun linearizeSrgbChannel(c: Double): Double =
    if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

private fun delinearizeSrgbChannel(c: Double): Double =
    if (c <= 0.0031308) 12.92 * c else 1.055 * c.pow(1.0 / 2.4) - 0.055

/** sRGB (0..255 per channel) → CIE L*a*b*, D65 white point, standard formulas. */
fun srgbToLab(r: Int, g: Int, b: Int): Lab {
    val rl = linearizeSrgbChannel(r / 255.0)
    val gl = linearizeSrgbChannel(g / 255.0)
    val bl = linearizeSrgbChannel(b / 255.0)

    val x = 0.4124564 * rl + 0.3575761 * gl + 0.1804375 * bl
    val y = 0.2126729 * rl + 0.7151522 * gl + 0.0721750 * bl
    val z = 0.0193339 * rl + 0.1191920 * gl + 0.9503041 * bl

    val xn = 0.95047
    val yn = 1.0
    val zn = 1.08883
    fun f(t: Double): Double = if (t > 0.008856) t.pow(1.0 / 3.0) else (7.787 * t + 16.0 / 116.0)
    val fx = f(x / xn)
    val fy = f(y / yn)
    val fz = f(z / zn)

    val l = 116.0 * fy - 16.0
    val a = 500.0 * (fx - fy)
    val bb = 200.0 * (fy - fz)
    return Lab(l, a, bb)
}

/** [Lab] → LCH: C = |a,b| magnitude, H = angle of (a,b) in degrees, L unchanged. */
fun labToLch(lab: Lab): Lch {
    val chroma = sqrt(lab.a * lab.a + lab.b * lab.b)
    var hueDeg = Math.toDegrees(atan2(lab.b, lab.a))
    if (hueDeg < 0.0) hueDeg += 360.0
    return Lch(lab.l, chroma, hueDeg)
}

/** CIE L*a*b* → sRGB (0..1 per channel, unclamped — callers gamut-check with [inGamut] or clamp
 *  as appropriate). The inverse of [srgbToLab]. */
fun lab2rgb(l: Double, a: Double, b: Double): FloatArray {
    val fy = (l + 16.0) / 116.0
    val fx = fy + a / 500.0
    val fz = fy - b / 200.0
    fun finv(t: Double): Double { val t3 = t * t * t; return if (t3 > 0.008856) t3 else (t - 16.0 / 116.0) / 7.787 }
    val x = 0.95047 * finv(fx)
    val y = 1.0 * finv(fy)
    val z = 1.08883 * finv(fz)
    val r = x * 3.2404542 + y * -1.5371385 + z * -0.4985314
    val g = x * -0.9692660 + y * 1.8760108 + z * 0.0415560
    val bl = x * 0.0556434 + y * -0.2040259 + z * 1.0572252
    return floatArrayOf(delinearizeSrgbChannel(r).toFloat(), delinearizeSrgbChannel(g).toFloat(), delinearizeSrgbChannel(bl).toFloat())
}

/** LCH → [Lab], parameter order (H, C, L) matching the source's own `lch2lab(H, C, L)`. */
fun lch2lab(hDeg: Double, c: Double, l: Double): Lab {
    val h = Math.toRadians(hDeg)
    return Lab(l, c * cos(h), c * sin(h))
}

private fun inGamut(rgb: FloatArray): Boolean = rgb.all { it >= -0.0008f && it <= 1.0008f }

/** Binary search for the largest chroma still in the sRGB gamut at hue [hDeg] / lightness [l] —
 *  the source's `maxChroma`, 16 bisection steps. */
private fun maxChroma(hDeg: Double, l: Double): Double {
    var lo = 0.0
    var hi = CMAX.toDouble()
    repeat(16) {
        val mid = (lo + hi) / 2.0
        val lab = lch2lab(hDeg, mid, l)
        if (inGamut(lab2rgb(lab.l, lab.a, lab.b))) lo = mid else hi = mid
    }
    return lo
}

// ── the region + palette engine — a real port of the source's genPalette/inRanges/simDeut,
// not a re-invented simpler scheme (task item 3) ─────────────────────────────────────────────

/** Per-tab native channel names, in the source's own per-family order (`famAxes().names`). */
private val FAM_NAMES: Array<Array<String>> = arrayOf(
    arrayOf("R", "G", "B"), arrayOf("H", "S", "V"), arrayOf("L", "a", "b"), arrayOf("H", "C", "L"),
)

/** Per-tab native channel ranges (`famAxes().ranges` — note these are the *region/palette*
 *  ranges, fractional for RGB/HSV, not the 0-255 / 0-100% ranges [ChannelTable] displays). */
internal fun famRanges(tab: Int): List<ClosedFloatingPointRange<Float>> = when (tab) {
    0 -> listOf(0f..1f, 0f..1f, 0f..1f)
    1 -> listOf(0f..360f, 0f..1f, 0f..1f)
    2 -> listOf(0f..100f, -100f..100f, -110f..95f)
    else -> listOf(0f..360f, 0f..CMAX, 0f..100f)
}

/** The current tab's native coordinates for an arbitrary RGB candidate (`sliceVals()`), used
 *  only by the region/palette system — the interactive ring/slice stay HSV always (file KDoc). */
internal fun sliceValsFor(tab: Int, rgb: FloatArray, hueCache: Float): FloatArray = when (tab) {
    0 -> floatArrayOf(rgb[0], rgb[1], rgb[2])
    1 -> {
        val hsv = FloatArray(3)
        android.graphics.Color.RGBToHSV(
            (rgb[0] * 255f).roundToInt().coerceIn(0, 255),
            (rgb[1] * 255f).roundToInt().coerceIn(0, 255),
            (rgb[2] * 255f).roundToInt().coerceIn(0, 255),
            hsv,
        )
        val h = if (hsv[1] < 0.001f) hueCache else hsv[0]
        floatArrayOf(h, hsv[1], hsv[2])
    }
    2 -> {
        val lab = srgbToLab((rgb[0] * 255f).roundToInt().coerceIn(0, 255), (rgb[1] * 255f).roundToInt().coerceIn(0, 255), (rgb[2] * 255f).roundToInt().coerceIn(0, 255))
        floatArrayOf(lab.l.toFloat(), lab.a.toFloat(), lab.b.toFloat())
    }
    else -> {
        val lab = srgbToLab((rgb[0] * 255f).roundToInt().coerceIn(0, 255), (rgb[1] * 255f).roundToInt().coerceIn(0, 255), (rgb[2] * 255f).roundToInt().coerceIn(0, 255))
        val lch = labToLch(lab)
        val h = if (lch.c < 0.4) hueCache.toDouble() else lch.h
        floatArrayOf(h.toFloat(), lch.c.toFloat(), lch.l.toFloat())
    }
}

private fun inRegion(tab: Int, rgb: FloatArray, region: List<ClosedFloatingPointRange<Float>>, hueCache: Float): Boolean {
    val v = sliceValsFor(tab, rgb, hueCache)
    for (i in 0..2) if (v[i] < region[i].start - 1e-4f || v[i] > region[i].endInclusive + 1e-4f) return false
    return true
}

/** Deuteranopia (Machado 2009, severity 1.0) simulated in linear sRGB — verbatim the source's
 *  `simDeut`, used by the colour-blind-safe checkbox during palette sampling. */
private fun simDeut(rgb: FloatArray): FloatArray {
    val r = linearizeSrgbChannel(rgb[0].toDouble())
    val g = linearizeSrgbChannel(rgb[1].toDouble())
    val b = linearizeSrgbChannel(rgb[2].toDouble())
    fun clamp01(x: Double) = x.coerceIn(0.0, 1.0)
    return floatArrayOf(
        delinearizeSrgbChannel(clamp01(0.367322 * r + 0.860646 * g - 0.227968 * b)).toFloat(),
        delinearizeSrgbChannel(clamp01(0.280085 * r + 0.672501 * g + 0.047413 * b)).toFloat(),
        delinearizeSrgbChannel(clamp01(-0.011820 * r + 0.042940 * g + 0.968881 * b)).toFloat(),
    )
}

private fun metricLab(rgb: FloatArray, cvd: Boolean): Lab {
    val c = if (cvd) simDeut(rgb) else rgb
    return srgbToLab((c[0] * 255f).roundToInt().coerceIn(0, 255), (c[1] * 255f).roundToInt().coerceIn(0, 255), (c[2] * 255f).roundToInt().coerceIn(0, 255))
}

private fun labDist(a: Lab, b: Lab): Double {
    val dl = a.l - b.l; val da = a.a - b.a; val db = a.b - b.b
    return sqrt(dl * dl + da * da + db * db)
}

/** Farthest-point sampling within the current region — verbatim the source's `genPalette`: an
 *  11-step-per-channel RGB grid filtered to the region, then greedy farthest-point selection in
 *  (optionally CVD-simulated) CIE Lab distance, starting from a random candidate. */
internal fun genPalette(tab: Int, region: List<ClosedFloatingPointRange<Float>>, n: Int, cvd: Boolean, hueCache: Float): List<FloatArray> {
    val gridSteps = 11
    val candidates = mutableListOf<FloatArray>()
    for (i in 0 until gridSteps) for (j in 0 until gridSteps) for (k in 0 until gridSteps) {
        val rgb = floatArrayOf(i / (gridSteps - 1f), j / (gridSteps - 1f), k / (gridSteps - 1f))
        if (inRegion(tab, rgb, region, hueCache)) candidates.add(rgb)
    }
    if (candidates.isEmpty()) return emptyList()
    val labs = candidates.map { metricLab(it, cvd) }
    val minDist = DoubleArray(candidates.size) { Double.POSITIVE_INFINITY }
    val chosen = mutableListOf<Int>()
    fun take(idx: Int) {
        chosen.add(idx)
        val l = labs[idx]
        for (i in candidates.indices) {
            val d = labDist(labs[i], l)
            if (d < minDist[i]) minDist[i] = d
        }
    }
    take(Random.nextInt(candidates.size))
    while (chosen.size < min(n, candidates.size)) {
        var bestIdx = 0
        var bestDist = -1.0
        for (i in candidates.indices) if (minDist[i] > bestDist) { bestDist = minDist[i]; bestIdx = i }
        take(bestIdx)
    }
    return chosen.map { candidates[it] }
}

private fun toRgbFor(tab: Int, b: FloatArray): FloatArray = when (tab) {
    0 -> floatArrayOf(b[0], b[1], b[2])
    1 -> {
        val argb = android.graphics.Color.HSVToColor(floatArrayOf(b[0], b[1].coerceIn(0f, 1f), b[2].coerceIn(0f, 1f)))
        floatArrayOf(android.graphics.Color.red(argb) / 255f, android.graphics.Color.green(argb) / 255f, android.graphics.Color.blue(argb) / 255f)
    }
    2 -> lab2rgb(b[0].toDouble(), b[1].toDouble(), b[2].toDouble())
    else -> {
        val lab = lch2lab(b[0].toDouble(), b[1].toDouble(), b[2].toDouble())
        lab2rgb(lab.l, lab.a, lab.b)
    }
}

/** A dimmed preview colour for the region slider's track, holding the other two channels at the
 *  source's own heuristic defaults (`famColor`/`chanGradient`): S/V → 1, C → 0.8×max, L → 62. */
private fun famColor(tab: Int, ci: Int, value: Float): FloatArray {
    val ranges = famRanges(tab)
    val names = FAM_NAMES[tab]
    val b = FloatArray(3) { i -> (ranges[i].start + ranges[i].endInclusive) / 2f }
    for (i in 0..2) {
        if (i == ci) continue
        when (names[i]) {
            "S", "V" -> b[i] = 1f
            "C" -> b[i] = ranges[i].endInclusive * 0.8f
            "L" -> b[i] = 62f
        }
    }
    b[ci] = value
    val rgb = toRgbFor(tab, b)
    return floatArrayOf(rgb[0] * 0.66f, rgb[1] * 0.66f, rgb[2] * 0.66f)
}

private fun regionTrackBrush(tab: Int, ci: Int): Brush {
    val range = famRanges(tab)[ci]
    val stops = (0..8).map { s ->
        val t = s / 8f
        val rgb = famColor(tab, ci, range.start + t * (range.endInclusive - range.start))
        Color(rgb[0].coerceIn(0f, 1f), rgb[1].coerceIn(0f, 1f), rgb[2].coerceIn(0f, 1f))
    }
    return Brush.horizontalGradient(stops)
}

// ── the palette & region panel (task item 3) ─────────────────────────────────────────────────

/** The collapsible disclosure button — the source's `#paltoggle` ("🎲 Palette & region ▾"). */
@Composable
private fun PaletteDisclosure(open: Boolean, onToggle: () -> Unit) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val shape = RoundedCornerShape(11.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(shape)
            .background(c.raised, shape)
            .border(1.dp, c.hairline, shape)
            .clickable { haptics.tap(); onToggle() }
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text("Palette & region", style = MaterialTheme.typography.labelMedium, color = c.textHigh)
        Text(if (open) "  ▴" else "  ▾", style = MaterialTheme.typography.labelMedium, color = c.textMid)
    }
}

@Composable
private fun PalettePanel(
    tab: Int,
    region: List<ClosedFloatingPointRange<Float>>,
    onRegionChange: (Int, ClosedFloatingPointRange<Float>) -> Unit,
    onRegionReset: () -> Unit,
    cvd: Boolean,
    onCvdChange: (Boolean) -> Unit,
    count: Int,
    onCountChange: (Int) -> Unit,
    palette: List<FloatArray>,
    active: Int,
    onSelect: (Int) -> Unit,
    onRoll: () -> Unit,
    onAdd: () -> Unit,
    onReplace: () -> Unit,
    onDelete: () -> Unit,
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val panelShape = RoundedCornerShape(11.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .clip(panelShape)
            .background(c.raised, panelShape)
            .border(1.dp, c.hairline, panelShape)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        // Region — reset + one dual-handle slider per current-tab channel.
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Region", style = MaterialTheme.typography.labelSmall, color = c.textMid)
            Text(
                "reset",
                style = MaterialTheme.typography.labelSmall,
                color = c.violet,
                modifier = Modifier.clickable { haptics.tap(); onRegionReset() }.padding(2.dp),
            )
        }
        val names = FAM_NAMES[tab]
        val ranges = famRanges(tab)
        for (i in 0..2) {
            RegionRangeRow(
                name = names[i],
                bounds = ranges[i],
                value = region[i],
                angular = names[i] == "H",
                trackBrush = regionTrackBrush(tab, i),
                onChange = { onRegionChange(i, it) },
            )
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Palette", style = MaterialTheme.typography.labelSmall, color = c.textMid)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.clickable { haptics.tap(); onCvdChange(!cvd) },
            ) {
                Box(
                    Modifier.size(15.dp).clip(RoundedCornerShape(3.dp))
                        .background(if (cvd) c.violet else c.inset)
                        .border(1.dp, c.hairline, RoundedCornerShape(3.dp)),
                )
                Text("colorblind-safe", style = MaterialTheme.typography.labelSmall, color = c.textMid)
            }
        }

        CountSlider(name = "Colors", value = count, range = 2..12, onChange = onCountChange)

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            palette.forEachIndexed { i, rgb ->
                val swatchShape = RoundedCornerShape(8.dp)
                Box(
                    Modifier
                        .size(39.dp)
                        .clip(swatchShape)
                        .background(Color(rgb[0], rgb[1], rgb[2]), swatchShape)
                        .border(if (i == active) 2.dp else 1.dp, if (i == active) c.violet else c.hairline, swatchShape)
                        .clickable { haptics.tap(); onSelect(i) },
                )
            }
        }

        HyleSplitButton(text = "Roll", onClick = { haptics.tap(); onRoll() }, trailing = "🎲", onTrailingClick = { haptics.tap(); onRoll() }, modifier = Modifier.fillMaxWidth())
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            PanelButton("Add", Modifier.weight(1f)) { haptics.tap(); onAdd() }
            PanelButton("Replace", Modifier.weight(1f), enabled = active in palette.indices) { haptics.tap(); onReplace() }
            PanelButton("Del", Modifier.weight(1f), enabled = active in palette.indices) { haptics.tap(); onDelete() }
        }
    }
}

@Composable
private fun PanelButton(text: String, modifier: Modifier = Modifier, enabled: Boolean = true, onClick: () -> Unit) {
    val c = LocalHyleColors.current
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier
            .height(36.dp)
            .clip(shape)
            .background(c.inset, shape)
            .border(1.dp, c.hairline, shape)
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = if (enabled) c.textHigh else c.textDisabled)
    }
}

/** A dual-handle range slider — the source's `.rng-track` + two `.rng-h` thumbs. Each handle
 *  reads the live [value] via [rememberUpdatedState] so mid-drag recomposition (every channel
 *  edit re-renders this row) doesn't restart the drag gesture. */
@Composable
private fun RegionRangeRow(
    name: String,
    bounds: ClosedFloatingPointRange<Float>,
    value: ClosedFloatingPointRange<Float>,
    angular: Boolean,
    trackBrush: Brush,
    onChange: (ClosedFloatingPointRange<Float>) -> Unit,
) {
    val c = LocalHyleColors.current
    val span = (bounds.endInclusive - bounds.start).coerceAtLeast(1e-4f)
    val fmt: (Float) -> String = when {
        angular -> { v -> "${v.roundToInt()}°" }
        bounds.endInclusive <= 1f -> { v -> "%.2f".format(v) }
        else -> { v -> "${v.roundToInt()}" }
    }
    val valueState = rememberUpdatedState(value)
    val onChangeState = rememberUpdatedState(onChange)

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = c.textHigh)
            Text("${fmt(value.start)} – ${fmt(value.endInclusive)}", style = MaterialTheme.typography.labelSmall, color = c.textMid)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(trackBrush)
                .border(1.dp, c.hairline, RoundedCornerShape(6.dp)),
        ) {
            val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            fun xToVal(px: Float) = bounds.start + (px / wPx).coerceIn(0f, 1f) * span
            fun valToX(v: Float) = ((v - bounds.start) / span).coerceIn(0f, 1f) * wPx

            val loFrac = (valToX(value.start) / wPx)
            val hiFrac = (valToX(value.endInclusive) / wPx)
            Box(
                Modifier
                    .fillMaxHeight()
                    .offset { IntOffset((loFrac * wPx).roundToInt(), 0) }
                    .width(with(LocalDensity.current) { ((hiFrac - loFrac) * wPx).toDp() })
                    .background(c.violet.copy(alpha = 0.30f)),
            )
            RangeHandle(xPx = valToX(value.start)) { dx ->
                val cur = valueState.value
                val newStart = xToVal(valToX(cur.start) + dx).coerceAtMost(cur.endInclusive)
                onChangeState.value(newStart..cur.endInclusive)
            }
            RangeHandle(xPx = valToX(value.endInclusive)) { dx ->
                val cur = valueState.value
                val newEnd = xToVal(valToX(cur.endInclusive) + dx).coerceAtLeast(cur.start)
                onChangeState.value(cur.start..newEnd)
            }
        }
    }
}

@Composable
private fun RangeHandle(xPx: Float, onDragPx: (Float) -> Unit) {
    Box(
        Modifier
            .offset { IntOffset((xPx - 20.dp.toPx() / 2f).roundToInt(), -2) }
            .size(20.dp, 24.dp)
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount -> change.consume(); onDragPx(dragAmount.x) }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(13.dp, 25.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.45f), RoundedCornerShape(5.dp)),
        )
    }
}

/** Single-handle version of [RegionRangeRow] for the "Colors" (palette count, 2..12) slider —
 *  the source's `#palCountTrack`. */
@Composable
private fun CountSlider(name: String, value: Int, range: IntRange, onChange: (Int) -> Unit) {
    val c = LocalHyleColors.current
    val valueState = rememberUpdatedState(value)
    val onChangeState = rememberUpdatedState(onChange)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(name, style = MaterialTheme.typography.labelMedium, color = c.textHigh)
            Text("$value", style = MaterialTheme.typography.labelSmall, color = c.textMid)
        }
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(20.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(c.inset)
                .border(1.dp, c.hairline, RoundedCornerShape(6.dp)),
        ) {
            val wPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
            val span = (range.last - range.first).coerceAtLeast(1)
            fun valToX(v: Int) = ((v - range.first).toFloat() / span) * wPx
            Box(
                Modifier.fillMaxHeight().fillMaxWidth((valToX(value) / wPx).coerceIn(0f, 1f))
                    .background(c.violet.copy(alpha = 0.30f)),
            )
            Box(
                Modifier
                    .offset { IntOffset((valToX(value) - 10.dp.toPx()).roundToInt(), -2) }
                    .size(20.dp, 24.dp)
                    .pointerInput(Unit) {
                        detectDragGestures { change, dragAmount ->
                            change.consume()
                            val cur = valueState.value
                            val newPx = valToX(cur) + dragAmount.x
                            val newVal = (range.first + (newPx / wPx) * span).roundToInt().coerceIn(range.first, range.last)
                            if (newVal != cur) onChangeState.value(newVal)
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier.size(13.dp, 25.dp).clip(RoundedCornerShape(5.dp))
                        .background(Color.White).border(1.dp, Color.Black.copy(alpha = 0.45f), RoundedCornerShape(5.dp)),
                )
            }
        }
    }
}

// ── kept as-is (task item 4): the read-only per-tab channel table + hex readout/copy ────────

/** Read-only numeric channel table for the tab currently selected in [HyleColorPicker3D]. */
@Composable
private fun ChannelTable(tab: Int, argb: Int, hue: Float, sat: Float, value: Float) {
    val c = LocalHyleColors.current
    val r = android.graphics.Color.red(argb)
    val g = android.graphics.Color.green(argb)
    val b = android.graphics.Color.blue(argb)
    val rows: List<Pair<String, String>> = when (tab) {
        0 -> listOf("R" to r.toString(), "G" to g.toString(), "B" to b.toString())
        1 -> listOf(
            "H" to "%.0f°".format(hue),
            "S" to "%.0f%%".format(sat * 100f),
            "V" to "%.0f%%".format(value * 100f),
        )
        2 -> {
            val lab = srgbToLab(r, g, b)
            listOf(
                "L" to "%.1f".format(lab.l),
                "a" to "%.1f".format(lab.a),
                "b" to "%.1f".format(lab.b),
            )
        }
        else -> {
            val lch = labToLch(srgbToLab(r, g, b))
            listOf(
                "L" to "%.1f".format(lch.l),
                "C" to "%.1f".format(lch.c),
                "H" to "%.0f°".format(lch.h),
            )
        }
    }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        rows.forEach { (label, v) ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(label, style = MaterialTheme.typography.labelSmall, color = c.textMid)
                Text(v, style = MaterialTheme.typography.labelMedium, color = c.textHigh)
            }
        }
    }
}

/** Hex readout + copy-to-clipboard, reusing [toHexRgb] (no reimplemented formatting). Stays
 *  read-only-plus-copy per task item 4 — see file KDoc's scope-cuts section on the source's
 *  live-editable hex input, which this pass didn't add. */
@Composable
private fun HexRow(color: Color, haptics: HyleHaptics) {
    val c = LocalHyleColors.current
    val clipboard = LocalClipboardManager.current
    val hex = color.toHexRgb().uppercase()
    val fieldShape = RoundedCornerShape(8.dp)
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
                .clip(fieldShape)
                .background(c.raised, fieldShape)
                .border(1.dp, c.hairline, fieldShape)
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(hex, style = MaterialTheme.typography.labelLarge, color = c.textHigh)
        }
        Box(
            modifier = Modifier
                .height(40.dp)
                .clip(fieldShape)
                .background(c.violet, fieldShape)
                .clickable {
                    clipboard.setText(AnnotatedString(hex))
                    haptics.confirm()
                }
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text("Copy", style = MaterialTheme.typography.labelMedium, color = c.onViolet)
        }
    }
}
