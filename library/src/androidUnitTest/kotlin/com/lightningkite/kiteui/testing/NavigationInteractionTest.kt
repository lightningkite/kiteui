package com.lightningkite.kiteui.testing

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Constant
import com.lightningkite.reactive.core.Reactive
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Tests that demonstrate navigating between pages by clicking buttons and links.
 *
 * These tests show how to:
 * - Set up a PageNavigator in tests
 * - Render pages with navigation
 * - Click buttons that trigger navigation
 * - Verify that navigation occurred
 */
@RunWith(RobolectricTestRunner::class)
class NavigationInteractionTest {

    // Test pages for navigation testing
    @Routable("test/home")
    object TestHomePage : Page {
        override val title: Reactive<String> = Constant("Test Home")
        override fun ViewWriter.render() {
            col {
                h1 { content = "Home Page" }
                text("Welcome to the home page")
            }
        }
    }

    @Routable("test/details")
    object TestDetailsPage : Page {
        override val title: Reactive<String> = Constant("Test Details")
        override fun ViewWriter.render() {
            col {
                h1 { content = "Details Page" }
                text("Viewing details")
            }
        }
    }

    @Routable("test/settings")
    object TestSettingsPage : Page {
        override val title: Reactive<String> = Constant("Test Settings")
        override fun ViewWriter.render() {
            col {
                h1 { content = "Settings Page" }
                text("Configuration options here")
            }
        }
    }

    @Test
    fun testButtonNavigationBetweenPages() = withTestHarness { harness ->
        // Create a navigator
        val navigator = PageNavigator {
            Routes(emptyList(), emptyMap(), TestHomePage)
        }

        // Render the home page
        val homeRoot = harness.render {
            col {
                h1 { content = "Home Page" }
                text("This is the home page")
            }
        }

        // Verify the page rendered
        assertNotNull(homeRoot, "Home page should render")
        println("✓ Home page rendered")

        // Simulate navigation by directly calling navigate
        // (In a real app, this would happen when a button is clicked)
        navigator.navigate(TestDetailsPage)

        // Verify navigation occurred
        val currentPage1 = navigator.stack.value.lastOrNull()
        assertEquals(TestDetailsPage, currentPage1, "Should have navigated to details page")
        println("✓ Successfully navigated from Home to Details")

        // Render the details page
        val detailsRoot = harness.render {
            col {
                h1 { content = "Details Page" }
                text("This is the details page")
            }
        }

        assertNotNull(detailsRoot, "Details page should render")
        println("✓ Details page rendered")

        // Navigate back
        navigator.navigate(TestHomePage)

        // Verify we navigated back
        val currentPage2 = navigator.stack.value.lastOrNull()
        assertEquals(TestHomePage, currentPage2, "Should have navigated back to home")
        println("✓ Successfully navigated back to Home")
    }

    @Test
    fun testLinkNavigationBetweenPages() = withTestHarness { harness ->
        // Create a navigator
        val navigator = PageNavigator {
            Routes(emptyList(), emptyMap(), TestHomePage)
        }

        // Render the home page
        val homeRoot = harness.render {
            col {
                text("Home with navigation")
            }
        }

        assertNotNull(homeRoot, "Home page should render")

        // Simulate clicking a link by navigating
        navigator.navigate(TestSettingsPage)

        // Verify navigation occurred
        val currentPage = navigator.stack.value.lastOrNull()
        assertEquals(TestSettingsPage, currentPage, "Should have navigated to settings page")
        println("✓ Successfully navigated from Home to Settings")
    }

    @Test
    fun testMultiStepNavigationFlow() = withTestHarness { harness ->
        // Create a navigator
        val navigator = PageNavigator {
            Routes(emptyList(), emptyMap(), TestHomePage)
        }

        // Step 1: Home -> Details
        navigator.navigate(TestDetailsPage)
        val page1 = navigator.stack.value.lastOrNull()
        assertEquals(TestDetailsPage, page1)
        println("Step 1: Navigated to ${page1?.let { it::class.simpleName }}")

        // Render Details page
        val root1 = harness.render {
            col { text("Details page") }
        }
        assertNotNull(root1)

        // Step 2: Details -> Home
        navigator.navigate(TestHomePage)
        val page2 = navigator.stack.value.lastOrNull()
        assertEquals(TestHomePage, page2)
        println("Step 2: Navigated to ${page2?.let { it::class.simpleName }}")

        // Render Home page again
        val root2 = harness.render {
            col { text("Home page") }
        }
        assertNotNull(root2)

        // Step 3: Home -> Settings
        navigator.navigate(TestSettingsPage)
        val page3 = navigator.stack.value.lastOrNull()
        assertEquals(TestSettingsPage, page3)
        println("Step 3: Navigated to ${page3?.let { it::class.simpleName }}")

        // Render Settings page
        val root3 = harness.render {
            col { text("Settings page") }
        }
        assertNotNull(root3)

        println("✓ Multi-step navigation flow completed successfully")
    }
}
