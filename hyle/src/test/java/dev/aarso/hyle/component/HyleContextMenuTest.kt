package dev.aarso.hyle.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [planHyleContextMenu] — the pure item-grouping/separator-placement logic behind
 * [HyleContextMenu], exercised with no Compose/Android dependency.
 */
class HyleContextMenuTest {

    private fun item(id: String, destructive: Boolean = false, enabled: Boolean = true) =
        HyleContextMenuItem(id = id, label = id, destructive = destructive, enabled = enabled)

    @Test fun `mockup's own five-row menu - one separator right before Delete`() {
        val items = listOf(
            item("create"),
            item("rename"),
            item("expand"),
            item("collapse"),
            item("delete", destructive = true),
        )
        val rows = planHyleContextMenu(items)
        assertEquals(listOf("create", "rename", "expand", "collapse", "delete"), rows.map { it.item.id })
        assertEquals(listOf(false, false, false, false, true), rows.map { it.separatorBefore })
    }

    @Test fun `destructive items are grouped to the end regardless of input interleaving`() {
        val items = listOf(item("delete", destructive = true), item("rename"), item("open"))
        val rows = planHyleContextMenu(items)
        // Normal items keep their relative order; the destructive one moves after them.
        assertEquals(listOf("rename", "open", "delete"), rows.map { it.item.id })
        assertTrue(rows.last().item.destructive)
    }

    @Test fun `relative order within each group is stable`() {
        val items = listOf(
            item("z-normal"), item("a-normal"),
            item("z-destructive", destructive = true), item("a-destructive", destructive = true),
        )
        val rows = planHyleContextMenu(items)
        assertEquals(
            listOf("z-normal", "a-normal", "z-destructive", "a-destructive"),
            rows.map { it.item.id },
        )
    }

    @Test fun `no separator when there are no destructive items`() {
        val rows = planHyleContextMenu(listOf(item("a"), item("b")))
        assertTrue(rows.none { it.separatorBefore })
    }

    @Test fun `no orphan separator when every item is destructive`() {
        val rows = planHyleContextMenu(listOf(item("a", destructive = true), item("b", destructive = true)))
        assertTrue(rows.none { it.separatorBefore })
    }

    @Test fun `exactly one separator no matter how many destructive items follow`() {
        val rows = planHyleContextMenu(
            listOf(item("open"), item("del1", destructive = true), item("del2", destructive = true), item("del3", destructive = true)),
        )
        assertEquals(1, rows.count { it.separatorBefore })
        assertTrue(rows[1].separatorBefore) // right before the first destructive row
        assertFalse(rows[2].separatorBefore)
        assertFalse(rows[3].separatorBefore)
    }

    @Test fun `empty menu plans to an empty row list`() {
        assertTrue(planHyleContextMenu(emptyList()).isEmpty())
    }

    @Test fun `disabled flag passes through untouched by grouping`() {
        val rows = planHyleContextMenu(listOf(item("a", enabled = false), item("b", destructive = true, enabled = false)))
        assertFalse(rows.first { it.item.id == "a" }.item.enabled)
        assertFalse(rows.first { it.item.id == "b" }.item.enabled)
    }

    @Test fun `row count always equals input count`() {
        val items = listOf(item("a"), item("b", destructive = true), item("c"), item("d", destructive = true))
        assertEquals(items.size, planHyleContextMenu(items).size)
    }
}
