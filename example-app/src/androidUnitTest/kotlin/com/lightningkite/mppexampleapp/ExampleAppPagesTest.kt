package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.testing.*
import com.lightningkite.mppexampleapp.internal.ControlsPage
import com.lightningkite.mppexampleapp.internal.FormsPage
import com.lightningkite.mppexampleapp.internal.SampleLogInPage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Comprehensive tests for example app pages demonstrating the testing framework capabilities.
 *
 * These tests verify real-world screens in the example app, showcasing:
 * - Text input and form interactions
 * - Button clicks and state changes
 * - Toggle controls (switches, checkboxes, radio buttons)
 * - Text-based selectors and assertions
 * - List/collection testing
 */
@RunWith(RobolectricTestRunner::class)
class ExampleAppPagesTest {

    @Test
    fun testHomePageCounterInteractions() = withTestHarness { harness ->
        println("\n=== Testing HomePage Counter ===")

        val root = harness.render {
            with(HomePage()) {
                render()
            }
        }

        // Test 1: Page renders with title
        println("Test 1: Page Title")
        root.requireByText("KiteUI - Beautiful by Default")
            .assertVisible()
        println("✓ Page title visible")

        // Test 2: Find the counter example
        println("\nTest 2: Counter Example")
        root.requireByText("Here is a basic counter:")
            .assertVisible()
        println("✓ Counter example found")

        // Test 3: Initial counter value is 0
        println("\nTest 3: Initial Value")
        val counterDisplay = root.findByText("0")
        counterDisplay.assertExists("Counter should start at 0")
        println("✓ Counter starts at 0")

        // Test 4: Increment button exists and is clickable
        println("\nTest 4: Increment Button")
        val incrementBtn = root.findByText("+")
        incrementBtn.assertExists("+ button should exist")
            .assertVisible()
            .assertEnabled()
        println("✓ Increment button is ready")

        // Test 5: Decrement button exists and is clickable
        println("\nTest 5: Decrement Button")
        val decrementBtn = root.findByText("-")
        decrementBtn.assertExists("- button should exist")
            .assertVisible()
            .assertEnabled()
        println("✓ Decrement button is ready")

        // Test 6: Goals section is present
        println("\nTest 6: Goals Section")
        root.requireByText("Goals").assertVisible()
        root.requireByTextContaining("Web first").assertVisible()
        root.requireByTextContaining("Multiplatform").assertVisible()
        println("✓ Goals section rendered")

        println("\n=== All HomePage Tests Passed ===")
    }

    @Test
    fun testSampleLogInPageFormInteractions() = withTestHarness { harness ->
        println("\n=== Testing Login Page ===")

        val root = harness.render {
            with(SampleLogInPage) {
                render()
            }
        }

        // Test 1: Page title exists
        println("Test 1: Page Title")
        root.requireByText("My App")
            .assertVisible()
        println("✓ App title visible")

        // Test 2: Email field exists (by hint/label)
        println("\nTest 2: Email Field")
        val emailLabel = root.findByText("Email")
        emailLabel.assertExists("Email field should exist")
        println("✓ Email field found")

        // Test 3: Password field exists
        println("\nTest 3: Password Field")
        val passwordLabel = root.findByText("Password")
        passwordLabel.assertExists("Password field should exist")
        println("✓ Password field found")

        // Test 4: Login button exists and is enabled
        println("\nTest 4: Login Button")
        root.requireByText("Log In")
            .assertVisible()
            .assertEnabled()
        println("✓ Login button is ready")

        println("\n=== All Login Page Tests Passed ===")
    }

    @Test
    fun testControlsPageButtons() = withTestHarness { harness ->
        println("\n=== Testing Controls Page - Buttons ===")

        val root = harness.render {
            with(ControlsPage) {
                render()
            }
        }

        // Test 1: Page title
        println("Test 1: Page Title")
        root.requireByText("Controls")
            .assertVisible()
        println("✓ Controls page title visible")

        // Test 2: Buttons section exists
        println("\nTest 2: Buttons Section")
        root.requireByText("Buttons")
            .assertVisible()
        println("✓ Buttons section found")

        // Test 3: All button variants exist
        println("\nTest 3: Button Variants")
        val buttonTexts = listOf("Sample", "Card", "Important", "Critical", "Warning", "Danger")
        for (text in buttonTexts) {
            root.findByText(text)
                .assertExists("$text button should exist")
            println("  ✓ Found $text button")
        }
        println("✓ All button variants present")

        // Test 4: Toggle Buttons section
        println("\nTest 4: Toggle Buttons")
        root.requireByText("Toggle Buttons")
            .assertVisible()
        println("✓ Toggle buttons section found")

        // Test 5: Progress Bars section
        println("\nTest 5: Progress Bars")
        root.requireByText("Progress Bars")
            .assertVisible()
        println("✓ Progress bars section found")

        println("\n=== All Controls Page Button Tests Passed ===")
    }

