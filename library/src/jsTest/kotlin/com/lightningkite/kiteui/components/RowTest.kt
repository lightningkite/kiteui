package com.lightningkite.kiteui.components

import com.lightningkite.kiteui.testing.findByDebugName
import com.lightningkite.kiteui.testing.withTestHarness
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * Tests for Row component (horizontal layout).
 *
 * Row arranges children horizontally.
 */
class RowTest {

    @Test
    fun testEmptyRow() = withTestHarness { harness ->
        val root = harness.render {
            row {
                debugName = "empty-row"
            }
        }

        val rowView = root.findByDebugName("empty-row")
        assertNotNull(rowView, "Empty row should be found")
    }

    @Test
    fun testRowWithSingleChild() = withTestHarness { harness ->
        val root = harness.render {
            row {
                debugName = "single-child-row"
                text("Only child").apply { debugName = "child" }
            }
        }

        assertNotNull(root.findByDebugName("single-child-row"), "Row should be found")
        assertNotNull(root.findByDebugName("child"), "Child should be found")
    }

    @Test
    fun testRowWithMultipleChildren() = withTestHarness { harness ->
        val root = harness.render {
            row {
                debugName = "multi-child-row"
                text("First").apply { debugName = "first" }
                text("Second").apply { debugName = "second" }
                text("Third").apply { debugName = "third" }
            }
        }

        assertNotNull(root.findByDebugName("multi-child-row"), "Row should be found")
        assertNotNull(root.findByDebugName("first"), "First child should be found")
        assertNotNull(root.findByDebugName("second"), "Second child should be found")
        assertNotNull(root.findByDebugName("third"), "Third child should be found")
    }

    @Test
    fun testNestedRows() = withTestHarness { harness ->
        val root = harness.render {
            row {
                debugName = "outer-row"
                text("Left").apply { debugName = "left" }
                row {
                    debugName = "inner-row"
                    text("Inner 1").apply { debugName = "inner1" }
                    text("Inner 2").apply { debugName = "inner2" }
                }
                text("Right").apply { debugName = "right" }
            }
        }

        assertNotNull(root.findByDebugName("outer-row"), "Outer row should be found")
        assertNotNull(root.findByDebugName("inner-row"), "Inner row should be found")
        assertNotNull(root.findByDebugName("left"), "Left child should be found")
        assertNotNull(root.findByDebugName("right"), "Right child should be found")
        assertNotNull(root.findByDebugName("inner1"), "Inner child 1 should be found")
        assertNotNull(root.findByDebugName("inner2"), "Inner child 2 should be found")
    }

    @Test
    fun testRowInColumn() = withTestHarness { harness ->
        val root = harness.render {
            col {
                debugName = "column"
                row {
                    debugName = "row-in-col"
                    text("A").apply { debugName = "a" }
                    text("B").apply { debugName = "b" }
                }
            }
        }

        assertNotNull(root.findByDebugName("column"), "Column should be found")
        assertNotNull(root.findByDebugName("row-in-col"), "Row in column should be found")
        assertNotNull(root.findByDebugName("a"), "Child A should be found")
        assertNotNull(root.findByDebugName("b"), "Child B should be found")
    }

    @Test
    fun testRowWithMixedChildren() = withTestHarness { harness ->
        val root = harness.render {
            row {
                debugName = "mixed-row"
                text("Text").apply { debugName = "text-child" }
                col {
                    debugName = "col-child"
                    text("Nested").apply { debugName = "nested-text" }
                }
                frame {
                    debugName = "frame-child"
                    text("Framed").apply { debugName = "framed-text" }
                }
            }
        }

        assertNotNull(root.findByDebugName("mixed-row"), "Row should be found")
        assertNotNull(root.findByDebugName("text-child"), "Text child should be found")
        assertNotNull(root.findByDebugName("col-child"), "Col child should be found")
        assertNotNull(root.findByDebugName("frame-child"), "Frame child should be found")
        assertNotNull(root.findByDebugName("nested-text"), "Nested text should be found")
        assertNotNull(root.findByDebugName("framed-text"), "Framed text should be found")
    }
}
