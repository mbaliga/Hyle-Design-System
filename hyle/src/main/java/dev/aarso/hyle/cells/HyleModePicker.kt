package dev.aarso.hyle.cells

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.HyleColors
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * One entry in a [HyleModePicker]: identity + the copy the *caller* supplies. Hyle ships the
 * chooser's structure (card, glyph, selection register), never app copy — [title]/[description]
 * are plain strings so the same component serves every app in the constellation with its own
 * wording, not just Fonebrew's "Regular"/"asoc" pair.
 *
 * [badge] is an optional small outlined tag (e.g. an app passing `"experimental"` for a mode
 * still being shaped) — rendered next to the title whenever it's non-null, independent of
 * [HyleModePicker.selected].
 */
data class HyleModeOption(
    val id: String,
    val title: String,
    val description: String,
    val badge: String? = null,
)

/** Which abstract preview motif an option's card draws. */
enum class HyleModePickerGlyph { LIST, CARD }

/**
 * Pure layout decisions behind [HyleModePicker], kept free of Compose/Android so they're
 * JVM-testable on their own — the repo's "render is owner-verified on device, the decision
 * logic behind it is unit-tested" convention (see e.g. `HyleFileTabLayout`, `resolveHyleFieldState`).
 */
object HyleModePickerLayout {
    /** Below this width two cards no longer fit side by side with a readable two-line
     *  description, so the picker stacks vertically instead. */
    private val STACK_BREAKPOINT = 360.dp

    fun stacked(width: Dp): Boolean = width < STACK_BREAKPOINT

    /**
     * Which motif the option at [index] draws. Cycles by pairs rather than keying off an
     * option's id/title, so the component stays correct for two OR more options: index 0
     * always draws [HyleModePickerGlyph.LIST] (the bottom-bar-and-list wireframe — Regular,
     * for a caller with exactly two modes), index 1 draws [HyleModePickerGlyph.CARD] (the
     * center-card-with-edge-slivers motif — asoc), and a third-plus option repeats the cycle
     * rather than drawing nothing.
     */
    fun glyphForIndex(index: Int): HyleModePickerGlyph =
        if (index % 2 == 0) HyleModePickerGlyph.LIST else HyleModePickerGlyph.CARD
}

/**
 * A two-OR-MORE-option chooser: each [HyleModeOption] renders as its own card carrying an
 * abstract preview glyph, a title, a caller-supplied two-line description, and an optional
 * [HyleModeOption.badge] tag. Cards sit side by side when there's room, stacked in a column on
 * narrow widths (owner ruling 2026-09-15: this is how Fonebrew's Regular/asoc split — and its
 * siblings across the constellation — get chosen; Hyle ships the chooser, not the app copy).
 *
 * Selection is carried by **two** non-colour-alone channels — a heavier border on the selected
 * card, plus an explicit check glyph — never by hue alone: the owner is red-green colorblind,
 * and a violet-only border-colour swap would still fail that bar on its own. [selected] is the
 * chosen option's [HyleModeOption.id], or null when nothing is chosen yet.
 */
