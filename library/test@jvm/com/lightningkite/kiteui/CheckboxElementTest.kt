package com.lightningkite.kiteui

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Per-element behavioral and structural tests for [Checkbox].
 *
 * Coverage:
 * A. Renders — appears in snapshot with expected debugName
 * B. Binds checked — toggle drives the bound Signal; echo reflects change
 * C. Driver value — initial value reported as "false"
 * D. Toggle fires state change
 * E. setValue accepts "true"/"false" strings
 * F. No leak after shutdown
 *
 * Note: Checkbox does not have a "theme applied" (E-original) test because
 * it has no semantic theme modifier comparable to important.button; the
 * element uses fieldTheme internally, which is not observable via drawsBackground
 * at the element level on jvmSsr.
 */
class CheckboxElementTest {

    // A. Renders

    @Test
    fun renders() = uiTest(
        content = {
            col {
                checkbox { debugName = "myCheck" }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("myCheck"), "Snapshot should contain checkbox debugName: $snap")
    }

    // B. Binds checked — toggle drives bound Signal

    @Test
    fun bindsCheckedToSignal() = uiTest(
        content = {
            col {
                val agreed = Signal(false)
                checkbox {
                    debugName = "agree"
                    checked bind agreed
                }
                text {
                    debugName = "echo"
                    ::content { "agreed:${agreed()}" }
                }
            }
        }
    ) {
        assertValue("echo", "agreed:false")
        toggle("agree")
        assertValue("echo", "agreed:true")
        toggle("agree")
        assertValue("echo", "agreed:false")
    }

    // C. Driver value reflects initial boolean state

    @Test
    fun initialDriverValueIsFalse() = uiTest(
        content = {
            col {
                checkbox { debugName = "chk" }
            }
        }
    ) {
        assertValue("chk", "false")
    }

    // D. Toggle flips state

    @Test
    fun toggleFlipsState() = uiTest(
        content = {
            col {
                checkbox { debugName = "chk" }
            }
        }
    ) {
        assertValue("chk", "false")
        toggle("chk")
        assertValue("chk", "true")
        toggle("chk")
        assertValue("chk", "false")
    }

    // D. Checkbox always has toggle action (it's always interactive)

    @Test
    fun toggleActionAlwaysPresent() = uiTest(
        content = {
            col {
                checkbox { debugName = "chk" }
            }
        }
    ) {
        val results = findAll("chk")
        assertTrue(results.isNotEmpty(), "Should find checkbox")
        assertTrue("toggle" in results.first().actions, "Checkbox should have toggle action: ${results.first().actions}")
    }

    // E. setValue accepts boolean strings

    @Test
    fun setValueAcceptsTrueAndFalse() = uiTest(
        content = {
            col {
                checkbox { debugName = "chk" }
            }
        }
    ) {
        assertValue("chk", "false")
        setValue("chk", "true")
        assertValue("chk", "true")
        setValue("chk", "false")
        assertValue("chk", "false")
    }

    // F. No leak after shutdown

    @Test
    fun noLeakAfterShutdown() {
        val wasEnabled = Element.Debugger.countInstances
        Element.Debugger.countInstances = true
        val baseline = Element.Debugger.liveInstanceTotal
        try {
            val tree = elementTree {
                checkbox { debugName = "leakCheck" }
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
