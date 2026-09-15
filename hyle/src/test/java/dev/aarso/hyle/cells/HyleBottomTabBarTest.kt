package dev.aarso.hyle.cells

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class HyleBottomTabBarTest {
    @Test fun `wide layouts retain every file-tab label`() {
        assertEquals(HyleFileTabStage.FULL, HyleFileTabLayout.stage(380.dp))
    }

    @Test fun `phone and IME layouts retain only the active label`() {
        assertEquals(HyleFileTabStage.SELECTED_LABEL, HyleFileTabLayout.stage(379.dp))
        assertEquals(HyleFileTabStage.SELECTED_LABEL, HyleFileTabLayout.stage(210.dp))
    }

    @Test fun `narrow layouts preserve every tab as an icon`() {
        assertEquals(HyleFileTabStage.ICONS_ONLY, HyleFileTabLayout.stage(209.dp))
    }
}
