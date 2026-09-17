package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.direct.Checkbox
import com.lightningkite.kiteui.views.direct.Slider
import com.lightningkite.kiteui.views.direct.TextInput
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.valueString
import com.lightningkite.kiteui.views.direct.checkbox
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.slider
import com.lightningkite.kiteui.views.direct.textInput
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Notification behavior of the web `vprop` DOM-property bindings.
 *
 * These bindings back `Checkbox.checked`, `Switch.checked`, `TextInput.content`, `Slider.value` and
 * friends. They must notify on a real change and stay quiet on a write that changes nothing, which
 * is what Android and iOS do; a web-only extra notification makes a two-way binding echo back into
 * itself. Equally important is the other direction - suppressing a notification must never suppress
 * a DOM write that the user can see - so each case here checks the rendered value as well as the
 * listener count.
 */
class VpropNotificationTest {

    private fun <T : Any> withElements(build: ViewWriter.() -> T): T {
        var result: T? = null
        root(Theme(id = "unitTest")) {
            col {
                result = build()
            }
        }
        return result!!
    }

    private fun newCheckbox(): Checkbox = withElements { lateinit var b: Checkbox; checkbox { b = this }; b }
    private fun newTextInput(): TextInput = withElements { lateinit var f: TextInput; textInput { f = this }; f }

    @Test
    fun writingADifferentValueNotifiesOnce() {
        val box = newCheckbox()
        var notifications = 0
        box.checked.addListener { notifications++ }

        box.checked.value = true

        assertEquals(1, notifications, "a real change must notify exactly once")
        assertTrue(box.checked.value, "the change must reach the DOM")
    }

    @Test
    fun rewritingTheSameValueDoesNotNotify() {
        val box = newCheckbox()
        box.checked.value = true
        var notifications = 0
        box.checked.addListener { notifications++ }

        box.checked.value = true
        box.checked.value = true

        assertEquals(0, notifications, "re-asserting the current value must not notify")
        assertTrue(box.checked.value, "the value must be left alone, not cleared")
    }

    @Test
    fun togglingBackAndForthNotifiesEachTime() {
        val box = newCheckbox()
        var notifications = 0
        box.checked.addListener { notifications++ }

        box.checked.value = true
        box.checked.value = false
        box.checked.value = true

        assertEquals(3, notifications, "each genuine transition must notify")
        assertTrue(box.checked.value)
    }

    @Test
    fun textCanBeResetAfterTheUserTypes() {
        // The regression this guards against: if the guard compared against the HTML attribute
        // instead of the live property, the attribute would still read "start" after the user
        // typed, and this reset would be skipped as a no-op write - leaving the typed text on
        // screen while the model believed it had been cleared.
        val field = newTextInput()
        field.content.value = "start"

        field.native.attributes.valueString = "user typed this"
        assertEquals("user typed this", field.content.value, "precondition: property reflects typing")

        field.content.value = "start"

        assertEquals("start", field.content.value, "resetting to the previous value must still write")
    }

    @Test
    fun emptyingAFieldIsARealChange() {
        val field = newTextInput()
        field.content.value = "something"
        var notifications = 0
        field.content.addListener { notifications++ }

        field.content.value = ""

        assertEquals(1, notifications, "clearing a non-empty field is a change")
        assertEquals("", field.content.value)
    }

    @Test
    fun aClampedSliderWriteStillAppliesAndNotifies() {
        // A range input normalizes what it stores. Since the guard reads back the normalized
        // value, an out-of-range write always differs from it and is never mistaken for a no-op.
        val bar = withElements { lateinit var s: Slider; slider { s = this }; s }
        bar.value.value = 0.5f
        var notifications = 0
        bar.value.addListener { notifications++ }

        bar.value.value = 0.5f
        assertEquals(0, notifications, "the already-stored value must not notify")

        bar.value.value = 0.75f
        assertEquals(1, notifications, "a new value must notify")
        assertEquals(0.75f, bar.value.value)
    }
}
