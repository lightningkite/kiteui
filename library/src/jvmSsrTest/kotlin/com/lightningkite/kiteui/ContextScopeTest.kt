package com.lightningkite.kiteui

import com.lightningkite.kiteui.exceptions.ExceptionHandler
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.overlayFrame
import com.lightningkite.kiteui.views.l2.titleDepth
import com.lightningkite.kiteui.views.l2.titledSection
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertSame

private var ElementContext.label: String by contextAddon("unset")
private val ElementContext.madeFrom: ElementContext by lazyContextAddon { it }
private val ElementContext.madeFromGlobally: ElementContext by lazyContextAddon(ContextAddon.Scope.Global) { it }

/** A value set in a container is seen by everything inside it and nothing outside it. */
class ContextScopeTest {
    @Test
    fun aPlainDefaultIsNotStored() {
        val root = ElementContext("/")
        assertEquals("unset", root.split().label)
        assertFalse(root.addons.containsKey("label"))
    }

    @Test
    fun aMadeDefaultIsMadeFromTheContextReadAndSharedInsideIt() {
        val root = ElementContext("/")
        val section = root.split()
        val inside = section.split()
        assertSame(section, section.madeFrom)
        assertSame(section, inside.madeFrom, "inside the reader, its default is seen as if set there")
        val sibling = root.split()
        assertSame(sibling, sibling.madeFrom, "a sibling makes its own")
        assertFalse(root.addons.containsKey("madeFrom"))
        assertSame(root, inside.madeFromGlobally, "a global default is made from the root")
    }

    @Test
    fun valuesStayInsideTheContainerThatSetThem() {
        lateinit var nested: ElementContext
        lateinit var sibling: ElementContext
        val tree = elementTree {
            col {
                context.label = "a"
                text { debugName = "leaf" }
                col { nested = context }
            }
            col { sibling = context }
        }
        try {
            assertEquals("a", tree.findByName("leaf")!!.context.label)
            assertEquals("a", nested.label)
            assertEquals("unset", sibling.label)
            assertEquals("unset", tree.context.label)
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun nestedValuesShadowWithoutChangingTheOuterOne() {
        lateinit var outer: ElementContext
        lateinit var inner: ElementContext
        val tree = elementTree {
            col {
                context.label = "outer"
                outer = context
                col { context.label = "inner"; inner = context }
            }
        }
        try {
            assertEquals("outer", outer.label)
            assertEquals("inner", inner.label)
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun titleDepthHoldsAfterTheSectionHasRendered() {
        lateinit var section: ElementContext
        lateinit var nested: ElementContext
        val tree = elementTree {
            titledSection("Outer") {
                section = context
                titledSection("Inner") { nested = context }
            }
        }
        try {
            // Claude: read after rendering, as a deferred render (a forEach, say) would.
            assertEquals(1, section.titleDepth)
            assertEquals(2, nested.titleDepth)
            assertEquals(0, tree.context.titleDepth)
        } finally {
            tree.shutdown()
        }
    }

    // Claude: overlays are shown in, and read the context of, the overlay frame; the opener's scoped values
    // (exception handlers, heading depth, the popover it's in) would be wrong there.
    @Test
    fun popoverContentDoesNotSeeTheOpenersValues() {
        var seen: String? = null
        val tree = elementTree {
            val overlay = this
            col {
                context.label = "opener"
                with(popoverWriter(overlay) {}) { col { seen = context.label } }
            }
        }
        tree.shutdown()
        assertEquals("unset", seen)
    }

    @Test
    fun aDialogOpenedFromAPopoverIsNotInIt() {
        var inside: Popover? = Popover {}
        val tree = elementTree {
            context.overlayFrame = this as ContainerElement
            val overlay = this
            with(popoverWriter(overlay) {}) {
                col { context.overlay(modal = false) { col { inside = context.enclosingPopover } } }
            }
        }
        tree.shutdown()
        assertNull(inside)
    }

    @Test
    fun errorsInAPopoverSkipTheOpenersHandlers() {
        val caught = ArrayList<String>()
        lateinit var inside: Element
        val tree = elementTree {
            val overlay = this
            col {
                context.exceptionHandlers += ExceptionHandler(1f) { e -> caught += e.message!!; {} }
                with(popoverWriter(overlay) {}) { col { text { inside = this } } }
            }
        }
        try {
            inside.context.handleException(Exception("boom"))
            assertEquals(emptyList(), caught)
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun eachTreeHasItsOwnDialogStack() {
        val a = elementTree {}
        val b = elementTree {}
        try {
            var dismissed = false
            val unregister = a.context.pushDismissableDialog { dismissed = true }
            assertFalse(b.context.dismissTopDialog())
            assertTrue(a.context.dismissTopDialog())
            assertTrue(dismissed)
            unregister()
        } finally {
            a.shutdown()
            b.shutdown()
        }
    }
}
