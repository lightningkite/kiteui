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
 * Per-element behavioral and structural tests for [Slider].
 *
 * Coverage:
 * A. Renders — appears in snapshot with expected debugName
 * B. Binds value — setValue drives the bound Signal; echo reflects change
 * C. setValue action always present
 * D. setValue clamps to min/max range
 * E. Theme: N/A — Slider carries no standalone semantic theme modifier.
 * F. No leak after shutdown
 */
class SliderElementTest {

    // A. Renders

    @Test
    fun renders() = uiTest(
        content = {
            col {
                slider {
                    debugName = "mySlider"
                    min = 0f
                    max = 100f
                }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("mySlider"), "Snapshot should contain slider debugName: $snap")
    }

    // B. Binds value — setValue drives bound Signal

    @Test
    fun bindsValueToSignal() = uiTest(
        content = {
            col {
                val volume = Signal(0f)
                slider {
                    debugName = "vol"
                    min = 0f
                    max = 100f
                    value bind volume
                }
                text {
                    debugName = "echo"
                    ::content { "vol:${volume()}" }
                }
            }
        }
    ) {
        setValue("vol", "50.0")
        assertValue("echo", "vol:50.0")
        setValue("vol", "75.0")
        assertValue("echo", "vol:75.0")
    }

    // C. setValue action always present

    @Test
    fun setValueActionAlwaysPresent() = uiTest(
        content = {
            col {
                slider {
                    debugName = "sl"
                    min = 0f
                    max = 1f
                }
            }
        }
    ) {
        val results = findAll("sl")
        assertTrue(results.isNotEmpty(), "Should find slider")
        assertTrue("setValue" in results.first().actions, "Slider should always have setValue action: ${results.first().actions}")
    }

    // D. setValue and driver value reflect the set value

    @Test
    fun setValueUpdatesDriverValue() = uiTest(
        content = {
            col {
                slider {
                    debugName = "sl"
                    min = 0f
                    max = 100f
                }
            }
        }
    ) {
        setValue("sl", "42.0")
        assertValue("sl", "42.0")
    }

    // D. setValue clamps values outside min/max to the boundary

    @Test
    fun setValueClampsToRange() = uiTest(
        content = {
            col {
                slider {
                    debugName = "sl"
                    // Set max before min: setting min above the default max (1.0) first would
                    // momentarily create an inverted [10, 1] range and throw during coercion.
                    max = 90f
                    min = 10f
                }
            }
        }
    ) {
        // Above max → clamped to 90
        setValue("sl", "200.0")
        assertValue("sl", "90.0")

        // Below min → clamped to 10
        setValue("sl", "-5.0")
        assertValue("sl", "10.0")
    }

    // D. setValue requires a numeric argument

    @Test
    fun setValueRequiresNumber() = uiTest(
        content = {
            col {
                slider { debugName = "sl"; min = 0f; max = 1f }
            }
        }
    ) {
        assertFailsWith<DriverActionException>("Non-numeric setValue should throw") {
            setValue("sl", "not-a-number")
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
                slider {
                    debugName = "leakSlider"
                    min = 0f
                    max = 1f
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
