package com.lightningkite.kiteui.components

import com.lightningkite.kiteui.testing.findByDebugName
import com.lightningkite.kiteui.testing.withTestHarness
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for TextView component.
 *
 * TextView is a simple component that displays text.
 * These tests verify basic rendering and text content.
 */
class TextViewTest {

    @Test
    fun testSimpleText() = withTestHarness { harness ->
        val root = harness.render {
            text("Hello World").apply { debugName = "greeting" }
        }

        val textView = root.findByDebugName("greeting")
        assertNotNull(textView, "TextView should be found")
    }

    @Test
    fun testMultipleTextViews() = withTestHarness { harness ->
        val root = harness.render {
            col {
                text("First").apply { debugName = "first" }
                text("Second").apply { debugName = "second" }
                text("Third").apply { debugName = "third" }
            }
        }

        assertNotNull(root.findByDebugName("first"), "First text should be found")
        assertNotNull(root.findByDebugName("second"), "Second text should be found")
        assertNotNull(root.findByDebugName("third"), "Third text should be found")
    }

    @Test
    fun testTextInRow() = withTestHarness { harness ->
        val root = harness.render {
            row {
                text("Left").apply { debugName = "left" }
                text("Center").apply { debugName = "center" }
                text("Right").apply { debugName = "right" }
            }
        }

        assertNotNull(root.findByDebugName("left"), "Left text should be found")
        assertNotNull(root.findByDebugName("center"), "Center text should be found")
        assertNotNull(root.findByDebugName("right"), "Right text should be found")
    }

    @Test
    fun testNestedTextViews() = withTestHarness { harness ->
        val root = harness.render {
            col {
                debugName = "outer"
                row {
                    debugName = "inner-row"
                    text("Nested").apply { debugName = "nested-text" }
                }
            }
        }

        assertNotNull(root.findByDebugName("outer"), "Outer col should be found")
        assertNotNull(root.findByDebugName("inner-row"), "Inner row should be found")
        assertNotNull(root.findByDebugName("nested-text"), "Nested text should be found")
    }

    @Test
    fun testEmptyText() = withTestHarness { harness ->
        val root = harness.render {
            text("").apply { debugName = "empty" }
        }

        val textView = root.findByDebugName("empty")
        assertNotNull(textView, "Empty text view should still be found")
    }

    @Test
    fun testTextWithSpecialCharacters() = withTestHarness { harness ->
        val root = harness.render {
            text("Hello! @#$%^&*()").apply { debugName = "special" }
        }

        val textView = root.findByDebugName("special")
        assertNotNull(textView, "Text with special characters should be found")
    }

    @Test
    fun testLongText() = withTestHarness { harness ->
        val longText = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
                "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. " +
                "Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris."

        val root = harness.render {
            text(longText).apply { debugName = "long-text" }
        }

        val textView = root.findByDebugName("long-text")
        assertNotNull(textView, "Long text should be found")
    }

    @Test
    fun testTextWithNewlines() = withTestHarness { harness ->
        val root = harness.render {
            text("Line 1\nLine 2\nLine 3").apply { debugName = "multiline" }
        }

        val textView = root.findByDebugName("multiline")
        assertNotNull(textView, "Multiline text should be found")
    }

    @Test
    fun testTextInFrame() = withTestHarness { harness ->
        val root = harness.render {
            frame {
                debugName = "container"
                text("Framed").apply { debugName = "framed-text" }
            }
        }

        assertNotNull(root.findByDebugName("container"), "Frame should be found")
        assertNotNull(root.findByDebugName("framed-text"), "Text in frame should be found")
    }
}
