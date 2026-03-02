// by Claude - verifies UiTestScope via the SSR uiTest() entry point.
// These tests exercise the full pipeline: render → snapshot → dispatch → assert.
package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class UiTestScopeTest {

    @Test
    fun snapshotContainsComponents() = uiTest(content = {
        textInput {
            debugName = "email"
        }
        button {
            debugName = "submit"
            text("Submit")
        }
    }) {
        val snap = snapshot()
        assertNotNull(snap.findById("email"), "email should be in snapshot")
        assertNotNull(snap.findById("submit"), "submit should be in snapshot")
    }

    @Test
    fun setValueUpdatesReactiveState() = uiTest(content = {
        val name = Signal("")
        textInput {
            debugName = "name"
            content bind name
        }
    }) {
        setValue("name", "Alice")
        assertValue("name", "Alice")
    }

    @Test
    fun clickInvokesHandler() = uiTest(content = {
        var clicked = false
        button {
            debugName = "btn"
            text("Click Me")
            onClick(frequencyCap = null) { clicked = true }
        }
        // Use a text field to observe the click side-effect
        val status = Signal("not clicked")
        textInput {
            debugName = "status"
            content bind status
        }
        button {
            debugName = "action-btn"
            text("Do it")
            onClick(frequencyCap = null) { status.value = "clicked!" }
        }
    }) {
        click("action-btn")
        assertValue("status", "clicked!")
    }

    @Test
    fun requireThrowsForMissingComponent() = uiTest(content = {
        text("Hello")
    }) {
        var threw = false
        try {
            require("nonexistent")
        } catch (e: AssertionError) {
            threw = true
            assertTrue(e.message?.contains("nonexistent") == true)
        }
        assertTrue(threw, "require() should throw for missing component")
    }

    @Test
    fun assertVisibleWorks() = uiTest(content = {
        textInput {
            debugName = "visible-field"
        }
    }) {
        assertVisible("visible-field")
    }

    @Test
    fun dumpSnapshotDoesNotThrow() = uiTest(content = {
        text("Hello World")
    }) {
        // Just verify it doesn't throw
        dumpSnapshot()
    }
}
