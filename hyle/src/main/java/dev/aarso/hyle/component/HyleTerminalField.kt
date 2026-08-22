package dev.aarso.hyle.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.HyleColors

/**
 * The terminal's input row (owner ask, 2026-08-21: the field the terminal uses belongs in Hyle,
 * not app-local) — a dark, monospace, single-line field styled to sit under a terminal grid:
 * a fill slightly raised from the panel behind it so it still reads as a distinct control,
 * hairline border from the SAME dark Hyle ramp the rest of the terminal chrome uses, accent
 * cursor. Deliberately terminal-idiom rather than the Aeon slant-edge field ([HyleField] /
 * `cells.HyleField`) — a console input imitating a form field would be the wrong register.
 *
 * [colors] should be the dark half of the ramp (`darkHyleColors(...)`) regardless of the app
 * theme — the canvas a terminal sits on is pinned dark. [fg]/[bg] default to the terminal
 * grid's own tones; pass the exact grid constants when mounting next to one so the pair can't
 * drift.
 *
 * **[onSubmit] is what makes Enter mean Enter.** This was a plain single-line [BasicTextField]
 * with no IME action and no key handling, while its call site's placeholder read "Enter to
 * send" — so the newline was stripped and pressing Enter did precisely nothing, on a control
 * whose entire job is sending lines. Passing [onSubmit] puts [ImeAction.Send] on the soft
 * keyboard *and* catches a hardware/Bluetooth Enter, which never routes through the IME action
 * at all. Leave it null for a field that only edits text.
 *
 * The keyboard is also told this is not prose: autocorrect and auto-capitalisation would quietly
 * rewrite `cd /Foo` or `git` into something the shell never receives. Soft-keyboard behaviour is
 * owner-verified — there is no device in this build environment.
 */
@Composable
fun HyleTerminalField(
    value: String,
    onValueChange: (String) -> Unit,
    colors: HyleColors,
    modifier: Modifier = Modifier,
    placeholder: String? = null,
    fg: Color = Color(0xFFE8E8E8),
    bg: Color = Color(0xFF141414),
    onSubmit: (() -> Unit)? = null,
) {
    val shape = RoundedCornerShape(6.dp)
    val submit = onSubmit
    Box(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(bg, shape)
            .border(1.dp, colors.hairline, shape)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = fg, fontFamily = FontFamily.Monospace),
            cursorBrush = SolidColor(colors.violet),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                autoCorrectEnabled = false,
                keyboardType = KeyboardType.Ascii,
                imeAction = if (submit != null) ImeAction.Send else ImeAction.Default,
            ),
            keyboardActions = KeyboardActions(onSend = { submit?.invoke() }),
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (submit != null) {
                        Modifier.onPreviewKeyEvent { event ->
                            val enter = event.key == Key.Enter || event.key == Key.NumPadEnter
                            if (enter && event.type == KeyEventType.KeyDown) { submit(); true } else false
                        }
                    } else {
                        Modifier
                    },
                ),
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            placeholder,
                            style = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
                            color = fg.copy(alpha = 0.4f),
                            maxLines = 1,
                        )
                    }
                    inner()
                }
            },
        )
    }
}
