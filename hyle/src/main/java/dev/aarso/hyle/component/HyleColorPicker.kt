package dev.aarso.hyle.component

import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.min
import kotlin.math.roundToInt

// ── Hyle dark palette — same locked values as the probe's AeonAtomsProbe.kt ────────────
private val Raised = Color(0xFF16181D)
private val Inset = Color(0xFF20242B)
private val Hairline = Color(0x24ECEDEF)
private val Violet = Color(0xFF8E7BFF)
private val TextHigh = Color(0xFFECEDEF)
private val TextMid = Color(0xFF9CA3AF)
private val TextDisabled = Color(0xFF4A4E57)
private val Warning = Color(0xFFE0941A) // tokens/HyleTokens.kt colorFeedbackWarning

// ── HyleTokens' "control" family — the hardware-dial vocabulary (groove/edge/rim/
// indicator/screen) already named in tokens/HyleTokens.kt, applied here to a real
// widget: an LCD-style numeric readout beside each slider. ─────────────────────────────
private val ControlGroove = Color(0xFF050506)
private val ControlIndicator = Color(0xFF6B6760)
private val ControlScreen = Color(0xFF141210)
private val ControlScreenInk = Color(0xFFDDDBD6)

enum class ColorPickerMode(val label: String) { RGB("RGB"), HSV("HSV"), HEX("HEX") }

/**
 * A colour picker in Hyle's own idiom: [HyleBottomTabRow] on the bottom edge (RGB / HSV /
 * HEX), the active tab merging flush into the panel it drives. Each pane holds the colour
 * *space itself as a shape* — the same colour, represented through the geometry of the
 * model chosen: the HSV pane is a hue/saturation wheel, the RGB pane a cube face, the HEX
 * pane plain notation (a hex triplet has no geometry — the caption says so rather than
 * inventing one).
 *
 * **Gamut honesty** (owner-set): a colour-space shape drawn edge-to-edge in colour would
 * claim this screen shows every colour the model describes — it does not. So the coloured
 * shape is deliberately *smaller* than the model's space, which is drawn around it as a
 * **dotted outline**; the empty band between them is the set of colours the pipeline
 * cannot reproduce, made visible instead of pretended away. The proportion is schematic
 * ([ColorSpaceGeometry.solidFraction]) — an honest "strict subset", not a colorimetric
 * per-hue boundary (that would need a CMS; parked). On top of that, when a system display
 * mode (Night Light, vendor vivid/adaptive) is shifting rendition, a warning row states
 * that what the eye sees is not the stored value. Display facts come from [display] —
 * pass [rememberHyleDisplayContext] for best-effort detection, or supply your own.
 *
 * Sliders reuse Material3's [Slider] for real drag/accessibility behaviour, restyled with
 * Hyle's groove/indicator tokens and paired with the "screen" readout pill
 * (`controlScreen`/`controlScreenInk`). Wheel/square gesture and render behaviour is
 * owner-verified on device — this container has no display of any gamut at all.
 */
@Composable
fun HyleColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
    display: HyleDisplayContext = HyleDisplayContext.Unknown,
) {
    var mode by remember { mutableStateOf(ColorPickerMode.HSV) }

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(Raised)
                .border(1.dp, Hairline, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                SwatchRow(color)
                DisplayCaveatRow(display)
                when (mode) {
                    ColorPickerMode.RGB -> RgbPanel(color, display, onColorChange)
                    ColorPickerMode.HSV -> HsvPanel(color, display, onColorChange)
                    ColorPickerMode.HEX -> HexPanel(color, onColorChange)
                }
            }
        }
        HyleBottomTabRow(
            tabs = ColorPickerMode.entries.map { it.label },
            selected = mode.ordinal,
            onSelect = { mode = ColorPickerMode.entries[it] },
            panelColor = Raised,
            inactiveColor = Inset,
            activeTextColor = TextHigh,
            inactiveTextColor = TextMid,
        )
    }
}

/**
 * Best-effort read of the display facts the picker's honesty features need. Wide-gamut
 * comes from the platform's own `Configuration.isScreenWideColorGamut`; the
 * rendition-altered flag probes two settings keys behind `runCatching` — AOSP's Night
 * Light (`night_display_activated`) and Samsung's screen mode (`screen_mode_setting`,
 * where 0 = adaptive/vivid) — because there is no public cross-vendor API for "is the
 * panel lying about colours right now". Vendors vary; a host with better knowledge of its
 * device should build its own [HyleDisplayContext] instead. Detection behaviour is
 * owner-verified only.
 */
