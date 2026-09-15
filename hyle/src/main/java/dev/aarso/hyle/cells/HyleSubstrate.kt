package dev.aarso.hyle.cells

import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Paint
import android.graphics.Shader
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import dev.aarso.hyle.Pulse
import dev.aarso.hyle.RadiantHues
import dev.aarso.hyle.theme.LocalHyleColors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * The substrate — the bottom stratum of the lens model (`docs/LENS.md`).
 *
 * Grainy, matte, and **never interactive**. It carries exactly one thing:
 * *background activity*, expressed as motion. The root law is that state is
 * shown by material behavior, and the sub-law is that **motion occurs only when
 * there is real state** — so when [activity] is zero this is a still image, and
 * that stillness is itself the honest signal that nothing is happening.
 *
 * The grain is two layers, mirroring `field/ARCHITECTURE.md`: a fixed tiled noise
 * (the "film grain" term) and, when active, drifting wisps that read as depth
 * beneath the lens rather than as objects on it.
 *
 * ### Why no AGSL
 * Form-World's raymarcher is a fragment shader, and the Android equivalent is
 * `RuntimeShader` — which needs API 33, while Fonebrew's floor is 31. Rather than
 * raise the floor or fork the render path, this is built from ordinary Compose
 * primitives that exist at 31: a tiled noise bitmap plus a handful of gradient
 * sprites. It is a cheaper substrate than Form-World, deliberately — it sits
 * behind a lens that blurs it, so detail there would be spent and then thrown
 * away.
 */
@Composable
fun HyleSubstrate(
    modifier: Modifier = Modifier,
    /**
     * How much is going on beneath, 0..1. Zero is dead still. This is a
     * *quantity of real work*, never a decoration — wire it to actual state
     * (a generation streaming, a model loading, a git write), never to "looks
     * nicer when it moves".
     */
    activity: Float = 0f,
    /** Motion rate. [Pulse.STILL] is the correct default when there is nothing to say. */
    pulse: Pulse = Pulse.WATCHED,
    /** Tint of the drifting wisps. Radium = of-here; cold cyan = from-elsewhere. */
    wispHue: Color = Color(RadiantHues.RADIUM),
    grainIntensity: Float = 0.5f,
) {
    val c = LocalHyleColors.current
    val amount = activity.coerceIn(0f, 1f)
    // Ease the whole motion layer in and out, so starting or finishing work is a
    // settle rather than a jump-cut.
    val live by animateFloatAsState(amount, tween(900), label = "hyleSubstrateActivity")

    val transition = rememberInfiniteTransition(label = "hyleSubstrate")
    // One slow cycle drives every sprite; each reads it at a different phase, so
    // they stay related rather than independently random — "heartbeat, not weather".
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulse.periodMs * 8, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "hyleSubstrateCycle",
    )
    // The breath itself, within pulse's alpha band.
    val breath by transition.animateFloat(
        initialValue = pulse.minAlphaPct / 100f,
        targetValue = pulse.maxAlphaPct / 100f,
        animationSpec = infiniteRepeatable(
            animation = tween(pulse.periodMs, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "hyleSubstrateBreath",
    )

    val grain = remember(grainIntensity, c.raised) { noiseTile(96, c.raised) }
    val sprites = remember { List(6) { Sprite.seeded(it) } }

    Box(modifier.background(c.ink)) {
        Canvas(Modifier.fillMaxSize()) {
            // Layer 1 — wisps, beneath the grain so the grain reads as being *on* the
            // ground rather than floating over the light.
            if (live > 0.01f) {
                sprites.forEach { it.draw(this, t, live * breath, wispHue) }
            }
            // Layer 2 — fixed film grain.
            val paint = Paint().apply {
                shader = BitmapShader(grain, Shader.TileMode.REPEAT, Shader.TileMode.REPEAT)
                // Same 14% ceiling as Modifier.hyleTexture: enough to read as textured,
                // never enough to become a contrast hazard.
                alpha = (grainIntensity.coerceIn(0f, 1f) * 0.14f * 255f).toInt().coerceIn(0, 36)
            }
            drawContext.canvas.nativeCanvas
                .drawRect(0f, 0f, size.width, size.height, paint)
        }
    }
}

/**
 * One drifting wisp. Deliberately elongated along its direction of travel —
 * `field/ARCHITECTURE.md` is explicit that these must read as wisps and never as
 * spheres, so a plain radial falloff is not acceptable.
 */
private class Sprite(
    val phase: Float,
    val yBand: Float,
    val speed: Float,
    val length: Float,
    val tilt: Float,
) {
    fun draw(scope: DrawScope, t: Float, strength: Float, hue: Color) = with(scope) {
        // Travel across and slightly down, wrapping. Position is a pure function of
        // the shared cycle, so setting the cycle still freezes everything cleanly.
        val p = (t * speed + phase) % 1f
        val x = (p * 1.4f - 0.2f) * size.width
        val y = (yBand + 0.06f * sin((p + phase) * 2f * Math.PI).toFloat()) * size.height
        val len = length * size.width
        val thick = len * 0.10f
        val a = strength * 0.5f
        rotate(degrees = tilt, pivot = Offset(x, y)) {
            drawRect(
                brush = Brush.horizontalGradient(
                    0f to Color.Transparent,
                    0.45f to hue.copy(alpha = a * 0.55f),
                    0.72f to hue.copy(alpha = a),
                    1f to Color.Transparent,
                    startX = x - len / 2f,
                    endX = x + len / 2f,
                ),
                topLeft = Offset(x - len / 2f, y - thick / 2f),
                size = androidx.compose.ui.geometry.Size(len, thick),
            )
        }
    }

    companion object {
        /** Seeded, not random at runtime: the same build always drifts the same way. */
        fun seeded(i: Int): Sprite {
            val r = Random(i * 7919)
            return Sprite(
                phase = r.nextFloat(),
                yBand = 0.12f + r.nextFloat() * 0.76f,
                speed = 0.55f + r.nextFloat() * 0.9f,
                length = 0.16f + r.nextFloat() * 0.22f,
                tilt = -14f + r.nextFloat() * 28f,
            )
        }
    }
}

/** A small greyscale-noise tile centred on [tint]'s brightness. */
private fun noiseTile(size: Int, tint: Color): Bitmap {
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val rnd = Random(0x0FF1CE)
    val base = ((tint.red + tint.green + tint.blue) / 3f * 255f).toInt().coerceIn(0, 255)
    val px = IntArray(size * size)
    for (i in px.indices) {
        val n = (base + rnd.nextInt(-42, 42)).coerceIn(0, 255)
        px[i] = (0xFF shl 24) or (n shl 16) or (n shl 8) or n
    }
    bmp.setPixels(px, 0, size, 0, 0, size, size)
    return bmp
}