@Composable
fun HyleModePicker(
    selected: String?,
    options: List<HyleModeOption>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier) {
        val cards: @Composable (Modifier) -> Unit = { cardModifier ->
            options.forEachIndexed { index, option ->
                HyleModeOptionCard(
                    option = option,
                    glyph = HyleModePickerLayout.glyphForIndex(index),
                    isSelected = option.id == selected,
                    onClick = { onSelect(option.id) },
                    modifier = cardModifier,
                )
            }
        }
        if (HyleModePickerLayout.stacked(maxWidth)) {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                cards(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                cards(Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HyleModeOptionCard(
    option: HyleModeOption,
    glyph: HyleModePickerGlyph,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalHyleColors.current
    val haptics = rememberHyleHaptics()
    val shape = RoundedCornerShape(10.dp)
    // The primary selection signal: a heavier border, not a re-coloured one — width changes
    // read under any colour vision. The violet fill/border tint is supplementary, never the
    // only cue (the check glyph below is the other explicit, non-colour half of the signal).
    val borderWidth = if (isSelected) 2.dp else 1.dp
    val borderColor = if (isSelected) c.violet else c.hairline
    Column(
        modifier
            .clip(shape)
            .background(if (isSelected) c.violetDim else c.raised, shape)
            .border(borderWidth, borderColor, shape)
            .clickable(onClick = { haptics.tap(); onClick() })
            .semantics {
                role = Role.RadioButton
                this.selected = isSelected
                contentDescription = option.title
            }
            .padding(14.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Box(Modifier.size(44.dp)) {
                Canvas(Modifier.fillMaxSize()) {
                    val tint = if (isSelected) c.violet else c.textMid
                    when (glyph) {
                        HyleModePickerGlyph.LIST -> drawModePickerListGlyph(tint)
                        HyleModePickerGlyph.CARD -> drawModePickerCardGlyph(tint)
                    }
                }
            }
            // The check glyph: present ONLY when selected, in its own slot so it never
            // collides with the badge (which is independent of selection — see below).
            if (isSelected) {
                Box(
                    Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(c.violet),
                    contentAlignment = Alignment.Center,
                ) {
                    Canvas(Modifier.size(12.dp)) { drawModePickerCheckGlyph(c.onViolet) }
                }
            }
        }
        Spacer(Modifier.height(10.dp))
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                option.title,
                style = MaterialTheme.typography.titleSmall,
                color = c.textHigh,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (option.badge != null) {
                Spacer(Modifier.width(8.dp))
                HyleModePickerBadge(option.badge, c)
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(
            option.description,
            style = MaterialTheme.typography.bodySmall,
            color = c.textMid,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The "experimental"-style tag: a small outlined chip, deliberately quieter than [HyleChip]
 *  (which is the *selection* register) — this is informational, never itself clickable. */
@Composable
private fun HyleModePickerBadge(text: String, c: HyleColors, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(4.dp)
    Box(
        modifier
            .clip(shape)
            .border(1.dp, c.hairline, shape)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelSmall, color = c.textMid, maxLines = 1)
    }
}

/** Regular's motif: a short stack of list rows over a solid bottom bar — plain shapes only. */
private fun DrawScope.drawModePickerListGlyph(tint: Color) {
    val w = size.width
    val h = size.height
    val rowStroke = (w * 0.07f).coerceAtLeast(1.5f)
    val rowsTop = h * 0.18f
    val rowGap = h * 0.20f
    for (row in 0 until 3) {
        val y = rowsTop + row * rowGap
        drawLine(
            color = tint,
            start = Offset(w * 0.16f, y),
            end = Offset(w * 0.84f, y),
            strokeWidth = rowStroke,
            cap = StrokeCap.Round,
        )
    }
    drawRoundRect(
        color = tint,
        topLeft = Offset(w * 0.12f, h * 0.80f),
        size = Size(w * 0.76f, h * 0.12f),
        cornerRadius = CornerRadius(h * 0.03f),
    )
}

/** asoc's motif: a center card ringed by four short edge slivers — the "enhanced" frame. */
private fun DrawScope.drawModePickerCardGlyph(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = (w * 0.07f).coerceAtLeast(1.5f)
    val cardW = w * 0.5f
    val cardH = h * 0.5f
    drawRoundRect(
        color = tint,
        topLeft = Offset((w - cardW) / 2f, (h - cardH) / 2f),
        size = Size(cardW, cardH),
        cornerRadius = CornerRadius(w * 0.06f),
        style = Stroke(width = stroke),
    )
    val sliver = w * 0.14f
    // top / bottom / left / right, each a short line just outside the center card.
    drawLine(tint, Offset(w * 0.5f, h * 0.05f), Offset(w * 0.5f, h * 0.05f + sliver), stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.5f, h * 0.95f - sliver), Offset(w * 0.5f, h * 0.95f), stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.05f, h * 0.5f), Offset(w * 0.05f + sliver, h * 0.5f), stroke, cap = StrokeCap.Round)
    drawLine(tint, Offset(w * 0.95f - sliver, h * 0.5f), Offset(w * 0.95f, h * 0.5f), stroke, cap = StrokeCap.Round)
}

/** The selected-state check: a simple two-segment tick, the non-colour half of the signal. */
private fun DrawScope.drawModePickerCheckGlyph(tint: Color) {
    val w = size.width
    val h = size.height
    val path = Path().apply {
        moveTo(w * 0.06f, h * 0.55f)
        lineTo(w * 0.40f, h * 0.86f)
        lineTo(w * 0.94f, h * 0.14f)
    }
    drawPath(
        path,
        color = tint,
        style = Stroke(width = w * 0.18f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}
