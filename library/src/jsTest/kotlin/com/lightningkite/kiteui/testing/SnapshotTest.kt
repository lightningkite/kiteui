package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test HTML snapshot functionality for web platform.
 */
class SnapshotTest {

    @Test
    fun testBasicHtmlSnapshot() = withTestHarness { harness ->
        val theme = Theme(
            id = "test-light",
            background = Color.white,
            foreground = Color.black
        )
        val root = harness.render(theme) {
            col {
                text("Snapshot Test").apply { debugName = "title" }
                text("This view should be captured as HTML")
            }
        }

        // Capture HTML snapshot
        val snapshot = harness.screenshot("test-snapshot")
        assertNotNull(snapshot, "Snapshot should not be null")
        assertTrue(snapshot.isNotEmpty(), "Snapshot should contain data")

        // Verify it's valid HTML by checking for DOCTYPE
        val html = snapshot.decodeToString()
        assertTrue(html.contains("<!DOCTYPE html>"), "Should be a complete HTML document")
        assertTrue(html.contains("<html>"), "Should contain html tag")
        assertTrue(html.contains("Snapshot Test"), "Should contain the test content")

        println("HTML snapshot length: ${html.length} characters")
        println("First 500 chars: ${html.take(500)}")
    }

    @Test
    fun testViewHtmlSnapshot() = withTestHarness { harness ->
        val root = harness.render {
            col {
                row {
                    debugName = "test-row"
                    text("Left")
                    text("Right")
                }
                text("Below row")
            }
        }

        // Capture snapshot of specific view
        val rowView = root.findByDebugName("test-row")
        assertNotNull(rowView, "Row should be found")

        val snapshot = harness.screenshotView(rowView!!, "row-snapshot")
        assertNotNull(snapshot, "Snapshot should not be null")
        assertTrue(snapshot.isNotEmpty(), "Snapshot should contain data")

        val html = snapshot.decodeToString()
        assertTrue(html.contains("<!DOCTYPE html>"), "Should be a complete HTML document")
        assertTrue(html.contains("Left"), "Should contain left text")
        assertTrue(html.contains("Right"), "Should contain right text")

        println("Row snapshot length: ${html.length} characters")
    }

    @Test
    fun testComplexLayoutHtmlSnapshot() = withTestHarness { harness ->
        val root = harness.render {
            col {
                text("Header").apply { debugName = "header" }
                row {
                    text("Item 1")
                    text("Item 2")
                    text("Item 3")
                }
                frame {
                    text("Framed Content")
                }
                col {
                    text("Nested 1")
                    text("Nested 2")
                }
            }
        }

        val snapshot = harness.screenshot("complex-layout")
        assertNotNull(snapshot, "Snapshot should not be null")
        assertTrue(snapshot.isNotEmpty(), "Snapshot should contain data")

        val html = snapshot.decodeToString()
        assertTrue(html.contains("Header"), "Should contain header")
        assertTrue(html.contains("Item 1"), "Should contain item 1")
        assertTrue(html.contains("Framed Content"), "Should contain framed content")
        assertTrue(html.contains("Nested 1"), "Should contain nested content")

        // Check that styles are embedded
        assertTrue(html.contains("style="), "Should have inline styles")

        println("Complex layout snapshot length: ${html.length} characters")
    }
}
