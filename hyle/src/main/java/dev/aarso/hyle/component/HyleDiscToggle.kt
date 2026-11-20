package dev.aarso.hyle.component

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription

/**
 * The flat disc + white pill toggle (owner's pick from the round-toggle study, 2026-11-20:
 * "the top row of buttons is most my style").
 *
 * Off-state colour carries meaning (owner, 2026-11-20): [HyleDiscOff.Negative] (red) when "off" is
 * itself a negative outcome; [HyleDiscOff.Rest] (grey) when "off" is just the resting state.
 * On is always green. The pill's angle is the other signal: 0° is vertical, positive is clockwise.
 *
 * Measured from the owner's reference image: disc grey #989898 / green #a5be00 / red #ff0404, an
 * inner shadow (about 21% black, blurred ~2.6 units, offset down 2.6 on a 64-unit disc) so the top
 * edge is darkest whatever the pill's angle, a ~9×44 white pill, a faint drop shadow.
 *
 * Size is [HyleControlHeight] square, the same as [HyleField]'s height.
 */
enum class HyleDiscOff { Rest, Negative }

/** Where the pill points. [Flip]: off horizontal, on vertical. [Lean45]/[Lean60]: off leans left,
 *  on leans right by that many degrees, auto (three-state only) vertical. */
enum class HyleDiscPose { Flip, Lean45, Lean60 }

enum class HyleAutoState { Off, Auto, On }

private val DiscRest = Color(0xFF989898)
private val DiscNegative = Color(0xFFFF0404)
private val DiscOn = Color(0xFFA5BE00) // measured from the reference; Success is the token-true alternative
private val DiscAuto = Color(0xFF3D7BE0) // not in the reference: a third, distinct state (owner to confirm)

private fun offColor(o: HyleDiscOff) = if (o == HyleDiscOff.Negative) DiscNegative else DiscRest

private fun angleFor(pose: HyleDiscPose, state: HyleAutoState): Float = when (pose) {
    HyleDiscPose.Flip -> when (state) { HyleAutoState.Off -> 90f; HyleAutoState.Auto -> 45f; HyleAutoState.On -> 0f }
    HyleDiscPose.Lean45 -> when (state) { HyleAutoState.Off -> -45f; HyleAutoState.Auto -> 0f; HyleAutoState.On -> 45f }
    HyleDiscPose.Lean60 -> when (state) { HyleAutoState.Off -> -60f; HyleAutoState.Auto -> 0f; HyleAutoState.On -> 60f }
}

/** Two-state toggle. */
@Composable
fun HyleDiscToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    offTone: HyleDiscOff = HyleDiscOff.Rest,
    pose: HyleDiscPose = HyleDiscPose.Flip,
    description: String? = null,
) {
    val state = if (checked) HyleAutoState.On else HyleAutoState.Off
    DiscFace(
        state = state, pose = pose, offTone = offTone, enabled = enabled,
        modifier = modifier
            .toggleable(
                value = checked,
                enabled = enabled && onCheckedChange != null,
                role = Role.Switch,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onValueChange = { onCheckedChange?.invoke(it) },
            )
            .semantics {
                if (description != null) contentDescription = description
                stateDescription = if (checked) "On" else "Off"
            },
    )
}

/** Three-state toggle: off, auto, on. A tap advances off → auto → on → off. */
@Composable
fun HyleAutoToggle(
    state: HyleAutoState,
    onStateChange: ((HyleAutoState) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    offTone: HyleDiscOff = HyleDiscOff.Rest,
    pose: HyleDiscPose = HyleDiscPose.Lean45,
    description: String? = null,
) {
    DiscFace(
        state = state, pose = pose, offTone = offTone, enabled = enabled,
        modifier = modifier
            .clickable(
                enabled = enabled && onStateChange != null,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onStateChange?.invoke(HyleAutoState.values()[(state.ordinal + 1) % 3]) }
            .semantics {
                if (description != null) contentDescription = description
                stateDescription = state.name
            },
    )
}

@Composable
private fun DiscFace(
    state: HyleAutoState,
    pose: HyleDiscPose,
    offTone: HyleDiscOff,
    enabled: Boolean,
    modifier: Modifier,
) {
    val spec = tween<Float>(HYLE_CAP_MILLIS, easing = EaseOutCubic)
    val angle by animateFloatAsState(angleFor(pose, state), spec, label = "hyleDiscAngle")
    val toOn by animateFloatAsState(if (state == HyleAutoState.On) 1f else 0f, spec, label = "hyleDiscOn")
    val toAuto by animateFloatAsState(if (state == HyleAutoState.Auto) 1f else 0f, spec, label = "hyleDiscAuto")
    val fill = lerp(lerp(offColor(offTone), DiscAuto, toAuto), DiscOn, toOn)
    Canvas(
        modifier
            .size(HyleControlHeight)
            .graphicsLayer { alpha = if (enabled) 1f else 0.45f },
    ) { drawDisc(fill, angle) }
}

private fun DrawScope.drawDisc(fill: Color, angleDeg: Float) {
    val u = size.minDimension / 64f // the study's 64-unit frame
    val c = Offset(size.width / 2f, size.height / 2f)
    val r = 23.5f * u
    // Faint drop shadow under the disc.
    drawIntoCanvas { canvas ->
        val p = Paint().apply {
            color = Color.Black.copy(alpha = 0.16f)
            asFrameworkPaint().maskFilter = BlurMaskFilter(1.5f * u, BlurMaskFilter.Blur.NORMAL)
        }
        canvas.drawCircle(c + Offset(0f, 1.5f * u), r, p)
    }
    drawCircle(fill, r, c)
    // Inner shadow: a blurred black ring just outside the disc, offset down, clipped to the disc.
    val disc = Path().apply { addOval(androidx.compose.ui.geometry.Rect(c, r)) }
    clipPath(disc) {
        drawIntoCanvas { canvas ->
            val hole = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(androidx.compose.ui.geometry.Rect(Offset.Zero, size).inflate(30f * u))
                addOval(androidx.compose.ui.geometry.Rect(c + Offset(0f, 2.6f * u), r))
            }
            val p = Paint().apply {
                color = Color.Black.copy(alpha = 0.26f)
                asFrameworkPaint().maskFilter = BlurMaskFilter(2.6f * u, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawPath(hole, p)
        }
    }
    // The pill: 9 × 44, vertical at 0°, rotating clockwise. Its own faint shadow.
    rotate(angleDeg, c) {
        val tl = Offset(c.x - 4.5f * u, c.y - 22f * u)
        drawIntoCanvas { canvas ->
            val p = Paint().apply {
                color = Color.Black.copy(alpha = 0.28f)
                asFrameworkPaint().maskFilter = BlurMaskFilter(0.8f * u, BlurMaskFilter.Blur.NORMAL)
            }
            canvas.drawRoundRect(
                tl.x, tl.y + 0.6f * u, tl.x + 9f * u, tl.y + 44f * u + 0.6f * u, 4.5f * u, 4.5f * u, p,
            )
        }
        drawRoundRect(Color.White, tl, Size(9f * u, 44f * u), CornerRadius(4.5f * u))
    }
}
