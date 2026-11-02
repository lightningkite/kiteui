package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotSame
import kotlin.test.assertNull
import kotlin.test.assertSame

// Claude: values are keyed by property name, so each addon here has its own name.
private var ElementContext.globalLabel: String by contextAddon("unset", ContextAddon.Scope.Global)
private var ElementContext.cascadingLabel: String? by contextAddon("unset")
private var ElementContext.localLabel: String by contextAddon("unset", ContextAddon.Scope.Local)
private val ElementContext.localToken: Any by lazyContextAddon(ContextAddon.Scope.Local) { Any() }
private var ElementContext.globalLate: String by lateInitContextAddon(ContextAddon.Scope.Global)
private var ElementContext.cascadingLate: String by lateInitContextAddon()
private var ElementContext.localLate: String by lateInitContextAddon(ContextAddon.Scope.Local)

/** Each [ContextAddon.Scope] decides which contexts see a value set on one. */
class ContextAddonScopeTest {
    @Test
    fun aGlobalValueSetAnywhereIsSeenEverywhere() {
        val root = ElementContext("/")
        val deep = root.child().child()
        val sibling = root.child()
        deep.globalLabel = "deep"
        assertEquals("deep", root.globalLabel)
        assertEquals("deep", sibling.globalLabel)
        sibling.globalLabel = "sibling"
        assertEquals("sibling", deep.globalLabel, "setting it again replaces it everywhere")
    }

    @Test
    fun aCascadingValueIsSeenInsideButNotAboveOrBeside() {
        val root = ElementContext("/")
        val section = root.child()
        val deep = section.child().child()
        val sibling = root.child()
        section.cascadingLabel = "section"
        assertEquals("section", deep.cascadingLabel)
        assertEquals("unset", root.cascadingLabel)
        assertEquals("unset", sibling.cascadingLabel)
    }

    @Test
    fun aCascadingValueSetAfterAChildIsMadeIsSeenByIt() {
        val root = ElementContext("/")
        val section = root.child()
        val inside = section.child()
        section.cascadingLabel = "later"
        assertEquals("later", inside.cascadingLabel)
    }

    @Test
    fun aCascadingNullHidesTheDefault() {
        val root = ElementContext("/")
        val section = root.child()
        section.cascadingLabel = null
        assertNull(section.child().cascadingLabel)
    }

    @Test
    fun aLocalValueIsSeenOnlyWhereItWasSet() {
        val root = ElementContext("/")
        val section = root.child()
        val inside = section.child()
        section.localLabel = "section"
        assertEquals("section", section.localLabel)
        assertEquals("unset", inside.localLabel)
        assertEquals("unset", root.localLabel)
    }

    @Test
    fun aLocalMadeDefaultIsKeptPerContext() {
        val section = ElementContext("/").child()
        val inside = section.child()
        assertSame(section.localToken, section.localToken)
        assertNotSame(section.localToken, inside.localToken)
    }

    @Test
    fun aLocalValueIsSeenByLeavesOfItsContainerButNotNestedContainers() {
        lateinit var nested: ElementContext
        val tree = elementTree {
            col {
                context.localLabel = "col"
                text { debugName = "leaf" }
                col { nested = context }
            }
        }
        try {
            assertEquals("col", tree.findByName("leaf")!!.context.localLabel)
            assertEquals("unset", nested.localLabel)
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun aGlobalLateInitIsSetForEveryContextAtOnce() {
        val root = ElementContext("/")
        assertFailsWith<IllegalStateException> { root.globalLate }
        root.child().globalLate = "set"
        assertEquals("set", root.child().globalLate)
    }

    @Test
    fun aCascadingLateInitThrowsOutsideWhereItWasSet() {
        val root = ElementContext("/")
        val section = root.child()
        section.cascadingLate = "set"
        assertEquals("set", section.child().cascadingLate)
        assertFailsWith<IllegalStateException> { root.cascadingLate }
    }

    @Test
    fun aLocalLateInitThrowsInsideWhereItWasSet() {
        val section = ElementContext("/").child()
        section.localLate = "set"
        assertEquals("set", section.localLate)
        assertFailsWith<IllegalStateException> { section.child().localLate }
    }
}
