package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.titleDepth
import com.lightningkite.kiteui.views.l2.titledSection
import kotlin.test.Test
import kotlin.test.assertEquals

private var ElementContext.label: String by contextAddon("unset")

/** A value set in a container is seen by everything inside it and nothing outside it. */
class ContextScopeTest {
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
}
