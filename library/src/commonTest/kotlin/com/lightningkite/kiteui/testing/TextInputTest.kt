package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.direct.textInput
import com.lightningkite.reactive.Property
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests for text input interactions.
 *
 * These tests demonstrate how to use the typeText() function to simulate
 * user input in text fields.
 */
class TextInputTest {

    @Test
    fun testTypeTextReplaces() = withTestHarness { harness ->
        val emailProperty = Property("")

        val root = harness.render {
            col {
                textInput {
                    debugName = "email-input"
                    content bind emailProperty
                }
            }
        }

        // Find the input field
        val emailInput = root.findByDebugName("email-input")
        assertNotNull(emailInput, "Email input should exist")

        // Type text into it
        emailInput.typeText("user@example.com")

        // Verify the property was updated
        assertEquals("user@example.com", emailProperty.value, "Email property should be updated")
    }

    @Test
    fun testTypeTextAppends() = withTestHarness { harness ->
        val messageProperty = Property("Hello ")

        val root = harness.render {
            col {
                textInput {
                    debugName = "message-input"
                    content bind messageProperty
                }
            }
        }

        val messageInput = root.findByDebugName("message-input")
        assertNotNull(messageInput)

        // Append text to existing content
        messageInput.typeText("World!", append = true)

        // Verify text was appended
        assertEquals("Hello World!", messageProperty.value, "Message should have text appended")
    }

    @Test
    fun testMultipleInputs() = withTestHarness { harness ->
        val usernameProperty = Property("")
        val passwordProperty = Property("")

        val root = harness.render {
            col {
                textInput {
                    debugName = "username"
                    hint = "Username"
                    content bind usernameProperty
                }
                textInput {
                    debugName = "password"
                    hint = "Password"
                    content bind passwordProperty
                }
            }
        }

        // Fill in both inputs
        root.findByDebugName("username")?.typeText("alice")
        root.findByDebugName("password")?.typeText("secret123")

        // Verify both were updated
        assertEquals("alice", usernameProperty.value, "Username should be set")
        assertEquals("secret123", passwordProperty.value, "Password should be set")
    }

    @Test
    fun testFormWorkflow() = withTestHarness { harness ->
        val emailProperty = Property("")
        val submittedEmail = Property<String?>(null)

        val root = harness.render {
            col {
                text("Enter your email:")

                textInput {
                    debugName = "email"
                    content bind emailProperty
                }

                button {
                    debugName = "submit-button"
                    text("Submit")
                    onClick {
                        submittedEmail.value = emailProperty.value
                    }
                }

                text {
                    debugName = "result"
                    ::content { submittedEmail.value ?: "Not submitted" }
                }
            }
        }

        // Type email
        val emailInput = root.findByDebugName("email")
        assertNotNull(emailInput)
        emailInput.typeText("test@example.com")

        // Click submit button
        val submitButton = root.findByDebugName("submit-button")
        assertNotNull(submitButton)
        submitButton.click()

        // Verify form was submitted
        assertEquals("test@example.com", submittedEmail.value, "Email should be submitted")
    }

    @Test
    fun testClearAndReplace() = withTestHarness { harness ->
        val textProperty = Property("Initial text")

        val root = harness.render {
            col {
                textInput {
                    debugName = "text-input"
                    content bind textProperty
                }
            }
        }

        val input = root.findByDebugName("text-input")
        assertNotNull(input)

        // Replace with new text (default behavior)
        input.typeText("New text")

        assertEquals("New text", textProperty.value, "Text should be replaced")

        // Replace again
        input.typeText("Updated text")

        assertEquals("Updated text", textProperty.value, "Text should be replaced again")
    }
}
