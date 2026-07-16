package com.lightningkite.kiteui

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.DriverActionException
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Per-element behavioral and structural tests for [Select].
 *
 * Coverage:
 * A. Renders — appears in snapshot with expected debugName
 * B. Binds value — setValue drives the bound Signal; echo reflects change
 * C. setValue action present after bind() is called
 * D. setValue fires state change and updates bound Signal
 * E. Theme: N/A — Select carries no standalone semantic theme modifier;
 *    it inherits theme but draws no background itself.
 * F. No leak after shutdown
 */
class SelectElementTest {

    // A. Renders

    @Test
    fun renders() = uiTest(
        content = {
            col {
                val choice = Signal("Red")
                select {
                    debugName = "mySelect"
                    bind(
                        edits = choice,
                        data = Signal(listOf("Red", "Green", "Blue")),
                        render = { it }
                    )
                }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("mySelect"), "Snapshot should contain select debugName: $snap")
    }

    // B. Binds value — setValue drives bound Signal

    @Test
    fun bindsValueToSignal() = uiTest(
        content = {
            col {
                val choice = Signal("Red")
                select {
                    debugName = "colorSelect"
                    bind(
                        edits = choice,
                        data = Signal(listOf("Red", "Green", "Blue")),
                        render = { it }
                    )
                }
                text {
                    debugName = "echo"
                    ::content { "color:${choice()}" }
                }
            }
        }
    ) {
        assertValue("echo", "color:Red")
        setValue("colorSelect", "Blue")
        assertValue("echo", "color:Blue")
        setValue("colorSelect", "Green")
        assertValue("echo", "color:Green")
    }

    // C. setValue action present after bind() is called

    @Test
    fun setValueActionPresentAfterBind() = uiTest(
        content = {
            col {
                val choice = Signal("A")
                select {
                    debugName = "sel"
                    bind(
                        edits = choice,
                        data = Signal(listOf("A", "B", "C")),
                        render = { it }
                    )
                }
            }
        }
    ) {
        val results = findAll("sel")
        assertTrue(results.isNotEmpty(), "Should find select")
        assertTrue("setValue" in results.first().actions, "Select should have setValue after bind: ${results.first().actions}")
    }

    // D. setValue fires state change (initial value reported correctly too)

    @Test
    fun setValueUpdatesDriverValue() = uiTest(
        content = {
            col {
                val choice = Signal("A")
                select {
                    debugName = "sel"
                    bind(
                        edits = choice,
                        data = Signal(listOf("A", "B", "C")),
                        render = { it }
                    )
                }
            }
        }
    ) {
        assertValue("sel", "A")
        setValue("sel", "B")
        assertValue("sel", "B")
        setValue("sel", "C")
        assertValue("sel", "C")
    }

    // D. Invalid option throws DriverActionException

    @Test
    fun invalidOptionThrows() = uiTest(
        content = {
            col {
                val choice = Signal("A")
                select {
                    debugName = "sel"
                    bind(
                        edits = choice,
                        data = Signal(listOf("A", "B")),
                        render = { it }
                    )
                }
            }
        }
    ) {
        assertFailsWith<DriverActionException>("Should throw for unknown option") {
            setValue("sel", "Z")
        }
    }

    // E. Theme: N/A — Select inherits the ambient theme from its parent but
    // does not draw its own background through a semantic theme modifier.
    // Structural cascade is verified in ElementThemeCascadeTest.

    // F. No leak after shutdown

    @Test
    fun noLeakAfterShutdown() {
        val wasEnabled = Element.Debugger.countInstances
        Element.Debugger.countInstances = true
        val baseline = Element.Debugger.liveInstanceTotal
        try {
            val tree = elementTree {
                val choice = Signal("A")
                select {
                    debugName = "leakSel"
                    bind(
                        edits = choice,
                        data = Signal(listOf("A", "B")),
                        render = { it }
                    )
                }
            }
            assertTrue(Element.Debugger.liveInstanceTotal > baseline, "Building tree must create instances")
            tree.shutdown()
            assertEquals(baseline, Element.Debugger.liveInstanceTotal,
                "All instances must be released after shutdown")
        } finally {
            Element.Debugger.countInstances = wasEnabled
        }
    }
}
