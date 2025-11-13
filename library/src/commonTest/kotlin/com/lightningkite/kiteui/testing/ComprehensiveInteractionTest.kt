package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.Property
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Comprehensive test demonstrating all testing utilities.
 *
 * This test showcases:
 * - View property accessors (textContent, isVisibleInTest, isEnabledInTest, hintText)
 * - Async wait utilities (waitUntil, waitFor, withTestHarnessSuspend)
 * - Scroll interactions (scrollBy, scrollToView)
 * - View hierarchy debugging (printHierarchy, viewCount, printStats)
 * - Text input and click interactions
 */
class ComprehensiveInteractionTest {

    @Test
    fun testViewPropertiesAndTextContent() = withTestHarness { harness ->
        val root = harness.render {
            col {
                text("Welcome") {
                    debugName = "title"
                }
                textInput {
                    debugName = "email"
                    hint = "Enter email"
                }
                button {
                    debugName = "submit"
                    text("Submit")
                    enabled = false
                }
            }
        }

        // Test text content
        val title = root.findByDebugName("title")!!
        assertEquals("Welcome", title.textContent)

        // Test hint text
        val emailInput = root.findByDebugName("email")!!
        assertEquals("Enter email", emailInput.hintText)

        // Test enabled state
        val submitButton = root.findByDebugName("submit")!!
        assertFalse(submitButton.isEnabledInTest, "Submit should start disabled")

        // Test visibility
        assertTrue(title.isVisibleInTest, "Title should be visible")
    }

    @Test
    fun testAsyncWaitUtilities() = runTest {
        withTestHarnessSuspend { harness ->
            val isLoading = Property(true)
            val data = Property<String?>(null)

            val root = harness.render {
                col {
                    if (isLoading()) {
                        text("Loading...") {
                            debugName = "loading"
                        }
                    } else {
                        text {
                            debugName = "data"
                            ::content { data() ?: "No data" }
                        }
                    }
                }
            }

            // Verify loading state
            var loadingView = root.findByDebugName("loading")
            assertNotNull(loadingView, "Loading view should exist initially")

            // Simulate data loading
            kotlinx.coroutines.delay(100)
            isLoading.value = false
            data.value = "Loaded Data"

            // Wait for update to propagate
            TestWaits.waitForUpdate()

            // Verify data is shown
            val dataView = root.findByDebugName("data")
            assertNotNull(dataView, "Data view should exist after loading")
            assertEquals("Loaded Data", dataView.textContent)
        }
    }

    @Test
    fun testScrollInteractions() = withTestHarness { harness ->
        val items = (1..50).map { "Item $it" }

        val root = harness.render {
            scrolling - col {
                debugName = "scroll-container"

                items.forEachIndexed { index, item ->
                    text(item) {
                        debugName = "item-$index"
                    }
                }
            }
        }

        val scrollView = root.findByDebugName("scroll-container")
        assertNotNull(scrollView, "Scroll container should exist")

        // Test scrollBy
        scrollView.scrollBy(dy = 500)
        println("✓ Scrolled down 500 pixels")

        // Test scrollToView
        val item10 = root.findByDebugName("item-10")
        if (item10 != null) {
            scrollView.scrollToView(item10)
            println("✓ Scrolled to item 10")
        }
    }

    @Test
    fun testViewHierarchyDebugging() = withTestHarness { harness ->
        val root = harness.render {
            col {
                debugName = "root-col"

                text("Header") {
                    debugName = "header"
                }

                row {
                    debugName = "button-row"

                    button {
                        debugName = "btn-1"
                        text("Button 1")
                    }

                    button {
                        debugName = "btn-2"
                        text("Button 2")
                    }
                }

                textInput {
                    debugName = "input"
                    hint = "Type here"
                }
            }
        }

        // Print hierarchy
        println("\n=== View Hierarchy ===")
        root.printHierarchy()

        // Count views
        val totalViews = root.viewCount()
        println("\nTotal views in hierarchy: $totalViews")
        assertTrue(totalViews >= 5, "Should have at least 5 views")

        // Print stats
        println("\n=== Hierarchy Statistics ===")
        root.printStats()

        // Get hierarchy as string for assertions
        val hierarchyString = root.hierarchyString()
        assertTrue(hierarchyString.contains("header"), "Hierarchy should contain header")
        assertTrue(hierarchyString.contains("button-row"), "Hierarchy should contain button-row")
    }

