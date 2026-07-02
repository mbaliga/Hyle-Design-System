package dev.aarso.hyle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guards the canonical provenance idiom + its colour-blind-safe invariant (WCAG 1.4.1).
 */
class ProvenanceTest {

    @Test
    fun `on-device is warm radium, cloud is cold cyan`() {
        assertEquals(RadiantHues.RADIUM, Provenance.OnDevice.hue)
        assertEquals(RadiantHues.COLD_CYAN, Provenance.Cloud.hue)
    }

    @Test
    fun `provenance is colour-blind-safe — hue is never the sole differentiator`() {
        // Any two distinct provenances must differ by the non-colour glyph channel too,
        // so the signal survives colour-blindness and greyscale.
        val all = Provenance.all
        for (a in all) for (b in all) {
            if (a !== b) {
                assertNotEquals("hue must differ", a.hue, b.hue)
                assertNotEquals("glyph (non-colour channel) must differ", a.glyph, b.glyph)
            }
        }
    }

    @Test
    fun `both sources emit on the watched breath at their own hue`() {
        for (p in Provenance.all) {
            val f = p.finish
            assertTrue("expected a Radiant finish", f is Finish.Radiant)
            f as Finish.Radiant
            assertEquals(Pulse.WATCHED, f.pulse)
            assertEquals(p.hue, f.tint)
        }
    }
}
