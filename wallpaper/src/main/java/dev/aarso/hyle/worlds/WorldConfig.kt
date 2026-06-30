package dev.aarso.hyle.worlds

import android.content.Context
import android.content.SharedPreferences
import dev.aarso.hyle.tokens.HyleTokens

/** RGB tri(0..1), the form the shader's vec3 colour uniforms want. */
typealias Rgb = FloatArray

/** 0xAARRGGBB → linear-ish RGB floats (alpha dropped; the field is opaque). */
fun argbToRgb(argb: Long): Rgb =
    floatArrayOf(
        ((argb shr 16) and 0xFF).toFloat() / 255f,
        ((argb shr 8) and 0xFF).toFloat() / 255f,
        (argb and 0xFF).toFloat() / 255f,
    )

/** Stone / smog / light — kept separate, the brutalist key (see ARCHITECTURE.md). */
data class Palette(
    val name: String,
    val stone: Rgb,
    val smog: Rgb,
    val light: Rgb,
    val glow: Float = 1.2f,
    val atmos: Float = 0.5f,
    val texType: Int = 0, // 0 soil · 3 facet
) {
    companion object {
        /** The default brutalist trio, straight from the Form-World defaults. */
        val STONE = Palette("Stone", argbToRgb(0xFF9B958C), argbToRgb(0xFFCC1A0A), argbToRgb(0xFFFFD2B0))

        /** Token-derived: the design system's material trio with the violet accent as light. */
        val HYLE = Palette(
            "Hyle",
            argbToRgb(HyleTokens.Color.colorPaletteMaterialStone),
            argbToRgb(HyleTokens.Color.colorPaletteMaterialSmog),
            argbToRgb(HyleTokens.Color.colorPaletteAccentViolet),
            glow = 1.1f,
            atmos = 0.55f,
        )

        /** "Dark subject against bright dust" (README recipe). */
        val BLADE_RUNNER = Palette(
            "Blade Runner",
            argbToRgb(0xFF9B958C), argbToRgb(0xFFE0894F), argbToRgb(0xFFFFD2B0),
            glow = 0.45f, atmos = 0.82f,
        )

        /** Faceted, painterly pastels (README recipe). */
        val GRIS = Palette(
            "Gris",
            argbToRgb(0xFFB7A6C6), argbToRgb(0xFF6A7FB0), argbToRgb(0xFFFFE6EC),
            glow = 1.25f, atmos = 0.45f, texType = 3,
        )

        val ALL = listOf(STONE, HYLE, BLADE_RUNNER, GRIS)
        fun byName(n: String?) = ALL.firstOrNull { it.name == n } ?: STONE
    }
}

val SCENES = listOf("Bowl", "Helix", "Towers", "Arch", "Ruins", "Planet")

/** Live-wallpaper configuration, persisted in SharedPreferences. */
data class WorldConfig(
    val scene: Int = 0,
    val palette: Palette = Palette.STONE,
    val active: Boolean = false, // calm by default — Passive ≈ 0.15× motion
    val recursive: Boolean = false,
    val speed: Float = 0.28f,
    val targetFps: Int = 30, // calm + battery; the field is slow by design
) {
    companion object {
        const val PREFS = "hyle_worlds"
        private const val K_SCENE = "scene"
        private const val K_PALETTE = "palette"
        private const val K_ACTIVE = "active"
        private const val K_RECURSIVE = "recursive"
        private const val K_SPEED = "speed"
        private const val K_FPS = "fps"

        fun prefs(ctx: Context): SharedPreferences =
            ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        fun load(ctx: Context): WorldConfig {
            val p = prefs(ctx)
            return WorldConfig(
                scene = p.getInt(K_SCENE, 0).coerceIn(0, SCENES.size - 1),
                palette = Palette.byName(p.getString(K_PALETTE, Palette.STONE.name)),
                active = p.getBoolean(K_ACTIVE, false),
                recursive = p.getBoolean(K_RECURSIVE, false),
                speed = p.getFloat(K_SPEED, 0.28f),
                targetFps = p.getInt(K_FPS, 30),
            )
        }

        fun save(ctx: Context, c: WorldConfig) {
            prefs(ctx).edit().apply {
                putInt(K_SCENE, c.scene)
                putString(K_PALETTE, c.palette.name)
                putBoolean(K_ACTIVE, c.active)
                putBoolean(K_RECURSIVE, c.recursive)
                putFloat(K_SPEED, c.speed)
                putInt(K_FPS, c.targetFps)
                apply()
            }
        }
    }
}
