package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.reformatWithSelection
import com.lightningkite.kiteui.views.direct.PhoneNumberFormat
import kotlin.test.Test
import kotlin.test.assertEquals

class PhoneNumberFormatTest {
    private val format = PhoneNumberFormat.USA

    // Each '|' marks a caret; two of them mark a selection.  The input is the field's text right after the
    // browser/OS applied an edit, the output is what the field shows after reformatting.
    private fun edit(textWithCaret: String): String {
        val carets = textWithCaret.indices.filter { textWithCaret[it] == '|' }
        val dirty = textWithCaret.replace("|", "")
        val (result, selection) = format.reformatWithSelection(dirty, carets.first() until carets.last() - (carets.size - 1))
        return if (selection.isEmpty()) result.substring(0, selection.first) + "|" + result.substring(selection.first)
        else result.substring(0, selection.first) + "|" + result.substring(selection) + "|" + result.substring(selection.last + 1)
    }

    @Test fun usNumbers() {
        assertEquals("(801) 555-0123|", edit("8015550123|"))
        assertEquals("(801) 555-0123|", edit("(801) 555-0123|"))
        assertEquals("(801) 555-012|", edit("801555012|"))
        assertEquals("(801|) ", edit("801|"))
        assertEquals("|", edit("|"))
    }

    @Test fun countryCodeOneStaysVisible() {
        assertEquals("+1 (801) 555-0123|", edit("+18015550123|"))
        assertEquals("1 (801) 555-0123|", edit("18015550123|"))
        assertEquals("+1|", edit("+1|"))
        assertEquals("+1 (8|", edit("+18|"))
    }

    @Test fun otherCountryCodesAreShownAsTyped() {
        assertEquals("+442079460958|", edit("+44 20 7946 0958|"))
        assertEquals("+|", edit("+|"))
    }

    @Test fun nothingIsDropped() {
        assertEquals("(801) 555-012345|", edit("801555012345|"))
        assertEquals("+1 (801) 555-01234|", edit("+180155501234|"))
        assertEquals("+4420794609581234|", edit("+4420794609581234|"))
    }

    @Test fun plusIsDataOnlyAtTheStart() {
        assertEquals("(801) 555-0123|", edit("801+5550123|"))
        assertEquals("(801) 5|55-0123", edit("(801) 5+|55-0123"))
        assertEquals("+|8015550123", edit("+|(801) 555-0123"))
    }

    @Test fun insertingMidNumberKeepsTheCaretOnTheTypedDigit() {
        assertEquals("(801) 59|5-50123", edit("(801) 59|55-0123"))
        assertEquals("+1 (801) 59|5-50123", edit("+1 (801) 59|55-0123"))
    }

    @Test fun backspaceOverFormattingMovesTheCaretBackOverIt() {
        assertEquals("(801|) 555-0123", edit("(801| 555-0123"))
        assertEquals("(801|) 555-0123", edit("(801)|555-0123"))
        assertEquals("(801) 555|-0123", edit("(801) 555|0123"))
        assertEquals("+1| (801) 555-0123", edit("+1|(801) 555-0123"))
    }

    @Test fun pasteReplacingASelection() {
        assertEquals("(801) 777|-0123", edit("(801) 777|-0123"))
        assertEquals("+1 (435) 555-9999|", edit("+1 (435) 555-9999|"))
    }

    // Every platform reads the value back from the shown text, so formatting it again must not change it.
    @Test fun valueIsTheDataOfWhatIsShown() {
        val shapes = mapOf(
            "8015550123" to "8015550123",
            "(801) 555-0123" to "8015550123",
            "+18015550123" to "+18015550123",
            "18015550123" to "18015550123",
            "+44 20 7946 0958" to "+442079460958",
            "801555012345" to "801555012345",
            "801+5550123" to "8015550123",
        )
        for ((typed, value) in shapes) {
            val shown = format.format(typed.filter(format::isRawData))
            assertEquals(value, shown.filter(format::isRawData), typed)
            assertEquals(shown, format.format(value), typed)
        }
    }
}
