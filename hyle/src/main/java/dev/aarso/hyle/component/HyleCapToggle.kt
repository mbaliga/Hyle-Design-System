package dev.aarso.hyle.component

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asFrameworkPaint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import dev.aarso.hyle.cells.rememberHyleHaptics
import dev.aarso.hyle.theme.Success

/**
 * The Hyle cap toggle — THE Hyle switch. Every on/off and two-way choice in every app is this
 * control, as drawn (owner, 2026-11-19; the Figma "Toggle On" / "Toggle Off" SVGs): a light tray
 * with a cap that covers the active side. The cap is ONE shape that morphs between its two
 * forms — each vertex travels to its clockwise counterpart, so the leaning edge straightens
 * while the straight edge leans in — rather than a shape that slides, mirrors or fades.
 *
 * Two ways to use it:
 *  - [HyleOnOffToggle]: a boolean. The tray stays grey; when ON the black cap
 *    turns green and glows green (owner, 2026-11-19: "It has to turn green and glow green"),
 *    when off it does not. Labels optional (none, or "No"/"Yes").
 *  - [HyleCapToggle]: a choice between two peers (`#` / `*`, "Phone" / "Remote"). Ink cap.
 *
 * Height is [HyleControlHeight] — the same as [HyleField] — and the width follows the design's
 * 639:321 proportion. The tray is the design's fixed light grey in both themes (the cap is the
 * dark half of the pairing), as the previous cap toggle was.
 *
 * Labels are never drawn by colour alone: the same label is dark on the tray and white on the
 * cap, switching exactly at the cap's edge as the cap travels under it (the design's
 * `exclusion` blend, done as two clipped passes so it also works over a green or grey cap).
 *
 * NOT pixel-transcribed (the SVGs can't carry them): the animation duration/easing
 * ([HYLE_CAP_MILLIS], ease-out cubic — read from a screen recording, not from Figma), the glow
 * (colour [Success], radius [GLOW_RADIUS], strength [GLOW_ALPHA] — not in the SVGs), the disabled
 * treatment (45% strength), and the inner edge shadow (a crisp two-step approximation of
 * Figma's blurred one).
 */
const val HYLE_CAP_MILLIS = 180

/** What a half of the tray says. */
sealed interface HyleCapLabel {
    /** Nothing — the empty variant: the cap's position (and, for on/off, its colour) is the state. */
    object None : HyleCapLabel

    /** The design's slanted `#` glyph. */
    object Hash : HyleCapLabel

    /** The design's five-armed rounded `*` glyph. */
    object Star : HyleCapLabel

    /** A short word (≤ ~4 characters fits the tray). */
    data class Text(val value: String) : HyleCapLabel
}

/** [Ink]: a plain near-black cap (a choice between peers). [State]: the cap turns green and glows
 *  green while it is on the right (on); black, no glow when on the left (off). */
enum class HyleCapTone { Ink, State }

/** Labels for [HyleOnOffToggle]. */
enum class HyleOnOffLabels { None, YesNo }

/** Tray width for the toggle height: the design's 639 x 321 proportion. */
val HyleCapToggleWidth = HyleControlHeight * (639f / 321f)

/**
 * A boolean. Tap anywhere to flip it. [onCheckedChange] null draws it display-only (no tap, no
 * role) for a row that is itself the one control.
 */
@Composable
fun HyleOnOffToggle(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    labels: HyleOnOffLabels = HyleOnOffLabels.None,
    description: String? = null,
) {
    val haptics = rememberHyleHaptics()
    val left = if (labels == HyleOnOffLabels.YesNo) HyleCapLabel.Text("No") else HyleCapLabel.None
    val right = if (labels == HyleOnOffLabels.YesNo) HyleCapLabel.Text("Yes") else HyleCapLabel.None
    // Order matters: toggleable first, semantics after, so the description and state description
    // are merged INTO the toggleable's node (one control for a screen reader), not left on a
    // separate outer node.
    Box(
        modifier
            .size(HyleCapToggleWidth, HyleControlHeight)
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        enabled = enabled,
                        role = Role.Switch,
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        onValueChange = { haptics.tap(); onCheckedChange(it) },
                    )
                } else {
                    Modifier
                },
            )
            .semantics {
                if (description != null) contentDescription = description
                stateDescription = if (checked) "On" else "Off"
            },
    ) {
        CapToggleFace(
            toRight = checked,
            tone = HyleCapTone.State,
            left = left,
            right = right,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
        )
    }
}

/**
 * A choice between two peers. [selected] 0 = the cap on the left, 1 = on the right; tapping
 * either half selects it. [onSelect] null draws it display-only.
 */
