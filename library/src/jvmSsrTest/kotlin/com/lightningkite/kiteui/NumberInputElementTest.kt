package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
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
 * Per-element behavioral and structural tests for [NumberInput].
 *
 * Coverage:
 * A. Renders — appears in snapshot with expected debugName
 * B. Binds content — setValue drives the bound Signal; echo reflects change
 * C. setValue action always present; submit only when action set
 * D. setValue fires content update; non-numeric argument throws
 * E. Theme: N/A — NumberInput carries no semantic theme modifier.
 * F. No leak after shutdown
 */
class NumberInputElementTest {

    // A. Renders

    @Test
    fun renders() = uiTest(
        content = {
            col {
                numberInput {
                    debugName = "myNumber"
                    hint = "Enter amount"
                }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("myNumber"), "Snapshot should contain numberInput debugName: $snap")
    }

    // B. Binds content — setValue drives bound Signal

    @Test
    fun bindsContentToSignal() = uiTest(
        content = {
            col {
                val amount = Signal<Double?>(null)
                numberInput {
                    debugName = "amount"
                    content bind amount
                }
                text {
                    debugName = "echo"
                    ::content { "amount:${amount()}" }
                }
            }
        }
    ) {
        setValue("amount", "42.5")
        assertValue("echo", "amount:42.5")
        setValue("amount", "100.0")
        assertValue("echo", "amount:100.0")
    }

    // C. setValue action always present

    @Test
    fun setValueActionAlwaysPresent() = uiTest(
        content = {
            col {
                numberInput { debugName = "num" }
            }
        }
    ) {
        val results = findAll("num")
        assertTrue(results.isNotEmpty(), "Should find numberInput")
        assertTrue("setValue" in results.first().actions, "NumberInput should always have setValue action: ${results.first().actions}")
    }

    // C. Submit action only present when action is set

    @Test
    fun submitActionPresentOnlyWhenActionSet() = uiTest(
        content = {
            col {
                val result = Signal<Double?>(null)
                numberInput {
                    debugName = "num"
                    action = Action("Submit", Icon.send) { result.value = content.value }
                }
            }
        }
    ) {
        val results = findAll("num")
        assertTrue(results.isNotEmpty(), "Should find numberInput")
        assertTrue("submit" in results.first().actions, "NumberInput with action should have submit: ${results.first().actions}")
    }

    // D. setValue fires content update

    @Test
    fun setValueUpdatesContent() = uiTest(
        content = {
            col {
                numberInput { debugName = "num" }
            }
        }
    ) {
        setValue("num", "7.0")
        assertValue("num", "7.0")
    }

    // D. Non-numeric value throws DriverActionException

    @Test
    fun nonNumericValueThrows() = uiTest(
        content = {
            col {
                numberInput { debugName = "num" }
            }
        }
    ) {
        assertFailsWith<DriverActionException>("Non-numeric setValue should throw") {
            setValue("num", "not-a-number")
        }
    }

    // D. Range clamping when range is set

    @Test
    fun setValueClampsToRange() = uiTest(
        content = {
            col {
                numberInput {
                    debugName = "num"
                    range = 0.0..10.0
                }
            }
        }
    ) {
        // Above max → clamped to 10
        setValue("num", "100.0")
        assertValue("num", "10.0")

        // Below min → clamped to 0
        setValue("num", "-5.0")
        assertValue("num", "0.0")
    }

    // F. No leak after shutdown

    @Test
    fun noLeakAfterShutdown() {
        val wasEnabled = Element.Debugger.countInstances
        Element.Debugger.countInstances = true
        val baseline = Element.Debugger.liveInstanceTotal
        try {
            val tree = elementTree {
                numberInput {
                    debugName = "leakNum"
                    hint = "test"
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
