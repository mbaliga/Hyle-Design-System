package dev.aarso.hyle.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.cells.rememberHyleHaptics
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * HyleTree — desktop-class kit §1D: a generic hierarchical tree. The caller owns the data
 * (nodes + labels + icon kind + hasChildren), the expand-state (a set of expanded ids) and the
 * selection; this component only flattens, lays out, and dispatches the three tap targets a
 * row exposes — chevron (toggle expand), row body (select/open), long-press (context menu
 * hook). See [flattenHyleTree] for the pure flattening/expand-state/guide-depth logic, tested
 * independently of Compose in `HyleTreeTest`.
 */

/** What a row's leading glyph looks like. CUSTOM defers entirely to the caller's [HyleTree]
 *  `icon` slot — this component draws no default glyph for it. */
enum class HyleTreeIconKind { FOLDER, FILE, CUSTOM }

/** The tree as the caller supplies it: a forest of nodes, each optionally holding children.
 *  [hasChildren] is a genuine property of [HyleTreeItem] (not derived from [children].isNotEmpty())
 *  so a caller doing **lazy** loading can mark a node expandable before its children are fetched. */
data class HyleTreeItem(
    val id: String,
    val label: String,
    val iconKind: HyleTreeIconKind = HyleTreeIconKind.FILE,
    val hasChildren: Boolean = false,
    val children: List<HyleTreeItem> = emptyList(),
)

/** One flattened, renderable row: everything [HyleTree] needs to lay it out, with no tree
 *  traversal left to do at render time. [depth] doubles as the indentation-guide count — see
 *  [flattenHyleTree]'s doc for why one full-height guide per row, at the same depth, is
 *  sufficient to read as one continuous line down a whole expanded subtree. */
data class HyleTreeRow(
    val id: String,
    val label: String,
    val iconKind: HyleTreeIconKind,
    val depth: Int,
    val hasChildren: Boolean,
    val expanded: Boolean,
)

/**
 * Pure depth-first flatten, gated by [expandedIds]: a node's children are only emitted when
 * its id is a member of [expandedIds] AND it [HyleTreeItem.hasChildren]. No Compose/Android
 * dependency — JVM-unit-tested.
 *
 * Guide-depth: each row's [HyleTreeRow.depth] is exactly the number of ancestor indentation
 * guides it should draw (one hairline per ancestor level, per the spec). Rendering each row's
 * guides at full row height and at a fixed per-depth x-offset is sufficient to make them read
 * as one continuous line "spanning the full height of the expanded span" without the renderer
 * tracking span boundaries itself: consecutive rows sharing an ancestor draw that ancestor's
 * guide at the same x on every row between the ancestor and the last row still inside its
 * subtree, and flattening naturally stops emitting that depth the moment the subtree ends.
 */
fun flattenHyleTree(roots: List<HyleTreeItem>, expandedIds: Set<String>): List<HyleTreeRow> {
    val out = mutableListOf<HyleTreeRow>()
    fun visit(items: List<HyleTreeItem>, depth: Int) {
        for (item in items) {
            val expanded = item.hasChildren && item.id in expandedIds
            out += HyleTreeRow(
                id = item.id,
                label = item.label,
                iconKind = item.iconKind,
                depth = depth,
                hasChildren = item.hasChildren,
                expanded = expanded,
            )
            if (expanded) visit(item.children, depth + 1)
        }
    }
    visit(roots, 0)
    return out
}

private val ROW_HEIGHT = 44.dp
private val INDENT_UNIT = 18.dp
private val CHEVRON_SLOT = 28.dp
private val ICON_SIZE = 18.dp

/**
 * Renders [roots] flattened by [expandedIds] as a scrolling list of rows. Chevron tap and row
 * tap are separate targets per the spec's clickability rule — a row with children still opens
 * on body-tap (caller decides what "open" means for a container; toggling expand from the
 * chevron is independent so keyboard/pointer users have a small, precise target too).
 *
 * [icon] lets a caller fully override the leading glyph (required for [HyleTreeIconKind.CUSTOM]
 * rows; optional otherwise) — default renders a simple outline folder/file glyph.
 */
@Composable
fun HyleTree(
    roots: List<HyleTreeItem>,
    expandedIds: Set<String>,
    selectedId: String?,
    onToggleExpand: (String) -> Unit,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: ((String) -> Unit)? = null,
    icon: @Composable (HyleTreeRow) -> Unit = { HyleTreeDefaultIcon(it.iconKind) },
) {
    val rows = remember(roots, expandedIds) { flattenHyleTree(roots, expandedIds) }
    LazyColumn(modifier.fillMaxWidth()) {
        items(rows, key = { it.id }) { row ->
            HyleTreeRowView(
                row = row,
                selected = row.id == selectedId,
                onToggleExpand = { onToggleExpand(row.id) },
                onSelect = { onSelect(row.id) },
                onLongPress = onLongPress?.let { { it(row.id) } },
                icon = icon,
            )
        }
    }
}

