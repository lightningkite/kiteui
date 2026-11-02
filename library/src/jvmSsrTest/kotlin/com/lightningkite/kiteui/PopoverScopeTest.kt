package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/** One popover is open at a time, along with those opened from inside it; closing one closes those too. */
class PopoverScopeTest {
    private val closed = ArrayList<String>()

    /** Opens an outer popover from here and an inner one from inside it, returning the inner one's context. */
    private fun ViewWriter.nested(): ElementContext {
        val overlay = this
        lateinit var inner: ElementContext
        with(popoverWriter(overlay) { closed += "outer" }) {
            col { with(popoverWriter(overlay) { closed += "inner" }) { col { inner = context } } }
        }
        return inner
    }

    private fun withTree(content: ViewWriter.() -> Unit) {
        elementTree(content = content).shutdown()
    }

    @Test
    fun noPopoverOutsideAnyPopover() = withTree {
        col {
            assertNull(context.enclosingPopover)
            assertNull(context.currentPopover)
        }
    }

    @Test
    fun openingAPopoverClosesTheOpenOne() = withTree {
        val overlay = this
        col { popoverWriter(overlay) { closed += "a" } }
        lateinit var b: Popover
        col { b = popoverWriter(overlay) { closed += "b" }.context.enclosingPopover!! }
        assertEquals(listOf("a"), closed)
        assertSame(b, context.currentPopover)
    }

    @Test
    fun aPopoverOpenedFromInsideOneIsItsChild() = withTree {
        val inner = nested()
        val outer = context.currentPopover!!
        assertSame(inner.enclosingPopover, outer.child)
        assertSame(outer, inner.currentPopover)
        assertEquals(emptyList(), closed)
    }

    @Test
    fun openingASecondChildClosesOnlyTheFirst() = withTree {
        val overlay = this
        with(popoverWriter(overlay) { closed += "outer" }) {
            col {
                popoverWriter(overlay) { closed += "a" }
                val b = popoverWriter(overlay) { closed += "b" }.context.enclosingPopover
                assertSame(b, context.enclosingPopover!!.child)
            }
        }
        assertEquals(listOf("a"), closed)
    }

    @Test
    fun closingTheCurrentPopoverClosesEverything() = withTree {
        nested().currentPopover!!.close()
        assertEquals(listOf("inner", "outer"), closed)
        assertNull(context.currentPopover)
    }

    @Test
    fun closingTheEnclosingPopoverLeavesTheOneItWasOpenedFrom() = withTree {
        val inner = nested()
        inner.enclosingPopover!!.close()
        assertEquals(listOf("inner"), closed)
        assertNull(context.currentPopover!!.child)
    }

    @Test
    fun closingTwiceDoesNothing() = withTree {
        val outer = nested().currentPopover!!
        outer.close()
        outer.close()
        assertEquals(listOf("inner", "outer"), closed)
    }
}
