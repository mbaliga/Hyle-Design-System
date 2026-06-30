package dev.aarso.hyle.worlds

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aarso.hyle.tokens.HyleTokens

private fun tok(argb: Long) = Color(argb)
private val FIELD = tok(HyleTokens.Color.colorPaletteFieldNear)
private val SURFACE = tok(HyleTokens.Color.colorPaletteFieldRaised)
private val INK = tok(HyleTokens.Color.colorPaletteInkPure)
private val DIM = tok(HyleTokens.Color.colorTextSecondary)
private val FAINT = tok(HyleTokens.Color.colorTextFaint)
private val ACCENT = tok(HyleTokens.Color.colorPaletteAccentViolet)
private val HAIRLINE = tok(HyleTokens.Color.colorBorderHairline)

class WallpaperSettingsActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { SettingsScreen(load = { WorldConfig.load(this) }, save = { WorldConfig.save(this, it) }, onApply = ::applyWallpaper) }
    }

    private fun applyWallpaper() {
        startActivity(
            Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER).putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, HyleWorldsWallpaperService::class.java),
            ),
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsScreen(load: () -> WorldConfig, save: (WorldConfig) -> Unit, onApply: () -> Unit) {
    var cfg by remember { mutableStateOf(load()) }
    fun update(c: WorldConfig) { cfg = c; save(c) }

    Column(
        Modifier
            .fillMaxSize()
            .background(FIELD)
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp),
    ) {
        Text("HYLE WORLDS", color = DIM, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 3.4.sp)
        Text("A Brutalist live wallpaper — monumental forms in coloured haze.", color = DIM, fontSize = 13.sp)

        Label("World")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            SCENES.forEachIndexed { i, name ->
                Chip(name, cfg.scene == i) { update(cfg.copy(scene = i)) }
            }
        }

        Label("Palette")
        FlowRow(horizontalArrangement = Arrangement.spacedBy(7.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Palette.ALL.forEach { p ->
                Chip(p.name, cfg.palette.name == p.name) { update(cfg.copy(palette = p)) }
            }
        }

        Label("Motion")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = cfg.active,
                onCheckedChange = { update(cfg.copy(active = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = INK,
                    checkedTrackColor = ACCENT,
                    uncheckedThumbColor = DIM,
                    uncheckedTrackColor = SURFACE,
                    uncheckedBorderColor = HAIRLINE,
                ),
            )
            Text(if (cfg.active) "  Active" else "  Passive (calm)", color = DIM, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }

        Label("Recursive")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Switch(
                checked = cfg.recursive,
                onCheckedChange = { update(cfg.copy(recursive = it)) },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = INK, checkedTrackColor = ACCENT,
                    uncheckedThumbColor = DIM, uncheckedTrackColor = SURFACE, uncheckedBorderColor = HAIRLINE,
                ),
            )
            Text(if (cfg.recursive) "  Endless field" else "  Single form", color = DIM, fontSize = 13.sp, modifier = Modifier.padding(start = 4.dp))
        }

        Label("Speed")
        Slider(
            value = cfg.speed,
            onValueChange = { update(cfg.copy(speed = it)) },
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(thumbColor = INK, activeTrackColor = ACCENT, inactiveTrackColor = SURFACE),
        )

        Button(
            onClick = onApply,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(containerColor = ACCENT, contentColor = tok(HyleTokens.Color.colorPaletteFieldNear)),
        ) {
            Text("Set as live wallpaper", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(vertical = 4.dp))
        }
        Text("Light only where thinking happens.", color = FAINT, fontSize = 11.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = FAINT, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 2.sp)
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        text,
        color = if (selected) tok(HyleTokens.Color.colorPaletteFieldNear) else DIM,
        fontSize = 13.sp,
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) INK else Color.Transparent, CircleShape)
            .border(1.dp, if (selected) INK else HAIRLINE, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}
