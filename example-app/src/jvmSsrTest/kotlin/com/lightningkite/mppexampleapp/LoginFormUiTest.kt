// by Claude - form input tests for SampleLogInPage
package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.mppexampleapp.internal.SampleLogInPage
import kotlin.test.Test
import kotlin.test.assertNotNull

class LoginFormUiTest {

    private fun loginTest(block: suspend com.lightningkite.kiteui.testing.UiTestScope.() -> Unit) {
        uiTest(content = {
            with(SampleLogInPage) { render() }
        }, block = block)
    }

    @Test
    fun emailInputExists() = loginTest {
        assertVisible("emailInput")
    }

    @Test
    fun passwordInputExists() = loginTest {
        assertVisible("passwordInput")
    }

    @Test
    fun canSetEmailValue() = loginTest {
        setValue("emailInput", "test@example.com")
        assertValue("emailInput", "test@example.com")
    }

    @Test
    fun canSetPasswordValue() = loginTest {
        setValue("passwordInput", "secret123")
        assertValue("passwordInput", "secret123")
    }
}
