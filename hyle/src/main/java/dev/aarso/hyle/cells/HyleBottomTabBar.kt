package dev.aarso.hyle.cells

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.Ink
import dev.aarso.hyle.theme.LocalHyleColors
import dev.aarso.hyle.theme.TextHigh

/** Responsive label policy for the centre-room file tabs. */
enum class HyleFileTabStage { FULL, SELECTED_LABEL, ICONS_ONLY }

object HyleFileTabLayout {
    fun stage(width: Dp): HyleFileTabStage = when {
        width >= 380.dp -> HyleFileTabStage.FULL
        width >= 210.dp -> HyleFileTabStage.SELECTED_LABEL
        else -> HyleFileTabStage.ICONS_ONLY
    }
}

private val BottomFileTabShape: Shape = GenericShape { size, _ ->
    val slant = size.height * HyleSeam.SLOPE
    moveTo(0f, 0f)
    lineTo(size.width, 0f)
    lineTo(size.width - slant, size.height)
    lineTo(slant, size.height)
    close()
}

private val TopFileTabShape: Shape = GenericShape { size, _ ->
    val slant = size.height * HyleSeam.SLOPE
    moveTo(slant, 0f)
    lineTo(size.width - slant, 0f)
    lineTo(size.width, size.height)
    lineTo(0f, size.height)
    close()
}

/**
 * The centre room's file-tab switcher.
 *
 * The active tab is cut from the same material as the room and shares a full-width merge edge
 * with it. Inactive tabs remain on the black application ground, separated by Hyle's canonical
 * slash seam. TOP mirrors the visual order so the active Chat tab anchors the upper-right corner;
 * BOTTOM anchors it at the lower-left, matching the centre-room reference set. Labels degrade from
 * all -> selected only -> icons only as horizontal space contracts (including IME layouts).
 */
@Composable
fun HyleBottomTabBar(
    tabs: List<HyleTabSpec>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    position: String = "BOTTOM",
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val top = position.equals("TOP", ignoreCase = true)
    val ordered = if (top) tabs.indices.reversed() else tabs.indices

    BoxWithConstraints(modifier.background(Ink)) {
        val stage = HyleFileTabLayout.stage(maxWidth)
        Row(
            modifier = Modifier.fillMaxWidth().height(40.dp),
            verticalAlignment = if (top) Alignment.Bottom else Alignment.Top,
            horizontalArrangement = if (top) Arrangement.End else Arrangement.Start,
        ) {
            ordered.forEachIndexed { visualIndex, sourceIndex ->
                if (visualIndex > 0) FileTabSlash(tint = TextHigh.copy(alpha = 0.42f))
                val tab = tabs[sourceIndex]
                val active = sourceIndex == selected
                val showLabel = stage == HyleFileTabStage.FULL ||
                    (stage == HyleFileTabStage.SELECTED_LABEL && active)
                val shape = if (top) TopFileTabShape else BottomFileTabShape
                val background = if (active) c.raised else Ink
                val tint = if (active) c.violet else TextHigh
                Row(
                    modifier = Modifier
                        .height(if (active) 40.dp else 36.dp)
                        .clip(shape)
                        .background(background, shape)
                        .clickable {
                            haptics.tap()
                            onSelect(sourceIndex)
                        }
                        .semantics {
                            role = Role.Tab
                            selected = active
                            contentDescription = tab.label
                        }
                        .padding(horizontal = if (showLabel) 13.dp else 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Canvas(Modifier.size(17.dp)) { tab.glyph(this, tint) }
                    if (showLabel) {
                        Spacer(Modifier.width(7.dp))
                        Text(
                            tab.label,
                            style = MaterialTheme.typography.labelMedium,
                            color = tint,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FileTabSlash(tint: Color) {
    Box(Modifier.width(9.dp).height(36.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.size(width = 8.dp, height = 24.dp)) {
            val run = size.height * HyleSeam.SLOPE
            val cx = size.width / 2f
            drawLine(
                color = tint,
                start = Offset(cx + run / 2f, 0f),
                end = Offset(cx - run / 2f, size.height),
                strokeWidth = 1.4f,
            )
        }
    }
}
