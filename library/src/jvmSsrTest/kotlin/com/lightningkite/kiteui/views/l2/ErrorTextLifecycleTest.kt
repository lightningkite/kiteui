package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertTrue

class ErrorTextLifecycleTest {
    @Test
    fun removedErrorTextStopsCatchingErrors() {
        val withOldErrorText = Signal(true)
        uiTest(
            content = {
                col {
                    swapping(current = { withOldErrorText() }) { withOld ->
                        col { if (withOld) errorText() }
                    }
                    button {
                        debugName = "save"
                        text("Save")
                        onClick { throw PlainTextException("Choose a date.") }
                    }
                    col {
                        debugName = "pageErrors"
                        errorText()
                    }
                }
            }
        ) {
            withOldErrorText.value = false
            click("save")
            assertTrue(snapshot("pageErrors").contains("Choose a date."), snapshot("root"))
        }
    }
}
