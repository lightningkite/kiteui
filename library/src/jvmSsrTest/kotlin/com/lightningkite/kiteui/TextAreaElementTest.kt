package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Per-element behavioral and structural tests for [TextArea].
 *
 * Coverage:
 * A. Renders — appears in snapshot with expected debugName
 * B. Binds content — setValue drives the bound Signal; echo reflects change
 * C. setValue action always present; submit only when action set
 * D. setValue fires content update
 * E. Theme: N/A — TextArea carries no semantic theme modifier.
 * F. No leak after shutdown
 */
class TextAreaElementTest {

    // A. Renders

    @Test
    fun renders() = uiTest(
        content = {
            col {
                textArea {
                    debugName = "myArea"
                    hint = "Enter notes"
                }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("myArea"), "Snapshot should contain textArea debugName: $snap")
    }

    // B. Binds content — setValue drives bound Signal

    @Test
    fun bindsContentToSignal() = uiTest(
        content = {
            col {
                val notes = Signal("initial notes")
                textArea {
                    debugName = "area"
                    content bind notes
                }
                text {
                    debugName = "echo"
                    ::content { "notes:${notes()}" }
                }
            }
        }
    ) {
        assertValue("echo", "notes:initial notes")
        setValue("area", "updated notes")
        assertValue("echo", "notes:updated notes")
    }

    // C. setValue action always present

    @Test
    fun setValueActionAlwaysPresent() = uiTest(
        content = {
            col {
                textArea { debugName = "area" }
            }
        }
    ) {
        val results = findAll("area")
        assertTrue(results.isNotEmpty(), "Should find textArea")
        assertTrue("setValue" in results.first().actions, "TextArea should always have setValue action: ${results.first().actions}")
    }

    // C. Submit action only present when action is set

    @Test
    fun submitActionPresentOnlyWhenActionSet() = uiTest(
        content = {
            col {
                val result = Signal("")
                textArea {
                    debugName = "area"
                    action = Action("Submit", Icon.send) { result.value = content.value }
                }
            }
        }
    ) {
        val results = findAll("area")
        assertTrue(results.isNotEmpty(), "Should find textArea")
        assertTrue("submit" in results.first().actions, "TextArea with action should have submit: ${results.first().actions}")
    }

    @Test
    fun submitActionAbsentWithNoAction() = uiTest(
        content = {
            col {
                textArea { debugName = "area" }
            }
        }
    ) {
        val results = findAll("area")
        assertTrue(results.isNotEmpty(), "Should find textArea")
        assertTrue("submit" !in results.first().actions, "TextArea without action should not have submit: ${results.first().actions}")
    }

    // D. setValue fires content update

    @Test
    fun setValueUpdatesContent() = uiTest(
        content = {
            col {
                textArea { debugName = "area" }
            }
        }
    ) {
        setValue("area", "some text")
        assertValue("area", "some text")
    }

    // D. setValue preserves multiword content (spaces in value)

    @Test
    fun setValuePreservesSpaces() = uiTest(
        content = {
            col {
                textArea { debugName = "area" }
            }
        }
    ) {
        setValue("area", "hello world with spaces")
        assertValue("area", "hello world with spaces")
    }

    // F. No leak after shutdown

    @Test
    fun noLeakAfterShutdown() {
        val wasEnabled = Element.Debugger.countInstances
        Element.Debugger.countInstances = true
        val baseline = Element.Debugger.liveInstanceTotal
        try {
            val tree = elementTree {
                textArea {
                    debugName = "leakArea"
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
