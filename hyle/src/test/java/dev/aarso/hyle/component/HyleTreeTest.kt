package dev.aarso.hyle.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [flattenHyleTree] — the pure flattening / expand-state / guide-depth logic behind
 * [HyleTree], exercised with no Compose/Android dependency.
 */
class HyleTreeTest {

    private fun leaf(id: String, label: String = id) =
        HyleTreeItem(id = id, label = label, iconKind = HyleTreeIconKind.FILE, hasChildren = false)

    private fun folder(id: String, children: List<HyleTreeItem>, label: String = id) =
        HyleTreeItem(id = id, label = label, iconKind = HyleTreeIconKind.FOLDER, hasChildren = true, children = children)

    @Test fun `flat leaves - all depth 0, no chevrons`() {
        val roots = listOf(leaf("a"), leaf("b"), leaf("c"))
        val rows = flattenHyleTree(roots, expandedIds = emptySet())
        assertEquals(listOf("a", "b", "c"), rows.map { it.id })
        assertTrue(rows.all { it.depth == 0 && !it.hasChildren && !it.expanded })
    }

    @Test fun `collapsed folder emits itself but not its children`() {
        val roots = listOf(folder("docs", listOf(leaf("readme"))))
        val rows = flattenHyleTree(roots, expandedIds = emptySet())
        assertEquals(listOf("docs"), rows.map { it.id })
        assertFalse(rows.single().expanded)
    }

    @Test fun `expanded folder emits itself then its children at depth+1`() {
        val roots = listOf(folder("docs", listOf(leaf("readme"), leaf("license"))))
        val rows = flattenHyleTree(roots, expandedIds = setOf("docs"))
        assertEquals(listOf("docs", "readme", "license"), rows.map { it.id })
        assertEquals(0, rows[0].depth)
        assertTrue(rows[0].expanded)
        assertEquals(1, rows[1].depth)
        assertEquals(1, rows[2].depth)
        assertFalse(rows[1].expanded) // leaves never read as expanded
    }

    @Test fun `three levels deep - guide depth equals ancestor count at every row`() {
        val tree = folder(
            "root",
            listOf(
                folder(
                    "mid",
                    listOf(leaf("leaf1"), folder("mid2", listOf(leaf("leaf2")))),
                ),
            ),
        )
        val rows = flattenHyleTree(listOf(tree), expandedIds = setOf("root", "mid", "mid2"))
        val byId = rows.associateBy { it.id }
        assertEquals(0, byId.getValue("root").depth)
        assertEquals(1, byId.getValue("mid").depth)
        assertEquals(2, byId.getValue("leaf1").depth)
        assertEquals(2, byId.getValue("mid2").depth)
        assertEquals(3, byId.getValue("leaf2").depth)
        // Depth-first order, siblings in authored order, preserved end to end.
        assertEquals(listOf("root", "mid", "leaf1", "mid2", "leaf2"), rows.map { it.id })
    }

    @Test fun `collapsing an ancestor mid-tree hides only that subtree`() {
        val tree = folder(
            "root",
            listOf(
                folder("a", listOf(leaf("a1"))),
                folder("b", listOf(leaf("b1"))),
            ),
        )
        // root and b expanded, a collapsed: a's child must not appear, b's must.
        val rows = flattenHyleTree(listOf(tree), expandedIds = setOf("root", "b"))
        assertEquals(listOf("root", "a", "b", "b1"), rows.map { it.id })
    }

    @Test fun `hasChildren false is authoritative even if the children list is non-empty`() {
        // A lazy-loading caller may pre-populate children before marking a node expandable;
        // flatten must never expand on children.isNotEmpty() alone.
        val fauxLeaf = HyleTreeItem(
            id = "x",
            label = "x",
            hasChildren = false,
            children = listOf(leaf("hidden")),
        )
        val rows = flattenHyleTree(listOf(fauxLeaf), expandedIds = setOf("x"))
        assertEquals(listOf("x"), rows.map { it.id })
        assertFalse(rows.single().expanded)
    }

    @Test fun `expandedIds naming a node absent from the tree is a harmless no-op`() {
        val roots = listOf(leaf("a"))
        val rows = flattenHyleTree(roots, expandedIds = setOf("ghost", "a"))
        assertEquals(listOf("a"), rows.map { it.id })
    }

    @Test fun `multiple roots - a forest - flatten independently in authored order`() {
        val roots = listOf(
            folder("proj1", listOf(leaf("f1"))),
            folder("proj2", listOf(leaf("f2"))),
        )
        val rows = flattenHyleTree(roots, expandedIds = setOf("proj1", "proj2"))
        assertEquals(listOf("proj1", "f1", "proj2", "f2"), rows.map { it.id })
    }

    @Test fun `empty tree flattens to an empty list`() {
        assertTrue(flattenHyleTree(emptyList(), emptySet()).isEmpty())
    }

    @Test fun `iconKind and label pass through unchanged`() {
        val rows = flattenHyleTree(listOf(leaf("readme", label = "README.md")), emptySet())
        assertEquals("README.md", rows.single().label)
        assertEquals(HyleTreeIconKind.FILE, rows.single().iconKind)
    }
}
