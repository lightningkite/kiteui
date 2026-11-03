package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.ElementContext
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import platform.UIKit.UIBarButtonItem
import platform.UIKit.UIToolbar
import platform.UIKit.UIViewController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Regression coverage for the iOS date/time field "clear" fix. `content` was nullable but a user
 * could never unset it once a [platform.UIKit.UIDatePicker] value had been picked (there is no
 * inline "no selection" state for a UIDatePicker). [LocalDateField], [LocalTimeField] and
 * [LocalDateTimeField] now each get a Clear button in their `inputAccessoryView` toolbar, backed by
 * [ClearDoneTrigger], which is exposed with `internal` visibility specifically so tests can invoke
 * [ClearDoneTrigger.clear]/[ClearDoneTrigger.done] directly as ordinary Kotlin calls instead of
 * routing through `NSObject.performSelector` (which segfaults for void-returning selectors in
 * Kotlin/Native, since `performSelector` is typed to return `id` and Kotlin tries to retain
 * whatever garbage happens to be in the return register).
 *
 * These fields are constructed directly against a bare `ElementContext`/`UIViewController` rather
 * than through the full `vc.setup { }` view-tree bootstrap, since `hint`/`content`/`action` (and
 * here, `range`/`inputAccessoryView`) all work against the wrapped native control without needing
 * the theming pipeline to have run — see the other iOS field tests for the full-tree pattern.
 */
class LocalDateTimeFieldClearTest {

    private fun context() = ElementContext(UIViewController(null, null))

    // -- LocalDateField ------------------------------------------------------------------------

    @Test
    fun localDateFieldClearSetsContentNullAndShowsDash() {
        val field = LocalDateField(context())
        field.content.value = LocalDate(2024, 1, 15)
        assertEquals(LocalDate(2024, 1, 15), field.content.value)

        field.trigger.clear()

        assertNull(field.content.value)
        assertEquals("-", field.textField.text)
    }

    @Test
    fun localDateFieldToolbarHasClearAndDoneButtons() {
        val field = LocalDateField(context())
        assertToolbarHasClearAndDone(field.textField.inputAccessoryView as UIToolbar, doneTitle = "Done")

        field.action = Action("Save") {}
        assertToolbarHasClearAndDone(field.textField.inputAccessoryView as UIToolbar, doneTitle = "Save")
    }

    // -- LocalTimeField --------------------------------------------------------------------------

    @Test
    fun localTimeFieldClearSetsContentNullAndShowsDash() {
        val field = LocalTimeField(context())
        field.content.value = LocalTime(13, 30)
        assertEquals(LocalTime(13, 30), field.content.value)

        field.trigger.clear()

        assertNull(field.content.value)
        assertEquals("-", field.textField.text)
    }

    @Test
    fun localTimeFieldToolbarHasClearAndDoneButtons() {
        val field = LocalTimeField(context())
        assertToolbarHasClearAndDone(field.textField.inputAccessoryView as UIToolbar, doneTitle = "Done")

        field.action = Action("Save") {}
        assertToolbarHasClearAndDone(field.textField.inputAccessoryView as UIToolbar, doneTitle = "Save")
    }

    // -- LocalDateTimeField ----------------------------------------------------------------------

    @Test
    fun localDateTimeFieldClearSetsContentNullAndShowsDash() {
        val field = LocalDateTimeField(context())
        field.content.value = LocalDateTime(2024, 1, 15, 13, 30)
        assertEquals(LocalDateTime(2024, 1, 15, 13, 30), field.content.value)

        field.trigger.clear()

        assertNull(field.content.value)
        assertEquals("-", field.textField.text)
    }

    @Test
    fun localDateTimeFieldToolbarHasClearAndDoneButtons() {
        val field = LocalDateTimeField(context())
        assertToolbarHasClearAndDone(field.textField.inputAccessoryView as UIToolbar, doneTitle = "Done")

        field.action = Action("Save") {}
        assertToolbarHasClearAndDone(field.textField.inputAccessoryView as UIToolbar, doneTitle = "Save")
    }

    private fun assertToolbarHasClearAndDone(toolbar: UIToolbar, doneTitle: String) {
        val titles = toolbar.items?.map { (it as UIBarButtonItem).title }
        assertEquals(listOf("Clear", null, doneTitle), titles)
    }
}