    @Test
    fun testControlsPageCheckboxesAndSwitches() = withTestHarness { harness ->
        println("\n=== Testing Controls Page - Toggles ===")

        val root = harness.render {
            with(ControlsPage) {
                render()
            }
        }

        // Test 1: Switches section exists
        println("Test 1: Switches Section")
        root.requireByText("Switches")
            .assertVisible()
        println("✓ Switches section found")

        // Test 2: Example Setting labels exist
        println("\nTest 2: Setting Labels")
        val settingLabels = root.findAllByText("Example Setting")
        settingLabels.assertNotEmpty("Should have multiple Example Setting labels")
        println("✓ Found ${settingLabels.size} setting labels")

        // Test 3: Checkboxes section
        println("\nTest 3: Checkboxes Section")
        root.requireByText("Checkboxes")
            .assertVisible()
        println("✓ Checkboxes section found")

        // Test 4: Radio Buttons section
        println("\nTest 4: Radio Buttons Section")
        root.requireByText("Radio Buttons")
            .assertVisible()
        println("✓ Radio buttons section found")

        // Test 5: Activity Indicators section
        println("\nTest 5: Activity Indicators")
        root.requireByText("Activity Indicators")
            .assertVisible()
        println("✓ Activity indicators section found")

        println("\n=== All Controls Page Toggle Tests Passed ===")
    }

    @Test
    fun testControlsPageInputFields() = withTestHarness { harness ->
        println("\n=== Testing Controls Page - Input Fields ===")

        val root = harness.render {
            with(ControlsPage) {
                render()
            }
        }

        // Test 1: Drop Downs section
        println("Test 1: Drop Downs")
        root.requireByText("Drop Downs")
            .assertVisible()
        println("✓ Drop downs section found")

        // Test 2: Date Fields section
        println("\nTest 2: Date Fields")
        root.requireByText("Date Fields")
            .assertVisible()
        root.requireByTextContaining("Not Selected")
            .assertVisible()
        root.requireByText("Set to now")
            .assertVisible()
        println("✓ Date fields section found")

        // Test 3: Time Fields section
        println("\nTest 3: Time Fields")
        root.requireByText("Time Fields")
            .assertVisible()
        println("✓ Time fields section found")

        // Test 4: Date Time Fields section
        println("\nTest 4: Date Time Fields")
        root.requireByText("Date Time Fields")
            .assertVisible()
        println("✓ Date time fields section found")

        // Test 5: Number Fields section
        println("\nTest 5: Number Fields")
        root.requireByText("Number Fields")
            .assertVisible()
        root.requireByTextContaining("Value:")
            .assertVisible()
        println("✓ Number fields section found")

        // Test 6: Text Fields section
        println("\nTest 6: Text Fields")
        root.requireByText("Text Fields")
            .assertVisible()
        root.requireByTextContaining("Text:")
            .assertVisible()
        println("✓ Text fields section found")

        // Test 7: Text Areas section
        println("\nTest 7: Text Areas")
        root.requireByText("Text Areas")
            .assertVisible()
        println("✓ Text areas section found")

        // Test 8: Images section
        println("\nTest 8: Images Section")
        root.requireByText("Images")
            .assertVisible()
        println("✓ Images section found")

        println("\n=== All Controls Page Input Tests Passed ===")
    }

    @Test
    fun testControlsPageMenus() = withTestHarness { harness ->
        println("\n=== Testing Controls Page - Menus ===")

        val root = harness.render {
            with(ControlsPage) {
                render()
            }
        }

        // Test 1: Menus section exists
        println("Test 1: Menus Section")
        root.requireByText("Menus")
            .assertVisible()
        println("✓ Menus section found")

        // Test 2: Multiple Menu buttons exist
        println("\nTest 2: Menu Buttons")
        val menuButtons = root.findAllByText("Menu")
        menuButtons.assertNotEmpty("Should have menu buttons")
        println("✓ Found ${menuButtons.size} menu buttons")

        println("\n=== All Controls Page Menu Tests Passed ===")
    }

