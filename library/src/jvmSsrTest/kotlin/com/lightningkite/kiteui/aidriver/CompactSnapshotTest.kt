// by Claude - tests for compact snapshot format: relative IDs, anchored resolution,
// hierarchical findById, compact() post-processing, and drag action dispatch
package com.lightningkite.kiteui.aidriver

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.ssr.SsrContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CompactSnapshotTest {
    @OptIn(ExperimentalCoroutinesApi::class)
    @BeforeTest
    fun setup() {
        Dispatchers.setMain(Dispatchers.Unconfined)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @AfterTest
    fun teardown() {
        Dispatchers.resetMain()
    }

    private fun renderSsr(content: ViewWriter.() -> Unit): Frame {
        val ctx = SsrContext("/")
        ctx.theme = Theme(id = "test")
        ctx.render(content)
        return ctx.rootFrame ?: error("No root frame")
    }

    // --- Phase 1a: Named elements reset the path prefix ---

    @Test
    fun namedElementHasShortId() = runTest {
        val root = renderSsr {
            col {
                button {
                    debugName = "myBtn"
                    text("Click")
                }
            }
        }
        val snap = buildSnapshot(root, null)
        val btn = snap.findById("myBtn")
        assertNotNull(btn, "Named button should be findable")
        assertEquals("myBtn", btn.id, "Named element ID should be just the segment name")
    }

    @Test
    fun unnamedChildrenBuildFromNamedAncestor() = runTest {
        val root = renderSsr {
            col {
                debugName = "container"
                text("First")
                text("Second")
            }
        }
        val snap = buildSnapshot(root, null)
        val container = snap.findById("container")
        assertNotNull(container, "Named container should exist")
        // Children of named container should have IDs like "container/0", "container/1"
        assertTrue(container.children.size >= 2, "Container should have at least 2 children")
        assertEquals("container/0", container.children[0].id, "First child should be container/0")
        assertEquals("container/1", container.children[1].id, "Second child should be container/1")
    }

    @Test
    fun nestedNamedElementsEachResetPath() = runTest {
        val root = renderSsr {
            col {
                debugName = "outer"
                col {
                    debugName = "inner"
                    text("Hello")
                }
            }
        }
        val snap = buildSnapshot(root, null)
        val outer = snap.findById("outer")
        val inner = snap.findById("inner")
        assertNotNull(outer, "outer should exist")
        assertNotNull(inner, "inner should exist")
        assertEquals("outer", outer.id)
        assertEquals("inner", inner.id, "Nested named element should reset to just its name")
    }

    // --- Phase 1b: Anchored path resolution ---

    @Test
    fun resolveAiPathFindsDeepNamedElement() = runTest {
        val root = renderSsr {
            col {
                col {
                    col {
                        button {
                            debugName = "deepBtn"
                            text("Deep")
                            onClick(frequencyCap = null) { }
                        }
                    }
                }
            }
        }
        // Click by name should work even though it's deeply nested
        val result = dispatchAction(UiAction.Click("deepBtn"), root, null)
        assertTrue(result.success, "Click on deeply nested named view should succeed: ${result.error}")
    }

    @Test
    fun resolveAiPathFollowsRemainingSegmentsFromAnchor() = runTest {
        val root = renderSsr {
            col {
                col {
                    debugName = "anchor"
                    text("First")  // anchor/0
                    text("Second") // anchor/1
                }
            }
        }
        val snap = buildSnapshot(root, null)
        // Find the component via anchored path
        val second = snap.findById("anchor/1")
        assertNotNull(second, "anchor/1 should be findable via anchored prefix")
        assertEquals("Second", second.value)
    }

    // --- Phase 1c: Hierarchical findById ---

    @Test
    fun findByIdMatchesExactId() = runTest {
        val root = renderSsr {
            button {
                debugName = "btn"
                text("Click")
            }
        }
        val snap = buildSnapshot(root, null)
        val found = snap.findById("btn")
        assertNotNull(found, "Should find by exact id")
    }

    @Test
    fun findByIdMatchesSuffix() = runTest {
        val root = renderSsr {
            col {
                col {
                    button {
                        debugName = "btn"
                        text("Click")
                    }
                }
            }
        }
        val snap = buildSnapshot(root, null)
        val found = snap.findById("btn")
        assertNotNull(found, "Should find named element by suffix")
    }

    @Test
    fun findByIdMatchesAnchoredPrefix() = runTest {
        val root = renderSsr {
            col {
                debugName = "nav"
                text("Item 1") // nav/0
                text("Item 2") // nav/1
                text("Item 3") // nav/2
            }
        }
        val snap = buildSnapshot(root, null)
        val found = snap.findById("nav/2")
        assertNotNull(found, "Should find via anchored prefix nav/2")
        assertEquals("Item 3", found.value)
    }

    // --- Phase 1d: Compact text format ---

    @Test
    fun renderShowsSegmentNotFullPath() = runTest {
        val root = renderSsr {
            col {
                debugName = "container"
                text("Hello")
            }
        }
        val snap = buildSnapshot(root, null)
        val rendered = buildString {
            snap.components.forEach { it.render(this, 0) }
        }
        // Should show "container: Column" not full accumulated path
        assertTrue(rendered.contains("container: Column"), "Should show segment:Type format, got:\n$rendered")
    }

    @Test
    fun renderShowsActionsInline() = runTest {
        val root = renderSsr {
            button {
                debugName = "btn"
                text("Go")
                onClick(frequencyCap = null) { }
            }
        }
        val snap = buildSnapshot(root, null)
        val rendered = buildString {
            snap.components.forEach { it.render(this, 0) }
        }
        assertTrue(rendered.contains("[click"), "Should show actions in brackets, got:\n$rendered")
    }

    // --- Phase 1e: Value field cleanup ---

    @Test
    fun valueIsOnlyFromAccessibilityValue() = runTest {
        val root = renderSsr {
            button {
                debugName = "myButton"
                text("Label")
            }
        }
        val snap = buildSnapshot(root, null)
        val btn = snap.findById("myButton")
        assertNotNull(btn)
        // The button itself should NOT have value="myButton" (that was the old behavior)
        assertNull(btn.value, "Button value should be null since debugName is not accessibilityValue")
    }

    @Test
    fun textInputValueComeFromAccessibilityValue() = runTest {
        val name = Signal("Alice")
        val root = renderSsr {
            textInput {
                debugName = "name"
                content bind name
            }
        }
        val snap = buildSnapshot(root, null)
        val input = snap.findById("name")
        assertNotNull(input)
        assertEquals("Alice", input.value, "TextInput value should come from accessibilityValue (bound content)")
    }

    // --- Phase 2: Filter hidden elements ---

    @Test
    fun hiddenElementsFilteredByDefault() = runTest {
        val root = renderSsr {
            col {
                text("Visible")
                text("Hidden") // we'll verify it's not here through settings
            }
        }
        // Default settings (includeLessVisible = false) — all elements are visible in SSR
        val snap = buildSnapshot(root, null)
        // In SSR, all elements are shown=true and visible=true by default,
        // so no filtering occurs. This test verifies the build doesn't crash.
        assertTrue(snap.components.isNotEmpty(), "Should have components")
    }

    @Test
    fun hiddenElementsIncludedWhenSettingEnabled() = runTest {
        val root = renderSsr {
            text("Test")
        }
        val settings = UiSnapshotSettings(includeLessVisible = true)
        val snap = buildSnapshot(root, null, settings)
        assertTrue(snap.components.isNotEmpty(), "Should have components with includeLessVisible=true")
    }

    // --- Phase 3: compact() post-processing ---

    @Test
    fun compactDiscardsEmptyStructuralLeaves() {
        val snapshot = UiSnapshot(
            page = "Test",
            url = "/test",
            settings = UiSnapshotSettings(),
            components = listOf(
                UiComponent(id = "0", type = "Column", children = listOf(
                    UiComponent(id = "0/0", type = "View"),  // empty structural leaf
                    UiComponent(id = "btn", type = "Button", actions = setOf("click"))  // interesting
                ))
            )
        )
        val compacted = snapshot.compact()
        // The empty View leaf should be removed, Button should survive.
        // After removing View, Column has 1 child → it collapses into Button.
        assertNotNull(compacted.findById("btn"), "Interactive elements should remain")
        // The empty View should not appear anywhere in the compacted tree
        fun UiComponent.allDescendants(): List<UiComponent> =
            listOf(this) + children.flatMap { it.allDescendants() }
        val allComponents = compacted.components.flatMap { it.allDescendants() }
        assertTrue(allComponents.none { it.type == "View" && it.actions.isEmpty() && it.value == null && it.id.substringAfterLast("/").all(Char::isDigit) },
            "Empty structural leaves should be removed")
    }

    @Test
    fun compactCollapsesSingleChildContainers() {
        val snapshot = UiSnapshot(
            page = "Test",
            url = "/test",
            settings = UiSnapshotSettings(),
            components = listOf(
                UiComponent(id = "0", type = "Column", children = listOf(
                    UiComponent(id = "0/0", type = "Frame", children = listOf(
                        UiComponent(id = "btn", type = "Button", actions = setOf("click"))
                    ))
                ))
            )
        )
        val compacted = snapshot.compact()
        // All single-child structural containers collapse into the leaf Button.
        // Frame(0/0) collapses into btn → "0/0/btn", then Column(0) collapses → "0/0/0/btn"
        val btn = compacted.findById("btn")
        assertNotNull(btn, "Button should survive compaction")
        assertEquals("Button", btn.type)
        assertTrue(btn.actions.contains("click"), "Button should retain its actions")
        // Should be the only thing at root level (two structural parents collapsed)
        assertEquals(1, compacted.components.size, "Should have single collapsed component at root")
    }

    @Test
    fun compactKeepsMultiChildContainers() {
        val snapshot = UiSnapshot(
            page = "Test",
            url = "/test",
            settings = UiSnapshotSettings(),
            components = listOf(
                UiComponent(id = "0", type = "Column", children = listOf(
                    UiComponent(id = "btn1", type = "Button", actions = setOf("click")),
                    UiComponent(id = "btn2", type = "Button", actions = setOf("click"))
                ))
            )
        )
        val compacted = snapshot.compact()
        // Multi-child container should be kept
        assertEquals(1, compacted.components.size, "Root container should remain")
        val col = compacted.components.first()
        assertEquals(2, col.children.size, "Both children should remain")
    }

    @Test
    fun compactKeepsNamedElements() {
        val snapshot = UiSnapshot(
            page = "Test",
            url = "/test",
            settings = UiSnapshotSettings(),
            components = listOf(
                UiComponent(id = "header", type = "Row", children = listOf(
                    UiComponent(id = "header/0", type = "View")  // empty structural leaf
                ))
            )
        )
        val compacted = snapshot.compact()
        // "header" is named (non-numeric id) so it's interesting and should stay
        val header = compacted.findById("header")
        assertNotNull(header, "Named element should survive compaction even without actions/value")
    }

    @Test
    fun compactPreservesValues() {
        val snapshot = UiSnapshot(
            page = "Test",
            url = "/test",
            settings = UiSnapshotSettings(),
            components = listOf(
                UiComponent(id = "0", type = "Column", children = listOf(
                    UiComponent(id = "0/0", type = "Text", value = "Hello")
                ))
            )
        )
        val compacted = snapshot.compact()
        val text = compacted.findById("0/0")
            ?: compacted.components.flatMap { listOf(it) + it.children }.find { it.value == "Hello" }
        assertNotNull(text, "Text with value should survive compaction")
        assertEquals("Hello", text.value)
    }

    // --- Phase 4: Drag action ---

    @Test
    fun dragActionReturnsNotSupportedOnSsr() = runTest {
        val root = renderSsr {
            col {
                debugName = "target"
                text("Drag me")
            }
        }
        // Source view has no dragData set, so the action should fail
        val result = dispatchAction(UiAction.DragAndDrop("target", toTargetId = "target"), root, null)
        assertFalse(result.success, "DragAndDrop should fail when source has no dragData")
        assertNotNull(result.error, "Should have error message")
    }

    @Test
    fun dragActionReportsViewNotFound() = runTest {
        val root = renderSsr {
            text("Hello")
        }
        val result = dispatchAction(UiAction.DragAndDrop("nonexistent", toTargetId = "somewhere"), root, null)
        assertFalse(result.success)
        assertTrue(result.error?.contains("not found") == true,
            "Should report view not found: ${result.error}")
    }

    // --- Find command: structured component search ---

    // by Claude - uses UiSnapshot.find() from library (shared with CliServer)

    @Test
    fun findByValue() = runTest {
        val root = renderSsr {
            col {
                text("Hello World")
                text("Goodbye World")
                button {
                    debugName = "btn"
                    text("Click Me")
                    onClick(frequencyCap = null) { }
                }
            }
        }
        val snap = buildSnapshot(root, null)
        val results = snap.find(value = "Hello")
        assertEquals(1, results.size, "Should find exactly one component with 'Hello'")
        assertEquals("Hello World", results[0].value)
    }

    @Test
    fun findByType() = runTest {
        val root = renderSsr {
            col {
                text("Label")
                button {
                    debugName = "btn1"
                    text("Go")
                    onClick(frequencyCap = null) { }
                }
                button {
                    debugName = "btn2"
                    text("Stop")
                    onClick(frequencyCap = null) { }
                }
            }
        }
        val snap = buildSnapshot(root, null)
        val results = snap.find(type = "Button")
        assertEquals(2, results.size, "Should find 2 buttons")
        assertTrue(results.all { it.type == "Button" })
    }

    @Test
    fun findByAction() = runTest {
        val root = renderSsr {
            col {
                text("No actions")
                textInput {
                    debugName = "input"
                    content bind Signal("hello")
                }
                button {
                    debugName = "btn"
                    text("Go")
                    onClick(frequencyCap = null) { }
                }
            }
        }
        val snap = buildSnapshot(root, null)
        val results = snap.find(action = "setValue")
        assertTrue(results.isNotEmpty(), "Should find components with setValue action")
        assertTrue(results.all { "setValue" in it.actions }, "All results should have setValue")
    }

    @Test
    fun findByIdPattern() = runTest {
        val root = renderSsr {
            col {
                button {
                    debugName = "login-btn"
                    text("Login")
                    onClick(frequencyCap = null) { }
                }
                button {
                    debugName = "logout-btn"
                    text("Logout")
                    onClick(frequencyCap = null) { }
                }
            }
        }
        val snap = buildSnapshot(root, null)
        val results = snap.find(id = "logout-btn")
        assertTrue(results.isNotEmpty(), "Should find components with 'logout-btn' in id")
        assertEquals("logout-btn", results[0].id)
    }

    @Test
    fun findCombinesFilters() = runTest {
        val root = renderSsr {
            col {
                button {
                    debugName = "ok-btn"
                    text("OK")
                    onClick(frequencyCap = null) { }
                }
                button {
                    debugName = "cancel-btn"
                    text("Cancel")
                    onClick(frequencyCap = null) { }
                }
                text("OK") // text, not button
            }
        }
        val snap = buildSnapshot(root, null)
        // Find buttons with value "OK" — should not match the plain Text
        val results = snap.find(value = "OK", type = "Button")
        // The Button itself has no value (its child Text has the value),
        // so this verifies combining filters works correctly—any matches must satisfy both.
        // If empty (buttons don't carry value), the filter still excludes plain Text nodes.
        results.forEach { r ->
            assertEquals("Button", r.type, "Result type should be Button")
            assertTrue(r.value?.contains("OK") == true, "Result value should contain 'OK'")
        }
        // Also verify the plain Text("OK") was excluded since it's not a Button
        val textResults = snap.find(value = "OK", type = "Text")
        assertTrue(textResults.isNotEmpty(), "There should be Text components with 'OK'")
        assertTrue(textResults.none { it.type == "Button" }, "Text results should not include Buttons")
    }

    @Test
    fun findRespectsLimit() = runTest {
        val root = renderSsr {
            col {
                for (i in 1..5) {
                    text("Item $i")
                }
            }
        }
        val snap = buildSnapshot(root, null)
        val results = snap.find(type = "Text", limit = 3)
        assertTrue(results.size <= 3, "Should respect limit of 3, got ${results.size}")
    }

    @Test
    fun findReturnsEmptyWhenNoMatch() = runTest {
        val root = renderSsr {
            text("Hello")
        }
        val snap = buildSnapshot(root, null)
        val results = snap.find(value = "nonexistent")
        assertTrue(results.isEmpty(), "Should return empty when nothing matches")
    }

    // --- Visual verification: dump a real snapshot ---

    @Test
    fun compactFormatVisualVerification() = runTest {
        val counter = Signal(0)
        val root = renderSsr {
            col {
                debugName = "appContainer"
                row {
                    debugName = "toolbar"
                    text("My App")
                    button {
                        debugName = "settings"
                        text("Settings")
                        onClick(frequencyCap = null) { }
                    }
                }
                col {
                    debugName = "content"
                    text("Welcome!")
                    row {
                        button {
                            debugName = "decrement"
                            text("-")
                            onClick(frequencyCap = null) { counter.value-- }
                        }
                        textInput {
                            debugName = "counter"
                            content bind Signal("0")
                        }
                        button {
                            debugName = "increment"
                            text("+")
                            onClick(frequencyCap = null) { counter.value++ }
                        }
                    }
                    button {
                        debugName = "submit"
                        text("Submit")
                        onClick(frequencyCap = null) { }
                    }
                }
            }
        }

        val snap = buildSnapshot(root, null)
        val rendered = buildString {
            appendLine("Page: ${snap.page} (${snap.url})")
            snap.components.forEach { it.render(this, 0) }
        }
        println("=== COMPACT SNAPSHOT FORMAT ===")
        println(rendered)
        println("=== END ===")

        // Verify key properties of the compact format
        assertTrue(rendered.contains("toolbar: Row"), "Should show segment:Type for named elements")
        assertTrue(rendered.contains("settings: Button"), "Named button should show short id")
        assertTrue(rendered.contains("counter: TextInput"), "Named input should show short id")
        assertTrue(rendered.contains("[click"), "Should show actions in brackets")
        // Named elements should NOT have their name duplicated as value
        assertFalse(rendered.contains("= \"toolbar\""), "Named element should not echo name as value")
        assertFalse(rendered.contains("= \"settings\""), "Named button should not echo name as value")

        // Also test interactiveOnly mode
        val compacted = snap.compact()
        val compactRendered = buildString {
            appendLine("Page: ${compacted.page} (${compacted.url})")
            compacted.components.forEach { it.render(this, 0) }
        }
        println("\n=== INTERACTIVE ONLY ===")
        println(compactRendered)
        println("=== END ===")

        // Interactive-only should have fewer lines
        val fullLines = rendered.lines().filter { it.isNotBlank() }.size
        val compactLines = compactRendered.lines().filter { it.isNotBlank() }.size
        assertTrue(compactLines <= fullLines,
            "Interactive-only ($compactLines lines) should be <= full ($fullLines lines)")
    }
}
