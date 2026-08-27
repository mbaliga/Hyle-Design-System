package dev.aarso.hyle.cells

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.Ink
import dev.aarso.hyle.theme.LocalHyleColors
import dev.aarso.hyle.theme.TextHigh

/**
 * The centre room's Chat/Terminal/Background-Tasks switcher, per the owner's reference mockup
 * (2026-08-27): a floating dark dock, not a themed surface — it's built on [Ink]/[TextHigh], the
 * fixed dark-palette constants, rather than [LocalHyleColors.current]. The reference stays a
 * near-black bar even against its own light-theme content, so a theme-following background would
 * go near-white in light mode and lose the dock's identity as an anchored, always-legible strip.
 *
 * Selection is colour only — the current accent ([dev.aarso.hyle.theme.HyleColors.violet], so a
 * re-tinted accent still reads correctly) against a muted on-dark white — with no per-tab fill,
 * so the three tabs read as ONE continuous dock rather than [HyleTabBar]'s discrete seam-grammar
 * cells. That continuity, plus corners rounded only on the edge facing open space, is what gives
 * this switcher its own unambiguous "tab bar" identity (the owner's ask) instead of
 * [HyleSlashTabBar]'s quiet inline register — the two stay distinct components because they
 * answer different questions (compare each one's own doc comment).
 *
 * [position] is `"TOP"` or `"BOTTOM"` (matching [HyleTabBar]'s convention): it rounds the corners
 * on the pair of edges facing AWAY from the screen edge this dock is placed against — flush where
 * it meets the room's own content, rounded where it meets open space — so the dock always reads
 * as anchored to that edge no matter which one the caller docks it to.
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
    // 20dp: Material's own bottom-sheet corner convention — this dock reads as a floating
    // sheet-like anchor, not a seam-grammar cell (those use HyleSeam.CORNER's tighter 8dp).
    val corner = 20.dp
    val shape = if (position == "BOTTOM") {
        RoundedCornerShape(topStart = corner, topEnd = corner)
    } else {
        RoundedCornerShape(bottomStart = corner, bottomEnd = corner)
    }
    val onDarkMuted = TextHigh.copy(alpha = 0.55f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ink, shape)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        tabs.forEachIndexed { i, tab ->
            val on = i == selected
            val tint = if (on) c.violet else onDarkMuted
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable { haptics.tap(); onSelect(i) }
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Canvas(Modifier.size(22.dp)) { tab.glyph(this, tint) }
                Spacer(Modifier.height(4.dp))
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
