package com.lightningkite.kiteui

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Per-element behavioral and structural tests for [Switch].
 *
 * Coverage:
 * A. Renders — appears in snapshot with expected debugName
 * B. Binds checked — toggle drives the bound Signal; echo reflects change
 * C. Driver value — initial value reported as "false"
 * D. Toggle fires state change
 * E. setValue accepts "true"/"false" strings
 * F. No leak after shutdown
 *
 * Note on E (theme): Switch does not carry a semantic theme like important;
 * structural theme cascade testing is not applicable to Switch.
 */
class SwitchElementTest {

    // A. Renders

    @Test
    fun renders() = uiTest(
        content = {
            col {
                switch { debugName = "mySwitch" }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("mySwitch"), "Snapshot should contain switch debugName: $snap")
    }

    // B. Binds checked — toggle drives bound Signal

    @Test
    fun bindsCheckedToSignal() = uiTest(
        content = {
            col {
                val enabled = Signal(false)
                switch {
                    debugName = "sw"
                    checked bind enabled
                }
                text {
                    debugName = "echo"
                    ::content { "enabled:${enabled()}" }
                }
            }
        }
    ) {
        assertValue("echo", "enabled:false")
        toggle("sw")
        assertValue("echo", "enabled:true")
        toggle("sw")
        assertValue("echo", "enabled:false")
    }

    // C. Driver value reflects initial boolean state

    @Test
    fun initialDriverValueIsFalse() = uiTest(
        content = {
            col {
                switch { debugName = "sw" }
            }
        }
    ) {
        assertValue("sw", "false")
    }

    // D. Toggle flips state

    @Test
    fun toggleFlipsState() = uiTest(
        content = {
            col {
                switch { debugName = "sw" }
            }
        }
    ) {
        assertValue("sw", "false")
        toggle("sw")
        assertValue("sw", "true")
    }

    // D. Switch always has toggle action

    @Test
    fun toggleActionAlwaysPresent() = uiTest(
        content = {
            col {
                switch { debugName = "sw" }
            }
        }
    ) {
        val results = findAll("sw")
        assertTrue(results.isNotEmpty(), "Should find switch")
        assertTrue("toggle" in results.first().actions, "Switch should have toggle action: ${results.first().actions}")
    }

    // E. setValue accepts boolean strings

    @Test
    fun setValueAcceptsTrueAndFalse() = uiTest(
        content = {
            col {
                switch { debugName = "sw" }
            }
        }
    ) {
        assertValue("sw", "false")
        setValue("sw", "true")
        assertValue("sw", "true")
        setValue("sw", "false")
        assertValue("sw", "false")
    }

    // F. No leak after shutdown

    @Test
    fun noLeakAfterShutdown() {
        val wasEnabled = Element.Debugger.countInstances
        Element.Debugger.countInstances = true
        val baseline = Element.Debugger.liveInstanceTotal
        try {
            val tree = elementTree {
                switch { debugName = "leakSwitch" }
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
