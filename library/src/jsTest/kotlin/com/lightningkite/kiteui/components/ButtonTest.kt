package com.lightningkite.kiteui.components

import com.lightningkite.kiteui.testing.click
import com.lightningkite.kiteui.testing.findByDebugName
import com.lightningkite.kiteui.testing.withTestHarness
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.direct.onClick
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class ButtonTest {
    @Test
    fun testButtonClick() = withTestHarness { harness ->
        var clickCount = 0

        val root = harness.render {
            button {
                debugName = "test-button"
                text("Click Me")
                onClick { clickCount++ }
            }
        }

        val button = root.findByDebugName("test-button")
        assertNotNull(button, "Button should exist")

        // Click the button
        button.click()

        // Verify click was handled
        assertEquals(1, clickCount, "Button should have been clicked once")

        // Click again
        button.click()
        assertEquals(2, clickCount, "Button should have been clicked twice")
    }

    @Test
    fun testButtonWithReactiveState() = withTestHarness { harness ->
        var counter = 0

        val root = harness.render {
            button {
                debugName = "counter-button"
                text("Click to increment")
                onClick { counter++ }
            }
        }

        val button = root.findByDebugName("counter-button")
        assertNotNull(button)

        // Initial state
        assertEquals(0, counter)

        // Click and verify state changes
        button.click()
        assertEquals(1, counter)

        button.click()
        assertEquals(2, counter)
    }
}
