// by Claude - click interaction tests for ClickTestPage
package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.testing.UiTestConfig
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.mppexampleapp.internal.ClickTestPage
import kotlin.test.Test

class ClickTestUiTest {

    private fun clickTest(block: suspend com.lightningkite.kiteui.testing.UiTestScope.() -> Unit) {
        // ClickTestPage uses link {} which requires a pageNavigator
        val navigator = PageNavigator { AutoRoutes }
        uiTest(
            config = UiTestConfig(navigator = navigator),
            content = { with(ClickTestPage) { render() } }
        ) {
            // Reset singleton state at start of each test
            click("clearLog")
            block()
        }
    }

    @Test
    fun clickButton1UpdatesLastClicked() = clickTest {
        click("button1")
        assertValue("lastClicked", "Button 1")
    }

    @Test
    fun clickButton5UpdatesLastClicked() = clickTest {
        click("button5")
        assertValue("lastClicked", "Button 5")
    }

    @Test
    fun clickButton9UpdatesLastClicked() = clickTest {
        click("button9")
        assertValue("lastClicked", "Button 9")
    }

    @Test
    fun clearLogResetsLastClicked() = clickTest {
        click("button3")
        assertValue("lastClicked", "Button 3")
        click("clearLog")
        assertValue("lastClicked", "(none)")
    }
}
