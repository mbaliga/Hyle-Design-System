package dev.aarso.hyle.cells

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.LocalHyleColors
import kotlin.math.cos
import kotlin.math.sin

/** One wedge of a [HyleRadialMenu]. */
class HyleRadialMenuItem(
    val label: String,
    val destructive: Boolean = false,
    val glyph: DrawScope.(tint: Color) -> Unit,
    val onClick: () -> Unit,
)

/**
 * A tap-and-fan context menu, anchored at the point that was long-pressed rather than a dialog
 * that appears wherever the platform feels like centring it — the physical gesture that opened it
 * stays legible as *where these options came from*. No scrim (`docs/LENS.md`'s rule holds here
 * too: a plain outside-tap dismisses, nothing dims); each item is its own small filled circle at a
 * fixed radius, fanned across the upper arc above [anchor] so the options land above the finger
 * that triggered them rather than under it.
 *
 * Built for small option counts (2–5) — a canvas's "add a node" or "what do you want to do with
 * this node" palettes, not a long scrolling list; [HyleTabBar]/[HyleDropdownField] already cover
 * that shape.
 *
 * @param anchor the long-press point, in the SAME coordinate space as this composable's parent
 *   (e.g. the canvas-local pixel offset already tracked for the gesture that triggered this menu).
 * @param modifier give this `Modifier.fillMaxSize()` (or match the surface [anchor] is measured
 *   against) — the outside-tap-to-dismiss area and the connector threads are drawn across
 *   whatever bounds this resolves to, not implicitly the whole screen.
 */
@Composable
fun HyleRadialMenu(
    visible: Boolean,
    anchor: Offset,
    items: List<HyleRadialMenuItem>,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!visible) return
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val density = LocalDensity.current

    val radiusPx = with(density) { RADIUS.toPx() }
    val n = items.size.coerceAtLeast(1)
    // Fan across a 140° arc centred on "straight up" (270° in screen coords, where 0° is right
    // and angle grows clockwise/downward) — options land above the finger, never under it, and
    // never crowd the screen edges the way a full circle would for n<=5.
    val spread = 140f
    val start = 270f - spread / 2f
    val positions = (0 until n).map { i ->
        val angleDeg = if (n == 1) 270f else start + spread * i / (n - 1)
        val angleRad = Math.toRadians(angleDeg.toDouble())
        Offset(anchor.x + (radiusPx * cos(angleRad)).toFloat(), anchor.y + (radiusPx * sin(angleRad)).toFloat())
    }

    Box(modifier.pointerInput(Unit) { detectTapGestures { onDismiss() } }) {
        // One thread per item, anchor to its circle — "you can see where this came from."
        Canvas(Modifier.fillMaxSize()) {
            positions.forEach { p -> drawLine(c.hairline, anchor, p, strokeWidth = 2.dp.toPx()) }
        }
        items.forEachIndexed { i, item ->
            val p = positions[i]
            val (offX, offY) = with(density) { (p.x.toDp() - ITEM_SIZE / 2) to (p.y.toDp() - ITEM_SIZE / 2) }
            Box(
                modifier = Modifier
                    .offset(offX, offY)
                    .size(ITEM_SIZE)
                    .clip(CircleShape)
                    .background(if (item.destructive) c.error else c.raised, CircleShape)
                    .clickable {
                        haptics.tap()
                        item.onClick()
                        onDismiss()
                    },
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.size(GLYPH_SIZE)) {
                    item.glyph(this, if (item.destructive) c.onViolet else c.textHigh)
                }
            }
            Text(
                item.label,
                style = MaterialTheme.typography.labelSmall,
                color = if (item.destructive) c.error else c.textMid,
                modifier = Modifier.offset(offX - LABEL_PAD, offY + ITEM_SIZE),
            )
        }
    }
}

private val RADIUS = 76.dp
private val ITEM_SIZE = 52.dp
private val GLYPH_SIZE = 22.dp
private val LABEL_PAD = 14.dp
