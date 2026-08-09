package dev.aarso.hyle.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.min

/** Which colour space the live model shows. */
enum class SpaceModelKind { HSV_CONE, RGB_CUBE }

/**
 * The live 3D model of the colour space — the third element of the kit picker (hue ring,
 * slice, model), ported natively: no THREE.js, no WebView, just [SpaceModelGeometry]'s
 * rotation/projection maths onto a Compose canvas. Drag to orbit (yaw free, pitch
 * clamped). The same gamut honesty as the slices applies in depth: the **solid point
 * cloud** is the space scaled to the display fraction, the **dotted silhouette** is the
 * model's full space around it, and the white-ringed marker is the current colour sitting
 * inside the subset it actually lives in.
 *
 * A painter's-algorithm point cloud (a few hundred points, resorted per drag frame) is
 * deliberate: this is a legibility instrument, not a renderer. Perf and feel are
 * owner-verified on device, like every canvas in this module.
 */
@Composable
fun SpaceModel(
    kind: SpaceModelKind,
    color: Color,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    var yaw by remember { mutableFloatStateOf(35f) }
    var pitch by remember { mutableFloatStateOf(-22f) }

    // The cloud is view-independent — regenerate only when the shape or fraction changes.
    val cloud = remember(kind, fraction) {
        when (kind) {
            SpaceModelKind.HSV_CONE -> buildList {
                for (hi in 0 until 18) {
                    val h = hi * 20f
                    for (vi in 1..6) {
                        val v = vi / 6f
                        for (si in 1..4) {
                            val s = si / 4f
                            add(SpaceModelGeometry.conePoint(h, s, v, fraction) to Color.hsv(h, s, v))
                        }
                    }
                }
                add(SpaceModelGeometry.conePoint(0f, 0f, 0f, fraction) to Color.Black)
            }
            SpaceModelKind.RGB_CUBE -> buildList {
                for (ri in 0..5) for (gi in 0..5) for (bi in 0..5) {
                    val r = ri / 5f; val g = gi / 5f; val b = bi / 5f
                    add(SpaceModelGeometry.cubePoint(r, g, b, fraction) to Color(r, g, b))
                }
            }
        }
    }

    Canvas(
        modifier.pointerInput(Unit) {
            detectDragGestures { change, drag ->
                change.consume()
                yaw -= drag.x * 0.4f
                pitch = (pitch - drag.y * 0.4f).coerceIn(-80f, 80f)
            }
        },
    ) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val scale = min(size.width, size.height) * 0.82f
        val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 5.dp.toPx()))
        val outline = Color(0xFF4A4E57) // TextDisabled — same voice as the slices' dotting

        fun place(p: SpacePoint): Pair<Offset, Float> {
            val r = SpaceModelGeometry.rotated(p, yaw, pitch)
            val (dx, dy, depth) = SpaceModelGeometry.projected(r, scale)
            return Offset(c.x + dx, c.y + dy) to depth
        }

        // Dotted full-space silhouette, drawn behind the cloud.
        when (kind) {
            SpaceModelKind.HSV_CONE -> {
                val rim = (0..48).map { place(SpaceModelGeometry.conePoint(it * 7.5f, 1f, 1f)).first }
                for (i in 0 until rim.size - 1) {
                    drawLine(outline, rim[i], rim[i + 1], strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }
                val apex = place(SpaceModelGeometry.conePoint(0f, 0f, 0f)).first
                for (h in listOf(0f, 90f, 180f, 270f)) {
                    val edge = place(SpaceModelGeometry.conePoint(h, 1f, 1f)).first
                    drawLine(outline, apex, edge, strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }
            }
            SpaceModelKind.RGB_CUBE -> {
                SpaceModelGeometry.cubeEdges().forEach { (a, b) ->
                    drawLine(outline, place(a).first, place(b).first, strokeWidth = 1.dp.toPx(), pathEffect = dash)
                }
            }
        }

        // The solid cloud, far-to-near.
        cloud
            .map { (p, col) -> place(p) to col }
            .sortedBy { it.first.second }
            .forEach { (posDepth, col) ->
                drawCircle(col, radius = 2.2.dp.toPx(), center = posDepth.first)
            }

        // The current colour, inside the subset it actually lives in.
        val markerSpace = when (kind) {
            SpaceModelKind.HSV_CONE -> {
                val hsv = FloatArray(3)
                android.graphics.Color.colorToHSV(
                    android.graphics.Color.rgb(
                        (color.red * 255).toInt(),
                        (color.green * 255).toInt(),
                        (color.blue * 255).toInt(),
                    ),
                    hsv,
                )
                SpaceModelGeometry.conePoint(hsv[0], hsv[1], hsv[2], fraction)
            }
            SpaceModelKind.RGB_CUBE -> SpaceModelGeometry.cubePoint(color.red, color.green, color.blue, fraction)
        }
        val marker = place(markerSpace).first
        drawCircle(color.copy(alpha = 1f), radius = 5.dp.toPx(), center = marker)
        drawCircle(Color.White, radius = 5.dp.toPx(), center = marker, style = Stroke(1.5.dp.toPx()))
    }
}
