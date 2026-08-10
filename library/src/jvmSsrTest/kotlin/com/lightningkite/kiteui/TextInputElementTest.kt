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
 * Per-element behavioral and structural tests for [TextInput].
 *
 * Coverage:
 * A. Renders — appears in snapshot with expected debugName
 * B. Binds content — setValue drives the bound Signal; echo reflects change
 * C. Disabled/enabled — TextInput always has setValue; a bound action adds submit
 * D. setValue fires content update — value echoed reactively
 * E. Submit action fires when action is set
 * F. No leak after shutdown
 */
class TextInputElementTest {

    // A. Renders

    @Test
    fun renders() = uiTest(
        content = {
            col {
                textInput {
                    debugName = "myField"
                    hint = "Enter text"
                }
            }
        }
    ) {
        val snap = snapshot()
        assertTrue(snap.contains("myField"), "Snapshot should contain textInput debugName: $snap")
    }

    // B. Binds content — setValue drives bound Signal

    @Test
    fun bindsContentToSignal() = uiTest(
        content = {
            col {
                val model = Signal("original")
                textInput {
                    debugName = "field"
                    content bind model
                }
                text {
                    debugName = "echo"
                    ::content { "echo:${model()}" }
                }
            }
        }
    ) {
        assertValue("echo", "echo:original")
        setValue("field", "hello")
        assertValue("echo", "echo:hello")
    }

    // C. setValue is always available (TextInput always has the setValue driver action)

    @Test
    fun setValueActionAlwaysPresent() = uiTest(
        content = {
            col {
                textInput { debugName = "field" }
            }
        }
    ) {
        val results = findAll("field")
        assertTrue(results.isNotEmpty(), "Should find textInput")
        val result = results.first()
        assertTrue("setValue" in result.actions, "TextInput should always have setValue action: ${result.actions}")
    }

    // C (continued). Submit action only present when action is set

    @Test
    fun submitActionPresentOnlyWhenActionSet() = uiTest(
        content = {
            col {
                val submitted = Signal("")
                textInput {
                    debugName = "field"
                    action = Action("Submit", Icon.send) { submitted.value = content.value }
                }
            }
        }
    ) {
        val results = findAll("field")
        assertTrue(results.isNotEmpty(), "Should find textInput")
        val result = results.first()
        assertTrue("submit" in result.actions, "TextInput with action should have submit: ${result.actions}")
    }

    @Test
    fun submitActionAbsentWithNoAction() = uiTest(
        content = {
            col {
                textInput { debugName = "field" }
            }
        }
    ) {
        val results = findAll("field")
        assertTrue(results.isNotEmpty(), "Should find textInput")
        val result = results.first()
        assertTrue("submit" !in result.actions, "TextInput without action should not have submit: ${result.actions}")
    }

    // D. setValue fires content update reactively

    @Test
    fun setValueUpdatesContent() = uiTest(
        content = {
            col {
                textInput { debugName = "field" }
            }
        }
    ) {
        setValue("field", "typed text")
        assertValue("field", "typed text")
    }

    // D (extended). Multiple setValue calls

    @Test
    fun multipleSetValueUpdatesContent() = uiTest(
        content = {
            col {
                textInput { debugName = "field" }
            }
        }
    ) {
        setValue("field", "first")
        assertValue("field", "first")
        setValue("field", "second")
        assertValue("field", "second")
    }

    // D. submit fires the bound action

    @Test
    fun submitFiresAction() = uiTest(
        content = {
            col {
                val result = Signal("")
                textInput {
                    debugName = "search"
                    action = Action("Search", Icon.send) { result.value = content.value }
                }
                text {
                    debugName = "result"
                    ::content { "result:${result()}" }
                }
            }
        }
    ) {
        setValue("search", "query")
        submit("search")
        assertValue("result", "result:query")
    }

    // F. No leak after shutdown

    @Test
    fun noLeakAfterShutdown() {
        val wasEnabled = Element.Debugger.countInstances
        Element.Debugger.countInstances = true
        val baseline = Element.Debugger.liveInstanceTotal
        try {
            val tree = elementTree {
                textInput {
                    debugName = "leakField"
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
