package dev.aarso.hyle.cells

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.ParentDataModifier
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import dev.aarso.hyle.theme.LocalHyleColors
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * The **seam grammar** — the slant edge as a shared wall that adjacent cells pack
 * along, not a decoration on one control (owner direction 2026-07-24; references:
 * the Cohere nav's tessellating pills, the owner's Global/Button split action and
 * Global/Toggle). The ASOC Voronoi identity made compositional: cells fit together
 * along parallel `/` seams with a thin strip of ground between them.
 *
 * [HyleSegmentShape]: EVERY seam edge leans the same `/` way at the field's slope
 * ([HyleSeam.SLOPE]) — a slanted END has its bottom inset (top runs to full width)
 * and a slanted START has its top inset, so the two edges of any seam are PARALLEL
 * and the strip of ground between them is constant. (This is deliberately NOT the
 * nav chips' mirrored pair, which point at opposite screen edges — seams pack,
 * mirrors face.) Ends of a group keep square-rounded outer corners:
 *
 *   first  = HyleSegmentShape(slantStart = false, slantEnd = true )   ▐███/
 *   middle = HyleSegmentShape(slantStart = true,  slantEnd = true )   /███/
 *   last   = HyleSegmentShape(slantStart = true,  slantEnd = false)   /███▌
 *
 * Packing: because each box contains its own slant inset, a row must OVERLAP
 * adjacent boxes by (slant − [HyleSeam.GAP]) for the visible seam to read as
 * [HyleSeam.GAP] of ground. That overlap is a function of the cell HEIGHT, which
 * nothing knows at composition time — so it is computed once, at measure time, by
 * [HyleSeamRow]. Never hand-pack seam cells with `Arrangement.spacedBy(<literal>)`:
 * both call sites used to (Segments.kt's `SEAM_GAP − 10.dp` "at the 40dp button
 * height", Aeon.kt's `(-9).dp` "at tab-bar height"), and both were correct at
 * exactly one height and visibly wrong at every other.
 */
object HyleSeam {

    // The canonical slope is TRANSCRIBED, not chosen. [HyleFieldShape] is a
    // control-point-exact copy of the owner's Figma export, and its slant edge runs
    // from (13.7012, 269.522) to (78.844, 29.522) in the 3040x320 field canvas —
    // CellGeometry.kt:152-153, the `cubicTo(... l(13.7012f), y(269.522f))` /
    // `lineTo(l(78.844f), y(29.522f))` pair. Everything that leans derives from
    // those two numbers so that a field, a tab bar and a split action standing in
    // one column read as ONE wall.
    private const val FIGMA_RUN = 78.844f - 13.7012f // = 65.1428
    private const val FIGMA_RISE = 269.522f - 29.522f // = 240

    /**
     * dx per dy of every seam edge: 65.1428 / 240 = **0.271428…**
     *
     * Not 0.25. Segments.kt used to hand-type `0.25f // same slope as HyleFieldShape`
     * and Aeon.kt another `0.25f // measured`; both were ~8% shallow against the
     * transcription they claimed to match, which reads as sloppy rather than as a
     * deliberately different angle. Restate this nowhere — derive from it.
     */
    const val SLOPE: Float = FIGMA_RUN / FIGMA_RISE

    /** Gap of ground between packed cells — the visible "cell gap" of the identity. */
    val GAP: Dp = 3.dp

    /**
     * Outer corner radius of a seam cell. Raised 4dp → 8dp against the owner's Cohere
     * reference (2026-08-21): their split action's outer corners are visibly soft, and
     * at 4dp ours read as a cut rectangle. The corners where the slant meets top/bottom
     * stay tighter — see [HyleSegmentShape]'s dx/dy, which walks the same radius along
     * the *slanted* edge, so a leaning corner is naturally shorter than a square one.
     */
    val CORNER: Dp = 8.dp

    /**
     * Floor for a split action's height, matching HyleButton's 40dp register (Aeon.kt:
     * "Buttons: 6dp-radius rounded rects, 40dp tall — not M3 pills"). NOTE this is also
     * the trailing cell's touch-target height, which is therefore 40dp, not the 44/48dp
     * guideline — a register decision inherited from every other Hyle button, not one
     * this component gets to make on its own. Raising it is one constant here.
     */
    val SPLIT_MIN_HEIGHT: Dp = 40.dp

    /** Stable VISIBLE width of a split action's trailing affordance cell. */
    val SPLIT_TRAILING_WIDTH: Dp = 44.dp
}

class HyleSegmentShape(
    private val slantStart: Boolean,
    private val slantEnd: Boolean,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        if (size.width <= 0f || size.height <= 0f) return Outline.Generic(Path())
        val r = with(density) { HyleSeam.CORNER.toPx() }.coerceAtMost(size.height / 4f)
        // The slant is height * SLOPE with NO height cap. There used to be a
        // `.coerceAtMost(12.dp)`, which silently turned the slope into a variable: at
        // 48dp the run capped and the lean shallowed to 12/48 = 0.25, at 64dp to
        // 12/64 = 0.1875, so tall cells leaned at a different angle from short ones —
        // breaking the one invariant the seam grammar exists to guarantee.
        //
        // What the cap was really protecting against is a cell too NARROW to hold its
        // own diagonal (the top and bottom edges cross and the path self-intersects),
        // which is a width problem, not a height one — so clamp against the width
        // instead. No shipping mount comes near it: the tallest seam cell in the
        // constellation is a ~61dp HyleTabBar tab, ~17dp of run on an ~89dp-wide cell.
        val slant = (size.height * HyleSeam.SLOPE).coerceAtMost((size.width - 2f * r).coerceAtLeast(0f))
        val len = sqrt(slant * slant + size.height * size.height).coerceAtLeast(1f)
        val dx = r * slant / len
        val dy = r * size.height / len
        val w = size.width
        val h = size.height
        val tlx = if (slantStart) slant else 0f // top-left vertex x (slant leans "/": top inset right)
        val path = Path().apply {
            moveTo(tlx + r, 0f)
            lineTo(w - r, 0f)
            if (slantEnd) {
                // "/" end: top vertex at full width, BOTTOM inset by slant — parallel to
                // a following segment's slanted start.
                quadraticBezierTo(w, 0f, w - dx, dy)
                lineTo(w - slant + dx, h - dy)
                quadraticBezierTo(w - slant, h, w - slant - r, h)
            } else {
                quadraticBezierTo(w, 0f, w, r)
                lineTo(w, h - r)
                quadraticBezierTo(w, h, w - r, h)
            }
            lineTo(r, h)
            if (slantStart) {
                quadraticBezierTo(0f, h, dx, h - dy)
                lineTo(tlx - dx, dy)
                quadraticBezierTo(tlx, 0f, tlx + r, 0f)
            } else {
                quadraticBezierTo(0f, h, 0f, h - r)
                lineTo(0f, r)
                quadraticBezierTo(0f, 0f, r, 0f)
            }
            close()
        }
        return Outline.Generic(path)
    }
}