@Composable
fun rememberHyleDisplayContext(): HyleDisplayContext {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    return remember(configuration) {
        val gamut = if (configuration.isScreenWideColorGamut) DisplayGamut.WIDE else DisplayGamut.SRGB
        val nightLight = runCatching {
            Settings.Secure.getInt(context.contentResolver, "night_display_activated", 0) == 1
        }.getOrDefault(false)
        val vividMode = runCatching {
            Settings.System.getInt(context.contentResolver, "screen_mode_setting", -1) == 0
        }.getOrDefault(false)
        HyleDisplayContext(
            gamut = gamut,
            renditionAltered = nightLight || vividMode,
            alteredReason = when {
                nightLight -> "Night Light is on"
                vividMode -> "the display is in a vivid/adaptive colour mode"
                else -> null
            },
        )
    }
}

@Composable
private fun SwatchRow(color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(color)
                .border(1.dp, Hairline, RoundedCornerShape(6.dp)),
        )
        Text(hexOf(color), color = TextHigh, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

/** The rendition warning: shown only while a display mode is actually shifting colours. */
@Composable
private fun DisplayCaveatRow(display: HyleDisplayContext) {
    if (!display.renditionAltered) return
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(Warning.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("⚠", color = Warning, fontSize = 12.sp)
        Text(
            "${display.alteredReason ?: "A display mode is active"} — colours on this screen are " +
                "shown shifted. The value you pick is stored exactly.",
            color = Warning,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
    }
}

/** The one-line legend for the dotted outline, phrased per which constraint binds. */
@Composable
private fun GamutLegend(display: HyleDisplayContext) {
    Text(
        when (display.gamut) {
            DisplayGamut.WIDE ->
                "Dotted edge: the model's space beyond the 8-bit sRGB this picker stores — " +
                    "your wide-gamut screen could show more than the picker can express."
            DisplayGamut.SRGB, DisplayGamut.UNKNOWN ->
                "Dotted edge: colours the model describes that this screen cannot show."
        },
        color = TextDisabled,
        fontSize = 11.sp,
        lineHeight = 15.sp,
    )
}

// ── HSV — the hue/saturation wheel, value on a slider ─────────────────────────────────

@Composable
private fun HsvPanel(color: Color, display: HyleDisplayContext, onColorChange: (Color) -> Unit) {
    val argb = color.toArgb()
    val incoming = remember(argb) { colorToHsv(color) }
    // Hue/sat live locally so dragging to the desaturated centre — where hue is
    // mathematically undefined — doesn't lose the chosen angle; re-sync from [color] only
    // when it carries a real hue (same convention as the core accent picker).
    var hue by remember { mutableFloatStateOf(incoming[0]) }
    var sat by remember { mutableFloatStateOf(incoming[1]) }
    LaunchedEffect(argb) {
        if (incoming[1] > 0.01f) hue = incoming[0]
        sat = incoming[1]
    }
    val value = incoming[2]
    // The gesture closures below outlive any single composition (pointerInput restarts
    // only on [fraction]) — route the moving parts through rememberUpdatedState so a drag
    // never emits from a stale value/alpha.
    val currentValue by rememberUpdatedState(value)
    val currentAlpha by rememberUpdatedState(color.alpha)
    val currentOnChange by rememberUpdatedState(onColorChange)
    fun emitHs(h: Float, s: Float) = currentOnChange(hsvToColor(h, s, currentValue, currentAlpha))

    val fraction = ColorSpaceGeometry.solidFraction(display.gamut)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().height(210.dp), contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(210.dp)
                    .pointerInput(fraction) {
                        detectTapGestures { p ->
                            val c = size.width / 2f
                            val solid = (c - 2.dp.toPx()) * fraction
                            val (h, s) = ColorSpaceGeometry.wheelHit(p.x - c, p.y - c, solid)
                            hue = h; sat = s; emitHs(h, s)
                        }
                    }
                    .pointerInput(fraction) {
                        detectDragGestures { change, _ ->
                            val c = size.width / 2f
                            val solid = (c - 2.dp.toPx()) * fraction
                            val (h, s) = ColorSpaceGeometry.wheelHit(change.position.x - c, change.position.y - c, solid)
                            hue = h; sat = s; emitHs(h, s)
                        }
                    },
            ) {
                val c = Offset(size.width / 2f, size.height / 2f)
                val full = min(size.width, size.height) / 2f - 2.dp.toPx()
                val solid = full * fraction

                // The coloured shape: the wheel at the CURRENT value, so the shape never
                // shows brighter colours than the slider position actually yields.
                val v = value.coerceIn(0f, 1f)
                val sweep = (0..6).map { Color.hsv((it * 60f) % 360f, 1f, v) }
                drawCircle(Brush.sweepGradient(sweep, center = c), radius = solid, center = c)
                drawCircle(
                    Brush.radialGradient(
                        listOf(Color.hsv(0f, 0f, v), Color.hsv(0f, 0f, v).copy(alpha = 0f)),
                        center = c,
                        radius = solid,
                    ),
                    radius = solid,
                    center = c,
                )
                // The ring around the colour-space shape — a 1dp hairline, nothing more.
                drawCircle(Hairline, radius = solid, center = c, style = Stroke(1.dp.toPx()))
                // The model's full space, dotted: what HSV describes beyond this pipeline.
                drawCircle(
                    TextDisabled,
                    radius = full,
                    center = c,
                    style = Stroke(
                        1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx())),
                    ),
                )
                // Thumb: thin white ring on the current colour, inside the solid disc only.
                val (ux, uy) = ColorSpaceGeometry.wheelPosition(hue, sat)
                val thumb = Offset(c.x + ux * solid, c.y + uy * solid)
                drawCircle(Color.hsv(hue, sat, v), radius = 7.dp.toPx(), center = thumb)
                drawCircle(Color.White, radius = 7.dp.toPx(), center = thumb, style = Stroke(1.5.dp.toPx()))
            }
        }
        HsvSlider("V", value * 100f, range = 0f..100f, unit = "%", trackColor = TextHigh) { v ->
            emit(hue, sat, v / 100f)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenReadout("${hue.roundToInt()}°", minWidth = 42.dp)
            ScreenReadout("${(sat * 100).roundToInt()}%", minWidth = 42.dp)
        }
        GamutLegend(display)
    }
}

