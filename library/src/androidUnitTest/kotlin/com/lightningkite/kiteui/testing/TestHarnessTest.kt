package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.direct.text
import kotlin.test.Test
import kotlin.test.assertNotNull
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class TestHarnessTest {
    @Test
    fun testBasicRender() = withTestHarness { harness ->
        val root = harness.render {
            text("Hello World").apply { debugName = "greeting" }
        }

        assertNotNull(root, "Root view should not be null")

        val greeting = root.findByDebugName("greeting")
        assertNotNull(greeting, "Should find view by debugName")
    }
}
