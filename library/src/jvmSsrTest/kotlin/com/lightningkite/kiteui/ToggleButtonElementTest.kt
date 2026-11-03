package com.lightningkite.kiteui

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Per-element behavioral and structural tests for [ToggleButton].
 *
 * Coverage:
 * A. Renders — appears in snapshot with expected debugName
 * B. Binds checked — toggle drives the bound Signal; echo reflects change
 * C. Driver value — initial value reported as "false"
 * D. Toggle fires state change
 * E. Theme applied — active (checked) toggleButton draws a background in elementTree
 * F. No leak after shutdown
 */
class ToggleButtonElementTest {

    // A. Renders

    @Test
    fun renders() = uiTest(
        content = {
            col {
                toggleButton {
                    debugName = "myToggle"
                    text("Fav")
                }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("myToggle"), "Snapshot should contain toggleButton debugName: $snap")
    }

    // B. Binds checked — toggle drives bound Signal

    @Test
    fun bindsCheckedToSignal() = uiTest(
        content = {
            col {
                val selected = Signal(false)
                toggleButton {
                    debugName = "tb"
                    text("Select")
                    checked bind selected
                }
                text {
                    debugName = "echo"
                    ::content { "selected:${selected()}" }
                }
            }
        }
    ) {
        assertValue("echo", "selected:false")
        toggle("tb")
        assertValue("echo", "selected:true")
        toggle("tb")
        assertValue("echo", "selected:false")
    }

    // C. Driver value reflects initial boolean state

    @Test
    fun initialDriverValueIsFalse() = uiTest(
        content = {
            col {
                toggleButton {
                    debugName = "tb"
                    text("Toggle")
                }
            }
        }
    ) {
        assertValue("tb", "false")
    }

    // D. Toggle flips state

    @Test
    fun toggleFlipsState() = uiTest(
        content = {
            col {
                toggleButton {
                    debugName = "tb"
                    text("Toggle")
                }
            }
        }
    ) {
        assertValue("tb", "false")
        toggle("tb")
        assertValue("tb", "true")
        toggle("tb")
        assertValue("tb", "false")
    }

    // D. ToggleButton always has toggle action

    @Test
    fun toggleActionAlwaysPresent() = uiTest(
        content = {
            col {
                toggleButton {
                    debugName = "tb"
                    text("Toggle")
                }
            }
        }
    ) {
        val results = findAll("tb")
        assertTrue(results.isNotEmpty(), "Should find toggleButton")
        assertTrue("toggle" in results.first().actions, "ToggleButton should have toggle action: ${results.first().actions}")
    }

    // E. Theme: toggled-on toggleButton inside card draws background
    // The important modifier on the active semantic causes a background to appear.
    // We verify a toggleButton inside a card has a background when in the important theme context.

    @Test
    fun activeToggleButtonInsideImportantDrawsBackground() {
        val tree = elementTree {
            card.col {
                debugName = "card"
                important.toggleButton {
                    debugName = "activeToggle"
                    text("Active")
                }
            }
        }
        try {
            val tb = tree.findByDebugName("activeToggle")!!
            assertTrue(tb.drawsBackground,
                "important.toggleButton inside card must draw a background, got themeAndBack=${tb.themeAndBack}")
        } finally {
            tree.shutdown()
        }
    }

    // F. No leak after shutdown

    @Test
    fun noLeakAfterShutdown() {
        val wasEnabled = Element.Debugger.countInstances
        Element.Debugger.countInstances = true
        val baseline = Element.Debugger.liveInstanceTotal
        try {
            val tree = elementTree {
                toggleButton {
                    debugName = "leakTb"
                    text("test")
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
