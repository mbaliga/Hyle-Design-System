package dev.aarso.hyle.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ── Hyle dark palette — same locked values as the probe's AeonAtomsProbe.kt ────────────
private val Raised = Color(0xFF16181D)
private val Inset = Color(0xFF20242B)
private val Hairline = Color(0x24ECEDEF)
private val Violet = Color(0xFF8E7BFF)
private val TextHigh = Color(0xFFECEDEF)
private val TextMid = Color(0xFF9CA3AF)
private val TextDisabled = Color(0xFF4A4E57)

// ── HyleTokens' "control" family — the hardware-dial vocabulary (groove/edge/rim/
// indicator/screen) already named in tokens/HyleTokens.kt, applied here for the first
// time to a real widget: an LCD-style numeric readout beside each slider. ──────────────
private val ControlGroove = Color(0xFF050506)
private val ControlIndicator = Color(0xFF6B6760)
private val ControlScreen = Color(0xFF141210)
private val ControlScreenInk = Color(0xFFDDDBD6)

enum class ColorPickerMode(val label: String) { RGB("RGB"), HSV("HSV"), HEX("HEX") }

/**
 * A colour picker in Hyle's own idiom: [HyleBottomTabRow] on the bottom edge (RGB / HSV /
 * HEX), the active tab merging flush into the panel it drives — not a copy of any
 * reference app's colour, just its slanted-tab SHAPE, per the owner's instruction that the
 * reference screenshot was for tab geometry only.
 *
 * Sliders reuse Material3's [Slider] for real drag/accessibility behaviour (untested on a
 * hand-rolled gesture detector with no device in this sandbox to verify touch behaviour
 * on), restyled with Hyle's groove/indicator tokens, paired with a small "screen" readout
 * pill — the LCD-style numeric display the token names (`controlScreen`/`controlScreenInk`)
 * were clearly meant for but had never been drawn anywhere yet.
 */
@Composable
fun HyleColorPicker(
    color: Color,
    onColorChange: (Color) -> Unit,
    modifier: Modifier = Modifier,
) {
    var mode by remember { mutableStateOf(ColorPickerMode.RGB) }

    Column(modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .background(Raised)
                .border(1.dp, Hairline, RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                .padding(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                SwatchRow(color)
                when (mode) {
                    ColorPickerMode.RGB -> RgbPanel(color, onColorChange)
                    ColorPickerMode.HSV -> HsvPanel(color, onColorChange)
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

// ── RGB ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun RgbPanel(color: Color, onColorChange: (Color) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChannelSlider("R", color.red, Color(0xFFE5564B)) { onColorChange(color.copy(red = it)) }
        ChannelSlider("G", color.green, Color(0xFF5BBF7A)) { onColorChange(color.copy(green = it)) }
        ChannelSlider("B", color.blue, Color(0xFF4B8FE5)) { onColorChange(color.copy(blue = it)) }
    }
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

// ── HSV ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun HsvPanel(color: Color, onColorChange: (Color) -> Unit) {
    val hsv = remember(color) { colorToHsv(color) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HsvSlider("H", hsv[0], range = 0f..360f, unit = "°", trackColor = Violet) { h ->
            onColorChange(hsvToColor(h, hsv[1], hsv[2], color.alpha))
        }
        HsvSlider("S", hsv[1] * 100f, range = 0f..100f, unit = "%", trackColor = TextMid) { s ->
            onColorChange(hsvToColor(hsv[0], s / 100f, hsv[2], color.alpha))
        }
        HsvSlider("V", hsv[2] * 100f, range = 0f..100f, unit = "%", trackColor = TextHigh) { v ->
            onColorChange(hsvToColor(hsv[0], hsv[1], v / 100f, color.alpha))
        }
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

// ── Hex ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun HexPanel(color: Color, onColorChange: (Color) -> Unit) {
    var text by remember(color) { mutableStateOf(hexOf(color).removePrefix("#")) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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
    Color(android.graphics.Color.HSVToColor(floatArrayOf(h, s, v))).copy(alpha = alpha)
