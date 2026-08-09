package dev.aarso.hyle.component

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * What the panel under the picker can actually reproduce. The picker's pipeline stores
 * 8-bit sRGB regardless, so SRGB vs WIDE decides which side of the honesty warning fires:
 * on an sRGB panel the *display* is the ceiling; on a wide-gamut panel the *stored value*
 * is (the screen could show colours the picker cannot even express).
 */
enum class DisplayGamut { SRGB, WIDE, UNKNOWN }

/**
 * The display facts the picker needs to stay honest, supplied by the host (or by
 * [rememberHyleDisplayContext]'s best-effort detection). Pure data — no Android types —
 * so the geometry that depends on it stays JVM-testable.
 *
 * [renditionAltered] is true when a system display mode (Night Light, reading mode, a
 * vendor "vivid"/"adaptive" screen mode) is shifting every colour on screen, i.e. the
 * swatch the user is looking at is NOT the colour being stored. [alteredReason] is the
 * human-readable cause for the warning row.
 */
data class HyleDisplayContext(
    val gamut: DisplayGamut = DisplayGamut.UNKNOWN,
    val renditionAltered: Boolean = false,
    val alteredReason: String? = null,
) {
    companion object {
        val Unknown = HyleDisplayContext()
    }
}

/**
 * Geometry for the colour-space shapes: the hue/saturation wheel (HSV pane) and the
 * inset fraction shared with the RGB square. Pure functions so `:hyle:test` can pin the
 * mapping down on the JVM — the Canvas code in [HyleColorPicker] is only a projection of
 * these numbers onto pixels.
 *
 * Gamut honesty: the drawn model space (dotted outline, radius/extent 1.0) is always
 * larger than the coloured shape (solid, [solidFraction] of it). The fraction is a
 * *schematic* proportion — a truthful "strict subset", not a colorimetrically measured
 * per-hue boundary (that needs a CMS; see the KDoc on [HyleColorPicker]).
 */
object ColorSpaceGeometry {

    /** Solid-shape share of the full drawn space, per what the panel reproduces. */
    fun solidFraction(gamut: DisplayGamut): Float = when (gamut) {
        DisplayGamut.WIDE -> 0.88f
        DisplayGamut.SRGB, DisplayGamut.UNKNOWN -> 0.78f
    }

    /** Hue in degrees [0, 360) + saturation [0, 1] → unit-circle offset (y screen-down). */
    fun wheelPosition(hueDeg: Float, sat: Float): Pair<Float, Float> {
        val rad = Math.toRadians(hueDeg.toDouble())
        val r = sat.coerceIn(0f, 1f)
        return (r * cos(rad).toFloat()) to (r * sin(rad).toFloat())
    }

    /**
     * Inverse of [wheelPosition]: an offset from the wheel centre (px, y screen-down) →
     * (hueDeg in [0, 360), sat in [0, 1]). [solidRadiusPx] is the coloured disc's radius;
     * touches in the dotted annulus or beyond clamp to its rim — the picker never emits a
     * colour it cannot show.
     */
    fun wheelHit(dxPx: Float, dyPx: Float, solidRadiusPx: Float): Pair<Float, Float> {
        val dist = sqrt(dxPx * dxPx + dyPx * dyPx)
        val sat = if (solidRadiusPx <= 0f) 0f else min(dist / solidRadiusPx, 1f)
        var deg = Math.toDegrees(atan2(dyPx.toDouble(), dxPx.toDouble())).toFloat()
        if (deg < 0f) deg += 360f
        return (deg % 360f) to sat
    }
}
