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
    // 6. Element.Debugger.countInstances tracks element creation / shutdown:
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
