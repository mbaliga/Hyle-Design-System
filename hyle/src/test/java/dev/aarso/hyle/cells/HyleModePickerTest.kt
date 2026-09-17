package dev.aarso.hyle.cells

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [HyleModePickerLayout] is the pure decision logic behind [HyleModePicker] — no Compose render
 * involved, per this repo's "render is owner-verified on device, the decision logic is
 * JVM-tested" convention (see [HyleFileTabLayout], `HyleFieldStateTest`).
 */
class HyleModePickerTest {

    // ── stacked(): the side-by-side / stacked breakpoint ──────────────────────────────

    @Test fun `wide containers keep options side by side`() {
        assertFalse(HyleModePickerLayout.stacked(360.dp))
        assertFalse(HyleModePickerLayout.stacked(500.dp))
    }

    @Test fun `narrow containers stack options vertically`() {
        assertTrue(HyleModePickerLayout.stacked(359.dp))
        assertTrue(HyleModePickerLayout.stacked(200.dp))
    }

    // ── glyphForIndex(): cycles so 2+ options always render a motif, never nothing ────

    @Test fun `first option draws the list motif, second the card motif`() {
        assertEquals(HyleModePickerGlyph.LIST, HyleModePickerLayout.glyphForIndex(0))
        assertEquals(HyleModePickerGlyph.CARD, HyleModePickerLayout.glyphForIndex(1))
    }

    @Test fun `a third-plus option repeats the two-motif cycle rather than drawing nothing`() {
        assertEquals(HyleModePickerGlyph.LIST, HyleModePickerLayout.glyphForIndex(2))
        assertEquals(HyleModePickerGlyph.CARD, HyleModePickerLayout.glyphForIndex(3))
        assertEquals(HyleModePickerGlyph.LIST, HyleModePickerLayout.glyphForIndex(4))
    }

    // ── HyleModeOption: generic structure, no app copy baked in ───────────────────────

    @Test fun `badge defaults to null so a caller with no tag doesn't have to pass one`() {
        val option = HyleModeOption(id = "regular", title = "Regular", description = "…")
        assertNull(option.badge)
    }

    @Test fun `two callers can supply entirely different ids and copy through the same shape`() {
        // Hyle ships structure, not app copy — this is what makes the component reusable
        // across the constellation rather than hard-coded to Fonebrew's Regular/asoc pair.
        val fonebrewAsoc = HyleModeOption(
            id = "asoc",
            title = "asoc",
            description = "Enhanced interaction style, still being shaped.",
            badge = "experimental",
        )
        val otherAppOption = HyleModeOption(id = "focus", title = "Focus", description = "One task at a time.")
        assertEquals("experimental", fonebrewAsoc.badge)
        assertNull(otherAppOption.badge)
    }
}
