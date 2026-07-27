package dev.aarso.hyle.cells

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
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
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * The real colour picker — a hue ring, an HSV/RGB/Lab/HCL slice, a hex field, and a live
 * wireframe model of RGB space — **fully native Compose**, no WebView.
 *
 * This replaces an earlier WebView build that loaded a bundled THREE.js document
 * (`kit/color-picker.html`): on-device it rendered the 3D sub-view as a garbled,
 * mis-sized, semi-transparent artefact overlapping the hue ring — a classic
 * WebGL-inside-WebView failure (no hardware acceleration / lost GL context /
 * canvas sizing). Rather than chase that bug, every surface here is `drawScope`
 * Canvas + gesture detectors: the ring/square adapt [HyleColorPicker]'s linear
 * HSV drag maths to angular ring geometry, and the "3D model" is an honest
 * wireframe cube (ordinary rotation matrix + weak-perspective projection, no
 * OpenGL) showing where the current pick sits in RGB space.
 *
 * @param color the current colour (source of truth).
 * @param onColorChange called with the new opaque colour on each drag/tap.
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

    // Re-sync from an externally-set colour (e.g. a preset swatch tap), same rule as the flat
    // picker: preserve the local hue when the incoming colour is greyscale (undefined hue).
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

    var tab by remember { mutableIntStateOf(0) }
    var show3D by remember { mutableStateOf(false) }
    val currentArgb = remember(hue, sat, value) {
        android.graphics.Color.HSVToColor(floatArrayOf(hue.coerceIn(0f, 360f), sat.coerceIn(0f, 1f), value.coerceIn(0f, 1f)))
    }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        HueRingAndSvField(
            hue = hue,
            sat = sat,
            value = value,
            onHueChange = { hue = it; emit() },
            onSatValueChange = { s, v -> sat = s; value = v; emit() },
            haptics = haptics,
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            HyleSegmentedToggle(
                options = listOf("RGB", "HSV", "Lab", "HCL"),
                selected = tab,
                onSelect = { tab = it },
                modifier = Modifier.weight(1f),
            )
            SpaceToggle(active = show3D, onClick = { show3D = !show3D })
        }

        ChannelTable(tab = tab, argb = currentArgb, hue = hue, sat = sat, value = value)

        if (show3D) {
            Rgb3DView(argb = currentArgb)
        }

        HexRow(color = Color(currentArgb), haptics = haptics)
    }
}

/**
 * Hue ring + saturation/value square, the native replacement for the kit's circular picker.
 * Hue comes from the drag point's angle around the ring's centre (`atan2`, 0° at 3 o'clock,
 * clockwise — matching [Brush.sweepGradient]'s own convention); sat/value reuse
 * [HyleColorPicker]'s linear field maths verbatim, just on a square instead of a full-width
 * rectangle. The square sits centred over the ring's canvas and is drawn after it, so it
 * naturally captures its own touches (Compose hit-tests top-most first).
 */
