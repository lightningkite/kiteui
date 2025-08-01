package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyboardHints
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.kiteui.views.ViewWriter
import kotlin.coroutines.CoroutineContext

public inline fun CharSequence.substringOrNull(startIndex: Int, endIndex: Int): String? {
    if (startIndex !in indices) return null
    if (endIndex > length) return substring(startIndex, length)

    return substring(startIndex, endIndex)
}

public enum class PhoneNumberFormat {
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
    public val regex: Regex = Regex("""(?:\+\d+ )?\(\d{3}\) \d{3}-\d{4}|(?:\+\d+)?\d{10}|(?:\+\d+-)?\d{3}-\d{3}-\d{4}|(?:\+\d+ )?\d{3} \d{3} \d{4}""")
    /**
     * Determines if a character is valid data to be entered for the phone number.
     * */
    public open fun isRawData(char: Char): Boolean = char.isDigit()
    /**
     * Takes in a clean phone number or partial phone number and formats it properly relative to its completion
     * */
    public abstract fun format(clean: String): String
}

public class PhoneNumberInput(container: ViewWriter): ViewModifiable {
    public override val coroutineContext: CoroutineContext
        get() = input.coroutineContext
    private val input = container.formattedTextInput {
        keyboardHints = KeyboardHints.phone
        format(PhoneNumberFormat.USA::isRawData, PhoneNumberFormat.USA::format)
    }
    public override val rView: RView get() = input
    public var format: PhoneNumberFormat = PhoneNumberFormat.USA
        set(value) {
            field = value
            input.format(value::isRawData, value::format)
        }
    public var enabled: Boolean by input::enabled
    public val content: ImmediateWritable<String> by input::content
    public var hint: String by input::hint
    public var align: Align by input::align
}

public operator fun ViewWrapper.minus(view: PhoneNumberInput): ViewWrapper { return ViewWrapper }
public operator fun Boolean.minus(view: PhoneNumberInput): Boolean { return true }

public operator fun ViewWrapper.contains(view: PhoneNumberInput): Boolean { return true }
public operator fun Boolean.contains(view: PhoneNumberInput): Boolean { return true }