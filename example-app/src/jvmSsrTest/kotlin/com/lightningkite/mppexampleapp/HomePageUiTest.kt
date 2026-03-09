// by Claude - counter interaction tests for HomePage
package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.testing.uiTest
import kotlin.test.Test

class HomePageUiTest {

    private fun homeTest(block: suspend com.lightningkite.kiteui.testing.UiTestScope.() -> Unit) {
        uiTest(content = {
            with(HomePage()) { render() }
        }, block = block)
    }

    @Test
    fun counterStartsAtZero() = homeTest {
        assertValue("counter", "0")
    }

    @Test
    fun incrementButtonIncreasesCounter() = homeTest {
        click("increment")
        assertValue("counter", "1")
    }

    @Test
    fun decrementButtonDecreasesCounter() = homeTest {
        click("decrement")
        assertValue("counter", "-1")
    }

    @Test
    fun multipleIncrementsWork() = homeTest {
        click("increment")
        click("increment")
        click("increment")
        assertValue("counter", "3")
    }
}