@Composable
fun HyleCapToggle(
    left: HyleCapLabel,
    right: HyleCapLabel,
    selected: Int,
    onSelect: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: HyleCapTone = HyleCapTone.Ink,
) {
    val haptics = rememberHyleHaptics()
    Box(modifier.size(HyleCapToggleWidth, HyleControlHeight)) {
        CapToggleFace(
            toRight = selected == 1,
            tone = tone,
            left = left,
            right = right,
            enabled = enabled,
            modifier = Modifier.fillMaxSize(),
        )
        if (onSelect != null) {
            Row(Modifier.fillMaxSize()) {
                for (index in 0..1) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable(
                                enabled = enabled,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                role = Role.Switch,
                            ) { haptics.tap(); onSelect(index) },
                    )
                }
            }
        }
    }
}

/** String convenience: `"#"` and `"*"` become the design's glyphs, anything else is text. */
@Composable
fun HyleCapToggle(
    optionA: String,
    optionB: String,
    selected: Int,
    onSelect: ((Int) -> Unit)?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) = HyleCapToggle(
    left = optionA.asCapLabel(),
    right = optionB.asCapLabel(),
    selected = selected,
    onSelect = onSelect,
    modifier = modifier,
    enabled = enabled,
)

private fun String.asCapLabel(): HyleCapLabel = when (this) {
    "#" -> HyleCapLabel.Hash
    "*" -> HyleCapLabel.Star
    "" -> HyleCapLabel.None
    else -> HyleCapLabel.Text(this)
}

// ---- drawing ----------------------------------------------------------------------------

private val Tray = Color(0xFFD9D9D9)
private val GlyphOnTray = Color(0xFF262626) // the design's white-exclusion-over-#D9D9D9
private val GlyphOnCap = Color.White
private val CapInk = Color.Black

/** Design-space centres of the two label slots (the `#` and `*` glyphs' centres in the SVGs). */
private const val LEFT_LABEL_X = 454.6f
private const val RIGHT_LABEL_X = 790.4f
private const val LABEL_Y = 317f
private const val GLOW_RADIUS = 26f // design units
private const val GLOW_ALPHA = 0.9f
private const val TEXT_PX = 118f // design units; a word's height matches the glyphs'

@Composable
private fun CapToggleFace(
    toRight: Boolean,
    tone: HyleCapTone,
    left: HyleCapLabel,
    right: HyleCapLabel,
    enabled: Boolean,
    modifier: Modifier,
) {
    val t by animateFloatAsState(
        targetValue = if (toRight) 1f else 0f,
        animationSpec = tween(HYLE_CAP_MILLIS, easing = EaseOutCubic),
        label = "hyleCapToggle",
    )
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val fontSize = (TEXT_PX / (density.density * density.fontScale)).sp
    val style = remember(fontSize) { TextStyle(fontSize = fontSize, fontWeight = FontWeight.SemiBold) }
    val leftText = remember(left, style) { (left as? HyleCapLabel.Text)?.let { measurer.measure(AnnotatedString(it.value), style) } }
    val rightText = remember(right, style) { (right as? HyleCapLabel.Text)?.let { measurer.measure(AnnotatedString(it.value), style) } }

    Canvas(modifier.graphicsLayer { alpha = if (enabled) 1f else 0.45f }) {
        val s = size.height / HyleCapGeometry.TRAY_H
        withTransform({
            scale(s, s, Offset.Zero)
            translate(-HyleCapGeometry.TRAY_X, -HyleCapGeometry.TRAY_Y)
        }) {
            drawCapToggle(t, tone, left, right, leftText, rightText)
        }
    }
}

