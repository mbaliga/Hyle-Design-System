package dev.aarso.hyle.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.cells.rememberHyleHaptics
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * HyleToggle — desktop-class kit §1B (docs/design/desktop-class-kit.md). A **rounded-square**
 * track, not a pill, with a **slanted-edged square** thumb: the same parallelogram language as
 * [HyleField]'s tick, via the same [HyleSlantedSlab] leaning-slab shape. Deliberately NOT the
 * same object as [dev.aarso.hyle.cells.HyleSwitch] (square well, thumb travels vertically,
 * semantic green/grey — a *state* control) or
 * [dev.aarso.hyle.cells.HyleWellToggle] (a *selection* between two labelled peers) — this is
 * the mockup's plain binary on/off, thumb travelling horizontally, violet on-state, off/on
 * distinguished by BOTH track colour and thumb position (position is legible without colour).
 *
 * The off->on read is the ordinary "thumb slides right to turn on" affordance (grey track,
 * thumb at the leading/left edge = off; violet track, thumb at the trailing/right edge = on) —
 * the mockup's own off-state description trails off ("light thumb right…") without fully
 * specifying rest position, so this is an interpretation of the standard toggle convention
 * rather than a pixel-transcribed detail; flagged as such.
 */

private val TRACK_WIDTH = 52.dp
private val TRACK_HEIGHT = 30.dp
private val TRACK_RADIUS = 9.dp // rounded-SQUARE, not a pill (pill would be TRACK_HEIGHT / 2)
private val THUMB_SIZE = 24.dp
private val THUMB_MARGIN = 3.dp
private val THUMB_CORNER = 3f

/** The three renderable roles a toggle track resolves to — pure, so a caller (or a future
 *  test) can reason about track colour without touching Compose. */
enum class HyleToggleTrackRole { OFF, ON, DISABLED }

fun resolveHyleToggleTrackRole(checked: Boolean, enabled: Boolean): HyleToggleTrackRole = when {
    !enabled -> HyleToggleTrackRole.DISABLED
    checked -> HyleToggleTrackRole.ON
    else -> HyleToggleTrackRole.OFF
}

@Composable
fun HyleToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val role = resolveHyleToggleTrackRole(checked, enabled)
    // Thumb position carries the state independently of colour (WCAG dual-channel): even
    // read in greyscale, "thumb at the trailing edge" still reads as on.
    val t by animateFloatAsState(targetValue = if (checked) 1f else 0f, label = "hyleToggleThumb")
    val trackColor = when (role) {
        HyleToggleTrackRole.ON -> c.violet
        HyleToggleTrackRole.OFF -> c.textMid.copy(alpha = 0.35f)
        HyleToggleTrackRole.DISABLED -> if (checked) c.violet.copy(alpha = 0.3f) else c.textMid.copy(alpha = 0.15f)
    }
    val thumbColor = if (enabled) Color.White else Color.White.copy(alpha = 0.55f)
    val trackShape = RoundedCornerShape(TRACK_RADIUS)
    val thumbShape = remember { HyleSlantedSlab(THUMB_CORNER) }
    val travel = TRACK_WIDTH - THUMB_SIZE - THUMB_MARGIN * 2

    Box(
        modifier
            .size(width = TRACK_WIDTH, height = TRACK_HEIGHT)
            .clip(trackShape)
            .background(trackColor, trackShape)
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onValueChange = { haptics.tap(); onCheckedChange(it) },
                    )
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            Modifier
                .padding(THUMB_MARGIN)
                .offset(x = travel * t)
                .size(THUMB_SIZE)
                .clip(thumbShape)
                .background(thumbColor),
        )
    }
}
