package dev.aarso.hyle.component

import android.graphics.Bitmap
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import dev.aarso.hyle.cells.HyleFieldShape
import dev.aarso.hyle.cells.HyleTabBar
import dev.aarso.hyle.cells.HyleSegmentedToggle
import dev.aarso.hyle.cells.HyleSlashTabBar
import dev.aarso.hyle.cells.HyleTabSpec
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

    // ── Cap toggle: the owner's slant-cap A/B switch, both states + disabled ──
    private fun renderCapToggle(c: HyleColors, out: String) {
        compose.setContent {
            Ground(c) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Caption("HyleCapToggle — cap on A · cap on B · disabled", c)
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        HyleCapToggle("#", "*", selected = 0, onSelect = {})
                        HyleCapToggle("#", "*", selected = 1, onSelect = {})
                        HyleCapToggle("#", "*", selected = 0, onSelect = {}, enabled = false)
                    }
                    Caption("with text labels (terminal use)", c)
                    Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                        HyleCapToggle("Phone", "Remote", selected = 0, onSelect = {}, modifier = Modifier)
                        HyleCapToggle("Phone", "Remote", selected = 1, onSelect = {}, modifier = Modifier)
                    }
                }
            }
        }
        compose.waitForIdle()
        save(capture("board"), out)
    }

    @Test fun capToggleDark() = renderCapToggle(darkHyleColors(), "captoggle-dark.png")
    @Test fun capToggleLight() = renderCapToggle(lightHyleColors(), "captoggle-light.png")

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

    // ── Split action (the Cohere "Get Started ⟋ +" register) ──────────────────────────────
    // Owner reference 2026-08-21 (Cohere dashboard shots). HyleSplitButton already implements
    // this treatment but ships with ONE caller in the whole codebase (HyleColorPicker3D's
    // "Roll"), so it had never been looked at on a screen — and it was wrong. The height
    // ladder below was added to make the bug visible rather than argued, and now stands as the
    // regression proof for the fix:
    //
    //   BEFORE: Segments.kt hard-coded the cells' overlap as `spacedBy(SEAM_GAP - 10.dp)`
    //   "at the 40dp button height", while HyleSegmentShape derived the slant as
    //   (h * 0.25f) capped at 12dp. Correct at exactly h == 40dp. At 48dp the slant hit the
    //   cap while the overlap stayed -7dp, so the seam opened to 5dp; at 64dp the cap also
    //   shallowed the lean to 12/64 = 0.1875 against 0.25 at 40dp.
    //
    //   AFTER: HyleSeamRow derives the overlap from the MEASURED height, and the slant is
    //   h * HyleSeam.SLOPE with no height cap — so 40/48/64dp must now show the SAME strip of
    //   ground and the SAME lean. Any future change that reintroduces a literal overlap or a
    //   height cap shows up here as a widening or a rotating seam.
    //
    // The 200%-font-scale row is the "any height including large accessibility font scales"
    // case: nobody types that height, the text does.
    private data class SplitSpec(
        val caption: String,
        val height: Int?,
        val secondary: Boolean = false,
        val enabled: Boolean = true,
        val fontScale: Float = 1f,
    )

    private val splitSpecs = listOf(
        SplitSpec("PRIMARY · 40dp (the default floor)", 40),
        SplitSpec("SECONDARY · 40dp", 40, secondary = true),
        SplitSpec("DISABLED · 40dp", 40, enabled = false),
        SplitSpec("PRIMARY · 48dp — gap + angle must match 40dp", 48),
        SplitSpec("PRIMARY · 64dp — gap + angle must match 40dp", 64),
        SplitSpec("PRIMARY · intrinsic height @ 200% font scale", null, fontScale = 2f),
    )

    // The seam wall, rendered as the LAST board of the same stack (one ComposeTestRule may
    // only have its content set once, so every board of a scene is one indexed composition).
    // The whole point of the grammar is that unrelated controls stacked in one column read as
    // ONE leaning wall. Three surfaces, three different code paths to the same slope:
    // HyleSplitButton (HyleSegmentShape, packed by HyleSeamRow), HyleTabBar (the same shape at
    // a much taller, content-derived cell height), and HyleFieldShape (the literal Figma
    // transcription that HyleSeam.SLOPE is derived FROM — 65.1428 run / 240 rise). If any of
    // the three leans differently it is visible at a glance in this one board; before the fix
    // all three disagreed (0.25-capped, 0.25-capped, 0.271428).
    private fun renderSplit(c: HyleColors, out: String) {
        var idx by mutableStateOf(0)
        val dot: DrawScope.(Color) -> Unit = { tint -> drawCircle(tint, radius = size.minDimension / 3f) }
        val tabs = listOf(
            HyleTabSpec("Global", dot), HyleTabSpec("Image", dot),
            HyleTabSpec("Text", dot), HyleTabSpec("Video", dot),
        )
        compose.setContent {
            val spec = splitSpecs.getOrNull(idx)
            val density = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(density.density, spec?.fontScale ?: 1f),
            ) {
                Ground(c) {
                    if (spec != null) {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Caption(spec.caption, c)
                            dev.aarso.hyle.cells.HyleSplitButton(
                                text = "Get Started",
                                onClick = {},
                                modifier = spec.height?.let { Modifier.height(it.dp) } ?: Modifier,
                                enabled = spec.enabled,
                                secondary = spec.secondary,
                            )
                        }
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Caption("PARALLEL? field / split action / tab bar / slash bar", c)
                            // First, the REFERENCE: a slab clipped to HyleFieldShape, the Figma
                            // transcription HyleSeam.SLOPE is derived from. Everything below it
                            // must lean at the same angle as this one's left edge.
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(44.dp)
                                    .clip(HyleFieldShape)
                                    .background(c.raised),
                            )
                            dev.aarso.hyle.cells.HyleSplitButton(
                                text = "Get Started",
                                onClick = {},
                                modifier = Modifier.fillMaxWidth(),
                            )
                            HyleTabBar(tabs = tabs, selected = 0, onSelect = {})
                            // The fourth surface: HyleSlashTabBar's literal "/" thread, whose
                            // endpoints used to imply their own slope (0.8w→0.2w over 8x22dp
                            // = 0.218).
                            HyleSlashTabBar(tabs = tabs.take(3), selected = 0, onSelect = {})
                            // HyleSegmentedToggle rides the same HyleSegmentShape, so the
                            // slope + 8dp-corner change lands here too. The real call site
                            // (the app's Personas/Models toggle) is fillMaxWidth with long
                            // maxLines=1 labels, so this row is also the "did the seam
                            // padding push the second label off the end?" check.
                            HyleSegmentedToggle(
                                options = listOf("Personas — named experts", "Models — one prompt, diversity"),
                                selected = 1,
                                onSelect = {},
                                modifier = Modifier.fillMaxWidth(),
                            )
                            HyleSegmentedToggle(options = listOf("All", "Text", "Image"), selected = 1, onSelect = {})
                        }
                    }
                }
            }
        }
        val shots = (0..splitSpecs.size).map { i ->
            compose.runOnIdle { idx = i }
            compose.waitForIdle()
            capture("board")
        }
        save(concatVertically(shots, c), out)
    }

    @Test fun splitDark() = renderSplit(darkHyleColors(), "split-dark.png")
    @Test fun splitLight() = renderSplit(lightHyleColors(), "split-light.png")

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
