package dev.aarso.crashrecovery

import android.graphics.Color

/**
 * Plain platform colours (`@ColorInt Int`) — deliberately not tied to any design system's
 * token type, so a Hyle consumer can pass its Hyle-derived palette and a non-Hyle app
 * (Animalcules, Horizkeeb — see D-L) can pass its own without taking a dependency on
 * anything beyond this module. [Default] is tuned to look considered on its own.
 */
data class CrashRecoveryStyle(
    val background: Int,
    val surface: Int,
    val foreground: Int,
    val muted: Int,
    val accent: Int,
    val danger: Int,
    val traceText: Int,
) : java.io.Serializable {
    companion object {
        val Default = CrashRecoveryStyle(
            background = Color.parseColor("#101216"),
            surface = Color.parseColor("#181B21"),
            foreground = Color.parseColor("#E6E6E6"),
            muted = Color.parseColor("#9AA0A6"),
            accent = Color.parseColor("#7EB5C8"),
            danger = Color.parseColor("#D0786B"),
            traceText = Color.parseColor("#B7BDC4"),
        )
    }
}
