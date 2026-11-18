package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.kiteui.views.Element
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.ElementWithAction
import com.lightningkite.reactive.core.MutableReactiveValue

public fun CharSequence.substringOrNull(startIndex: Int, endIndex: Int): String? {
    if (startIndex !in indices) return null
    if (endIndex > length) return substring(startIndex, length)

    return substring(startIndex, endIndex)
}

public enum class PhoneNumberFormat {
    // This will need to be expanded over time, when the need arises. Phone numbers are complicated as hell.

    USA {
        // Never drops a digit: over-long input stays visible so the caller's validation can reject it.
        override fun format(clean: String): String {
            val digits = clean.filter { it.isDigit() }
            val countryCode = when {
                clean.startsWith('+') && !digits.startsWith('1') -> return "+$digits"
                clean.startsWith('+') -> "+1"
                // NANP area codes never start with 1, so a leading 1 can only be the country code.
                digits.startsWith('1') -> "1"
                else -> ""
            }
            val national = if (countryCode.isEmpty()) digits else digits.drop(1)
            val area = national.take(3)
            val exchange = national.drop(3).take(3)
            val line = national.drop(6)

            return buildString {
                append(countryCode)
                if (area.isEmpty()) return@buildString
                if (countryCode.isNotEmpty()) append(' ')
                append('(')
                append(area)
                if (area.length < 3) return@buildString
                append(") ")
                append(exchange)
                if (exchange.length < 3) return@buildString
                append('-')
                append(line)
            }
        }
    },
    ;

    /**
    * Regex that matches formatted or unformatted **complete** phone numbers
    * */
    public val regex: Regex = Regex("""(?:\+\d+ )?\(\d{3}\) \d{3}-\d{4}|(?:\+\d+)?\d{10}|(?:\+\d+-)?\d{3}-\d{3}-\d{4}|(?:\+\d+ )?\d{3} \d{3} \d{4}""")
    /**
     * Determines if a character is valid data to be entered for the phone number.
     * */
    public open fun isRawData(char: Char): Boolean = char.isDigit() || char == '+'
    /**
     * Takes in a clean phone number or partial phone number and formats it properly relative to its completion
     * */
    public abstract fun format(clean: String): String
}

public class PhoneNumberInput(private val input: FormattedTextInput): ElementWithAction by input {
    public constructor(context: ElementContext) : this(FormattedTextInput(context))

    init {
        input.keyboardHints = KeyboardHints.phone
        input.format(PhoneNumberFormat.USA::isRawData, PhoneNumberFormat.USA::format)
    }

    public var format: PhoneNumberFormat = PhoneNumberFormat.USA
        set(value) {
            field = value
            input.format(value::isRawData, value::format)
        }

    public val content: MutableReactiveValue<String> by input::content
    public var hint: String by input::hint
    public var align: Align? by input::align

    @Deprecated("No longer needed", ReplaceWith("this")) public inline val rView: Element get() = this
}