@Composable
private fun HyleTreeRowView(
    row: HyleTreeRow,
    selected: Boolean,
    onToggleExpand: () -> Unit,
    onSelect: () -> Unit,
    onLongPress: (() -> Unit)?,
    icon: @Composable (HyleTreeRow) -> Unit,
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val bodyInteraction = remember { MutableInteractionSource() }
    val pressed by bodyInteraction.collectIsPressedAsState()
    // Full-width highlight: selected wins over pressed so the current selection never
    // disappears mid-press.
    val highlight = when {
        selected -> c.violetDim
        pressed -> c.inset
        else -> Color.Transparent
    }
    val chevronRotation by animateFloatAsState(targetValue = if (row.expanded) 90f else 0f, label = "hyleTreeChevron")

    Box(
        Modifier
            .fillMaxWidth()
            .height(ROW_HEIGHT)
            .background(highlight)
            .drawBehind { drawIndentGuides(depth = row.depth, indentUnit = INDENT_UNIT, color = c.outline) }
            .then(
                if (onLongPress != null) {
                    Modifier.combinedClickable(
                        interactionSource = bodyInteraction,
                        indication = null,
                        onClick = { haptics.tap(); onSelect() },
                        onLongClick = { haptics.tap(); onLongPress() },
                    )
                } else {
                    Modifier.clickable(interactionSource = bodyInteraction, indication = null) {
                        haptics.tap()
                        onSelect()
                    }
                },
            )
            .padding(start = INDENT_UNIT * row.depth, end = 12.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
            // Chevron: its own tap target, separate from the row body (the spec's binding
            // clickability rule — every interactive affordance also has an ordinary tappable
            // path, and here the chevron and the body are two DIFFERENT ordinary taps rather
            // than one gesture standing in for both).
            Box(
                Modifier.width(CHEVRON_SLOT).height(ROW_HEIGHT).then(
                    if (row.hasChildren) {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { haptics.tap(); onToggleExpand() }
                    } else {
                        Modifier
                    },
                ),
                contentAlignment = Alignment.Center,
            ) {
                if (row.hasChildren) {
                    Text(
                        "›", // '›' — rotates 90° open, reading like '⌄'
                        color = c.textMid,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            }
            Box(Modifier.size(ICON_SIZE), contentAlignment = Alignment.Center) { icon(row) }
            Spacer(Modifier.width(8.dp))
            Text(
                row.label,
                color = if (selected) c.textHigh else c.textMid,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
            )
        }
    }
}

/** One thin vertical hairline per ancestor depth, spanning the row's full height — see
 *  [flattenHyleTree]'s doc for why per-row full-height segments are enough to read as one
 *  continuous guide down an expanded subtree. */
private fun DrawScope.drawIndentGuides(depth: Int, indentUnit: Dp, color: Color) {
    if (depth <= 0) return
    val unit = indentUnit.toPx()
    for (level in 0 until depth) {
        val x = unit * level + unit / 2f
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x, size.height),
            strokeWidth = 1.dp.toPx(),
        )
    }
}

/** Default outline folder/file glyph — a plain drawn silhouette, no icon-pack dependency. */
@Composable
private fun HyleTreeDefaultIcon(kind: HyleTreeIconKind, modifier: Modifier = Modifier) {
    val c = LocalHyleColors.current
    when (kind) {
        HyleTreeIconKind.FOLDER -> Canvas(modifier.size(ICON_SIZE)) {
            val w = size.width
            val h = size.height
            val tabW = w * 0.45f
            val tabH = h * 0.2f
            val path = Path().apply {
                moveTo(0f, tabH)
                lineTo(tabW * 0.5f, tabH)
                lineTo(tabW, 0f)
                lineTo(w, 0f)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path, color = c.textMid, style = Stroke(width = 1.4.dp.toPx()))
        }
        HyleTreeIconKind.FILE -> Canvas(modifier.size(ICON_SIZE)) {
            val w = size.width
            val h = size.height
            val fold = w * 0.3f
            val path = Path().apply {
                moveTo(0f, 0f)
                lineTo(w - fold, 0f)
                lineTo(w, fold)
                lineTo(w, h)
                lineTo(0f, h)
                close()
            }
            drawPath(path, color = c.textMid, style = Stroke(width = 1.4.dp.toPx()))
        }
        HyleTreeIconKind.CUSTOM -> {
            // No default: a CUSTOM row is expected to arrive via HyleTree's `icon` slot.
        }
    }
}
