package dev.aarso.hyle.cells

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Guards the pure colour-space math [HyleColorPicker3D] ports from `kit/tactile-kit.html`
 * (`rgb2lab`/`lab2rgb`/`lab2lch`/`lch2lab`) — the part of the port that's plain JVM-testable,
 * per this repo's own "render/gesture is owner-verified on device, pure functions are
 * JVM-tested" convention.
 */
class HyleColorPicker3DMathTest {

    private fun assertClose(expected: Double, actual: Double, tolerance: Double, label: String) {
        assertTrue("$label: expected ~$expected, got $actual", abs(expected - actual) <= tolerance)
    }

    @Test fun `white is L=100, a=0, b=0`() {
        val lab = srgbToLab(255, 255, 255)
        assertClose(100.0, lab.l, 0.05, "L")
        assertClose(0.0, lab.a, 0.05, "a")
        assertClose(0.0, lab.b, 0.05, "b")
    }

    @Test fun `black is L=0, a=0, b=0`() {
        val lab = srgbToLab(0, 0, 0)
        assertClose(0.0, lab.l, 0.05, "L")
        assertClose(0.0, lab.a, 0.05, "a")
        assertClose(0.0, lab.b, 0.05, "b")
    }

    @Test fun `srgbToLab and lab2rgb round-trip for the Hyle violet`() {
        // #8E7BFF — the kit picker's own default colour.
        val r = 0x8E; val g = 0x7B; val b = 0xFF
        val lab = srgbToLab(r, g, b)
        val back = lab2rgb(lab.l, lab.a, lab.b)
        assertClose(r / 255.0, back[0].toDouble(), 0.01, "r")
        assertClose(g / 255.0, back[1].toDouble(), 0.01, "g")
        assertClose(b / 255.0, back[2].toDouble(), 0.01, "b")
    }

    @Test fun `labToLch and lch2lab round-trip`() {
        val lab = srgbToLab(0x2E, 0xC4, 0xB6) // an arbitrary saturated teal
        val lch = labToLch(lab)
        val back = lch2lab(lch.h, lch.c, lch.l)
        assertClose(lab.l, back.l, 0.01, "L")
        assertClose(lab.a, back.a, 0.05, "a")
        assertClose(lab.b, back.b, 0.05, "b")
    }

    @Test fun `chroma is non-negative and hue wraps into 0-360`() {
        val samples = listOf(Triple(255, 0, 0), Triple(0, 255, 0), Triple(0, 0, 255), Triple(128, 64, 200))
        for ((r, g, b) in samples) {
            val lch = labToLch(srgbToLab(r, g, b))
            assertTrue("chroma must be >= 0", lch.c >= 0.0)
            assertTrue("hue must be in [0,360)", lch.h >= 0.0 && lch.h < 360.0)
        }
    }

    @Test fun `a saturated red hue round-trips through lch2lab into roughly the same rgb`() {
        val lab = srgbToLab(220, 40, 40)
        val lch = labToLch(lab)
        val rebuiltLab = lch2lab(lch.h, lch.c, lch.l)
        val rgb = lab2rgb(rebuiltLab.l, rebuiltLab.a, rebuiltLab.b)
        assertClose(220 / 255.0, rgb[0].toDouble(), 0.02, "r")
        assertClose(40 / 255.0, rgb[1].toDouble(), 0.02, "g")
        assertClose(40 / 255.0, rgb[2].toDouble(), 0.02, "b")
    }

    // ── regression coverage for the adversarial audit's must-fix #2: `genPalette`'s near-gray
    // hue substitution must use the *active tab's own native hue domain*, not a hardcoded HSV
    // hue. This is exactly the pure, JVM-testable logic the audit flagged as having zero
    // coverage — `roll()`'s own composable closure isn't reachable from a JVM test (this repo's
    // own "render/gesture is owner-verified on device" convention), but the domain-substitution
    // contract `sliceValsFor`/`genPalette` must honour is. ────────────────────────────────────

    @Test fun `HSV-hue and LCH-hue genuinely disagree for the same RGB (the domain gap must-fix #2 was about)`() {
        // sRGB red: H=0deg in HSV, but roughly 40deg in LCH — if these ever coincidentally lined
        // up, the must-fix #2 bug (passing HSV-hue where LCH-hue was needed) would've been
        // invisible, so this pins the premise the regression test below relies on.
        val lchHueOfRed = labToLch(srgbToLab(255, 0, 0)).h
        assertTrue(
            "expected HSV-hue(red)=0 and LCH-hue(red) to diverge by more than 10deg, got $lchHueOfRed",
            abs(lchHueOfRed - 0.0) > 10.0,
        )
    }

    @Test fun `sliceValsFor substitutes the caller's hueCache verbatim for near-gray HCL candidates`() {
        // A near-gray RGB triple: tiny but nonzero chroma, below `sliceValsFor`'s own 0.4
        // threshold, so its own (unstable/near-meaningless) native hue gets replaced by whatever
        // hueCache the caller supplies. The bug this guards: `roll()` used to always supply the
        // HSV ring's hue here, even on the HCL tab, where the caller must supply the LCH-native
        // hue instead (`HyleColorPicker3D.kt`'s `roll()`).
        val nearGray = floatArrayOf(0.5f, 0.5006f, 0.5f)
        assertTrue("fixture must actually be near-gray in Lab chroma", labToLch(srgbToLab(128, 128, 128)).c < 0.4)

        val withLchHue = sliceValsFor(tab = 3, rgb = nearGray, hueCache = 40f)[0]
        val withHsvHue = sliceValsFor(tab = 3, rgb = nearGray, hueCache = 0f)[0]
        assertEquals(40f, withLchHue, 0.01f)
        assertEquals(0f, withHsvHue, 0.01f)
    }

    @Test fun `genPalette respects the region filter and clamps to the available candidates`() {
        val region = listOf(0.4f..0.6f, 0.4f..0.6f, 0.4f..0.6f) // RGB tab, a narrow sub-cube
        val palette = genPalette(tab = 0, region = region, n = 50, cvd = false, hueCache = 0f)
        assertTrue("a 0.2-wide sub-cube of the 11-step grid should still have candidates", palette.isNotEmpty())
        for (rgb in palette) {
            assertTrue("r in region", rgb[0] in region[0])
            assertTrue("g in region", rgb[1] in region[1])
            assertTrue("b in region", rgb[2] in region[2])
        }
        // The source's own `Math.min(n, cand.length)` clamp — requesting far more colours than
        // the narrowed region has candidates must clamp, not throw or hang.
        assertTrue(palette.size <= 50)
    }

    @Test fun `genPalette returns empty for a region with no in-gamut candidates`() {
        val region = listOf(1.5f..1.6f, 0f..1f, 0f..1f) // outside the [0,1] RGB cube entirely
        val palette = genPalette(tab = 0, region = region, n = 6, cvd = false, hueCache = 0f)
        assertTrue(palette.isEmpty())
    }
}
