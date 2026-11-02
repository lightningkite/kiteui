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
        val deep = root.split().split()
        val sibling = root.split()
        deep.globalLabel = "deep"
        assertEquals("deep", root.globalLabel)
        assertEquals("deep", sibling.globalLabel)
        sibling.globalLabel = "sibling"
        assertEquals("sibling", deep.globalLabel, "setting it again replaces it everywhere")
    }

    @Test
    fun aCascadingValueIsSeenInsideButNotAboveOrBeside() {
        val root = ElementContext("/")
        val section = root.split()
        val deep = section.split().split()
        val sibling = root.split()
        section.cascadingLabel = "section"
        assertEquals("section", deep.cascadingLabel)
        assertEquals("unset", root.cascadingLabel)
        assertEquals("unset", sibling.cascadingLabel)
    }

    @Test
    fun aCascadingValueSetAfterAChildIsMadeIsSeenByIt() {
        val root = ElementContext("/")
        val section = root.split()
        val inside = section.split()
        section.cascadingLabel = "later"
        assertEquals("later", inside.cascadingLabel)
    }

    @Test
    fun aCascadingNullHidesTheDefault() {
        val root = ElementContext("/")
        val section = root.split()
        section.cascadingLabel = null
        assertNull(section.split().cascadingLabel)
    }

    @Test
    fun aLocalValueIsSeenOnlyWhereItWasSet() {
        val root = ElementContext("/")
        val section = root.split()
        val inside = section.split()
        section.localLabel = "section"
        assertEquals("section", section.localLabel)
        assertEquals("unset", inside.localLabel)
        assertEquals("unset", root.localLabel)
    }

    @Test
    fun aLocalMadeDefaultIsKeptPerContext() {
        val section = ElementContext("/").split()
        val inside = section.split()
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
        root.split().globalLate = "set"
        assertEquals("set", root.split().globalLate)
    }

    @Test
    fun aCascadingLateInitThrowsOutsideWhereItWasSet() {
        val root = ElementContext("/")
        val section = root.split()
        section.cascadingLate = "set"
        assertEquals("set", section.split().cascadingLate)
        assertFailsWith<IllegalStateException> { root.cascadingLate }
    }

    @Test
    fun aLocalLateInitThrowsInsideWhereItWasSet() {
        val section = ElementContext("/").split()
        section.localLate = "set"
        assertEquals("set", section.localLate)
        assertFailsWith<IllegalStateException> { section.split().localLate }
    }
}