@Composable
private fun HsvSlider(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    unit: String,
    trackColor: Color,
    onChange: (Float) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = TextMid, fontSize = 12.sp, modifier = Modifier.size(14.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = ControlIndicator,
                activeTrackColor = trackColor,
                inactiveTrackColor = ControlGroove,
            ),
        )
        ScreenReadout("${value.roundToInt()}$unit", minWidth = 42.dp)
    }
}

// ── RGB — a face of the cube (R × G at the current B), blue on a slider ───────────────

@Composable
private fun RgbPanel(color: Color, display: HyleDisplayContext, onColorChange: (Color) -> Unit) {
    val fraction = ColorSpaceGeometry.solidFraction(display.gamut)
    // Same staleness guard as the wheel: the pointerInput closures restart only on
    // [fraction], so they must read the live colour, not the one they closed over.
    val currentColor by rememberUpdatedState(color)
    val currentOnChange by rememberUpdatedState(onColorChange)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.fillMaxWidth().height(170.dp)) {
            Canvas(
                Modifier
                    .fillMaxWidth()
                    .height(170.dp)
                    .pointerInput(fraction) {
                        detectTapGestures { p -> rgSquareHit(p, size.width.toFloat(), size.height.toFloat(), fraction, currentColor, currentOnChange) }
                    }
                    .pointerInput(fraction) {
                        detectDragGestures { change, _ ->
                            rgSquareHit(change.position, size.width.toFloat(), size.height.toFloat(), fraction, currentColor, currentOnChange)
                        }
                    },
            ) {
                val fullW = size.width
                val fullH = size.height
                val solidW = fullW * fraction
                val solidH = fullH * fraction
                val left = (fullW - solidW) / 2f
                val top = (fullH - solidH) / 2f
                val b = color.blue

                // The cube face at the current blue: x → red, y (upward) → green. Built
                // additively — black base + red gradient, then green and the constant
                // blue plane summed on with BlendMode.Plus, so each channel stays exact.
                drawRect(
                    Brush.horizontalGradient(listOf(Color.Black, Color(1f, 0f, 0f)), startX = left, endX = left + solidW),
                    topLeft = Offset(left, top),
                    size = Size(solidW, solidH),
                )
                drawRect(
                    Brush.verticalGradient(listOf(Color(0f, 1f, 0f), Color.Black), startY = top, endY = top + solidH),
                    topLeft = Offset(left, top),
                    size = Size(solidW, solidH),
                    blendMode = BlendMode.Plus,
                )
                drawRect(
                    Color(0f, 0f, b),
                    topLeft = Offset(left, top),
                    size = Size(solidW, solidH),
                    blendMode = BlendMode.Plus,
                )
                // Thin hairline around the shape; the fuller space (wider gamuts the
                // stored 8-bit sRGB cube cannot address) dotted around it.
                drawRect(Hairline, topLeft = Offset(left, top), size = Size(solidW, solidH), style = Stroke(1.dp.toPx()))
                drawRoundRect(
                    TextDisabled,
                    topLeft = Offset.Zero,
                    size = Size(fullW, fullH),
                    cornerRadius = CornerRadius(6.dp.toPx()),
                    style = Stroke(
                        1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx())),
                    ),
                )
                val thumb = Offset(left + color.red * solidW, top + (1f - color.green) * solidH)
                drawCircle(color.copy(alpha = 1f), radius = 7.dp.toPx(), center = thumb)
                drawCircle(Color.White, radius = 7.dp.toPx(), center = thumb, style = Stroke(1.5.dp.toPx()))
            }
        }
        ChannelSlider("B", color.blue, Color(0xFF4B8FE5)) { onColorChange(color.copy(blue = it)) }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            ScreenReadout((color.red * 255).roundToInt().toString(), minWidth = 34.dp)
            ScreenReadout((color.green * 255).roundToInt().toString(), minWidth = 34.dp)
            ScreenReadout((color.blue * 255).roundToInt().toString(), minWidth = 34.dp)
        }
        GamutLegend(display)
    }
}

