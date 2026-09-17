package dev.aarso.hyle.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The full state table from docs/design/desktop-class-kit.md §1A, driven purely through
 * [resolveHyleFieldState] / [resolveHyleField] — no Compose/Android involved, so this exercises
 * exactly the same decision logic [HyleField] renders from.
 */
class HyleFieldStateTest {

    // ── resolveHyleFieldState: the full 3-boolean precedence truth table ──────────────

    @Test fun `disabled beats error and focus`() {
        assertEquals(HyleFieldState.DISABLED, resolveHyleFieldState(enabled = false, isError = true, focused = true))
        assertEquals(HyleFieldState.DISABLED, resolveHyleFieldState(enabled = false, isError = true, focused = false))
        assertEquals(HyleFieldState.DISABLED, resolveHyleFieldState(enabled = false, isError = false, focused = true))
        assertEquals(HyleFieldState.DISABLED, resolveHyleFieldState(enabled = false, isError = false, focused = false))
    }

    @Test fun `error beats focus when enabled`() {
        assertEquals(HyleFieldState.ERROR, resolveHyleFieldState(enabled = true, isError = true, focused = true))
        assertEquals(HyleFieldState.ERROR, resolveHyleFieldState(enabled = true, isError = true, focused = false))
    }

    @Test fun `focus alone promotes not-selected to selected`() {
        assertEquals(HyleFieldState.SELECTED, resolveHyleFieldState(enabled = true, isError = false, focused = true))
        assertEquals(HyleFieldState.NOT_SELECTED, resolveHyleFieldState(enabled = true, isError = false, focused = false))
    }

    // ── The seven mockup rows, verbatim ────────────────────────────────────────────────

    @Test fun `row 1 - not selected - grey tick, no border, no notch`() {
        val d = resolveHyleField(enabled = true, isError = false, focused = false, mandatory = false)
        assertEquals(HyleFieldState.NOT_SELECTED, d.state)
        assertEquals(HyleFieldColorRole.GREY, d.tickRole)
        assertFalse(d.tickHasNotch)
        assertFalse(d.borderVisible)
        assertNull(d.borderRole)
        assertFalse(d.asteriskVisible)
        assertNull(d.asteriskRole)
        assertFalse(d.textGhosted)
    }

    @Test fun `row 2 - selected - violet tick, full violet border`() {
        val d = resolveHyleField(enabled = true, isError = false, focused = true, mandatory = false)
        assertEquals(HyleFieldState.SELECTED, d.state)
        assertEquals(HyleFieldColorRole.VIOLET, d.tickRole)
        assertFalse(d.tickHasNotch)
        assertTrue(d.borderVisible)
        assertEquals(HyleFieldColorRole.VIOLET, d.borderRole)
        assertFalse(d.asteriskVisible)
    }

    @Test fun `row 3 - selected and mandatory - adds a violet trailing asterisk`() {
        val d = resolveHyleField(enabled = true, isError = false, focused = true, mandatory = true)
        assertEquals(HyleFieldState.SELECTED, d.state)
        assertEquals(HyleFieldColorRole.VIOLET, d.borderRole)
        assertTrue(d.asteriskVisible)
        assertEquals(HyleFieldColorRole.VIOLET, d.asteriskRole)
    }

    @Test fun `row 4 - error not selected - red notched tick, no border`() {
        val d = resolveHyleField(enabled = true, isError = true, focused = false, mandatory = false)
        assertEquals(HyleFieldState.ERROR, d.state)
        assertEquals(HyleFieldColorRole.RED, d.tickRole)
        assertTrue("error tick always carries the exclamation notch", d.tickHasNotch)
        assertFalse(d.borderVisible)
        assertNull(d.borderRole)
    }

    @Test fun `row 5 - error selected - red notched tick, thin red border`() {
        val d = resolveHyleField(enabled = true, isError = true, focused = true, mandatory = false)
        assertEquals(HyleFieldState.ERROR, d.state)
        assertTrue(d.tickHasNotch)
        assertTrue(d.borderVisible)
        assertEquals(HyleFieldColorRole.RED, d.borderRole)
    }

    @Test fun `row 6 - error and mandatory - red trailing asterisk`() {
        val d = resolveHyleField(enabled = true, isError = true, focused = true, mandatory = true)
        assertEquals(HyleFieldState.ERROR, d.state)
        assertEquals(HyleFieldColorRole.RED, d.borderRole)
        assertTrue(d.asteriskVisible)
        assertEquals(HyleFieldColorRole.RED, d.asteriskRole)
    }

    @Test fun `row 7 - disabled - ghosted grey tick, no border, text ghosted`() {
        val d = resolveHyleField(enabled = false, isError = false, focused = false, mandatory = false)
        assertEquals(HyleFieldState.DISABLED, d.state)
        assertEquals(HyleFieldColorRole.GREY, d.tickRole)
        assertFalse(d.tickHasNotch)
        assertFalse(d.borderVisible)
        assertTrue(d.textGhosted)
    }

    // ── Edge cases beyond the seven headline rows ──────────────────────────────────────

    @Test fun `disabled never borders even if the caller still passes focused`() {
        val d = resolveHyleField(enabled = false, isError = true, focused = true, mandatory = true)
        assertEquals(HyleFieldState.DISABLED, d.state)
        assertFalse(d.borderVisible)
        assertNull(d.borderRole)
    }

    @Test fun `mandatory asterisk is not gated on focus - spec has no 'only while selected' qualifier`() {
        // Not-selected + mandatory isn't one of the seven mockup rows, but the spec's own
        // instruction ("trailing asterisk when mandatory (state-colored)") carries no
        // selection qualifier, so it must still show — grey, matching the resting tick.
        val d = resolveHyleField(enabled = true, isError = false, focused = false, mandatory = true)
        assertTrue(d.asteriskVisible)
        assertEquals(HyleFieldColorRole.GREY, d.asteriskRole)
    }

    @Test fun `asterisk role always mirrors tick role`() {
        listOf(
            resolveHyleField(enabled = true, isError = false, focused = false, mandatory = true),
            resolveHyleField(enabled = true, isError = false, focused = true, mandatory = true),
            resolveHyleField(enabled = true, isError = true, focused = false, mandatory = true),
            resolveHyleField(enabled = true, isError = true, focused = true, mandatory = true),
            resolveHyleField(enabled = false, isError = false, focused = false, mandatory = true),
        ).forEach { d -> assertEquals(d.tickRole, d.asteriskRole) }
    }

    @Test fun `not mandatory never shows an asterisk in any state`() {
        listOf(
            resolveHyleField(enabled = true, isError = false, focused = false, mandatory = false),
            resolveHyleField(enabled = true, isError = false, focused = true, mandatory = false),
            resolveHyleField(enabled = true, isError = true, focused = false, mandatory = false),
            resolveHyleField(enabled = true, isError = true, focused = true, mandatory = false),
            resolveHyleField(enabled = false, isError = false, focused = false, mandatory = false),
        ).forEach { d ->
            assertFalse(d.asteriskVisible)
            assertNull(d.asteriskRole)
        }
    }

    @Test fun `only the error state carries the notch`() {
        assertFalse(resolveHyleField(true, false, false, false).tickHasNotch)
        assertFalse(resolveHyleField(true, false, true, false).tickHasNotch)
        assertFalse(resolveHyleField(false, false, false, false).tickHasNotch)
        assertTrue(resolveHyleField(true, true, false, false).tickHasNotch)
        assertTrue(resolveHyleField(true, true, true, false).tickHasNotch)
    }
}
