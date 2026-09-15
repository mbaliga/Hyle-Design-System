package dev.aarso.hyleprobe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.aarso.hyle.component.HyleContextMenu
import dev.aarso.hyle.component.HyleContextMenuItem
import dev.aarso.hyle.component.HyleField
import dev.aarso.hyle.component.HyleKeycap
import dev.aarso.hyle.component.HyleKeycapEmphasis
import dev.aarso.hyle.component.HyleKeycapPair
import dev.aarso.hyle.component.HyleToggle
import dev.aarso.hyle.component.HyleTree
import dev.aarso.hyle.component.HyleTreeIconKind
import dev.aarso.hyle.component.HyleTreeItem
import dev.aarso.hyle.theme.LocalHyleColors

/**
 * Previews for the desktop-class kit (docs/design/desktop-class-kit.md in the core repo):
 * [HyleField], [HyleToggle], [HyleKeycap], [HyleTree], [HyleContextMenu] — one probe function
 * per component, every state the spec names. Render/gesture feel is owner-verified on device;
 * this only proves the components compose and states switch, from the host activity.
 */

private const val PANEL_DP = 20

@Composable
private fun ProbeSection(title: String, content: @Composable () -> Unit) {
    val c = LocalHyleColors.current
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = c.textDisabled, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        content()
    }
}

// ── A. HyleField ────────────────────────────────────────────────────────────────────

@Composable
fun HyleFieldProbe() {
    val c = LocalHyleColors.current
    var notSelected by remember { mutableStateOf("") }
    var selectedValue by remember { mutableStateOf("Focused text") }
    var mandatoryValue by remember { mutableStateOf("") }
    var errorValue by remember { mutableStateOf("bad-input") }
    var multilineValue by remember { mutableStateOf("Line one\nLine two") }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = PANEL_DP.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(28.dp),
    ) {
        Text("HyleField", color = c.textHigh, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        ProbeSection("Not selected") {
            HyleField(
                value = notSelected,
                onValueChange = { notSelected = it },
                label = "Base URL",
                placeholder = "https://api.example.com",
            )
        }
        ProbeSection("Selected (focused) — tap in to see the violet tick + border") {
            HyleField(
                value = selectedValue,
                onValueChange = { selectedValue = it },
                label = "Display name",
            )
        }
        ProbeSection("Selected & mandatory — trailing violet asterisk") {
            HyleField(
                value = mandatoryValue,
                onValueChange = { mandatoryValue = it },
                label = "API key",
                mandatory = true,
            )
        }
        ProbeSection("Error, not selected — red notched tick, no border") {
            HyleField(
                value = errorValue,
                onValueChange = { },
                label = "Webhook URL",
                isError = true,
                supportingText = "Not a valid URL",
            )
        }
        ProbeSection("Error & mandatory — red notched tick, red border, red asterisk") {
            HyleField(
                value = errorValue,
                onValueChange = { errorValue = it },
                label = "Model endpoint",
                isError = true,
                mandatory = true,
                supportingText = "Required",
            )
        }
        ProbeSection("Disabled — ghosted grey tick, ghosted text") {
            HyleField(
                value = "read-only value",
                onValueChange = { },
                label = "Provider",
                enabled = false,
            )
        }
        ProbeSection("Multiline") {
            HyleField(
                value = multilineValue,
                onValueChange = { multilineValue = it },
                label = "Notes",
                singleLine = false,
                minLines = 3,
                maxLines = 5,
                supportingText = "Up to 5 lines",
            )
        }
    }
}

// ── B. HyleToggle ───────────────────────────────────────────────────────────────────

@Composable
fun HyleToggleProbe() {
    val c = LocalHyleColors.current
    var off by remember { mutableStateOf(false) }
    var on by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.ink)
            .padding(horizontal = PANEL_DP.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("HyleToggle", color = c.textHigh, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        ToggleRow("Off (tap to toggle)", off) { off = it }
        ToggleRow("On (tap to toggle)", on) { on = it }
        ToggleRow("Disabled, off", checked = false, onCheckedChange = null)
        ToggleRow("Disabled, on", checked = true, onCheckedChange = null)
    }
}

@Composable
private fun ToggleRow(label: String, checked: Boolean, onCheckedChange: ((Boolean) -> Unit)?) {
    val c = LocalHyleColors.current
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, color = c.textMid, fontSize = 13.sp)
        HyleToggle(checked = checked, onCheckedChange = onCheckedChange, enabled = onCheckedChange != null)
    }
}

// ── C. HyleKeycap ───────────────────────────────────────────────────────────────────

@Composable
fun HyleKeycapProbe() {
    val c = LocalHyleColors.current
    Column(
        Modifier
            .fillMaxSize()
            .background(c.ink)
            .padding(horizontal = PANEL_DP.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("HyleKeycap", color = c.textHigh, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        ProbeSection("Pair, no emphasis") {
            HyleKeycapPair(firstKey = "#", secondKey = "*")
        }
        ProbeSection("Pair, emphasis on first key") {
            HyleKeycapPair(firstKey = "⌘", secondKey = "K", emphasis = HyleKeycapEmphasis.FIRST)
        }
        ProbeSection("Pair, emphasis on second key") {
            HyleKeycapPair(firstKey = "⇧", secondKey = "⏎", emphasis = HyleKeycapEmphasis.SECOND)
        }
        ProbeSection("Standalone keycaps") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HyleKeycap("␛")
                HyleKeycap("A", emphasized = true)
            }
        }
    }
}

