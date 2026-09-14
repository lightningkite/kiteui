package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.FormattedTextInput
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.formattedTextInput
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * `FormattedTextInput.content` notification behavior on web.
 *
 * The setter used a braceless `if` that only guarded the DOM write; `invokeAllListeners()` ran on
 * every call regardless of whether anything changed. That made a two-way binding fire on every
 * no-op write - exactly the same contract `VpropNotificationTest` checks for the other web
 * inputs, so this follows the same pattern for this one.
 */
class FormattedTextInputNotificationTest {

    private fun newFormattedTextInput(): FormattedTextInput {
        lateinit var field: FormattedTextInput
        root(Theme(id = "unitTest")) {
            col { formattedTextInput { field = this } }
        }
        return field
    }

    @Test
    fun rewritingTheSameValueDoesNotNotify() {
        val field = newFormattedTextInput()
        field.content.value = "hello"
        var notifications = 0
        field.content.addListener { notifications++ }

        field.content.value = "hello"

        assertEquals(0, notifications, "re-asserting the current value must not notify")
    }

    @Test
    fun writingADifferentValueNotifiesOnce() {
        val field = newFormattedTextInput()
        var notifications = 0
        field.content.addListener { notifications++ }

        field.content.value = "hello"

        assertEquals(1, notifications, "a real change must notify exactly once")
        assertEquals("hello", field.content.value, "the change must reach the DOM")
    }
}