@Composable
private fun HueRingAndSvField(
    hue: Float,
    sat: Float,
    value: Float,
    onHueChange: (Float) -> Unit,
    onSatValueChange: (Float, Float) -> Unit,
    haptics: HyleHaptics,
    modifier: Modifier = Modifier,
) {
    val hueColors = remember {
        (0..360 step 60).map { Color(android.graphics.Color.HSVToColor(floatArrayOf(it.toFloat(), 1f, 1f))) }
    }
    val pureHue = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
    val ringSizePx = remember { mutableFloatStateOf(1f) }
    val strokeFrac = 0.16f

    fun setHueFromOffset(pos: Offset) {
        val cx = ringSizePx.floatValue / 2f
        val dx = pos.x - cx
        val dy = pos.y - cx
        var deg = atan2(dy, dx) * (180f / PI.toFloat())
        if (deg < 0f) deg += 360f
        onHueChange(deg)
    }

    fun isInRingBand(pos: Offset): Boolean {
        val cx = ringSizePx.floatValue / 2f
        val dx = pos.x - cx
        val dy = pos.y - cx
        val r = sqrt(dx * dx + dy * dy)
        val innerR = cx - ringSizePx.floatValue * strokeFrac
        return r >= innerR
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .pointerInput(Unit) {
                detectTapGestures { pos ->
                    if (isInRingBand(pos)) { setHueFromOffset(pos); haptics.settle() }
                }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = { haptics.settle() },
                    onDrag = { change, _ -> if (isInRingBand(change.position)) setHueFromOffset(change.position) },
                )
            }
            .semantics { contentDescription = "Hue ring" },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.fillMaxSize()) {
            ringSizePx.floatValue = size.width
            val cx = size.width / 2f
            val cy = size.height / 2f
            val strokeW = size.width * strokeFrac
            val outerR = cx - strokeW / 2f
            drawCircle(
                brush = Brush.sweepGradient(hueColors),
                radius = outerR,
                center = Offset(cx, cy),
                style = Stroke(width = strokeW),
            )
            val rad = hue * (PI.toFloat() / 180f)
            val tx = cx + outerR * cos(rad)
            val ty = cy + outerR * sin(rad)
            drawCircle(Color.Black.copy(alpha = 0.35f), strokeW * 0.34f, Offset(tx, ty + 2f))
            drawCircle(pureHue, strokeW * 0.34f, Offset(tx, ty))
            drawCircle(Color.White, strokeW * 0.34f, Offset(tx, ty), style = Stroke(width = 2.dp.toPx()))
        }

        BoxWithConstraints(
            Modifier
                .fillMaxSize(0.46f)
                .clip(RoundedCornerShape(6.dp))
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
                    .pointerInput(Unit) { detectTapGestures { setFromOffset(it.x, it.y); haptics.settle() } }
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragEnd = { haptics.settle() },
                            onDrag = { change, _ -> setFromOffset(change.position.x, change.position.y) },
                        )
                    },
            )
            val thumbX = (maxWidth - 14.dp) * sat.coerceIn(0f, 1f)
            val thumbY = (maxHeight - 14.dp) * (1f - value).coerceIn(0f, 1f)
            Box(
                Modifier
                    .offset { IntOffset(thumbX.roundToPx(), thumbY.roundToPx()) }
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value))), CircleShape)
                    .border(2.dp, Color.White, CircleShape),
            )
        }
    }
}

/** Small chip that toggles the supplementary 3D map — opt-in, matching how the broken kit's own
 *  small icon used to reveal its (broken) 3D sub-view, minus the bug. */
@Composable
private fun SpaceToggle(active: Boolean, onClick: () -> Unit) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val shape = RoundedCornerShape(8.dp)
    Box(
        modifier = Modifier
            .height(36.dp)
            .clip(shape)
            .background(if (active) c.violet else c.raised, shape)
            .border(1.dp, if (active) c.violet else c.hairline, shape)
            .clickable { haptics.tap(); onClick() }
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            "3D",
            style = MaterialTheme.typography.labelMedium,
            color = if (active) c.onViolet else c.textMid,
        )
    }
}

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

/** Hex readout + copy-to-clipboard, reusing [toHexRgb] (no reimplemented formatting). */
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

// ── The supplementary "where am I in the space" view ────────────────────────────────────────

private data class Vec3(val x: Float, val y: Float, val z: Float)

/** The 8 corners of the RGB cube, indexed so bit 0/1/2 of the index select the x/y/z sign. */
private val CUBE_CORNERS: List<Vec3> = (0..7).map { i ->
    Vec3(
        x = if (i and 1 != 0) 1f else -1f,
        y = if (i and 2 != 0) 1f else -1f,
        z = if (i and 4 != 0) 1f else -1f,
    )
}

/** The 12 edges: every pair of corners whose indices differ in exactly one bit. */
private val CUBE_EDGES: List<Pair<Int, Int>> = buildList {
    for (i in 0..7) {
        for (j in i + 1..7) {
            if ((i xor j).countOneBits() == 1) add(i to j)
        }
    }
}