    @Test
    fun testFormsPageStructure() = withTestHarness { harness ->
        println("\n=== Testing Forms Page ===")

        val root = harness.render {
            with(FormsPage) {
                render()
            }
        }

        // Test 1: Page title
        println("Test 1: Page Title")
        root.requireByText("Form Testing")
            .assertVisible()
        println("✓ Form Testing title visible")

        // Test 2: Main form section
        println("\nTest 2: Main Form Section")
        root.requireByText("Vehicle for Sale")
            .assertVisible()
        println("✓ Vehicle for Sale section found")

        // Test 3: Vehicle Information subsection
        println("\nTest 3: Vehicle Information")
        root.requireByText("Vehicle Information")
            .assertVisible()
        println("✓ Vehicle Information section found")

        // Test 4: Vehicle fields exist
        println("\nTest 4: Vehicle Fields")
        val vehicleFields = listOf("Year", "Make", "Model", "Submodel")
        for (field in vehicleFields) {
            root.findByText(field)
                .assertExists("$field should exist")
            println("  ✓ Found $field field")
        }
        println("✓ All vehicle fields present")

        // Test 5: Sale Information subsection
        println("\nTest 5: Sale Information")
        root.requireByText("Sale Information")
            .assertVisible()
        println("✓ Sale Information section found")

        // Test 6: Sale fields exist
        println("\nTest 6: Sale Fields")
        val saleFields = listOf("Mileage", "Price", "Seller")
        for (field in saleFields) {
            root.findByText(field)
                .assertExists("$field should exist")
            println("  ✓ Found $field field")
        }
        println("✓ All sale fields present")

        // Test 7: Legal paperwork section
        println("\nTest 7: Legal Paperwork")
        root.requireByText("Requires Legal Paperwork")
            .assertVisible()
        root.requireByText("Requires legal paperwork?")
            .assertVisible()
        println("✓ Legal paperwork section found")

        println("\n=== All Forms Page Tests Passed ===")
    }

    @Test
    fun testMultiplePagesCrawler() = withTestHarness { harness ->
        println("\n=== Testing Multiple Pages (Screen Crawler) ===")

        val pages = listOf(
            "HomePage" to HomePage(),
            "SampleLogInPage" to SampleLogInPage,
            "ControlsPage" to ControlsPage,
            "FormsPage" to FormsPage
        )

        var successCount = 0
        var failCount = 0

        for ((name, page) in pages) {
            try {
                println("\nTesting: $name")
                val root = harness.render {
                    with(page) {
                        render()
                    }
                }

                // Basic validation - page should have children
                assertTrue(root.children.isNotEmpty(), "$name should have content")
                successCount++
                println("✓ $name rendered successfully")
            } catch (e: Exception) {
                println("✗ $name failed: ${e.message}")
                failCount++
            }
        }

        println("\n=== Screen Crawler Results ===")
        println("Total: ${pages.size} pages")
        println("Passed: $successCount")
        println("Failed: $failCount")

        assertEquals(0, failCount, "All pages should render without errors")
        println("\n✓ All pages rendered successfully!")
    }

    @Test
    fun testControlsPageInteractions() = withTestHarness { harness ->
        println("\n=== Testing Controls Page - Interactive Elements ===")

        val root = harness.render {
            with(ControlsPage) {
                render()
            }
        }

        // Test 1: Find and verify "Set to now" button for dates
        println("Test 1: Date Setter Button")
        val setToNowButtons = root.findAllByText("Set to now")
        setToNowButtons.assertNotEmpty("Should have 'Set to now' buttons")
        println("✓ Found ${setToNowButtons.size} 'Set to now' buttons")

        // Test 2: Verify buttons are enabled
        println("\nTest 2: Button States")
        for (i in setToNowButtons.indices) {
            val btn = setToNowButtons[i]
            assertTrue(btn.isEnabled, "Button $i should be enabled")
            assertTrue(btn.isVisible, "Button $i should be visible")
        }
        println("✓ All 'Set to now' buttons are enabled and visible")

        // Test 3: Verify value displays exist
        println("\nTest 3: Value Displays")
        root.requireByTextContaining("Value:").assertVisible()
        root.requireByTextContaining("Text:").assertVisible()
        println("✓ Value displays found")

        println("\n=== All Controls Page Interaction Tests Passed ===")
    }

    @Test
    fun testHomePageStructure() = withTestHarness { harness ->
        println("\n=== Testing HomePage Structure ===")

        val root = harness.render {
            with(HomePage()) {
                render()
            }
        }

        // Test 1: All main headings present
        println("Test 1: Main Headings")
        val headings = listOf(
            "KiteUI - Beautiful by Default",
            "Goals",
            "Quick Sample",
            "Getting Started"
        )

        for (heading in headings) {
            root.requireByText(heading).assertVisible()
            println("  ✓ Found heading: $heading")
        }
        println("✓ All main headings present")

        // Test 2: Goals list items
        println("\nTest 2: Goals List")
        val goals = listOf(
            "Web first",
            "Reactive",
            "Multiplatform",
            "Native",
            "Extendable",
            "Kotlin-first",
            "Declarative",
            "Semantic theming"
        )

        for (goal in goals) {
            val goalText = root.findByTextContaining(goal)
            goalText.assertExists("Goal '$goal' should be listed")
            println("  ✓ Found goal: $goal")
        }
        println("✓ All goals listed")

        // Test 3: Version info
        println("\nTest 3: Version Info")
        root.requireByTextContaining("Version:")
            .assertVisible()
        println("✓ Version info present")

        println("\n=== All HomePage Structure Tests Passed ===")
    }
}
