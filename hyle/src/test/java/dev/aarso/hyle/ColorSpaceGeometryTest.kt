package dev.aarso.hyle

import dev.aarso.hyle.component.ColorSpaceGeometry
import dev.aarso.hyle.component.DisplayGamut
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the wheel geometry the HSV pane projects onto pixels, and the honesty invariant
 * behind the dotted outline: the coloured shape is a strict subset of the drawn model
 * space on every gamut, and a touch outside it clamps to its rim rather than emitting a
 * colour the pipeline cannot show.
 */
class ColorSpaceGeometryTest {

    private val eps = 1e-4f

    // ── solidFraction: the subset claim itself ──────────────────────────────────────

    @Test
    fun `solid shape is a strict subset of the model space on every gamut`() {
        DisplayGamut.entries.forEach { gamut ->
            val f = ColorSpaceGeometry.solidFraction(gamut)
            assertTrue("fraction for $gamut must be in (0, 1)", f > 0f && f < 1f)
        }
    }

    @Test
    fun `wide gamut shows more than sRGB but still not everything`() {
        val wide = ColorSpaceGeometry.solidFraction(DisplayGamut.WIDE)
        val srgb = ColorSpaceGeometry.solidFraction(DisplayGamut.SRGB)
        assertTrue(wide > srgb)
        assertTrue(wide < 1f)
    }

    @Test
    fun `unknown gamut is treated as the narrow case, never optimistically`() {
        assertEquals(
            ColorSpaceGeometry.solidFraction(DisplayGamut.SRGB),
            ColorSpaceGeometry.solidFraction(DisplayGamut.UNKNOWN),
            eps,
        )
    }

    // ── wheelPosition: hue/sat → unit offset ────────────────────────────────────────

    @Test
    fun `cardinal hues land on the cardinal axes, y screen-down`() {
        val (x0, y0) = ColorSpaceGeometry.wheelPosition(0f, 1f)
        assertEquals(1f, x0, eps); assertEquals(0f, y0, eps)
        val (x90, y90) = ColorSpaceGeometry.wheelPosition(90f, 1f)
        assertEquals(0f, x90, eps); assertEquals(1f, y90, eps)
        val (x180, y180) = ColorSpaceGeometry.wheelPosition(180f, 1f)
        assertEquals(-1f, x180, eps); assertEquals(0f, y180, eps)
        val (x270, y270) = ColorSpaceGeometry.wheelPosition(270f, 1f)
        assertEquals(0f, x270, eps); assertEquals(-1f, y270, eps)
    }

    @Test
    fun `zero saturation collapses to the centre whatever the hue`() {
        listOf(0f, 47f, 180f, 313f).forEach { hue ->
            val (x, y) = ColorSpaceGeometry.wheelPosition(hue, 0f)
            assertEquals(0f, x, eps)
            assertEquals(0f, y, eps)
        }
    }

    @Test
    fun `saturation beyond 1 is clamped on the way out`() {
        val (x, y) = ColorSpaceGeometry.wheelPosition(0f, 5f)
        assertEquals(1f, x, eps); assertEquals(0f, y, eps)
    }

    // ── wheelHit: pixels → hue/sat, clamped to the solid disc ───────────────────────

    @Test
    fun `hit and position round-trip through the solid radius`() {
        val solid = 100f
        listOf(30f to 0.4f, 200f to 1f, 359f to 0.05f).forEach { (hue, sat) ->
            val (ux, uy) = ColorSpaceGeometry.wheelPosition(hue, sat)
            val (h, s) = ColorSpaceGeometry.wheelHit(ux * solid, uy * solid, solid)
            assertEquals(hue, h, 0.01f)
            assertEquals(sat, s, 0.001f)
        }
    }

    @Test
    fun `a touch in the dotted annulus clamps to the rim of the shown colours`() {
        val solid = 100f
        val (_, s) = ColorSpaceGeometry.wheelHit(160f, 0f, solid) // beyond solid, inside dotted
        assertEquals(1f, s, eps)
    }

    @Test
    fun `hue is always reported in 0 until 360`() {
        val (below, _) = ColorSpaceGeometry.wheelHit(0f, -50f, 100f) // straight up = 270°
        assertEquals(270f, below, 0.01f)
        val (zero, _) = ColorSpaceGeometry.wheelHit(50f, 0f, 100f)
        assertEquals(0f, zero, 0.01f)
        assertTrue(below < 360f && below >= 0f)
    }

    @Test
    fun `degenerate solid radius yields the calm answer, not NaN`() {
        val (h, s) = ColorSpaceGeometry.wheelHit(10f, 10f, 0f)
        assertEquals(0f, s, eps)
        assertTrue(h in 0f..360f)
    }
}
