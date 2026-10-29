package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.forEach
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.core.Signal
import kotlin.test.Test
import kotlin.test.assertTrue

class ErrorTextLifecycleTest {
    @Suppress("DEPRECATION")
    @OptIn(InternalKiteUi::class)
    @Test
    fun removedErrorTextStopsCatchingErrors() {
        val items = Signal(listOf(1))
        uiTest(
            content = {
                col {
                    debugName = "page"
                    errorText()
                    col {
                        forEach(items) { item ->
                            if (item == 1) errorText()
                            else button {
                                debugName = "save"
                                text("Save")
                                onClick { throw PlainTextException("Choose a date.") }
                            }
                        }
                    }
                }
            }
        ) {
            items.value = listOf(2)
            click("save")
            assertTrue(snapshot("page").contains("Choose a date."), snapshot("root"))
        }
    }
}
