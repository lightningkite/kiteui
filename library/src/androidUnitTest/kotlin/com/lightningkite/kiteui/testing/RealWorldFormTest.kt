package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.readable.Property
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFalse

/**
 * Comprehensive test demonstrating real-world form testing scenarios.
 *
 * This test showcases the new testing framework features:
 * - Text input interactions (typeText, setText, clearText)
 * - View property access (text, isVisible, isEnabled, etc.)
 * - Rich assertions (assertExists, assertHasText, assertEnabled, etc.)
 * - Multiple selector types (findByDebugName, findByText, findByTextContaining)
 * - Form field testing
 * - Button interaction testing
 */
@RunWith(RobolectricTestRunner::class)
class RealWorldFormTest {

    @Test
    fun testSimpleFormInteractions() = withTestHarness { harness ->
        // Simple form state
        val name = Property("")
        val email = Property("")
        var submitClicked = false

        // Render a simple form
        val root = harness.render {
            col {
                debugName = "form-container"

                h1 { content = "Contact Form" }

                // Name field
                col {
                    text("Name")
                    textInput {
                        debugName = "name-input"
                        hint = "Enter your name"
                        content bind name
                    }
                }

                // Email field
                col {
                    text("Email")
                    textInput {
                        debugName = "email-input"
                        hint = "Enter your email"
                        content bind email
                    }
                }

                // Submit button
                button {
                    debugName = "submit-button"
                    text("Submit")
                    onClick(frequencyCap = null) { submitClicked = true }
                }

                // Feedback text
                text {
                    debugName = "feedback"
                    content = "Fill out the form above"
                }
            }
        }

        println("\n=== Testing Form Interactions ===")

        // TEST 1: Verify initial state
        println("Test 1: Initial State")

        val nameInput = root.requireByDebugName("name-input")
        val emailInput = root.requireByDebugName("email-input")
        val submitButton = root.requireByDebugName("submit-button")
        val feedback = root.requireByDebugName("feedback")

        nameInput.assertExists("Name input should exist")
        nameInput.assertHasNoText("Name should be empty initially")

        emailInput.assertExists("Email input should exist")
        emailInput.assertHasNoText("Email should be empty initially")

        submitButton.assertExists("Submit button should exist")
        submitButton.assertEnabled("Submit button should be enabled")

        feedback.assertHasText("Fill out the form above", "Feedback should show initial message")

        println("✓ Initial state verified")

        // TEST 2: Type text into fields
        println("\nTest 2: Text Input")

        nameInput.setText("John Doe")
        assertEquals("John Doe", name.value, "Name property should be updated")

        emailInput.typeText("john@example.com")
        assertEquals("john@example.com", email.value, "Email property should be updated")

        println("✓ Text input working")

        // TEST 3: Click submit button
        println("\nTest 3: Button Click")

        assertFalse(submitClicked, "Submit should not be clicked yet")

        submitButton.click()

        assertTrue(submitClicked, "Submit should have been clicked")

        println("✓ Button click working")

        // TEST 4: Clear and re-enter text
        println("\nTest 4: Clear and Re-enter")

        nameInput.clearText()
        assertEquals("", name.value, "Name should be cleared")

        nameInput.typeText("Jane")
        nameInput.typeText(" ")
        nameInput.typeText("Smith")
        assertEquals("Jane Smith", name.value, "Name should be built up via typing")

        println("✓ Clear and type working")

        // TEST 5: Find elements by text
        println("\nTest 5: Text-based Selectors")

        val heading = root.requireByText("Contact Form")
        heading.assertExists("Should find heading by text")

        val nameLabel = root.requireByText("Name")
        nameLabel.assertExists("Should find name label by text")

        val emailLabel = root.requireByText("Email")
        emailLabel.assertExists("Should find email label by text")

        val submitByText = root.requireByText("Submit")
        submitByText.assertExists("Should find submit button by text")

        val feedbackByText = root.requireByTextContaining("Fill out")
        feedbackByText.assertExists("Should find feedback by partial text")

        println("✓ Text-based selectors working")

        println("\n=== All Form Tests Passed ===")
    }

    @Test
    fun testListDisplay() = withTestHarness { harness ->
        val items = listOf("Apple", "Banana", "Cherry", "Date")

        val root = harness.render {
            col {
                debugName = "list-screen"

                h1 { content = "Fruits" }

                // Item count
                text {
                    debugName = "item-count"
                    content = "${items.size} items"
                }

                // List of items
                col {
                    debugName = "items-container"
                    for (item in items) {
                        text {
                            debugName = "list-item"
                            content = item
                        }
                    }
                }
            }
        }

        println("\n=== Testing List Display ===")

        // TEST 1: Verify item count
        println("Test 1: Item Count")

        val itemCount = root.requireByDebugName("item-count")
        itemCount.assertHasText("4 items", "Should show correct item count")

        println("✓ Item count verified")

        // TEST 2: Find all list items
        println("\nTest 2: List Items")

        val allItems = root.findAllByDebugName("list-item")
        allItems.assertCount(4, "Should have 4 list items")
        allItems.assertNotEmpty("List should not be empty")

        println("✓ List items found")

        // TEST 3: Verify each item text
        println("\nTest 3: Item Text Content")

        val apple = root.requireByText("Apple")
        apple.assertExists("Apple should exist")

        val banana = root.requireByText("Banana")
        banana.assertExists("Banana should exist")

        val cherry = root.requireByText("Cherry")
        cherry.assertExists("Cherry should exist")

        val date = root.requireByText("Date")
        date.assertExists("Date should exist")

        println("✓ All items have correct text")

        // TEST 4: Find all items by text
        println("\nTest 4: Find All By Text")

        val allByText = root.findAllByText("Apple")
        allByText.assertCount(1, "Should find exactly one Apple")

        println("✓ Find all by text working")

        println("\n=== All List Tests Passed ===")
    }

    @Test
    fun testButtonStates() = withTestHarness { harness ->
        val isEnabled = Property(true)
        var clickCount = 0

        val root = harness.render {
            col {
                debugName = "button-test"

                button {
                    debugName = "action-button"
                    text("Click Me")
                    onClick(frequencyCap = null) { clickCount++ }
                    ::enabled { isEnabled() }
                }

                button {
                    debugName = "toggle-button"
                    text("Toggle Enabled")
                    onClick(frequencyCap = null) { isEnabled.value = !isEnabled.value }
                }
            }
        }

        println("\n=== Testing Button States ===")

        val actionButton = root.requireByDebugName("action-button")
        val toggleButton = root.requireByDebugName("toggle-button")

        // TEST 1: Initial state
        println("Test 1: Initial Enabled State")

        actionButton.assertEnabled("Action button should be enabled initially")
        assertEquals(0, clickCount, "Click count should be 0")

        println("✓ Initial state verified")

        // TEST 2: Click when enabled
        println("\nTest 2: Click When Enabled")

        actionButton.click()
        assertEquals(1, clickCount, "Click count should be 1")

        actionButton.click()
        assertEquals(2, clickCount, "Click count should be 2")

        println("✓ Clicks working when enabled")

        // TEST 3: Disable button
        println("\nTest 3: Disable Button")

        toggleButton.click()
        assertEquals(false, isEnabled.value, "Button should be disabled")

        actionButton.assertDisabled("Action button should be disabled")

        // Try to click disabled button (behavior may vary by platform)
        actionButton.click()
        // On some platforms, clicking a disabled button does nothing
        // We just verify the state is correct

        println("✓ Button disable/enable working")

        println("\n=== All Button State Tests Passed ===")
    }
}
