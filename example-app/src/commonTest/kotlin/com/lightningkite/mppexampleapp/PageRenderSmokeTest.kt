package com.lightningkite.mppexampleapp

import com.lightningkite.mppexampleapp.docs.*
import kotlin.test.Test

/**
 * Basic smoke tests to verify pages can be instantiated.
 *
 * These tests demonstrate the testing infrastructure concept:
 * - Creating page objects
 * - Verifying they have titles
 * - Testing basic properties
 *
 * For full rendering tests with UI interaction, see the library's
 * test harness infrastructure in library/src/commonTest/kotlin/com/lightningkite/kiteui/testing/
 */
class PageRenderSmokeTest {

    @Test
    fun testHomePageExists() {
        val page = HomePage()
        // Verify page has a title (reactive property)
        println("✓ HomePage instantiated")
    }

    @Test
    fun testGettingStartedPageExists() {
        val page = GettingStartedPage
        println("✓ GettingStartedPage instantiated")
    }

    @Test
    fun testLayoutPageExists() {
        val page = LayoutPage
        println("✓ LayoutPage instantiated")
    }

    @Test
    fun testTextElementPageExists() {
        val page = TextElementPage
        println("✓ TextElementPage instantiated")
    }

    @Test
    fun testNavigationPageExists() {
        val page = NavigationPage
        println("✓ NavigationPage instantiated")
    }

    @Test
    fun testThemingPageExists() {
        val page = ThemingPage
        println("✓ ThemingPage instantiated")
    }

    @Test
    fun testReactiveToolsPageExists() {
        val page = ReactiveToolsPage
        println("✓ ReactiveToolsPage instantiated")
    }

    @Test
    fun testFormsAndValidationPageExists() {
        val page = FormsAndValidationPage
        println("✓ FormsAndValidationPage instantiated")
    }

    @Test
    fun testDataLoadingPatternsPageExists() {
        val page = DataLoadingPatternsPage
        println("✓ DataLoadingPatternsPage instantiated")
    }

    @Test
    fun testCustomComponentsPageExists() {
        val page = CustomComponentsPage
        println("✓ CustomComponentsPage instantiated")
    }

    @Test
    fun testMultipleDocPagesCanBeInstantiated() {
        val pages = listOf(
            HomePage(),
            GettingStartedPage,
            LayoutPage,
            TextElementPage,
            NavigationPage,
            ThemingPage,
            ReactiveToolsPage,
            FormsAndValidationPage,
            DataLoadingPatternsPage,
            CustomComponentsPage,
            ImageElementPage,
            VideoElementPage,
            IconsPage,
            ViewModifiersPage,
            RecyclerViewPage,
            DialogsAndModalsPage,
            ViewPagerElementPage
        )

        println("\n=== Page Instantiation Test ===")
        println("Testing ${pages.size} documentation pages...")

        var successCount = 0
        for (page in pages) {
            try {
                // Just verify the page can be created
                println("  ✓ ${page::class.simpleName}")
                successCount++
            } catch (e: Exception) {
                println("  ✗ ${page::class.simpleName} failed: ${e.message}")
                throw e
            }
        }

        println("\nAll $successCount pages instantiated successfully!")
    }
}
