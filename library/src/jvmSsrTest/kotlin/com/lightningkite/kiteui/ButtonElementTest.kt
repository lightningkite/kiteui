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
 * Per-element behavioral and structural tests for [Button].
 *
 * Coverage:
 * A. Renders — appears in snapshot with expected debugName
 * B. Binds content — reactive text label is reflected in snapshot
 * C. Disabled / enabled reflects action presence — findClickable returns empty when no action set
 * D. Click fires the reactive action — state changes after click
 * E. Theme applied — important.button draws a background in elementTree
 * F. No leak after shutdown — live instance count returns to baseline
 */
class ButtonElementTest {

    // A. Renders — button with a debugName appears in the snapshot

    @Test
    fun renders() = uiTest(
        content = {
            col {
                button {
                    debugName = "myBtn"
                    text("Press me")
                    onClick { }
                }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("myBtn"), "Snapshot should contain button debugName: $snap")
    }

    // B. Binds content — reactive label is visible in the snapshot

    @Test
    fun bindsReactiveLabel() = uiTest(
        content = {
            col {
                val label = Signal("initial")
                button {
                    debugName = "btn"
                    text { ::content { label() } }
                    onClick { }
                }
                // Trigger label change via a separate button so the test can drive it
                button {
                    debugName = "changer"
                    text("change")
                    onClick { label.value = "updated" }
                }
            }
        }
    ) {
        assertTrue(snapshot("btn").contains("initial"), "Initial label should be present: ${snapshot("btn")}")
        click("changer")
        assertTrue(snapshot("btn").contains("updated"), "Label should update reactively: ${snapshot("btn")}")
    }

    // C. Disabled/enabled reflects action presence
    //    A button with no action has no "click" driver action, so findClickable returns empty.

    @Test
    fun noActionMeansNotClickable() = uiTest(
        content = {
            col {
                button {
                    debugName = "inactiveBtn"
                    text("No action here")
                    // intentionally no onClick / action set
                }
            }
        }
    ) {
        val results = findClickable("inactiveBtn")
        assertTrue(results.isEmpty(), "Button with no action should not be clickable: $results")
    }

    @Test
    fun withActionMeansClickable() = uiTest(
        content = {
            col {
                button {
                    debugName = "activeBtn"
                    text("Click")
                    onClick { }
                }
            }
        }
    ) {
        assertEnabled("activeBtn")
    }

    // D. Click fires the reactive action — counter increments

    @Test
    fun clickFiresAction() = uiTest(
        content = {
            col {
                val counter = Signal(0)
                button {
                    debugName = "countBtn"
                    text("Inc")
                    onClick { counter.value++ }
                }
                text {
                    debugName = "count"
                    ::content { "Count: ${counter()}" }
                }
            }
        }
    ) {
        assertValue("count", "Count: 0")
        click("countBtn")
        assertValue("count", "Count: 1")
        // NB: a second immediate click is swallowed by the default 500ms Action frequency cap,
        // so one click is what we assert here (proves the click fires the reactive action).
    }

    // E. Theme applied — important.button draws a background

    @Test
    fun importantButtonDrawsBackground() {
        val tree = elementTree {
            important.button {
                debugName = "importantBtn"
                text("Important")
                onClick { }
            }
        }
        try {
            val btn = tree.findByDebugName("importantBtn")!!
            assertTrue(btn.drawsBackground,
                "important.button must draw a background, got themeAndBack=${btn.themeAndBack}")
        } finally {
            tree.shutdown()
        }
    }

    @Test
    fun plainButtonDoesNotDrawBackground() {
        val tree = elementTree {
            button {
                debugName = "plainBtn"
                text("Plain")
                onClick { }
            }
        }
        try {
            val btn = tree.findByDebugName("plainBtn")!!
            // A plain button inside the root theme does not switch themes → no background
            assertFalse(btn.drawsBackground,
                "Plain button should not draw a background, got themeAndBack=${btn.themeAndBack}")
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
                button {
                    debugName = "leakBtn"
                    text("test")
                    onClick { }
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
