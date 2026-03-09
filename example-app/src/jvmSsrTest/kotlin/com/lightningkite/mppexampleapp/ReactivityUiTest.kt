// by Claude - reactivity tests for ReactivityPage
package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.mppexampleapp.internal.ReactivityPage
import kotlin.test.Test

class ReactivityUiTest {

    private fun reactivityTest(block: suspend com.lightningkite.kiteui.testing.UiTestScope.() -> Unit) {
        uiTest(content = {
            with(ReactivityPage) { render() }
        }, block = block)
    }

    @Test
    fun localInputHasDefaultValue() = reactivityTest {
        assertValue("localInput", "Local")
    }

    @Test
    fun setValueUpdatesLocalDisplay() = reactivityTest {
        setValue("localInput", "Changed")
        assertValue("localDisplay", "local = Changed")
    }
}