private fun DrawScope.drawCapToggle(
    t: Float,
    tone: HyleCapTone,
    left: HyleCapLabel,
    right: HyleCapLabel,
    leftText: TextLayoutResult?,
    rightText: TextLayoutResult?,
) {
    val g = HyleCapGeometry
    val trayRect = Rect(g.TRAY_X, g.TRAY_Y, g.TRAY_X + g.TRAY_W, g.TRAY_Y + g.TRAY_H)
    val trayShape = RoundRect(trayRect, CornerRadius(g.TRAY_R))

    // Tray, with the design's inner edge shadow (top and left) as two crisp steps.
    drawRoundRect(Tray, trayRect.topLeft, trayRect.size, CornerRadius(g.TRAY_R))
    val trayPath = Path().apply { addRoundRect(trayShape) }
    clipPath(trayPath) {
        for ((off, alpha) in listOf(4f to 0.07f, 2f to 0.04f)) {
            val ring = Path().apply {
                fillType = PathFillType.EvenOdd
                addRect(trayRect.inflate(40f))
                addRoundRect(trayShape.translate(Offset(off, off)))
            }
            drawPath(ring, Color.Black.copy(alpha = alpha))
        }
    }

    // The cap: one path, morphed. Ink tone stays black. State tone turns the cap green AND glows
    // green when on (owner, 2026-11-19: "It has to turn green and glow green").
    val on = if (tone == HyleCapTone.State) t else 0f
    val base = androidx.compose.ui.graphics.lerp(CapInk, Success, on)
    val highlight = androidx.compose.ui.graphics.lerp(Color(0xFF565656), Success.copy(alpha = 1f).let { androidx.compose.ui.graphics.lerp(it, Color.White, 0.35f) }, on)
    val capPath = g.morph(g.capLeft, g.capRight, t, close = true)
    if (tone == HyleCapTone.State && t > 0.001f) {
        clipPath(trayPath) {
            drawIntoCanvas { canvas ->
                val paint = Paint().apply {
                    color = Success.copy(alpha = GLOW_ALPHA * t)
                    asFrameworkPaint().maskFilter = BlurMaskFilter(GLOW_RADIUS, BlurMaskFilter.Blur.NORMAL)
                }
                canvas.drawPath(capPath, paint)
            }
        }
    }
    drawPath(
        capPath,
        Brush.linearGradient(
            0f to highlight, 0.048f to base, 1f to base,
            start = Offset(0f, 168.5f), end = Offset(0f, 479f),
        ),
    )
    // The sheen on the cap's right edge, its gradient direction travelling with the cap.
    drawPath(
        g.morph(g.sheenLeft, g.sheenRight, t, close = true),
        Brush.linearGradient(
            0f to Color.White.copy(alpha = 0.34f), 0.1f to Color.Transparent, 1f to Color.Transparent,
            start = Offset(lerpF(651.5f, 954f, t), lerpF(305.5f, 332.225f, t)),
            end = Offset(lerpF(445.045f, 683.251f, t), lerpF(359.673f, 331.739f, t)),
        ),
    )
    // The registration hook and its rivet.
    drawPath(
        g.morph(g.hookLeft, g.hookRight, t, close = false),
        Color.White,
        style = Stroke(width = 8f, cap = StrokeCap.Round),
    )
    drawCircle(Color.White, radius = 4f, center = Offset(lerpF(605.143f, 928f, t) + 4f, 238f))

    // Labels: dark where they sit on the tray, white where the cap covers them.
    fun paintLabels(color: Color) {
        drawLabel(left, leftText, LEFT_LABEL_X, color)
        drawLabel(right, rightText, RIGHT_LABEL_X, color)
    }
    clipPath(capPath, ClipOp.Difference) { paintLabels(GlyphOnTray) }
    clipPath(capPath, ClipOp.Intersect) { paintLabels(GlyphOnCap) }
}

private fun DrawScope.drawLabel(label: HyleCapLabel, text: TextLayoutResult?, centerX: Float, color: Color) {
    when (label) {
        HyleCapLabel.None -> Unit
        HyleCapLabel.Hash -> drawPath(HyleCapGeometry.hashGlyph, color)
        HyleCapLabel.Star -> drawPath(HyleCapGeometry.starGlyph, color)
        is HyleCapLabel.Text -> if (text != null) {
            drawText(text, color, Offset(centerX - text.size.width / 2f, LABEL_Y - text.size.height / 2f))
        }
    }
}

private fun lerpF(a: Float, b: Float, t: Float) = a + (b - a) * t

/**
 * The design's geometry, in its own coordinate space (the Figma frame: tray at 315,158, 639 x
 * 321). Every outline is an 8-segment (cap, sheen) or 3-segment (hook) closed/open path in the
 * SAME clockwise vertex order in both states, which is what makes a straight vertex-by-vertex
 * morph correct: `capLeft` vertex k goes to `capRight` vertex k.
 */
internal object HyleCapGeometry {
    const val TRAY_X = 315f
    const val TRAY_Y = 158f
    const val TRAY_W = 639f
    const val TRAY_H = 321f
    const val TRAY_R = 40f

    /** Start point plus segments; a 2-number segment is a line, a 6-number one a cubic. */
    class Outline(val start: FloatArray, val segments: List<FloatArray>)

    private fun o(sx: Float, sy: Float, vararg segs: FloatArray) = Outline(floatArrayOf(sx, sy), segs.toList())
    private fun l(x: Float, y: Float) = floatArrayOf(x, y)
    private fun c(a: Float, b: Float, c: Float, d: Float, e: Float, f: Float) = floatArrayOf(a, b, c, d, e, f)

    // "Toggle Off": cap on the left (big radius left, leaning edge right).
    val capLeft = o(
        316.018f, 199.018f,
        c(316.008f, 176.919f, 333.919f, 159f, 356.018f, 159f), l(578.566f, 159f),
        c(596.616f, 159f, 612.429f, 171.088f, 617.165f, 188.506f), l(682.415f, 428.506f),
        c(689.331f, 453.944f, 670.178f, 479f, 643.816f, 479f), l(356.125f, 479f),
        c(334.041f, 479f, 316.135f, 461.102f, 316.125f, 439.018f), l(316.018f, 199.018f),
    )

