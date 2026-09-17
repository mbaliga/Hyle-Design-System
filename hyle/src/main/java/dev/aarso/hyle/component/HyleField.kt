package dev.aarso.hyle.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.error
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.HyleColors
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * HyleField — the desktop-class input-field state system (docs/design/desktop-class-kit.md §1A
 * in the core repo). NOT the same component as [dev.aarso.hyle.cells.HyleField] (the Aeon
 * selector: a whole-silhouette slant, [dev.aarso.hyle.cells.HyleField]'s own `FIELD_SLANT_RATIO
 * = 0.25f` geometry, still adopted at ~34 call sites in the host app via its thin
 * `dev.fonebrew.ui.hyle` wrapper) — deliberately kept in a **different package**
 * (`dev.aarso.hyle.component`, alongside [HyleSlant]/[HyleSlantedSlab]) rather than replacing
 * it, so the two coexist without a symbol clash or a breaking migration of every adopted call
 * site. This is the NEW mockup-transcribed design: a plain soft rounded-rect card (not a
 * slanted silhouette) with a slanted **tick** — [HyleSlantedSlab], the leaning-slab shape — as
 * the sole slant, hugging the left edge. Adopting this at existing `cells.HyleField` call sites
 * is a follow-up the spec explicitly defers to core (§3 of the doc); doing so here would touch
 * files outside this submodule.
 *
 * State is resolved from three raw inputs — [resolveHyleFieldState] — into exactly the four
 * enum values the spec names (NOT_SELECTED/SELECTED/ERROR/DISABLED); border visibility is a
 * SEPARATE signal driven by live focus (see [resolveHyleField]), because the spec's mockup
 * table draws error in both an unfocused (no border) and a focused (border) variant while
 * keeping both under the one "Error" tick colour — so state and border can't be collapsed into
 * a single enum value. All of this lives in pure functions with no Compose/Android dependency
 * so the full state table is JVM-unit-tested (`HyleFieldStateTest`).
 *
 * WCAG dual-channel (state is never colour-only): the error tick always carries the
 * exclamation notch in addition to turning red, and the mandatory marker is always a literal
 * `*` glyph, not a colour alone.
 */

/** The three colour channels every field-state signal (tick / border / asterisk) resolves to.
 *  Kept as roles, not raw [Color]s, so pure logic stays theme-agnostic and JVM-testable. */
enum class HyleFieldColorRole { GREY, VIOLET, RED }

/** The four states the spec names verbatim. Mutually exclusive by construction: DISABLED beats
 *  ERROR beats focus — see [resolveHyleFieldState]. */
enum class HyleFieldState { NOT_SELECTED, SELECTED, ERROR, DISABLED }

/** Every visual signal a single field render must show, precomputed and testable in isolation
 *  from the state table in docs/design/desktop-class-kit.md §1A. */
data class HyleFieldDecision(
    val state: HyleFieldState,
    val tickRole: HyleFieldColorRole,
    /** The error tick's exclamation notch — the non-colour half of the error signal. */
    val tickHasNotch: Boolean,
    val borderVisible: Boolean,
    val borderRole: HyleFieldColorRole?,
    val asteriskVisible: Boolean,
    val asteriskRole: HyleFieldColorRole?,
    /** Disabled text renders in a ghosted (placeholder-grey) tone. */
    val textGhosted: Boolean,
)

/**
 * Pure state resolution. Precedence, matching the mockup: a disabled field never reads as
 * errored or focused even if the caller still passes those flags; an invalid field always
 * reads as ERROR regardless of focus (focus only changes whether the border is drawn — see
 * [resolveHyleField]); otherwise focus alone promotes NOT_SELECTED to SELECTED.
 */
fun resolveHyleFieldState(enabled: Boolean, isError: Boolean, focused: Boolean): HyleFieldState = when {
    !enabled -> HyleFieldState.DISABLED
    isError -> HyleFieldState.ERROR
    focused -> HyleFieldState.SELECTED
    else -> HyleFieldState.NOT_SELECTED
}

/**
 * Full decision for one field render. [focused] is read twice on purpose: once (inside
 * [resolveHyleFieldState]) to promote NOT_SELECTED -> SELECTED, and again here to gate the
 * border — the table's "Error, not selected" (no border) vs "Error, selected" (thin red
 * border) rows both resolve to [HyleFieldState.ERROR], so border presence can't be read off
 * the state alone. The mandatory asterisk, by contrast, is NOT state-gated: the spec's own
 * instruction is "trailing asterisk when mandatory (state-colored)" with no "only while
 * selected" qualifier, so it shows in every state the field is mandatory in, tinted to match
 * the tick.
 */
fun resolveHyleField(
    enabled: Boolean,
    isError: Boolean,
    focused: Boolean,
    mandatory: Boolean,
): HyleFieldDecision {
    val state = resolveHyleFieldState(enabled, isError, focused)
    val tickRole = when (state) {
        HyleFieldState.ERROR -> HyleFieldColorRole.RED
        HyleFieldState.SELECTED -> HyleFieldColorRole.VIOLET
        HyleFieldState.NOT_SELECTED, HyleFieldState.DISABLED -> HyleFieldColorRole.GREY
    }
    val tickHasNotch = state == HyleFieldState.ERROR
    val borderVisible = focused && state != HyleFieldState.DISABLED
    val borderRole = if (!borderVisible) null else if (state == HyleFieldState.ERROR) HyleFieldColorRole.RED else HyleFieldColorRole.VIOLET
    val asteriskVisible = mandatory
    val asteriskRole = if (asteriskVisible) tickRole else null
    val textGhosted = state == HyleFieldState.DISABLED
    return HyleFieldDecision(
        state = state,
        tickRole = tickRole,
        tickHasNotch = tickHasNotch,
        borderVisible = borderVisible,
        borderRole = borderRole,
        asteriskVisible = asteriskVisible,
        asteriskRole = asteriskRole,
        textGhosted = textGhosted,
    )
}

private val FIELD_SHAPE_RADIUS = 15.dp // spec: "generously rounded rect (~14-16dp radius)"
private val FIELD_MIN_HEIGHT = 52.dp
private val TICK_WIDTH = 5.dp
private val TICK_CORNER = 2.dp
private val TICK_HEIGHT_SINGLE = 22.dp // "slightly taller than the text line"
private val TICK_NOTCH_GAP = 2.dp
private val TICK_NOTCH_DOT = 5.dp

private fun HyleFieldColorRole.color(c: HyleColors): Color = when (this) {
    HyleFieldColorRole.GREY -> c.textMid
    HyleFieldColorRole.VIOLET -> c.violet
    HyleFieldColorRole.RED -> c.error
}

/** The slanted leading tick — [HyleSlantedSlab], the leaning-slab shape (locked slope
 *  [HyleSlant]), coloured by state. When [hasNotch] the same silhouette splits into a shorter
 *  stem plus a small dot beneath it, so the error state reads as a literal "!" — the
 *  second, non-colour channel for WCAG dual-channel state. */
@Composable
private fun HyleFieldTick(role: HyleFieldColorRole, hasNotch: Boolean, height: Dp) {
    val c = LocalHyleColors.current
    val color = role.color(c)
    val slabShape: Shape = remember { HyleSlantedSlab(TICK_CORNER.value) }
    if (hasNotch) {
        Column(
            Modifier.width(TICK_WIDTH).height(height),
            verticalArrangement = Arrangement.spacedBy(TICK_NOTCH_GAP),
        ) {
            Box(
                Modifier
                    .width(TICK_WIDTH)
                    .weight(1f)
                    .clip(slabShape)
                    .background(color),
            )
            Box(
                Modifier
                    .width(TICK_WIDTH)
                    .height(TICK_NOTCH_DOT)
                    .clip(slabShape)
                    .background(color),
            )
        }
    } else {
        Box(
            Modifier
                .width(TICK_WIDTH)
                .height(height)
                .clip(slabShape)
                .background(color),
        )
    }
}

/**
 * The desktop-class input field. Single-line by default (BasicTextField's `singleLine` mode —
 * no wrap, horizontal scroll; a live-editable field ellipsizing its own caret line has no
 * faithful equivalent in the classic `BasicTextField(value, onValueChange, ...)` overload, so
 * "ellipsis single-line default" is approximated by no-wrap-plus-scroll rather than a literal
 * `…` — flagged as a render-fidelity deviation, owner-verifiable on device); pass
 * `singleLine = false` for the multiline variant (label/content top-aligned instead of
 * centred).
 */
@Composable
fun HyleField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    supportingText: String? = null,
    placeholder: String? = null,
    mandatory: Boolean = false,
    enabled: Boolean = true,
    isError: Boolean = false,
    singleLine: Boolean = true,
    minLines: Int = 1,
    maxLines: Int = if (singleLine) 1 else 5,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
) {
    val c = LocalHyleColors.current
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val decision = resolveHyleField(enabled = enabled, isError = isError, focused = focused, mandatory = mandatory)
    val shape = RoundedCornerShape(FIELD_SHAPE_RADIUS)
    val alignment = if (singleLine) Alignment.CenterVertically else Alignment.Top

    Column(modifier) {
        if (!label.isNullOrBlank()) {
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = c.textMid,
                modifier = Modifier.padding(bottom = 4.dp, start = 2.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .then(
                    // Resting/unfocused/disabled states read as a soft card via shadow rather
                    // than a border; a focused/errored border already carries the emphasis, so
                    // the shadow steps back to avoid double-signalling the same state.
                    if (!decision.borderVisible) Modifier.shadow(2.dp, shape, clip = false) else Modifier,
                )
                .clip(shape)
                .background(c.inset, shape)
                .then(
                    if (decision.borderVisible) {
                        Modifier.border(1.dp, decision.borderRole!!.color(c), shape)
                    } else {
                        Modifier
                    },
                )
                .then(
                    if (isError && !supportingText.isNullOrBlank()) {
                        Modifier.semantics { error(supportingText) }
                    } else {
                        Modifier
                    },
                ),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = FIELD_MIN_HEIGHT)
                    .padding(
                        start = 10.dp,
                        end = if (decision.asteriskVisible) 10.dp else 16.dp,
                        top = 10.dp,
                        bottom = 10.dp,
                    ),
                verticalAlignment = alignment,
            ) {
                HyleFieldTick(
                    role = decision.tickRole,
                    hasNotch = decision.tickHasNotch,
                    height = TICK_HEIGHT_SINGLE,
                )
                Spacer(Modifier.width(10.dp))
                Box(Modifier.weight(1f), contentAlignment = if (singleLine) Alignment.CenterStart else Alignment.TopStart) {
                    if (value.isEmpty() && !placeholder.isNullOrEmpty()) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyLarge,
                            color = c.textMid.copy(alpha = 0.75f),
                            maxLines = maxLines,
                        )
                    }
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        enabled = enabled,
                        singleLine = singleLine,
                        minLines = minLines,
                        maxLines = maxLines,
                        textStyle = MaterialTheme.typography.bodyLarge.copy(
                            color = if (decision.textGhosted) c.textDisabled else c.textHigh,
                        ),
                        cursorBrush = SolidColor(c.violet),
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        interactionSource = interaction,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                if (decision.asteriskVisible) {
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "*",
                        style = MaterialTheme.typography.bodyLarge,
                        color = decision.asteriskRole!!.color(c),
                    )
                }
            }
        }
        if (!supportingText.isNullOrBlank()) {
            Text(
                supportingText,
                style = MaterialTheme.typography.labelSmall,
                color = if (isError) c.error else c.textMid,
                modifier = Modifier.padding(top = 4.dp, start = 2.dp),
            )
        }
    }
}
