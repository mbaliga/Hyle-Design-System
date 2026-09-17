package dev.aarso.hyle.cells

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * Lens hosts — the call-site shape of `docs/LENS.md`.
 *
 * These are not new components so much as two ways of *growing the interaction
 * surface*. Both render [HyleLens]; they differ only in where the growth starts
 * and therefore which [Extent] it settles at.
 *
 * The rule these encode, and the reason there is no scrim anywhere below: **at any
 * moment exactly one lens is the interaction surface.** A question does not stack
 * a new plane on top of the app — the surface the user was already looking at
 * grows to hold it, and shrinks back afterwards.
 */

/**
 * A question that owns the whole screen: what an `AlertDialog` was.
 *
 * Grows to [Extent.Focus]. Uses a platform [Dialog] window only to escape the
 * parent's clip bounds and take the back button — visually it is a lens, with no
 * platform scrim (`usePlatformDefaultWidth = false`, transparent window), because
 * the blur is the scrim.
 *
 * @param onDismiss null makes this non-dismissible — the question must be answered.
 */
@Composable
fun HyleFocusLens(
    visible: Boolean,
    onDismiss: (() -> Unit)?,
    modifier: Modifier = Modifier,
    substrate: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    Dialog(
        onDismissRequest = { onDismiss?.invoke() },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = onDismiss != null,
            dismissOnClickOutside = onDismiss != null,
        ),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                // Tapping the ground dismisses, when dismissal is allowed at all.
                .pointerInput(onDismiss) { if (onDismiss != null) detectTapToDismiss(onDismiss) },
            contentAlignment = Alignment.Center,
        ) {
            HyleLens(
                extent = Extent.Focus,
                modifier = modifier.fillMaxWidth().padding(horizontal = 28.dp),
                substrate = { substrate() },
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                        // Swallow taps on the lens itself, so only the ground dismisses.
                        .pointerInput(Unit) {},
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    content = content,
                )
            }
        }
    }
}

/**
 * A set of choices grown from the bottom edge: what a `ModalBottomSheet` was.
 *
 * Settles at [Extent.Edge] — shallower than [HyleFocusLens], because a sheet
 * offers options rather than blocking on an answer, so the ground should recede
 * without disappearing.
 */
@Composable
fun HyleEdgeLens(
    visible: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    substrate: @Composable () -> Unit = {},
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!visible) return
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            Modifier
                .fillMaxSize()
                .pointerInput(Unit) { detectTapToDismiss(onDismiss) },
            contentAlignment = Alignment.BottomCenter,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = expandVertically(tween(240)) + fadeIn(tween(240)),
                exit = shrinkVertically(tween(180)) + fadeOut(tween(180)),
            ) {
                HyleLens(
                    extent = Extent.Edge,
                    modifier = modifier.fillMaxWidth(),
                    corner = 18.dp,
                    substrate = { substrate() },
                ) {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 18.dp, vertical = 16.dp)
                            .pointerInput(Unit) {},
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        content = content,
                    )
                }
            }
        }
    }
}

/**
 * A blocking operation, shown rather than said: the surface goes [Extent.Sealed].
 *
 * This replaces the spinner-and-label treatment, which contradicted the root
 * law ("No status words, no spinners") — see `docs/LENS.md`. What the user sees
 * is the ground pushed out of reach and *still moving*, because the computation
 * genuinely is running. [label] survives only as the accessibility description:
 * a screen-reader user cannot read material, so words are correct in that
 * channel and nowhere else.
 */
@Composable
fun HyleSealedLens(
    label: String,
    modifier: Modifier = Modifier,
    activity: Float = 1f,
) {
    Box(
        modifier
            .fillMaxSize()
            // An empty pointerInput still claims the gesture arena, so nothing beneath
            // ever sees a touch.
            .pointerInput(Unit) {}
            .semantics {
                liveRegion = LiveRegionMode.Assertive
                contentDescription = label
            },
    ) {
        HyleLens(
            extent = Extent.Sealed,
            modifier = Modifier.fillMaxSize(),
            corner = 0.dp,
            substrate = { HyleSubstrate(Modifier.fillMaxSize(), activity = activity) },
        ) {}
    }
}

/** Shared: a tap anywhere on the ground dismisses. */
private suspend fun androidx.compose.ui.input.pointer.PointerInputScope.detectTapToDismiss(
    onDismiss: () -> Unit,
) {
    detectTapGestures { onDismiss() }
}

/**
 * Title + body for a focus lens. Kept as a small helper rather than a parameter
 * list so call sites read as content, and so the type scale stays in one place.
 */
@Composable
fun HyleLensHeading(title: String, body: String? = null) {
    val c = LocalHyleColors.current
    Text(title, style = MaterialTheme.typography.titleMedium, color = c.textHigh)
    if (body != null) {
        Text(body, style = MaterialTheme.typography.bodyMedium, color = c.textMid)
    }
}

/** Trailing action row for a focus lens — confirm last, per platform convention. */
@Composable
fun HyleLensActions(content: @Composable () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.End,
    ) { content() }
}
