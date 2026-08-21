package dev.aarso.hyle.component

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.theme.HyleColors
import dev.aarso.hyle.theme.LocalHyleColors
import dev.aarso.hyle.theme.darkHyleColors
import dev.aarso.hyle.theme.lightHyleColors
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.io.File
import java.io.FileOutputStream

/**
 * Real-pixel renders of the desktop-class kit — Robolectric native graphics rasterizes the
 * actual shipped composables with Skia on the JVM, the only pixels this device-less repo can
 * produce. NOT part of the normal gate: runs only with `-Dhyle.renders=true` (Robolectric
 * pulls a full android-all jar; CI shouldn't pay that for a render artifact). Output:
 * PNG files under `hyle/build/renders`. Typography note: renders use the test default font, not the
 * host app's; geometry/color/state fidelity is exact, glyph shapes owner-verified on device.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35], qualifiers = "w411dp-h2000dp-420dpi")
class HyleKitRenderTest {

    @get:Rule val compose = createComposeRule()

    private val outDir = File("build/renders").apply { mkdirs() }

    @Before fun gate() = assumeTrue(System.getProperty("hyle.renders") == "true")

    private fun save(bitmap: Bitmap, name: String) {
        FileOutputStream(File(outDir, name)).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }

    // PixelCopy-based captureToImage times out under Robolectric here, so rasterize the way
    // screenshot libraries do: software-draw the real AndroidComposeView, crop to the node.
    private fun capture(tag: String): Bitmap {
        compose.waitForIdle()
        val node = compose.onNodeWithTag(tag).fetchSemanticsNode()
        val view = (node.root as androidx.compose.ui.platform.ViewRootForTest).view
        lateinit var out: Bitmap
        compose.runOnIdle {
            val full = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            view.draw(android.graphics.Canvas(full))
            val b = node.boundsInRoot
            out = Bitmap.createBitmap(
                full,
                b.left.toInt().coerceAtLeast(0),
                b.top.toInt().coerceAtLeast(0),
                b.width.toInt().coerceAtMost(full.width - b.left.toInt().coerceAtLeast(0)),
                b.height.toInt().coerceAtMost(full.height - b.top.toInt().coerceAtLeast(0)),
            )
        }
        return out
    }

    @Composable private fun Ground(c: HyleColors, content: @Composable () -> Unit) {
        CompositionLocalProvider(LocalHyleColors provides c) {
            MaterialTheme {
                Box(Modifier.background(c.ink).padding(20.dp).width(380.dp).testTag("board")) { content() }
            }
        }
    }

    @Composable private fun Caption(text: String, c: HyleColors) {
        Text(text, style = MaterialTheme.typography.labelMedium, color = c.textMid)
    }

    // ── Fields: all 7 mockup rows; SELECTED rows get REAL focus via FocusRequester ──
    private data class FieldSpec(
        val caption: String,
        val text: String,
        val mandatory: Boolean = false,
        val isError: Boolean = false,
        val enabled: Boolean = true,
        val focus: Boolean = false,
    )

    private val fieldSpecs = listOf(
        FieldSpec("NOT_SELECTED", "Not selected"),
        FieldSpec("SELECTED (real focus)", "Selected", focus = true),
        FieldSpec("SELECTED + mandatory", "Selected & mandatory", mandatory = true, focus = true),
        FieldSpec("ERROR, not selected", "Error / invalid input", isError = true),
        FieldSpec("ERROR, selected", "Error / invalid input", isError = true, focus = true),
        FieldSpec("ERROR + mandatory, selected", "Error & mandatory", isError = true, mandatory = true, focus = true),
        FieldSpec("DISABLED", "Disabled", enabled = false),
    )

    private fun renderFields(c: HyleColors, out: String) {
        var idx by mutableStateOf(0)
        val requester = FocusRequester()
        compose.setContent {
            val spec = fieldSpecs[idx]
            Ground(c) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Caption(spec.caption, c)
                    HyleField(
                        value = spec.text,
                        onValueChange = {},
                        modifier = Modifier.fillMaxWidth().focusRequester(requester),
                        mandatory = spec.mandatory,
                        enabled = spec.enabled,
                        isError = spec.isError,
                    )
                }
            }
        }
        val shots = fieldSpecs.indices.map { i ->
            compose.runOnIdle { idx = i }
            compose.waitForIdle()
            if (fieldSpecs[i].focus) {
                compose.runOnIdle { requester.requestFocus() }
                compose.waitForIdle()
            }
            capture("board")
        }
        save(concatVertically(shots, c), out)
    }

    @Test fun fieldsDark() = renderFields(darkHyleColors(), "fields-dark.png")
    @Test fun fieldsLight() = renderFields(lightHyleColors(), "fields-light.png")

    // ── Controls: toggle states + keycap pairs, one board ──
    private fun renderControls(c: HyleColors, out: String) {
        compose.setContent {
            Ground(c) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Caption("HyleToggle — off · on · disabled", c)
                    Row(horizontalArrangement = Arrangement.spacedBy(22.dp)) {
                        HyleToggle(checked = false, onCheckedChange = {})
                        HyleToggle(checked = true, onCheckedChange = {})
                        HyleToggle(checked = false, onCheckedChange = null, enabled = false)
                    }
                    Caption("HyleKeycapPair — emphasis SECOND · FIRST · NONE", c)
                    Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        HyleKeycapPair("#", "*", emphasis = HyleKeycapEmphasis.SECOND)
                        HyleKeycapPair("#", "*", emphasis = HyleKeycapEmphasis.FIRST)
                        HyleKeycapPair("#", "*", emphasis = HyleKeycapEmphasis.NONE)
                    }
                }
            }
        }
        compose.waitForIdle()
        save(capture("board"), out)
    }

    @Test fun controlsDark() = renderControls(darkHyleColors(), "controls-dark.png")
    @Test fun controlsLight() = renderControls(lightHyleColors(), "controls-light.png")

    // ── Tree: the repo-browser shape — expanded run, guides, one selected row ──
    private fun renderTree(c: HyleColors, out: String) {
        val roots = listOf(
            HyleTreeItem(
                "core-engine", "core-engine", HyleTreeIconKind.FOLDER, hasChildren = true,
                children = listOf(
                    HyleTreeItem(
                        "src", "src", HyleTreeIconKind.FOLDER, hasChildren = true,
                        children = listOf(
                            HyleTreeItem("main", "main", HyleTreeIconKind.FOLDER, hasChildren = true),
                            HyleTreeItem("build.gradle.kts", "build.gradle.kts", HyleTreeIconKind.FILE),
                        ),
                    ),
                ),
            ),
            HyleTreeItem("sdengine", "sdengine", HyleTreeIconKind.FOLDER, hasChildren = true),
            HyleTreeItem("NOTICE", "NOTICE", HyleTreeIconKind.FILE),
        )
        compose.setContent {
            Ground(c) {
                HyleTree(
                    roots = roots,
                    expandedIds = setOf("core-engine", "src"),
                    selectedId = "main",
                    onToggleExpand = {}, onSelect = {},
                )
            }
        }
        compose.waitForIdle()
        save(capture("board"), out)
    }

    @Test fun treeDark() = renderTree(darkHyleColors(), "tree-dark.png")
    @Test fun treeLight() = renderTree(lightHyleColors(), "tree-light.png")

    // ── Context menu: lives in a Popup window — capture the popup node itself ──
    private fun renderMenu(c: HyleColors, out: String) {
        val items = listOf(
            HyleContextMenuItem("open", "Open"),
            HyleContextMenuItem("expand", "Expand"),
            HyleContextMenuItem("collapse", "Collapse"),
            HyleContextMenuItem("copy", "Copy path"),
            HyleContextMenuItem("delete", "Delete", destructive = true),
        )
        compose.setContent {
            // HyleContextMenuCard is the exact surface HyleContextMenu's Popup shows —
            // rendered in the main window because a Popup lives in a window this software
            // capture cannot reach.
            Ground(c) {
                HyleContextMenuCard(items = items, onItemClick = {})
            }
        }
        compose.waitForIdle()
        save(capture("board"), out)
    }

    @Test fun menuDark() = renderMenu(darkHyleColors(), "menu-dark.png")
    @Test fun menuLight() = renderMenu(lightHyleColors(), "menu-light.png")

    // Vertical concatenation on the theme ground — captures are already labeled Compose pixels.
    private fun concatVertically(shots: List<Bitmap>, c: HyleColors, gap: Int = 24): Bitmap {
        val width = shots.maxOf { it.width }
        val height = shots.sumOf { it.height } + gap * (shots.size - 1)
        val outBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(outBmp)
        canvas.drawColor(android.graphics.Color.argb(255, (c.ink.red * 255).toInt(), (c.ink.green * 255).toInt(), (c.ink.blue * 255).toInt()))
        var y = 0
        shots.forEach { canvas.drawBitmap(it, 0f, y.toFloat(), null); y += it.height + gap }
        return outBmp
    }
}
