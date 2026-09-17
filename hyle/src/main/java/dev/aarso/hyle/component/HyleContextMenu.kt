package dev.aarso.hyle.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import dev.aarso.hyle.cells.rememberHyleHaptics
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * HyleContextMenu — desktop-class kit §1E: an anchored floating rounded card popup, the
 * discoverable "you can see your way into it" desktop-class action surface (the doc's binding
 * clickability rule §0 — long-press on touch, right-click/keyboard where a pointer exists).
 * Built on a raw [Popup] rather than Material3's `DropdownMenu` so the card's finish (rounded,
 * hairline-edged, elevated) is genuinely Hyle's rather than fought out of Material's default
 * Surface — matching the spec's adoption-map instruction that no new surface uses a bare
 * Material control.
 *
 * Item grouping/separator placement is pure — [planHyleContextMenu] — and JVM-unit-tested in
 * `HyleContextMenuTest` independent of the popup itself.
 */

data class HyleContextMenuItem(
    val id: String,
    val label: String,
    val destructive: Boolean = false,
    val enabled: Boolean = true,
    val icon: (@Composable () -> Unit)? = null,
)

/** One row to render, plus whether a hairline separator precedes it. */
data class HyleContextMenuRow(val item: HyleContextMenuItem, val separatorBefore: Boolean)

/**
 * Pure grouping: every non-destructive item first (input order preserved), then every
 * destructive item (input order preserved among themselves) — regardless of how the caller
 * interleaved them — with exactly one hairline separator immediately before the destructive
 * run, and only when BOTH groups are non-empty (an all-normal or all-destructive menu gets no
 * orphan separator).
 */
fun planHyleContextMenu(items: List<HyleContextMenuItem>): List<HyleContextMenuRow> {
    val normal = items.filterNot { it.destructive }
    val destructive = items.filter { it.destructive }
    val rows = mutableListOf<HyleContextMenuRow>()
    normal.forEach { rows += HyleContextMenuRow(it, separatorBefore = false) }
    destructive.forEachIndexed { i, item ->
        rows += HyleContextMenuRow(item, separatorBefore = i == 0 && normal.isNotEmpty())
    }
    return rows
}

private val MENU_SHAPE = RoundedCornerShape(14.dp)
private val ROW_MIN_HEIGHT = 46.dp // "generous row height"

/**
 * The popup itself. Positioned relative to wherever it's composed (wrap the pressed row in a
 * `Box` and place [HyleContextMenu] as its sibling to anchor "near the pressed row, overlapping
 * content", per the spec). Dismisses on outside-tap and back/Escape via [PopupProperties] —
 * both handled by the platform, not re-implemented here.
 */
@Composable
fun HyleContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    items: List<HyleContextMenuItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset.Zero,
) {
    if (!expanded) return
    val c = LocalHyleColors.current
    val density = LocalDensity.current
    val offsetPx = remember(offset, density) {
        with(density) { IntOffset(offset.x.roundToPx(), offset.y.roundToPx()) }
    }
    Popup(
        offset = offsetPx,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true, dismissOnBackPress = true, dismissOnClickOutside = true),
    ) {
        HyleContextMenuCard(
            items = items,
            onItemClick = { id -> onDismissRequest(); onItemClick(id) },
            modifier = modifier,
        )
    }
}

/**
 * The menu card itself, without the [Popup] host — the exact surface [HyleContextMenu]
 * shows, exposed so hosts can embed it in their own anchoring (bottom sheets, side panes on
 * wide screens) and so device-less render tests can rasterize the real card.
 */
@Composable
fun HyleContextMenuCard(
    items: List<HyleContextMenuItem>,
    onItemClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalHyleColors.current
    val rows = remember(items) { planHyleContextMenu(items) }
    Column(
        modifier
            .widthIn(min = 200.dp, max = 300.dp)
            .shadow(10.dp, MENU_SHAPE, clip = false)
            .clip(MENU_SHAPE)
            .background(c.raised, MENU_SHAPE)
            .border(1.dp, c.hairline, MENU_SHAPE)
            .padding(vertical = 6.dp),
    ) {
        rows.forEach { row ->
            if (row.separatorBefore) {
                HorizontalDivider(
                    color = c.hairline,
                    modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
                )
            }
            HyleContextMenuRowView(row.item, onClick = { onItemClick(row.item.id) })
        }
    }
}

@Composable
private fun HyleContextMenuRowView(item: HyleContextMenuItem, onClick: () -> Unit) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val textColor = when {
        !item.enabled -> c.textDisabled
        item.destructive -> c.error
        else -> c.textHigh
    }
    val iconColor = when {
        !item.enabled -> c.textDisabled
        item.destructive -> c.error
        else -> c.textMid
    }
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = ROW_MIN_HEIGHT)
            .then(
                if (item.enabled) {
                    Modifier.clickable { haptics.tap(); onClick() }
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (item.icon != null) {
            Box(Modifier.padding(end = 12.dp)) {
                CompositionLocalProvider(LocalContentColor provides iconColor) { item.icon.invoke() }
            }
        }
        Text(item.label, style = MaterialTheme.typography.bodyMedium, color = textColor, maxLines = 1)
    }
}
