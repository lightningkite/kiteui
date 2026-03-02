// by Claude - navigation tests using uiTest() with PageNavigator + AutoRoutes
package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.testing.UiTestConfig
import com.lightningkite.kiteui.testing.findByType
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.l2.navigatorView
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class NavigationUiTest {

    private fun navTest(block: suspend com.lightningkite.kiteui.testing.UiTestScope.() -> Unit) {
        val navigator = PageNavigator { AutoRoutes }
        navigator.reset(HomePage())
        uiTest(
            config = UiTestConfig(navigator = navigator),
            content = { navigatorView(navigator) },
            block = block
        )
    }

    @Test
    fun initialPageIsHomePage() = navTest {
        assertPage("HomePage")
    }

    @Test
    fun navigateToClickTestPage() = navTest {
        navigate("/click-test")
        assertPage("ClickTestPage")
    }

    @Test
    fun navigateToReactivityPage() = navTest {
        navigate("/reactivity")
        assertPage("ReactivityPage")
    }

    @Test
    fun navigateAndGoBack() = navTest {
        navigate("/click-test")
        assertPage("ClickTestPage")
        back()
        assertPage("HomePage")
    }

    @Test
    fun navigateMultiplePages() = navTest {
        navigate("/click-test")
        assertPage("ClickTestPage")
        navigate("/reactivity")
        assertPage("ReactivityPage")
        back()
        assertPage("ClickTestPage")
        back()
        assertPage("HomePage")
    }

    @Test
    fun navigateToLoginPage() = navTest {
        navigate("/sample/login")
        assertPage("SampleLogInPage")
    }

    @Test
    fun snapshotContainsPageContent() = navTest {
        val snap = snapshot()
        // HomePage should have rendered some text content
        val textComponents = snap.findByType("Text")
        assertTrue(textComponents.isNotEmpty(), "HomePage should contain text components")
    }
}
