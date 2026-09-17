package dev.aarso.hyle.cells

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * The lens — see `docs/LENS.md`.
 *
 * Hyle has three strata and only three: a grainy **substrate** that carries
 * background activity through motion, a smooth **lens** that blurs it, and the
 * **controls** on top. Dialog, sheet and surface are not three components; they
 * are this one material at three [Extent]s.
 *
 * The governing sentence is from `docs/PHILOSOPHY.md` — *the surface stays fixed
 * while depth changes*. A lens does not move, tint, or animate its edge to signal
 * anything. The single degree of freedom is **how deeply it blurs what is
 * beneath**, and that is enough.
 *
 * There is deliberately **no scrim parameter**: the blur *is* the scrim. Dimming
 * would be language ("this is disabled") painted over material that already says
 * it.
 */
enum class Extent(internal val blur: Dp) {
    /** At rest, sized to its own content. The grain still reads through. */
    Rest(6.dp),

    /** Grown from an edge — what a bottom sheet was. The ground recedes, not hidden. */
    Edge(16.dp),

    /** Grown from the control that asked the question — what a dialog was. */
    Focus(26.dp),

    /**
     * A blocking operation. Deepest of all: the substrate is out of reach.
     * Motion beneath **continues**, because computation genuinely is happening —
     * that is the whole signal. No spinner, no label.
     */
    Sealed(34.dp),
}

/**
 * Draws [substrate] blurred to [extent]'s depth, then the lens over it, then
 * [content] — the controls, which are the only interactive stratum.
 *
 * Blur radius animates; the lens itself does not move. Callers change [extent],
 * never the geometry, which is what keeps "the surface stays fixed" true in code
 * and not just in prose.
 *
 * At [Extent.Sealed] the caller is responsible for passing inert controls —
 * `enabled = false` cells, which go Reflective (no emission). The lens does not
 * cover them with something that *says* they are disabled; the depth already did.
 */
@Composable
fun HyleLens(
    extent: Extent,
    modifier: Modifier = Modifier,
    corner: Dp = 14.dp,
    substrate: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    val c = LocalHyleColors.current
    val shape = RoundedCornerShape(corner)
    val blur by animateDpAsState(extent.blur, tween(260), label = "hyleLensBlur")
    // The lens body thickens very slightly with depth, so a deeper lens reads as
    // further from the ground rather than merely blurrier.
    val veil by animateFloatAsState(
        targetValue = when (extent) {
            Extent.Rest -> 0.42f
            Extent.Edge -> 0.52f
            Extent.Focus -> 0.60f
            Extent.Sealed -> 0.68f
        },
        animationSpec = tween(260),
        label = "hyleLensVeil",
    )
    Box(modifier.clip(shape)) {
        // Substrate: grainy, never interactive, blurred to the lens's depth.
        Box(Modifier.matchParentSize().blur(blur), content = substrate)
        // Lens: a smooth body plus a single specular rim along the top edge. The rim
        // is Reflective in the Finish sense — it catches light, it does not emit.
        Box(
            Modifier
                .matchParentSize()
                .background(c.raised.copy(alpha = veil), shape)
                .drawWithContent {
                    drawContent()
                    val rim = 3.dp.toPx()
                    drawRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(Color.White.copy(alpha = 0.10f), Color.Transparent),
                            startY = 0f,
                            endY = rim,
                        ),
                        size = Size(size.width, rim),
                    )
                },
        )
        // Controls.
        content()
    }
}

/** Convenience: the lens's own blur depth, for callers animating alongside it. */
val Extent.depth: Dp get() = blur