    @Test
    fun testCompleteUserFlow() = withTestHarness { harness ->
        val username = Property("")
        val password = Property("")
        val isLoggedIn = Property(false)
        val errorMessage = Property<String?>(null)

        val root = harness.render {
            col {
                debugName = "login-form"

                text("Login") {
                    debugName = "title"
                }

                textInput {
                    debugName = "username-input"
                    hint = "Username"
                    content bind username
                }

                textInput {
                    debugName = "password-input"
                    hint = "Password"
                    content bind password
                }

                if (errorMessage() != null) {
                    text {
                        debugName = "error"
                        ::content { errorMessage() ?: "" }
                    }
                }

                button {
                    debugName = "login-button"
                    text("Log In")
                    onClick {
                        if (username.value.isEmpty() || password.value.isEmpty()) {
                            errorMessage.value = "Please fill all fields"
                        } else if (username.value == "admin" && password.value == "password") {
                            isLoggedIn.value = true
                            errorMessage.value = null
                        } else {
                            errorMessage.value = "Invalid credentials"
                        }
                    }
                }

                if (isLoggedIn()) {
                    text("Welcome, ${username()}!") {
                        debugName = "welcome"
                    }
                }
            }
        }

        // Test 1: Empty form submission
        val loginButton = root.findByDebugName("login-button")!!
        loginButton.click()

        var error = root.findByDebugName("error")
        assertNotNull(error, "Error should be shown for empty form")
        assertEquals("Please fill all fields", error.textContent)

        // Test 2: Fill in username only
        val usernameInput = root.findByDebugName("username-input")!!
        usernameInput.typeText("testuser")

        assertEquals("testuser", username.value, "Username should be set")

        loginButton.click()
        error = root.findByDebugName("error")
        assertNotNull(error, "Error should still be shown - password empty")

        // Test 3: Wrong credentials
        val passwordInput = root.findByDebugName("password-input")!!
        passwordInput.typeText("wrongpass")

        loginButton.click()
        error = root.findByDebugName("error")
        assertNotNull(error, "Error should be shown for wrong credentials")
        assertEquals("Invalid credentials", error.textContent)

        // Test 4: Correct credentials
        usernameInput.typeText("admin")  // Replace with correct username
        passwordInput.typeText("password")  // Replace with correct password

        loginButton.click()

        // Check logged in state
        assertTrue(isLoggedIn.value, "Should be logged in")

        val welcome = root.findByDebugName("welcome")
        assertNotNull(welcome, "Welcome message should be shown")
        assertTrue(welcome.textContent?.contains("admin") == true, "Welcome should show username")
    }

    @Test
    fun testThemingAndVisibility() = withTestHarness { harness ->
        val showDetails = Property(false)

        val theme = Theme(
            id = "test-theme",
            background = Color.white,
            foreground = Color.black
        )

        val root = harness.render(theme) {
            col {
                button {
                    debugName = "toggle"
                    text("Toggle Details")
                    onClick { showDetails.value = !showDetails.value }
                }

                if (showDetails()) {
                    col {
                        debugName = "details"
                        text("Detail 1")
                        text("Detail 2")
                        text("Detail 3")
                    }
                }
            }
        }

        // Initially details should not exist
        var details = root.findByDebugName("details")
        assertEquals(null, details, "Details should not exist initially")

        // Click toggle to show details
        val toggleButton = root.findByDebugName("toggle")!!
        toggleButton.click()

        // Now details should exist
        details = root.findByDebugName("details")
        assertNotNull(details, "Details should exist after toggle")

        // Click again to hide
        toggleButton.click()

        // Details should be gone
        details = root.findByDebugName("details")
        assertEquals(null, details, "Details should not exist after hiding")
    }

    @Test
    fun testViewFinderUtilities() = withTestHarness { harness ->
        val root = harness.render {
            col {
                button {
                    debugName = "btn-1"
                    text("Button 1")
                }

                button {
                    debugName = "btn-2"
                    text("Button 2")
                }

                button {
                    debugName = "btn-3"
                    text("Button 3")
                }

                text("Not a button") {
                    debugName = "label"
                }
            }
        }

        // Find all views matching a predicate
        val allButtons = root.findAll { view ->
            view.debugName?.startsWith("btn-") == true
        }

        assertEquals(3, allButtons.size, "Should find 3 buttons")

        // Verify we can interact with found views
        allButtons.forEach { button ->
            assertNotNull(button.textContent, "Button should have text content")
            assertTrue(button.isEnabledInTest, "Button should be enabled")
        }
    }
}
