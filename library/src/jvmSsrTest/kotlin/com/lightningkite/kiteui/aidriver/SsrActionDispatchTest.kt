// by Claude - verifies that AI driver action dispatch works on SSR views.
// CommonHtml view overrides use direct state manipulation, so they work
// even though SSR's FutureElement.addEventListener is a no-op.
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.ssr.SsrContext
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class SsrActionDispatchTest {
    init {
        @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    private fun renderSsr(content: com.lightningkite.kiteui.views.ViewWriter.() -> Unit): com.lightningkite.kiteui.views.direct.Frame {
        val ctx = SsrContext("/")
        ctx.theme = Theme(id = "test")
        ctx.render(content)
        return ctx.rootFrame ?: error("No root frame")
    }

    @Test
    fun textFieldSetValue() = runTest {
        val email = Signal("")

        val root = renderSsr {
            textInput {
                debugName = "email"
                content bind email
            }
        }

        // Verify initial snapshot has the text field
        val snap = buildSnapshot(root, null)
        val emailComponent = snap.findById("email")
        assertNotNull(emailComponent, "email component should exist in snapshot")
        assertTrue(emailComponent.actions.contains("setValue"), "textInput should support setValue")

        // Dispatch setValue
        val result = dispatchAction(UiAction.SetValue("email", "test@example.com"), root, null)
        assertTrue(result.success, "SetValue should succeed: ${result.error}")

        // Verify the reactive property was updated
        assertEquals("test@example.com", email.value, "Property should be updated")

        // Verify snapshot shows new value
        val snap2 = buildSnapshot(root, null)
        val emailComponent2 = snap2.findById("email")
        assertEquals("test@example.com", emailComponent2?.value, "Snapshot should reflect new value")
    }

    @Test
    fun buttonClick() = runTest {
        var clicked = false

        val root = renderSsr {
            button {
                debugName = "btn"
                text("Click Me")
                onClick(frequencyCap = null) { clicked = true }
            }
        }

        // Verify button exists in snapshot
        val snap = buildSnapshot(root, null)
        val btn = snap.findById("btn")
        assertNotNull(btn, "button should exist in snapshot")
        assertTrue(btn.actions.contains("click"), "button should support click")

        // Dispatch click
        val result = dispatchAction(UiAction.Click("btn"), root, null)
        assertTrue(result.success, "Click should succeed: ${result.error}")

        // Verify handler was called
        assertTrue(clicked, "onClick handler should have been called")
    }

    @Test
    fun viewNotFound() = runTest {
        val root = renderSsr {
            text("Hello")
        }

        val result = dispatchAction(UiAction.Click("nonexistent"), root, null)
        assertTrue(!result.success, "Should fail for nonexistent view")
        assertTrue(result.error?.contains("not found") == true, "Error should mention not found")
    }

    // by Claude - helper to find component in snapshot by ID
    private fun UiSnapshot.findById(id: String): UiComponent? {
        fun search(components: List<UiComponent>): UiComponent? {
            for (c in components) {
                if (c.id == id) return c
                search(c.children)?.let { return it }
            }
            return null
        }
        return search(components)
    }
}