    // "Toggle On": cap on the right (leaning edge left, big radius right).
    val capRight = o(
        652.844f, 188.522f,
        c(657.574f, 171.096f, 673.391f, 159f, 691.447f, 159f), l(914f, 159f),
        c(936.091f, 159f, 954f, 176.909f, 954f, 199f), l(954f, 439f),
        c(954f, 461.091f, 936.091f, 479f, 914f, 479f), l(626.304f, 479f),
        c(599.95f, 479f, 580.798f, 453.957f, 587.701f, 428.522f), l(652.844f, 188.522f),
    )

    val sheenLeft = o(
        460.111f, 206.254f,
        c(464.205f, 187.986f, 480.422f, 175f, 499.144f, 175f), l(593.971f, 175f),
        c(605.214f, 175f, 615.074f, 182.505f, 618.068f, 193.342f), l(683.004f, 428.347f),
        c(690.043f, 453.822f, 670.879f, 479f, 644.448f, 479f), l(448.954f, 479f),
        c(423.336f, 479f, 404.321f, 455.253f, 409.922f, 430.254f), l(460.111f, 206.254f),
    )

    val sheenRight = o(
        717.111f, 206.254f,
        c(721.205f, 187.986f, 737.422f, 175f, 756.144f, 175f), l(929f, 175f),
        c(942.807f, 175f, 954f, 186.193f, 954f, 200f), l(954f, 439f),
        c(954f, 461.091f, 936.091f, 479f, 914f, 479f), l(705.954f, 479f),
        c(680.336f, 479f, 661.321f, 455.253f, 666.922f, 430.254f), l(717.111f, 206.254f),
    )

    val hookLeft = o(
        558.143f, 182f,
        l(577.04f, 182f), c(587.104f, 182f, 595.89f, 188.818f, 598.39f, 198.567f), l(603.5f, 218.5f),
    )

    val hookRight = o(
        896f, 182f,
        l(909.959f, 182f), c(922.132f, 182f, 932f, 191.868f, 932f, 204.041f), l(932f, 218f),
    )

    /** Vertex-by-vertex interpolation of two same-structure outlines at [t] in 0..1. */
    fun morph(a: Outline, b: Outline, t: Float, close: Boolean): Path {
        val p = Path()
        p.moveTo(lerpF(a.start[0], b.start[0], t), lerpF(a.start[1], b.start[1], t))
        for (i in a.segments.indices) {
            val s = a.segments[i]
            val e = b.segments[i]
            val v = FloatArray(s.size) { lerpF(s[it], e[it], t) }
            if (v.size == 2) p.lineTo(v[0], v[1]) else p.cubicTo(v[0], v[1], v[2], v[3], v[4], v[5])
        }
        if (close) p.close()
        return p
    }

    private fun parse(d: String): Path = PathParser().parsePathString(d).toPath()

    /** The design's `#`, in tray space. */
    val hashGlyph: Path by lazy {
        parse("M412.278 371L420.396 342.732H403V332.584H423.295L432.283 301.416H414.887V291.268H435.183L443.301 263H454.898L446.78 291.268H477.223L485.341 263H496.938L488.82 291.268H506.216V301.416H485.921L476.933 332.584H494.329V342.732H474.034L465.915 371H454.318L462.436 342.732H431.993L423.875 371H412.278ZM434.893 332.584H465.336L474.323 301.416H443.881L434.893 332.584Z")
    }

    /** The design's `*`, in tray space. */
    val starGlyph: Path by lazy {
        parse("M763.916 368.965C761.277 372.401 756.382 373.112 752.875 370.569C749.241 367.934 748.485 362.825 751.201 359.251L773.585 329.798C775.239 327.621 774.271 324.464 771.682 323.588L740.272 312.964C736.214 311.592 733.968 307.254 735.192 303.147C736.47 298.855 741.035 296.461 745.293 297.849L776.88 308.15C779.466 308.993 782.12 307.066 782.12 304.347V271C782.12 266.582 785.781 263 790.2 263C794.618 263 798.279 266.582 798.279 271V304.334C798.279 307.057 800.94 308.984 803.527 308.134L835.032 297.786C839.198 296.418 843.71 298.72 845.123 302.871C846.547 307.052 844.333 311.659 840.152 313.084L809.319 323.59C806.743 324.468 805.777 327.606 807.412 329.781L829.58 359.259C832.268 362.834 831.469 367.943 827.847 370.568C824.353 373.102 819.446 372.43 816.799 369.02L793.376 338.835C791.77 336.765 788.64 336.773 787.044 338.851L763.916 368.965Z")
    }
}
