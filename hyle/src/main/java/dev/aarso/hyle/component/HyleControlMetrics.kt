package dev.aarso.hyle.component

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The one height every Hyle form control shares: [HyleField] (its minimum height) and the cap
 * toggles ([HyleCapToggle], [HyleOnOffToggle]). A toggle sitting beside a field in a settings
 * row is exactly as tall as the field (owner, 2026-11-19), so there is one constant, not two
 * numbers that have to be kept equal by hand.
 */
val HyleControlHeight: Dp = 52.dp
