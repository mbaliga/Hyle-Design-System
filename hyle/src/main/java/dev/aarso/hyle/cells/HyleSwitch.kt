package dev.aarso.hyle.cells

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * Binary on/off, travelling **vertically**: thumb at the top is on, at the bottom
 * is off. The owner's reference is the mute switch on an old iPhone — a slab
 * sitting in a recessed square slot — which is why the well is square rather than
 * a pill track, and why the thumb spans the full width instead of being a knob.
 *
 * State is legible from the thumb's *position* alone, so it survives without
 * colour; the green/grey pair reinforces it rather than carrying it.
 *
 * Deliberately NOT the same object as [HyleWellToggle]:
 *  - A switch is a **state** (on/off), so it carries semantic green/grey.
 *    The well toggle is a **selection** between two peers, so it carries the accent.
 *  - The recess here is a **black** inner shadow. The well toggle's is violet-tinted.
 *    That contrast between the two controls is owner-set, not an oversight — don't
 *    "unify" them.
 */

/** Authored geometry (square well). Proportions hold at any rendered size. */
private const val SIDE = 150f
private const val RADIUS = 30f
private const val MARGIN = 4f
private const val THUMB_H = 56f
private const val THUMB_R = 28f
private val ON_Y = MARGIN
private val OFF_Y = SIDE - MARGIN - THUMB_H

/** Default rendered size; the control is square. */
private val SWITCH_SIZE = 40.dp

@Composable
fun HyleSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val interaction = remember { MutableInteractionSource() }
    // Slight overshoot: the slab should feel like it lands, not glide.
    val t by animateFloatAsState(
        targetValue = if (checked) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "hyleSwitchThumb",
    )
    val wellOn = c.success
    val wellOff = Color(0xFF707070) // neutral grey, from the export; reads on light and dark
    Canvas(
        modifier
            .size(SWITCH_SIZE)
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        interactionSource = interaction,
                        indication = null,
                        onValueChange = { haptics.tap(); onCheckedChange(it) },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        drawSwitch(
            progress = t,
            well = androidx.compose.ui.graphics.lerp(wellOff, wellOn, t)
                .let { if (enabled) it else it.copy(alpha = 0.4f) },
            thumb = if (enabled) Color.White else Color.White.copy(alpha = 0.55f),
        )
    }
}

/** [progress] 0 = off (thumb at the bottom), 1 = on (thumb at the top). */
private fun DrawScope.drawSwitch(progress: Float, well: Color, thumb: Color) {
    val s = size.minDimension / SIDE
    val r = RADIUS * s
    val wellPath = Path().apply {
        addRoundRect(
            androidx.compose.ui.geometry.RoundRect(
                left = 0f, top = 0f, right = SIDE * s, bottom = SIDE * s,
                radiusX = r, radiusY = r,
            ),
        )
    }
    drawPath(wellPath, well)
    // Recess: black inner shadow, offset down-right, clipped to the well so only the
    // inside edge catches it.
    clipPath(wellPath) {
        translate(4f * s, 4f * s) {
            drawPath(
                wellPath,
                color = Color.Black.copy(alpha = 0.25f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 10f * s),
            )
        }
    }
    // Thumb travels between the two authored positions.
    val y = (OFF_Y + (ON_Y - OFF_Y) * progress) * s
    val thumbSize = Size(width = (SIDE - 2f * MARGIN) * s, height = THUMB_H * s)
    val thumbCorner = CornerRadius(THUMB_R * s, THUMB_R * s)
    // Cast shadow first, so the slab reads as sitting proud of the slot.
    drawRoundRect(
        color = Color.Black.copy(alpha = 0.25f),
        topLeft = Offset(MARGIN * s + 2f * s, y + 2f * s),
        size = thumbSize,
        cornerRadius = thumbCorner,
    )
    drawRoundRect(
        color = thumb,
        topLeft = Offset(MARGIN * s, y),
        size = thumbSize,
        cornerRadius = thumbCorner,
    )
}
