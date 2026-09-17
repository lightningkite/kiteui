package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.*

/**
 * Locks in element-level theme cascade invariants that require a live element tree.
 *
 * These cannot be tested at the pure model level (ThemeTest) because they depend
 * on:
 *  - The element lifecycle (onStartup → refreshTheming)
 *  - GetBaseTheme.fromParent inheriting the cascading theme
 *  - ThemeAndBack deduplication in the themeAndBack setter
 *
 * All tests use [elementTree] to build a minimal SSR-backed element tree and
 * inspect [themeAndBack] directly on the live elements.
 */
class ElementThemeCascadeTest {

    // -----------------------------------------------------------------------
    // 1. card modifier draws a background on the element that carries it.
    // -----------------------------------------------------------------------

    @Test
    fun cardModifier_drawsBackground() {
        val tree = elementTree {
            card.col {
                debugName = "cardCol"
            }
        }
        try {
            val cardCol = tree.findByName("cardCol")!!
            assertTrue(cardCol.drawsBackground,
                "card.col must draw a background (drawBackground=true), got themeAndBack=${cardCol.themeAndBack}")
        } finally {
            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 2. A plain col (no theme modifier) inside card.col does NOT draw its
    //    own background — it just inherits the cascading card theme.
    //
    //    This is the core "same cascading theme → no child background" invariant
    //    from ThemeRules.md.
    // -----------------------------------------------------------------------

    @Test
    fun plainChild_insideCard_doesNotDrawBackground() {
        val tree = elementTree {
            card.col {
                debugName = "cardParent"
                col {
                    debugName = "plainChild"
                }
            }
        }
        try {
            val cardParent = tree.findByName("cardParent")!!
            val plainChild = tree.findByName("plainChild")!!
            assertFalse(plainChild.drawsBackground,
                "A plain col inside card.col must NOT draw its own background. " +
                "Parent themeAndBack=${cardParent.themeAndBack}, child themeAndBack=${plainChild.themeAndBack}")
        } finally {
            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 3. important modifier inside card.col IS a different theme → draws its
    //    own background, switching away from the card theme.
    // -----------------------------------------------------------------------

    @Test
    fun importantChild_insideCard_drawsBackground() {
        val tree = elementTree {
            card.col {
                debugName = "cardParent"
                important.col {
                    debugName = "importantChild"
                }
            }
        }
        try {
            val importantChild = tree.findByName("importantChild")!!
            assertTrue(importantChild.drawsBackground,
                "important.col inside card.col switches to a different theme and must draw a background, " +
                "got themeAndBack=${importantChild.themeAndBack}")
        } finally {
            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 4. card inside card draws its own background — nested card applies the
    //    CardSemantic on top of an already-card-derived theme, producing a
    //    new derived theme (double-card) with drawBackground=true.
    // -----------------------------------------------------------------------

    @Test
    fun nestedCard_drawsOwnBackground() {
        val tree = elementTree {
            card.col {
                debugName = "outerCard"
                card.col {
                    debugName = "innerCard"
                }
            }
        }
        try {
            val inner = tree.findByName("innerCard")!!
            assertTrue(inner.drawsBackground,
                "card inside card must draw its own background (nested card → new derived theme), " +
                "got themeAndBack=${inner.themeAndBack}")
        } finally {
            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 5. ThemeAndBack deduplication: after the tree is built and stable,
    //    re-calling refreshTheming on a child that would compute the SAME
    //    ThemeAndBack must not change the stored value.
    //    The dedup guard (`if (value == field) return`) prevents unnecessary
    //    nativeApplyTheme calls when the theme hasn't actually changed.
    // -----------------------------------------------------------------------

    @Test
    fun themeAndBack_dedup_noChangeOnRedundantRefresh() {
        val tree = elementTree {
            card.col {
                debugName = "cardParent"
                col {
                    debugName = "child"
                }
            }
        }
        try {
            val child = tree.findByName("child")!!
            val before = child.themeAndBack

            // Force a theme recalculation; the computed value should be identical.
            child.underlyingNativeElement.refreshTheming()

            assertEquals(before, child.themeAndBack,
                "Redundant refreshTheming must not change themeAndBack when nothing has changed. " +
                "before=$before, after=${child.themeAndBack}")
        } finally {
            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 6. Changing the root theme after the tree is built must cascade through
    //    every container level (root → parent → child), updating each
    //    descendant's theme.  This is the container themeAndBackChanged hook
    //    in action: a real theme change triggers refreshTheming on children.
    // -----------------------------------------------------------------------

    @Test
    fun rootThemeSwitch_cascadesToAllDescendants() {
        val tree = elementTree {
            col {
                debugName = "parent"
                col {
                    debugName = "child"
                }
            }
        }
        try {
            val parent = tree.findByName("parent")!!
            val child = tree.findByName("child")!!
            assertEquals("test", parent.themeId, "Sanity: plain col inherits the root test theme")
            assertEquals("test", child.themeId, "Sanity: nested plain col inherits the root test theme")

            tree.root.themeChoice = ThemeDerivation.SetAsBase(Theme(id = "test2"))

            assertEquals("test2", parent.themeId,
                "Root theme switch must cascade to the first-level container")
            assertEquals("test2", child.themeId,
                "Root theme switch must cascade through containers to grandchildren")
        } finally {
            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 7. Root theme switch also cascades through a theme-switching container
    //    (card): the card re-derives its theme from the new base and its plain
    //    child follows.
    // -----------------------------------------------------------------------

    @Test
    fun rootThemeSwitch_cascadesThroughCard() {
        val tree = elementTree {
            card.col {
                debugName = "cardParent"
                col {
                    debugName = "inner"
                }
            }
        }
        try {
            val cardParent = tree.findByName("cardParent")!!
            val inner = tree.findByName("inner")!!
            val cardBefore = cardParent.themeAndBack
            val innerBefore = inner.themeAndBack

            tree.root.themeChoice = ThemeDerivation.SetAsBase(Theme(id = "test2"))

            assertNotEquals(cardBefore, cardParent.themeAndBack,
                "card container must re-derive its theme from the new base")
            assertNotEquals(innerBefore, inner.themeAndBack,
                "Root theme switch must cascade through the card container to its plain child")
            assertTrue(cardParent.drawsBackground, "card still draws its background after the switch")
            assertFalse(inner.drawsBackground, "plain child still draws no background after the switch")
        } finally {
            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 8. Re-setting the SAME root theme is a no-op for the whole tree: the
    //    themeAndBack setter's dedup check short-circuits before the cascade
    //    hook, so descendants keep their exact ThemeAndBack values.
    // -----------------------------------------------------------------------

    @Test
    fun sameRootThemeReset_leavesDescendantsUntouched() {
        val tree = elementTree {
            card.col {
                debugName = "cardParent"
                col {
                    debugName = "inner"
                }
            }
        }
        try {
            val before = tree.allThemeAndBacks()

            tree.root.themeChoice = ThemeDerivation.SetAsBase(ElementTestTheme)

            assertEquals(before, tree.allThemeAndBacks(),
                "Re-applying the identical root theme must not change any descendant's themeAndBack")
        } finally {
            tree.shutdown()
        }
    }

    // -----------------------------------------------------------------------
    // 9. Element.Debugger.countInstances tracks element creation / shutdown:
    //    after teardown, the live-instance count must return to its baseline.
    // -----------------------------------------------------------------------

    @Test
    fun instanceCount_returnsToBaselineAfterShutdown() {
        val wasEnabled = Element.Debugger.countInstances
        Element.Debugger.countInstances = true
        val baseline = Element.Debugger.liveInstanceTotal
        try {
            val tree = elementTree {
                card.col {
                    col { }
                    text { content = "hi" }
                }
            }
            val afterBuild = Element.Debugger.liveInstanceTotal
            assertTrue(afterBuild > baseline,
                "Building the tree must increase the live instance count (got $afterBuild, baseline $baseline)")

            tree.shutdown()
            val afterShutdown = Element.Debugger.liveInstanceTotal
            assertEquals(baseline, afterShutdown,
                "After shutdown all created elements must be deregistered; " +
                "expected $baseline, got $afterShutdown (possible leak)")
        } finally {
            Element.Debugger.countInstances = wasEnabled
        }
    }
}