// ── Packing ────────────────────────────────────────────────────────────────────────

/**
 * Per-cell instructions for [HyleSeamRow]. [slantStart]/[slantEnd] must match the
 * cell's own [HyleSegmentShape]: the shape uses them to cut the slant, the row uses
 * them to work out how much of the layout box that cut eats.
 */
private class SeamCellData(
    val slantStart: Boolean,
    val slantEnd: Boolean,
    val weight: Float,
    val visibleWidth: Dp?,
) : ParentDataModifier {
    override fun Density.modifyParentData(parentData: Any?): Any = this@SeamCellData
}

/**
 * Declares a child of [HyleSeamRow] as a seam cell.
 *
 * [weight] > 0 makes the row share its width equally (by weight) across such cells —
 * the tab-bar case, where the seams must come out of the tabs' own width rather than
 * making the bar overflow. [visibleWidth] pins what the eye actually measures rather
 * than the layout box: a slanted edge is flush at one end of the cell and `slant` in
 * at the other, so at MID-height — where a centred glyph sits — it eats `slant/2`.
 * Declaring 44dp of visible width therefore asks for a 44dp + slant/2 box.
 */
internal fun Modifier.seamCell(
    slantStart: Boolean,
    slantEnd: Boolean,
    weight: Float = 0f,
    visibleWidth: Dp? = null,
): Modifier = this.then(SeamCellData(slantStart, slantEnd, weight, visibleWidth))