private fun rotateY(v: Vec3, theta: Float): Vec3 {
    val ct = cos(theta)
    val st = sin(theta)
    return Vec3(v.x * ct + v.z * st, v.y, -v.x * st + v.z * ct)
}

private fun tiltX(v: Vec3, phi: Float): Vec3 {
    val cp = cos(phi)
    val sp = sin(phi)
    return Vec3(v.x, v.y * cp - v.z * sp, v.y * sp + v.z * cp)
}

/** Weak-perspective projection: rotate, then divide x/y by a depth-scaled factor. Ordinary
 *  3D-to-2D maths, no rendering library — this IS the whole "native 3D" implementation. */
private fun project(v: Vec3, focal: Float = 4f): Offset {
    val scale = focal / (focal + v.z)
    return Offset(v.x * scale, v.y * scale)
}

/**
 * A native, honest replacement for the broken WebGL "3D model" sub-view: an RGB cube drawn as a
 * wireframe (8 corners + 12 edges, cheap to redraw every frame), rotated by a horizontal drag —
 * that drag IS the "rotate and inspect" interaction the old kit offered, minus the crash. The
 * current colour is plotted as its own highlighted point so the view stays informative rather
 * than decorative.
 */
@Composable
private fun Rgb3DView(argb: Int, modifier: Modifier = Modifier) {
    val c = LocalHyleColors.current
    var rotation by remember { mutableFloatStateOf(0.7f) }
    val tilt = 0.4f
    val markerColor = Color(argb)
    val marker = remember(argb) {
        Vec3(
            x = (android.graphics.Color.red(argb) / 255f) * 2f - 1f,
            y = (android.graphics.Color.green(argb) / 255f) * 2f - 1f,
            z = (android.graphics.Color.blue(argb) / 255f) * 2f - 1f,
        )
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(180.dp)
            .pointerInput(Unit) {
                detectDragGestures { _, dragAmount -> rotation += dragAmount.x * 0.012f }
            }
            .semantics { contentDescription = "RGB cube, drag to rotate" },
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val scale = min(size.width, size.height) / 2f * 0.62f

        fun toScreen(v: Vec3): Offset {
            val rotated = tiltX(rotateY(v, rotation), tilt)
            val p = project(rotated)
            return Offset(cx + p.x * scale, cy - p.y * scale)
        }

        CUBE_EDGES.forEach { (i, j) ->
            drawLine(
                color = c.hairline,
                start = toScreen(CUBE_CORNERS[i]),
                end = toScreen(CUBE_CORNERS[j]),
                strokeWidth = 1.5.dp.toPx(),
            )
        }
        CUBE_CORNERS.forEach { corner ->
            val cornerColor = Color(
                android.graphics.Color.rgb(
                    (((corner.x + 1f) / 2f) * 255f).roundToInt().coerceIn(0, 255),
                    (((corner.y + 1f) / 2f) * 255f).roundToInt().coerceIn(0, 255),
                    (((corner.z + 1f) / 2f) * 255f).roundToInt().coerceIn(0, 255),
                ),
            )
            drawCircle(cornerColor, radius = 4.dp.toPx(), center = toScreen(corner))
        }
        val mp = toScreen(marker)
        drawCircle(Color.Black.copy(alpha = 0.4f), radius = 8.dp.toPx(), center = mp + Offset(0f, 2f))
        drawCircle(markerColor, radius = 8.dp.toPx(), center = mp)
        drawCircle(c.violet, radius = 8.dp.toPx(), center = mp, style = Stroke(width = 2.dp.toPx()))
    }
}

// ── sRGB → CIE Lab → LCH (HCL), pure functions so they're easy to unit-test later ───────────

/** CIE L*a*b* (D65 white point). */
data class Lab(val l: Double, val a: Double, val b: Double)

/** Cylindrical form of [Lab] — lightness/chroma/hue, i.e. LCH (the kit's "HCL" tab). */
data class Lch(val l: Double, val c: Double, val h: Double)

private fun linearizeSrgbChannel(c: Double): Double =
    if (c <= 0.04045) c / 12.92 else ((c + 0.055) / 1.055).pow(2.4)

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
