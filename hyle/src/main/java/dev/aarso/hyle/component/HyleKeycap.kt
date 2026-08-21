package dev.aarso.hyle.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * HyleKeycap — desktop-class kit §1C: keyboard-key chips for shortcut hints (e.g. a `#`/`*`
 * pair). A rounded light **tray** holds one or more dark **keycaps**, each with a subtle
 * top-right fold/highlight so it reads as a physical key rather than a flat chip. Used to
 * surface desktop-class shortcuts inline (terminal facet, keyboard-visible surfaces — see the
 * spec's adoption map §3).
 */

private val TRAY_SHAPE = RoundedCornerShape(9.dp)
private val KEY_SHAPE = RoundedCornerShape(5.dp)
private val KEY_SIZE = 26.dp
private val FOLD_SIZE = 7.dp

/** A single dark keycap. [emphasized] brightens the fill and text to violet — the mockup's
 *  "emphasis on first vs second key" variant. */
@Composable
fun HyleKeycap(key: String, modifier: Modifier = Modifier, emphasized: Boolean = false) {
    val c = LocalHyleColors.current
    val fill = if (emphasized) c.violetDim else c.ink
    val textColor = if (emphasized) c.violet else c.textHigh
    val fold = (if (emphasized) c.violet else c.textHigh).copy(alpha = 0.22f)
    Box(
        modifier
            .size(KEY_SIZE)
            .clip(KEY_SHAPE)
            .background(fill)
            // The fold: a small triangular highlight at the top-right corner, as if the key's
            // cap catches light along one bevel — the detail that reads "physical key" rather
            // than "flat chip".
            .drawBehind {
                val s = FOLD_SIZE.toPx().coerceAtMost(size.minDimension / 2f)
                val path = Path().apply {
                    moveTo(size.width - s, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width, s)
                    close()
                }
                drawPath(path, fold)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(key, style = MaterialTheme.typography.labelLarge, color = textColor, maxLines = 1)
    }
}

/**
 * The keycap **pair**: a light tray containing two keycaps, e.g. `#` and `*`. [emphasis]
 * selects which key (if either) brightens to violet.
 */
enum class HyleKeycapEmphasis { NONE, FIRST, SECOND }

@Composable
fun HyleKeycapPair(
    firstKey: String,
    secondKey: String,
    modifier: Modifier = Modifier,
    emphasis: HyleKeycapEmphasis = HyleKeycapEmphasis.NONE,
) {
    val c = LocalHyleColors.current
    Row(
        modifier
            .clip(TRAY_SHAPE)
            .background(c.raised, TRAY_SHAPE)
            .padding(5.dp),
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(5.dp),
    ) {
        HyleKeycap(firstKey, emphasized = emphasis == HyleKeycapEmphasis.FIRST)
        HyleKeycap(secondKey, emphasized = emphasis == HyleKeycapEmphasis.SECOND)
    }
}