/**
 * Padding that also pays for the slant, for content centred inside a seam cell.
 *
 * A cell with only ONE slanted edge is not symmetric about its layout box: at
 * mid-height a slant-start cell spans `[slant/2, w]`, so `Alignment.Center` lands the
 * label `slant/4` to the left of the cell's optical centre (≈3dp on a 40dp button —
 * small, and exactly the kind of thing that reads as "off" without being nameable).
 * Adding `slant/2` back on each slanted side re-centres it and keeps the gutter
 * between a label and the seam equal to the gutter on the outer side.
 *
 * Reads the height off its own incoming constraints, which [HyleSeamRow] fixes — the
 * same reason the overlap is computed at measure time and not at composition time.
 */
internal fun Modifier.seamPadding(
    start: Dp = 0.dp,
    end: Dp = 0.dp,
    slantStart: Boolean = false,
    slantEnd: Boolean = false,
): Modifier = this.layout { measurable, constraints ->
    val h = if (constraints.hasBoundedHeight) constraints.maxHeight else constraints.minHeight
    val half = (h * HyleSeam.SLOPE / 2f).roundToInt()
    val l = start.roundToPx() + if (slantStart) half else 0
    val r = end.roundToPx() + if (slantEnd) half else 0
    val placeable = measurable.measure(constraints.offset(horizontal = -(l + r)))
    layout(
        (placeable.width + l + r).coerceIn(constraints.minWidth, constraints.maxWidth),
        placeable.height,
    ) { placeable.place(l, 0) }
}

/**
 * A row of seam cells packed so the visible strip of ground between them is exactly
 * [HyleSeam.GAP] at EVERY height — including heights nobody typed, such as the one a
 * 200% font scale produces.
 *
 * Why a custom [Layout] rather than a `Row` with a computed spacing: the overlap is
 * `slant − GAP` and `slant` is `height * SLOPE`, but the height can arrive from an
 * explicit `Modifier.height()`, from a `heightIn` floor, or from the cells' own
 * content growing — and only the measure pass sees which. `BoxWithConstraints` would
 * report the incoming *constraints* (often unbounded here, since a tab bar sizes to
 * its content), and `SubcomposeLayout` would pay a whole extra composition for a
 * number arithmetic already has. So: resolve the height first, derive the slant from
 * it, then measure every cell exactly once at that height.
 *
 * Cell widths are decided before any child is measured (a `Measurable` may only be
 * measured once per pass), using intrinsics for the wrap case.
 */
