package dev.aarso.hyle.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontFamily
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
) {
    val shape = RoundedCornerShape(6.dp)
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
            modifier = Modifier.fillMaxWidth(),
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
