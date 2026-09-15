package dev.aarso.hyle.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * The slanted-tab atom, promoted from the probe (`hyle-probe/.../AeonAtomsProbe.kt`'s
 * `RoundedParallelogram`, locked spec: slope 0.2, corner radius 3.5dp) into the shipped
 * module, plus the one shape that atom never had: a tab anchored to the BOTTOM of its
 * content rather than the top.
 *
 * [RoundedHangingTab] is not [RoundedParallelogram] flipped — a top tab leans (both edges
 * slant the same direction, like italic text); a bottom tab tapers (edges slant toward
 * each other, symmetric), because its top edge has to span the full cell width to merge
 * flush with the panel above it, while a leaning top edge would leave a visible notch at
 * one corner of that seam.
 */

private const val SLANT = 0.2f
private const val CORNER_DP = 3.5f

/**
 * The locked slope every slanted Hyle shape leans at, exposed so surfaces that draw their
 * own slanted seams (the host app's view bar, its header buttons) lean by exactly the same
 * amount as this module's shapes rather than eyeballing a second constant.
 *
 * Convention: the top edge sits FURTHER RIGHT than the bottom edge by `height * slope`, so
 * a seam or divider reads as "/".
 */
const val HyleSlant: Float = SLANT

/**
 * A leaning slab — the [RoundedParallelogram] geometry from the probe's atom set, promoted
 * for host surfaces that need the same shape outside a tab row: the header's icon buttons
 * in the app shell, chips, badges. Both vertical edges lean "/" together (unlike
 * [HyleBottomTabRow]'s hanging tab, whose edges taper toward each other because its top
 * edge is a merge seam).
 */
class HyleSlantedSlab(private val cornerDp: Float = CORNER_DP) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { cornerDp.dp.toPx() }
        val s = size.height * SLANT
        val pts = listOf(
            Offset(s, 0f),
            Offset(size.width, 0f),
            Offset(size.width - s, size.height),
            Offset(0f, size.height),
        )
        return Outline.Generic(Path().apply { roundedPolygon(pts, r) })
    }
}

private fun Offset.unit(): Offset {
    val l = getDistance()
    return if (l == 0f) this else this / l
}

/** Same rounding primitive as the probe's atom: pull back [r] along each edge and sweep a
 *  quadratic through the original vertex, so every slanted Hyle shape shares one corner. */
private fun Path.roundedPolygon(pts: List<Offset>, r: Float) {
    for (i in pts.indices) {
        val curr = pts[i]
        val prev = pts[(i + pts.size - 1) % pts.size]
        val next = pts[(i + 1) % pts.size]
        val enter = curr + (prev - curr).unit() * r
        val exit = curr + (next - curr).unit() * r
        if (i == 0) moveTo(enter.x, enter.y) else lineTo(enter.x, enter.y)
        quadraticTo(curr.x, curr.y, exit.x, exit.y)
    }
    close()
}

/**
 * A tab hanging below a panel: full-width top edge (the merge seam), tapering inward at
 * slope [SLANT] to a narrower bottom edge (the free edge), corners rounded to [cornerDp].
 */
private class RoundedHangingTab(private val cornerDp: Float = CORNER_DP) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { cornerDp.dp.toPx() }
        val s = size.height * SLANT
        val pts = listOf(
            Offset(0f, 0f),
            Offset(size.width, 0f),
            Offset(size.width - s, size.height),
            Offset(s, size.height),
        )
        return Outline.Generic(Path().apply { roundedPolygon(pts, r) })
    }
}

private val HangingTabShape: Shape = RoundedHangingTab()

/**
 * A bottom-anchored row of slanted tabs: the active tab is filled with [panelColor] (the
 * same colour as the content above it) and pulled up flush against it so the seam
 * disappears; inactive tabs sit shorter, in [inactiveColor], with a visible gap on each
 * side — the same active/inactive language as [RoundedParallelogram] chips in the probe,
 * just re-anchored to hang below content instead of sitting inline with it.
 *
 * [tabs]: the tab labels, e.g. `listOf("RGB", "HSV", "HEX")` for a colour picker.
 */
@Composable
fun HyleBottomTabRow(
    tabs: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    panelColor: Color,
    inactiveColor: Color,
    activeTextColor: Color,
    inactiveTextColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        tabs.forEachIndexed { i, label ->
            val active = i == selected
            Box(
                Modifier
                    .weight(1f)
                    .clip(HangingTabShape)
                    .background(if (active) panelColor else inactiveColor)
                    .clickable { onSelect(i) }
                    .padding(
                        horizontal = 10.dp,
                        vertical = if (active) 12.dp else 8.dp,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    color = if (active) activeTextColor else inactiveTextColor,
                    fontSize = 12.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Normal,
                )
            }
        }
    }
}
