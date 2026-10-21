package com.lightningkite.kiteui.views.l2

import com.lightningkite.kiteui.exceptions.PlainTextException
import com.lightningkite.kiteui.testing.uiTest
import com.lightningkite.kiteui.views.direct.*
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FieldErrorScopeTest {
    @Test
    fun pageErrorDoesNotLandInFirstField() = uiTest(
        content = {
            col {
                field("Name") { textInput { } }.debugName = "nameField"
                button {
                    debugName = "save"
                    text("Save")
                    onClick { throw PlainTextException("Choose a date.") }
                }
                errorText()
            }
        }
    ) {
        click("save")
        assertFalse(snapshot("nameField").contains("Choose a date."), snapshot("nameField"))
        assertTextVisible("Choose a date.")
    }

    @Test
    fun errorInsideFieldLandsInThatField() = uiTest(
        content = {
            col {
                field("Name") {
                    button {
                        debugName = "check"
                        text("Check")
                        onClick { throw PlainTextException("Name taken.") }
                    }
                }.debugName = "nameField"
            }
        }
    ) {
        click("check")
        assertTrue(snapshot("nameField").contains("Name taken."), snapshot("nameField"))
    }
}
