package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWithAction
import com.lightningkite.reactive.core.MutableReactiveValue

fun CharSequence.substringOrNull(startIndex: Int, endIndex: Int): String? {
    if (startIndex !in indices) return null
    if (endIndex > length) return substring(startIndex, length)

    return substring(startIndex, endIndex)
}

enum class PhoneNumberFormat {
    // This will need to be expanded over time, when the need arises. Phone numbers are complicated as hell.

    USA {
        override fun format(clean: String): String {
            val area = clean.substringOrNull(0, 3)?.takeUnless { it.isBlank() } ?: return ""
            val g1 = clean.substringOrNull(3,6)
            val g2 = clean.substringOrNull(6,10)

            return buildString {
                append('(')
                append(area)
                if (area.length == 3) append(") ")
                if (g1 == null) return@buildString
                append(g1)
                if (g1.length == 3) append('-')
                if (g2 == null) return@buildString
                append(g2)
            }
        }
    },
    ;

    /**
    * Regex that matches formatted or unformatted **complete** phone numbers
    * */
    val regex: Regex = Regex("""(?:\+\d+ )?\(\d{3}\) \d{3}-\d{4}|(?:\+\d+)?\d{10}|(?:\+\d+-)?\d{3}-\d{3}-\d{4}|(?:\+\d+ )?\d{3} \d{3} \d{4}""")
    /**
     * Determines if a character is valid data to be entered for the phone number.
     * */
    open fun isRawData(char: Char): Boolean = char.isDigit()
    /**
     * Takes in a clean phone number or partial phone number and formats it properly relative to its completion
     * */
    abstract fun format(clean: String): String
}

class PhoneNumberInput(private val input: FormattedTextInput): ElementWithAction by input {
    constructor(context: ElementContext) : this(FormattedTextInput(context))

    init {
        input.keyboardHints = KeyboardHints.phone
        input.format(PhoneNumberFormat.USA::isRawData, PhoneNumberFormat.USA::format)
    }

    var format: PhoneNumberFormat = PhoneNumberFormat.USA
        set(value) {
            field = value
            input.format(value::isRawData, value::format)
        }

    val content: MutableReactiveValue<String> by input::content
    var hint: String by input::hint
    var align: Align? by input::align

    @Deprecated("No longer needed", ReplaceWith("this")) inline val rView: Element get() = this
}
