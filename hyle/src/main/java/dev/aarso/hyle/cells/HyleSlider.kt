package dev.aarso.hyle.cells

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * Continuous value, cells register.
 *
 * Ported from the tactile kit's **hairline** slider (`kit/tactile-kit.html`,
 * `.hsh`) rather than designed fresh — the kit already carries three horizontal
 * variants and this is the calm one, so it belongs in a settings row without
 * dragging the hardware idiom in with it. The heavier `.hsf` and the dimpled
 * `.hsd` stay in the kit for surfaces that *should* read as hardware.
 *
 * From the kit: a 3-unit rail with an inset groove shadow, an accent fill with a
 * soft glow, and a round thumb that catches a rim light along its top edge.
 */
@Composable
fun HyleSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    enabled: Boolean = true,
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    val width = remember { mutableFloatStateOf(1f) }
    val onChange by rememberUpdatedState(onValueChange)
    val thumbR = 13.dp

    fun emit(x: Float) {
        val f = (x / width.floatValue).coerceIn(0f, 1f)
        onChange(valueRange.start + f * span)
    }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(value, valueRange)
            }
            .then(
                if (enabled) {
                    Modifier
                        .pointerInput(Unit) {
                            detectTapGestures { haptics.tap(); emit(it.x) }
                        }
                        .pointerInput(Unit) {
                            detectHorizontalDragGestures { change, _ -> emit(change.position.x) }
                        }
                } else {
                    Modifier
                },
            ),
    ) {
        width.floatValue = size.width
        val cy = size.height / 2f
        val rail = 3.dp.toPx()
        val r = thumbR.toPx()
        val x = (fraction * size.width).coerceIn(r, size.width - r)

        val railColor = if (enabled) c.inset else c.inset.copy(alpha = 0.6f)
        val fill = if (enabled) c.violet else c.textDisabled

        // Rail — a recessed groove, so the track reads as carved rather than drawn.
        drawRoundRect(
            color = railColor,
            topLeft = Offset(0f, cy - rail / 2f),
            size = Size(size.width, rail),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(rail / 2f),
        )
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.35f),
            topLeft = Offset(0f, cy - rail / 2f),
            size = Size(size.width, rail / 2f),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(rail / 2f),
        )
        // Fill, with the kit's soft accent glow.
        if (fraction > 0f) {
            drawRoundRect(
                color = fill.copy(alpha = 0.35f),
                topLeft = Offset(0f, cy - rail * 1.6f),
                size = Size(x, rail * 3.2f),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(rail * 1.6f),
            )
            drawRoundRect(
                color = fill,
                topLeft = Offset(0f, cy - rail / 2f),
                size = Size(x, rail),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(rail / 2f),
            )
        }
        // Thumb — cast shadow, body, then a rim light on the upper edge only.
        drawCircle(Color.Black.copy(alpha = 0.45f), r, Offset(x, cy + 2.dp.toPx()))
        drawCircle(if (enabled) c.raised else c.inset, r, Offset(x, cy))
        drawCircle(
            color = Color.White.copy(alpha = if (enabled) 0.22f else 0.08f),
            radius = r,
            center = Offset(x, cy - 0.5.dp.toPx()),
            style = Stroke(width = 1.2.dp.toPx()),
        )
    }
}