@Composable
internal fun HyleSeamRow(
    modifier: Modifier = Modifier,
    minHeight: Dp = 0.dp,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        val n = measurables.size
        if (n == 0) return@Layout layout(constraints.minWidth, constraints.minHeight) {}
        val data = measurables.map { it.parentData as? SeamCellData }

        // 1. HEIGHT — everything else is a function of it.
        val h = if (constraints.hasFixedHeight) {
            constraints.maxHeight
        } else {
            maxOf(
                minHeight.roundToPx(),
                measurables.maxOf { it.maxIntrinsicHeight(Constraints.Infinity) },
            ).coerceIn(constraints.minHeight, constraints.maxHeight)
        }
        val slant = h * HyleSeam.SLOPE
        val overlap = (slant - HyleSeam.GAP.toPx()).roundToInt().coerceAtLeast(0)
        val seams = overlap * (n - 1)

        // 2. WIDTHS, all decided up front; -1 marks "share what's left".
        val widths = IntArray(n) { i ->
            val d = data[i]
            when {
                d != null && d.weight > 0f -> -1
                d?.visibleWidth != null -> {
                    val eaten = ((if (d.slantStart) slant / 2f else 0f) + (if (d.slantEnd) slant / 2f else 0f))
                    d.visibleWidth.roundToPx() + eaten.roundToInt()
                }
                else -> measurables[i].maxIntrinsicWidth(h)
            }
        }
        val weightSum = data.fold(0f) { acc, d -> acc + (d?.weight ?: 0f) }
        // A weighted cell shares the ROW's width, which only exists if the parent gives
        // one. In an unbounded-width parent (a horizontally scrollable row) fall back to
        // each cell's own intrinsic width, or a weighted bar would collapse to nothing.
        val share = weightSum > 0f && constraints.hasBoundedWidth
        if (weightSum > 0f && !share) {
            for (i in 0 until n) if (widths[i] < 0) widths[i] = measurables[i].maxIntrinsicWidth(h)
        }
        val pinned = widths.filter { it >= 0 }.sum()
        val naturalW = (pinned - seams).coerceAtLeast(0)
        val rowW = (if (share) constraints.maxWidth else naturalW)
            .coerceIn(constraints.minWidth, constraints.maxWidth)
        if (share) {
            // Weighted cells split the row PLUS the seams they are about to give back to
            // the overlap, so N equal-weight tabs come out equal-width and the bar ends
            // exactly at rowW. Remainder goes to the earliest cells, never dropped.
            var left = rowW + seams - pinned
            var leftWeight = weightSum
            for (i in 0 until n) {
                val d = data[i] ?: continue
                if (d.weight <= 0f) continue
                val w = (left * (d.weight / leftWeight)).roundToInt().coerceAtLeast(0)
                widths[i] = w
                left -= w
                leftWeight -= d.weight
            }
        } else if (rowW != naturalW) {
            // No weights: the FIRST cell — a split action's label cell — absorbs the
            // difference in BOTH directions. Growing is what `fillMaxWidth` on a split
            // action means; shrinking is a long label in a parent too narrow for it,
            // where squeezing the label is right and squeezing the 44dp affordance
            // (which has a tap-target floor to keep) is not.
            widths[0] = (widths[0] + rowW - naturalW).coerceAtLeast(0)
        }

        // 3. Measure once each, at the shared height, and lay them along the seam.
        val placeables = Array(n) { i ->
            measurables[i].measure(Constraints.fixed(widths[i].coerceAtLeast(0), h))
        }
        layout(rowW, h) {
            var x = 0
            placeables.forEachIndexed { i, p ->
                // Later cells draw over earlier ones; the overlap band is exactly the
                // transparent notch a slanted START leaves, so the neighbour shows
                // through it and the ground between reads as GAP.
                p.place(x, 0)
                x += widths[i] - overlap
            }
        }
    }
}

/**
 * Split action (the owner's Global/Button; Cohere's "TRY NOW" / "Get Started ⟋ +"
 * register): one action rendered as two cells packed along a seam — the label cell and
 * a trailing affordance cell ([trailing], default "+"). Both cells fire [onClick] unless
 * [onTrailingClick] gives the affordance its own action. Fill states match
 * [HyleButton] exactly (primary violet / pressed ramp step / secondary raised +
 * hairline / disabled inset).
 *
 * Height is free: [HyleSeamRow] re-derives the overlap from whatever height this ends
 * up at, so `Modifier.height(64.dp)` and a 200% font scale both keep the seam at
 * [HyleSeam.GAP] and the lean at [HyleSeam.SLOPE].
 */
