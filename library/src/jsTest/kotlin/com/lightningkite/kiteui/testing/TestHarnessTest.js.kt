package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.direct.text
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertEquals

class JsTestHarnessTest {
    @Test
    fun testBasicRender(): Unit {
        withTestHarness { harness ->
            val root = harness.render {
                text("Hello, Test!")
            }
            assertNotNull(root)
            println("Test harness works!")
        }
    }

    @Test
    fun testFindByDebugName(): Unit {
        withTestHarness { harness ->
            val root = harness.render {
                text("First").apply { debugName = "first-text" }
                text("Second").apply { debugName = "second-text" }
                text("Third")
            }

            val found = root.findByDebugName("second-text")
            assertNotNull(found, "Should find view with debugName 'second-text'")
            assertEquals("second-text", found.debugName)
            println("View finding works!")
        }
    }
}
