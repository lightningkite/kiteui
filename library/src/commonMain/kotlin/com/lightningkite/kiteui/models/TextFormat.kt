package com.lightningkite.kiteui.models

import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * Defines a string transformation, but in a way that text selection can be transformed
 * to match the formatted output.
 *
 * [format] receives only data characters. It may add formatting characters and drop data ones, but must never add a
 * data character, and must keep or drop each data character regardless of what comes after it. Formatting the data
 * of its own output must give that output again. The caret is placed by these rules, and the input's value is read
 * back from the shown text, so breaking them moves the caret to the wrong place or changes the value.
 */
public interface TextFormat {
    /** Returns `true` if [char] is data rather than formatting. */
    public fun isRawData(char: Char): Boolean

    /** Returns the data in [clean] laid out for display. */
    public fun format(clean: String): String

    public companion object {
        /** No-Op formatting */
        public val None: TextFormat = TextFormat(isRawData = { true }, format = { it })

        /** Limits the input to [n] characters, using [String.take]. All characters are allowed.  */
        public fun take(n: Int): TextFormat = TextFormat(isRawData = { true }, format = { it.take(n) })

        private fun numberFormat(allowDecimal: Boolean) =
            TextFormat(isRawData = { it.isDigit() || it == '.' || it == '-' }) { clean ->
                val sign = if (clean.startsWith('-')) "-" else ""
                val digits = clean.filterNot { it == '-' }
                val preDecimal = groupThousands(digits.substringBefore('.'))
                val postDecimal = digits.substringAfter('.', "").filterNot { it == '.' }
                if (allowDecimal && '.' in digits) "$sign$preDecimal.$postDecimal" else "$sign$preDecimal"
            }

        public val Decimal: TextFormat = numberFormat(allowDecimal = true)

        /** Shows a whole number with commas between each group of three digits, keeping a leading '-' and dropping everything from the first '.'. */
        public val Integer: TextFormat = numberFormat(allowDecimal = false)

        public fun number(allowDecimal: Boolean): TextFormat = if (allowDecimal) Decimal else Integer
    }
}

/** Returns a [TextFormat] that uses [isRawData] and [format]. */
public fun TextFormat(isRawData: (Char) -> Boolean, format: (clean: String) -> String): TextFormat = object : TextFormat {
    override fun isRawData(char: Char): Boolean = isRawData(char)
    override fun format(clean: String): String = format(clean)
}

/** Cleans and formats the inputted text, essentially a `map` operation. */
public fun TextFormat.reformat(text: String): String = format(text.filter(::isRawData))

/**
 * Returns [text] laid out by this format, with [selection] moved to stay beside the same data characters.
 *
 * [selection] follows the same convention as [ReformattedWithSelection.selection]. Each end of it lands right after the data
 * character it followed in [text], so it sits before any formatting that comes next.
 */
public fun TextFormat.reformatWithSelection(text: String, selection: IntRange): ReformattedWithSelection {
    val result = reformat(text)
    fun resultIndexOf(textIndex: Int): Int {
        // Claude: formats the text before the index rather than counting its data, so data the format drops
        // (like a '+' in the middle of a phone number) doesn't push the caret along.
        var remaining = format(text.substring(0, textIndex).filter(::isRawData)).count(::isRawData)
        var i = 0
        while (remaining > 0 && i < result.length) if (isRawData(result[i++])) remaining--
        return i
    }
    return ReformattedWithSelection(result, resultIndexOf(selection.first) until resultIndexOf(selection.last + 1))
}

public data class ReformattedWithSelection(val text: String, val selection: IntRange)


public fun Double.toStringNoExponential(): String {
    // Sign is handled separately since toLong() drops it for magnitudes under 1 (e.g. -0.5 -> 0)
    // and rem(1) keeps the dividend's sign, which otherwise corrupts the fractional digits below.
    val negative = this < 0.0
    val abs = abs(this)
    val preDecimal = abs.toLong().toString()
    val r = abs.rem(1)
    if (r == 0.0) return if (negative) "-$preDecimal" else preDecimal
    val availableDigits = 10 - preDecimal.length
    val postDecimal = r.times(10.0.pow(availableDigits)).roundToInt()
    val result = if (postDecimal == 0) preDecimal
    else preDecimal + "." + postDecimal.toString().padStart(availableDigits, '0').trimEnd('0')
    return if (negative) "-$result" else result
}

private fun groupThousands(digits: String): String =
    digits.reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