@Composable
fun HyleSplitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    secondary: Boolean = false,
    trailing: String = "+",
    onTrailingClick: (() -> Unit)? = null,
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()

    @Composable
    fun fillFor(pressed: Boolean) = when {
        !enabled -> c.inset
        secondary -> if (pressed) c.inset else c.raised
        pressed -> c.violetPressed
        else -> c.violet
    }

    val content = when {
        !enabled -> c.textDisabled
        secondary -> c.textHigh
        else -> c.onViolet
    }

    HyleSeamRow(modifier = modifier, minHeight = HyleSeam.SPLIT_MIN_HEIGHT) {
        val leadShape = HyleSegmentShape(slantStart = false, slantEnd = true)
        val leadInteraction = remember { MutableInteractionSource() }
        val leadPressed by leadInteraction.collectIsPressedAsState()
        Box(
            modifier = Modifier
                .seamCell(slantStart = false, slantEnd = true)
                .clip(leadShape)
                .background(fillFor(leadPressed), leadShape)
                .then(if (secondary) Modifier.border(1.dp, c.hairline, leadShape) else Modifier)
                .clickable(
                    enabled = enabled,
                    interactionSource = leadInteraction,
                    indication = LocalIndication.current,
                    onClick = { haptics.tap(); onClick() },
                )
                .seamPadding(start = 18.dp, end = 20.dp, slantEnd = true),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        }

        val tailShape = HyleSegmentShape(slantStart = true, slantEnd = false)
        val tailInteraction = remember { MutableInteractionSource() }
        val tailPressed by tailInteraction.collectIsPressedAsState()
        Box(
            modifier = Modifier
                // The declared width is the VISIBLE one. It used to be a flat
                // `.width(44.dp)` on the layout box, so the slant ate into it and the
                // tail got narrower the taller the button grew.
                .seamCell(slantStart = true, slantEnd = false, visibleWidth = HyleSeam.SPLIT_TRAILING_WIDTH)
                .clip(tailShape)
                .background(fillFor(tailPressed), tailShape)
                .then(if (secondary) Modifier.border(1.dp, c.hairline, tailShape) else Modifier)
                .clickable(
                    enabled = enabled,
                    interactionSource = tailInteraction,
                    indication = LocalIndication.current,
                    onClick = { haptics.tap(); (onTrailingClick ?: onClick)() },
                )
                .seamPadding(slantStart = true),
            contentAlignment = Alignment.Center,
        ) {
            Text(trailing, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
        }
    }
}

/**
 * Segmented toggle (the owner's Global/Toggle): one rounded container, hairline
 * edge; the SELECTED option is a filled cell whose slant edges are themselves the
 * dividers — first selection slants only its end, a middle one both, the last only
 * its start, so the filled cell always reads as packed against its neighbours.
 * Selection is also carried by the label colour (never hue alone).
 *
 * Cells are flush here (no [HyleSeamRow], no overlap) and that is correct: only ONE
 * cell is ever filled, so there is no second fill for a strip of ground to separate —
 * the slants read as the boundary of the selection, not as a seam. Labels still get
 * [seamPadding] so they stay centred in the parallelogram rather than in the box.
 */
@Composable
fun HyleSegmentedToggle(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val container = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .height(36.dp)
            .clip(container)
            .background(c.raised, container)
            .border(1.dp, c.hairline, container),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        options.forEachIndexed { i, label ->
            val isSelected = i == selected
            val slantStart = i > 0
            val slantEnd = i < options.lastIndex
            val cellShape = HyleSegmentShape(slantStart = slantStart, slantEnd = slantEnd)
            Box(
                modifier = Modifier
                    .height(36.dp)
                    .then(
                        if (isSelected) {
                            Modifier.clip(cellShape).background(c.violet, cellShape)
                        } else {
                            Modifier
                        },
                    )
                    .clickable(enabled = enabled && !isSelected, onClick = { haptics.tap(); onSelect(i) })
                    // Deliberately plain padding, NOT [seamPadding] — the one place in this
                    // file where the optical correction is refused. The gutter facing a
                    // slanted edge really is 16 − slant/2 ≈ 11.5dp rather than 16dp, so the
                    // label sits ~2.4dp off the parallelogram's centre. Paying that back
                    // costs ~5dp of width per slanted edge, and rendering the app's widest
                    // caller (ParticipantsScreen's "Personas — named experts" /
                    // "Models — one prompt, diversity", fillMaxWidth, maxLines = 1) shows
                    // the toggle already overflows a 380dp board and clips the second
                    // label. Buying 2.4dp of centring with 10dp of width there is a net
                    // loss, so this component's layout width stays byte-for-byte what it
                    // was and only its slant angle and corner radius move.
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.labelMedium,
                    color = when {
                        !enabled -> c.textDisabled
                        isSelected -> c.onViolet
                        else -> c.textMid
                    },
                    maxLines = 1,
                )
            }
        }
    }
}
