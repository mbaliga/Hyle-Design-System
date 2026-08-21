package dev.aarso.hyle.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * The cap toggle — the owner's slant-cap two-option switch (mockup 2026-08-21, the `#`/`*`
 * pair): a rounded tray holding two options, with an ink "keycap" covering the ACTIVE side.
 * The cap's inner edge (the one facing the inactive option) leans at [HyleSlant], wider at
 * the top, and the cap carries the little fold hook at its top-right corner. Tapping the
 * uncovered side slides the cap across; the slanted edge mirrors so it always faces inward.
 *
 * API mirrors HyleWellToggle (optionA/optionB/selected/onSelect) so the two A/B controls are
 * drop-in interchangeable. Active label renders on the cap in the tray's light tone; the
 * inactive label sits directly on the tray in ink. Dual-channel: the cap's position AND the
 * covered/uncovered label treatment carry the state, never color alone.
 */
@Composable
fun HyleCapToggle(
    optionA: String,
    optionB: String,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalHyleColors.current
    // The tray is the light half of the pairing, the cap the dark half — in BOTH themes the
    // cap reads as the near-black key from the mockup (derived from tokens, biased to black
    // so it never dissolves into a dark ground).
    val tray = c.inset
    // The cap is pinned near-black in BOTH themes (like the terminal chrome) — the mockup's
    // key is ink-dark on a light tray regardless of app theme; a theme-relative token would
    // dissolve it into a dark ground or grey it out on a light one.
    val cap = Color(0xFF121317)
    val capContent = Color.White
    val trayContent = c.textHigh
    val alphaAll = if (enabled) 1f else 0.45f

    val capOnRight = selected == 1
    val t by animateFloatAsState(targetValue = if (capOnRight) 1f else 0f, label = "hyleCapToggle")

    androidx.compose.foundation.layout.BoxWithConstraints(
        modifier
            .width(120.dp)
            .height(48.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(tray.copy(alpha = alphaAll)),
    ) {
        val trayW = maxWidth
        val capW = trayW * 0.55f
        // Two tap halves (whole control is one toggle; tapping either half selects it).
        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Switch,
                ) { onSelect(0) },
            )
            Box(
                Modifier.weight(1f).fillMaxHeight().clickable(
                    enabled = enabled,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    role = Role.Switch,
                ) { onSelect(1) },
            )
        }

        // The sliding cap: 55% of the tray, outer corners on the tray radius, inner edge
        // slanted (top overhangs toward the centre), fold hook at top-right.
        val density = LocalDensity.current
        val travel = with(density) { (trayW - capW).toPx() }
        val innerEdgeOnRight = !capOnRight // cap left -> slant faces right; cap right -> faces left
        Box(
            Modifier
                .offset { androidx.compose.ui.unit.IntOffset((travel * t).toInt(), 0) }
                .width(capW)
                .fillMaxHeight()
                .clip(HyleCapShape(innerEdgeOnRight = innerEdgeOnRight))
                .background(cap.copy(alpha = alphaAll))
                .drawWithContent {
                    drawContent()
                    // Fold hook: a small quarter-hook + dot at the cap's top-right, as drawn
                    // in the mockup. Kept inside the cap bounds.
                    val s = size.height
                    val stroke = s * 0.05f
                    val hookR = s * 0.13f
                    // The TOP edge is the cap's wide edge in both states, so the hook hugs the
                    // top-right corner directly — no slant compensation.
                    val cx = size.width - s * 0.20f
                    val cy = s * 0.16f
                    drawPath(
                        Path().apply {
                            moveTo(cx - hookR, cy)
                            quadraticBezierTo(cx + hookR * 0.9f, cy - hookR * 0.15f, cx + hookR * 0.55f, cy + hookR * 1.15f)
                        },
                        color = capContent.copy(alpha = 0.95f * alphaAll),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                    drawCircle(
                        color = capContent.copy(alpha = 0.95f * alphaAll),
                        radius = stroke * 0.62f,
                        center = Offset(cx + hookR * 0.55f, cy + hookR * 1.8f),
                    )
                },
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (capOnRight) optionB else optionA,
                color = capContent.copy(alpha = alphaAll),
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
        }

        // The uncovered option, sitting directly on the tray.
        Box(
            Modifier
                .align(if (capOnRight) Alignment.CenterStart else Alignment.CenterEnd)
                .width(trayW - capW)
                .fillMaxHeight(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                if (capOnRight) optionA else optionB,
                color = trayContent.copy(alpha = alphaAll),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

/**
 * The cap's silhouette: big radius on the two OUTER corners (matching the tray), a small
 * radius where the slanted inner edge meets top/bottom, and the inner edge leaning at
 * [HyleSlant] with its TOP overhanging toward the tray centre — exactly the mockup's key.
 */
class HyleCapShape(private val innerEdgeOnRight: Boolean, private val outerCornerDp: Float = 16f) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val r = with(density) { outerCornerDp.dp.toPx() }
        val slant = size.height * HyleSlant
        val path = Path()
        if (innerEdgeOnRight) {
            // Cap on the LEFT: rounded left corners, slanted right edge (top wider).
            path.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f, top = 0f, right = size.width, bottom = size.height,
                    topLeftCornerRadius = CornerRadius(r), bottomLeftCornerRadius = CornerRadius(r),
                    topRightCornerRadius = CornerRadius(r * 0.35f), bottomRightCornerRadius = CornerRadius(r * 0.35f),
                ),
            )
            // Carve the slant: keep top-right at full width, pull bottom-right in by `slant`.
            val carve = Path().apply {
                moveTo(size.width, 0f)
                lineTo(size.width, size.height)
                lineTo(size.width - slant, size.height)
                close()
            }
            path.op(path, carve, androidx.compose.ui.graphics.PathOperation.Difference)
        } else {
            // Cap on the RIGHT: rounded right corners, slanted left edge (top overhangs left).
            path.addRoundRect(
                androidx.compose.ui.geometry.RoundRect(
                    left = 0f, top = 0f, right = size.width, bottom = size.height,
                    topLeftCornerRadius = CornerRadius(r * 0.35f), bottomLeftCornerRadius = CornerRadius(r * 0.35f),
                    topRightCornerRadius = CornerRadius(r), bottomRightCornerRadius = CornerRadius(r),
                ),
            )
            val carve = Path().apply {
                moveTo(0f, 0f)
                lineTo(0f, size.height)
                lineTo(slant, size.height)
                close()
            }
            path.op(path, carve, androidx.compose.ui.graphics.PathOperation.Difference)
        }
        return Outline.Generic(path)
    }
}