// ── D. HyleTree ─────────────────────────────────────────────────────────────────────

private val treeData = listOf(
    HyleTreeItem(
        id = "app",
        label = "app",
        iconKind = HyleTreeIconKind.FOLDER,
        hasChildren = true,
        children = listOf(
            HyleTreeItem(
                id = "src",
                label = "src",
                iconKind = HyleTreeIconKind.FOLDER,
                hasChildren = true,
                children = listOf(
                    HyleTreeItem(id = "main.kt", label = "Main.kt", iconKind = HyleTreeIconKind.FILE),
                    HyleTreeItem(id = "utils.kt", label = "Utils.kt", iconKind = HyleTreeIconKind.FILE),
                ),
            ),
            HyleTreeItem(id = "readme", label = "README.md", iconKind = HyleTreeIconKind.FILE),
        ),
    ),
    HyleTreeItem(id = "gradle", label = "build.gradle.kts", iconKind = HyleTreeIconKind.FILE),
)

@Composable
fun HyleTreeProbe() {
    val c = LocalHyleColors.current
    var expanded by remember { mutableStateOf(setOf("app", "src")) }
    var selected by remember { mutableStateOf<String?>("main.kt") }
    var menuForRow by remember { mutableStateOf<String?>(null) }

    Column(Modifier.fillMaxSize().background(c.ink)) {
        Text(
            "HyleTree — long-press a row for its context menu",
            color = c.textHigh,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = PANEL_DP.dp, vertical = 20.dp),
        )
        Box(Modifier.fillMaxSize()) {
            HyleTree(
                roots = treeData,
                expandedIds = expanded,
                selectedId = selected,
                onToggleExpand = { id -> expanded = if (id in expanded) expanded - id else expanded + id },
                onSelect = { id -> selected = id },
                onLongPress = { id -> menuForRow = id },
            )
            HyleContextMenu(
                expanded = menuForRow != null,
                onDismissRequest = { menuForRow = null },
                items = listOf(
                    HyleContextMenuItem("open", "Open"),
                    HyleContextMenuItem("rename", "Rename"),
                    HyleContextMenuItem("expand", "Expand"),
                    HyleContextMenuItem("collapse", "Collapse"),
                    HyleContextMenuItem("delete", "Delete", destructive = true),
                ),
                onItemClick = { menuForRow = null },
            )
        }
    }
}

// ── E. HyleContextMenu ──────────────────────────────────────────────────────────────

@Composable
fun HyleContextMenuProbe() {
    val c = LocalHyleColors.current
    var normalOpen by remember { mutableStateOf(false) }
    var destructiveOpen by remember { mutableStateOf(false) }
    var disabledOpen by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .background(c.ink)
            .padding(horizontal = PANEL_DP.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Text("HyleContextMenu", color = c.textHigh, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        ProbeSection("No destructive group — no separator") {
            Box {
                TapTarget("Tap to open") { normalOpen = true }
                HyleContextMenu(
                    expanded = normalOpen,
                    onDismissRequest = { normalOpen = false },
                    items = listOf(
                        HyleContextMenuItem("open", "Open"),
                        HyleContextMenuItem("star", "Star"),
                        HyleContextMenuItem("rename", "Rename"),
                    ),
                    onItemClick = { normalOpen = false },
                )
            }
        }

        ProbeSection("Mockup's five rows — hairline separator before Delete (red)") {
            Box {
                TapTarget("Tap to open") { destructiveOpen = true }
                HyleContextMenu(
                    expanded = destructiveOpen,
                    onDismissRequest = { destructiveOpen = false },
                    items = listOf(
                        HyleContextMenuItem("create", "Create new folder"),
                        HyleContextMenuItem("rename", "Rename"),
                        HyleContextMenuItem("expand", "Expand"),
                        HyleContextMenuItem("collapse", "Collapse"),
                        HyleContextMenuItem("delete", "Delete", destructive = true),
                    ),
                    onItemClick = { destructiveOpen = false },
                )
            }
        }

        ProbeSection("A disabled row (dimmed, not clickable)") {
            Box {
                TapTarget("Tap to open") { disabledOpen = true }
                HyleContextMenu(
                    expanded = disabledOpen,
                    onDismissRequest = { disabledOpen = false },
                    items = listOf(
                        HyleContextMenuItem("copy", "Copy path"),
                        HyleContextMenuItem("share", "Share", enabled = false),
                        HyleContextMenuItem("delete", "Delete", destructive = true),
                    ),
                    onItemClick = { disabledOpen = false },
                )
            }
        }
    }
}

@Composable
private fun TapTarget(label: String, onClick: () -> Unit) {
    val c = LocalHyleColors.current
    Box(
        Modifier
            .height(44.dp)
            .fillMaxWidth()
            .background(c.raised)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(label, color = c.textMid, fontSize = 13.sp)
    }
}
