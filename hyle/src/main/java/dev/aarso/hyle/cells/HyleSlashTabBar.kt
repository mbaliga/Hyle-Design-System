package dev.aarso.hyle.cells

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * A view-switcher, not a room-navigator — a lighter register than [HyleTabBar]'s filled seam-
 * grammar chips. [HyleTabBar] answers "which top-level room am I in" (Settings/Models/Dev/About)
 * and earns a filled selection register for that weight; this answers "which view of the same
 * thing am I looking at" (Chat vs Terminal on one conversation, a Summary vs Graph vs Map view of
 * one document) — text + glyph only, no fill, tabs threaded by a literal "/" the way this app's
 * seam grammar already reads everywhere else (nav-chip slants, field silhouettes). [leading] is a
 * fixed slot before the first tab for whatever this screen's "back out of this whole switcher"
 * affordance is — a caller passes its own (e.g. "‹ Chats"), never a generic overflow icon.
 *
 * Selection is colour only here (violet vs muted), which is legible precisely because there is no
 * per-tab fill to compete with it — the flat register earns simplicity, the filled one
 * ([HyleTabBar]) earns unambiguous "current room."
 */
@Composable
fun HyleSlashTabBar(
    tabs: List<HyleTabSpec>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    leading: @Composable () -> Unit = {},
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        leading()
        tabs.forEachIndexed { i, tab ->
            if (i > 0) SlashSeparator()
            val isSelected = i == selected
            val tint = if (isSelected) c.violet else c.textMid
            Column(
                modifier = Modifier
                    .clickable { haptics.tap(); onSelect(i) }
                    .padding(vertical = 4.dp, horizontal = 2.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.size(18.dp)) { tab.glyph(this, tint) }
                Text(
                    tab.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = tint,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/** The literal "/" thread between tabs — the same slant every seam in this app leans. */
@Composable
private fun SlashSeparator() {
    val c = LocalHyleColors.current
    Canvas(Modifier.size(width = 8.dp, height = 22.dp)) {
        drawLine(
            color = c.hairline,
            start = Offset(size.width * 0.8f, 0f),
            end = Offset(size.width * 0.2f, size.height),
            strokeWidth = size.width * 0.22f,
        )
    }
}
