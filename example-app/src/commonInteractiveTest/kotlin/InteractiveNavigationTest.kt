package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.testing.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.docs.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Cross-platform interactive navigation tests.
 *
 * These tests demonstrate:
 * - Navigating between pages programmatically
 * - Tracking navigation state via stack
 * - Testing multi-page flows
 * - Verifying pages can be rendered
 *
 * Note: Uses expect/actual annotations for platform-specific test setup.
 * On Android, this runs with Robolectric. On other platforms, it runs natively.
 */
@JUnitRunWith(RobolectricTestRunner::class)
@RobolectricConfig
class InteractiveNavigationTest {

    @Test
    fun testNavigationBetweenDocumentationPages() = withTestHarness { harness ->
        println("\n=== Test: Navigation Between Documentation Pages ===")

        // Create a navigator starting at HomePage
        val homePage = HomePage()
        val navigator = PageNavigator {
            Routes(emptyList(), emptyMap(), homePage)
        }

        // Initialize the navigation stack with the home page
        navigator.reset(homePage)

        // Verify we start at HomePage
        assertEquals(homePage, navigator.stack.value.lastOrNull())
        println("✓ Started at HomePage")

        // Navigate to GettingStartedPage
        navigator.navigate(GettingStartedPage)
        assertEquals(GettingStartedPage, navigator.stack.value.lastOrNull())
        println("✓ Navigated to GettingStartedPage")

        // Verify we can render the page
        val root = harness.render {
            col {
                h1 { content = "Getting Started" }
                text("This is the getting started page")
            }
        }
        assertNotNull(root)
        println("✓ GettingStartedPage rendered successfully")

        // Navigate to LayoutPage
        navigator.navigate(LayoutPage)
        assertEquals(LayoutPage, navigator.stack.value.lastOrNull())
        println("✓ Navigated to LayoutPage")

        // Navigate back
        navigator.goBack()
        assertEquals(GettingStartedPage, navigator.stack.value.lastOrNull())
        println("✓ Navigated back to GettingStartedPage")

        // Navigate to TextElementPage
        navigator.navigate(TextElementPage)
        assertEquals(TextElementPage, navigator.stack.value.lastOrNull())
        println("✓ Navigated to TextElementPage")

        // Verify stack contains correct history
        val stack = navigator.stack.value
        assertEquals(3, stack.size, "Stack should have 3 pages")
        assertEquals(homePage, stack[0])
        assertEquals(GettingStartedPage, stack[1])
        assertEquals(TextElementPage, stack[2])
        println("✓ Navigation stack is correct")
    }

    @Test
    fun testNavigationThroughAllMainPages() = withTestHarness { harness ->
        println("\n=== Test: Navigation Through All Main Pages ===")

        val homePage = HomePage()
        val navigator = PageNavigator {
            Routes(emptyList(), emptyMap(), homePage)
        }

        // Initialize the navigation stack
        navigator.reset(homePage)

        val pagesToVisit = listOf(
            GettingStartedPage,
            LayoutPage,
            TextElementPage,
            NavigationPage,
            ThemingPage,
            ReactiveToolsPage
        )

        pagesToVisit.forEach { page ->
            navigator.navigate(page)
            val currentPage = navigator.stack.value.lastOrNull()
            assertEquals(page, currentPage, "Should have navigated to ${page::class.simpleName}")
            println("✓ Visited ${page::class.simpleName}")
        }

        // Verify final stack size
        assertEquals(7, navigator.stack.value.size, "Stack should have 7 pages (HomePage + 6 visited pages)")
        println("✓ Successfully navigated through main documentation pages")
    }

    @Test
    fun testNavigationWithReset() = withTestHarness { harness ->
        println("\n=== Test: Navigation With Reset ===")

        val homePage = HomePage()
        val navigator = PageNavigator {
            Routes(emptyList(), emptyMap(), homePage)
        }

        // Initialize the navigation stack
        navigator.reset(homePage)

        // Navigate to several pages
        navigator.navigate(GettingStartedPage)
        navigator.navigate(LayoutPage)
        navigator.navigate(TextElementPage)

        assertEquals(4, navigator.stack.value.size)
        println("✓ Navigated through multiple pages")

        // Reset to a new page
        navigator.reset(ThemingPage)

        // Stack should only contain the reset page
        assertEquals(1, navigator.stack.value.size)
        assertEquals(ThemingPage, navigator.stack.value.lastOrNull())
        println("✓ Reset navigation stack to ThemingPage")

        // Navigate from reset page
        val newHomePage = HomePage()
        navigator.navigate(newHomePage)
        assertEquals(2, navigator.stack.value.size)
        assertEquals(newHomePage, navigator.stack.value.lastOrNull())
        println("✓ Can navigate after reset")
    }
}
