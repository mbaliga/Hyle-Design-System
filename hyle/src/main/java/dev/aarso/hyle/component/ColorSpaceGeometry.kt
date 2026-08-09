package dev.aarso.hyle.component

import kotlin.math.PI
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

    /** Angle of a touch around the ring centre, degrees in [0, 360), y screen-down. */
    fun ringAngle(dxPx: Float, dyPx: Float): Float {
        var deg = Math.toDegrees(atan2(dyPx.toDouble(), dxPx.toDouble())).toFloat()
        if (deg < 0f) deg += 360f
        return deg % 360f
    }
}

// ── The live 3D model of the space (the kit picker's third element, ported) ──────────
//
// A point in the model's own unit space, y UP (the projection flips it to screen-down).
// Everything here is plain arithmetic — the Compose canvas only maps the results to
// pixels — so the rotation/projection/shape maths carries JVM tests like the wheel does.

data class SpacePoint(val x: Float, val y: Float, val z: Float)

object SpaceModelGeometry {

    /** Yaw about the +y (up) axis, then pitch about the +x axis. Degrees. */
    fun rotated(p: SpacePoint, yawDeg: Float, pitchDeg: Float): SpacePoint {
        val ya = (yawDeg * PI / 180.0)
        val cy = cos(ya).toFloat(); val sy = sin(ya).toFloat()
        val x1 = p.x * cy + p.z * sy
        val z1 = -p.x * sy + p.z * cy
        val pa = (pitchDeg * PI / 180.0)
        val cp = cos(pa).toFloat(); val sp = sin(pa).toFloat()
        val y2 = p.y * cp - z1 * sp
        val z2 = p.y * sp + z1 * cp
        return SpacePoint(x1, y2, z2)
    }

    /**
     * Orthographic projection to screen offsets from the canvas centre: x right, y DOWN
     * (model y is up, hence the sign flip). Returns (dx, dy, depth) — depth grows toward
     * the viewer, for painter's-algorithm sorting.
     */
    fun projected(p: SpacePoint, scalePx: Float): Triple<Float, Float, Float> =
        Triple(p.x * scalePx, -p.y * scalePx, p.z)

    /**
     * The HSV cone in unit space: apex (V = 0) at the bottom, the V = 1 disc on top,
     * radius s·v·0.5, height 1 centred on the origin. [scale] shrinks the whole shape —
     * the gamut-honest solid cloud passes the display fraction here; the dotted full-space
     * silhouette passes 1.
     */
    fun conePoint(hueDeg: Float, s: Float, v: Float, scale: Float = 1f): SpacePoint {
        val rad = Math.toRadians(hueDeg.toDouble())
        val r = s * v * 0.5f * scale
        return SpacePoint(
            (r * cos(rad)).toFloat(),
            (v - 0.5f) * scale,
            (r * sin(rad)).toFloat(),
        )
    }

    /** The RGB cube in unit space: each channel 0..1 mapped to −0.5..+0.5, then scaled. */
    fun cubePoint(r: Float, g: Float, b: Float, scale: Float = 1f): SpacePoint =
        SpacePoint((r - 0.5f) * scale, (g - 0.5f) * scale, (b - 0.5f) * scale)

    /** The cube's 12 edges as (corner, corner) pairs, for the dotted full-space outline. */
    fun cubeEdges(scale: Float = 1f): List<Pair<SpacePoint, SpacePoint>> {
        val c = (0..7).map { i ->
            cubePoint(if (i and 1 != 0) 1f else 0f, if (i and 2 != 0) 1f else 0f, if (i and 4 != 0) 1f else 0f, scale)
        }
        val idx = listOf(0 to 1, 0 to 2, 1 to 3, 2 to 3, 4 to 5, 4 to 6, 5 to 7, 6 to 7, 0 to 4, 1 to 5, 2 to 6, 3 to 7)
        return idx.map { (a, b) -> c[a] to c[b] }
    }
}