/** Shared tap/drag → (R, G) mapping for the cube face; clamps to the solid square. */
private fun rgSquareHit(
    p: Offset,
    fullW: Float,
    fullH: Float,
    fraction: Float,
    color: Color,
    onColorChange: (Color) -> Unit,
) {
    val solidW = fullW * fraction
    val solidH = fullH * fraction
    val left = (fullW - solidW) / 2f
    val top = (fullH - solidH) / 2f
    val r = ((p.x - left) / solidW).coerceIn(0f, 1f)
    val g = (1f - (p.y - top) / solidH).coerceIn(0f, 1f)
    onColorChange(color.copy(red = r, green = g))
}

/** One labelled 0–255 channel: slider + a live "screen" readout of the 0–255 value. */
@Composable
private fun ChannelSlider(label: String, value: Float, trackColor: Color, onChange: (Float) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, color = TextMid, fontSize = 12.sp, modifier = Modifier.size(14.dp))
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = 0f..1f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = ControlIndicator,
                activeTrackColor = trackColor,
                inactiveTrackColor = ControlGroove,
            ),
        )
        ScreenReadout((value * 255).roundToInt().toString(), minWidth = 34.dp)
    }
}

// ── Hex ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun HexPanel(color: Color, onColorChange: (Color) -> Unit) {
    var text by remember(color) { mutableStateOf(hexOf(color).removePrefix("#")) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Notation, not geometry — a hex triplet is the same 8-bit sRGB cube, written out.",
            color = TextDisabled,
            fontSize = 11.sp,
            lineHeight = 15.sp,
        )
        Text("6-digit hex, no leading #", color = TextDisabled, fontSize = 11.sp)
        TextField(
            value = text,
            onValueChange = { input ->
                val cleaned = input.filter { it.isDigit() || it in 'a'..'f' || it in 'A'..'F' }.take(6)
                text = cleaned
                if (cleaned.length == 6) {
                    runCatching { android.graphics.Color.parseColor("#$cleaned") }
                        .onSuccess { onColorChange(Color(it).copy(alpha = color.alpha)) }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            textStyle = LocalTextStyle.current.merge(TextStyle(fontSize = 15.sp, color = TextHigh)),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Inset,
                unfocusedContainerColor = Inset,
                focusedIndicatorColor = Violet,
                unfocusedIndicatorColor = Hairline,
                focusedTextColor = TextHigh,
                unfocusedTextColor = TextHigh,
            ),
        )
    }
}

// ── Shared "screen" readout (controlScreen / controlScreenInk) ─────────────────────────

@Composable
private fun ScreenReadout(text: String, minWidth: androidx.compose.ui.unit.Dp) {
    Box(
        Modifier
            .height(22.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(ControlScreen)
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            color = ControlScreenInk,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.size(width = minWidth, height = 14.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}

// ── Colour-space helpers ────────────────────────────────────────────────────────────

private fun hexOf(color: Color): String {
    val argb = color.toArgb()
    return "#%02X%02X%02X".format((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)
}

/** [h, s, v]: h in 0..360, s/v in 0..1 — wraps the platform's own HSV conversion rather
 *  than hand-rolling the algorithm. */
private fun colorToHsv(color: Color): FloatArray {
    val out = FloatArray(3)
    android.graphics.Color.colorToHSV(color.toArgb(), out)
    return out
}

private fun hsvToColor(h: Float, s: Float, v: Float, alpha: Float): Color =
    Color(
        android.graphics.Color.HSVToColor(
            floatArrayOf(h.coerceIn(0f, 360f), s.coerceIn(0f, 1f), v.coerceIn(0f, 1f)),
        ),
    ).copy(alpha = alpha)
