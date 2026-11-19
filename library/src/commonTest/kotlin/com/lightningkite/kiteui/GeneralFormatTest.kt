package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.TextFormat
import com.lightningkite.kiteui.models.reformat
import com.lightningkite.kiteui.models.reformatWithSelection
import com.lightningkite.kiteui.models.toStringNoExponential
import kotlin.test.Test
import kotlin.test.assertEquals

class GeneralFormatTest {
    val testCases = listOf(
        "1234567",
        "12345678",
        "123456789",
        "1234.5678",
        "1234.",
        "1,234,567",
        "12,345,678",
        "123,456,789",
        "1,234.56789",
        "1,234.",
    )

    @Test
    fun editsKeepPosition() {
        for (string in testCases) {
            for (i in 0..string.length) {
                val (post, selection) = TextFormat.Decimal.reformatWithSelection(string, i until i)
                string.getOrNull(i)?.takeIf { it == '.' || it.isDigit() }?.let {
                    // Position should be before the comma
                    var postPos = selection.first
                    if (post.getOrNull(postPos) == ',') postPos++
                    assertEquals(it, post.getOrNull(postPos), "$string at $i")
                }
            }
            for (i in string.indices) {
                val (post, selection) = TextFormat.Decimal.reformatWithSelection(string, i until i + 1)
                string[i].takeIf { it == '.' || it.isDigit() }?.let {
                    var postPos = selection.first
                    if (post.getOrNull(postPos) == ',') postPos++
                    assertEquals(it, post.getOrNull(postPos), "$string spanning $i")
                }
            }
        }
    }

    @Test
    fun insert() {
        for (string in testCases) {
            for (i in 0..string.length) {
                val (post, selection) = TextFormat.Decimal.reformatWithSelection(string.substring(0, i) + '0' + string.substring(i), i + 1 until i + 1)
                assertEquals('0', post.substring(0, selection.first).lastOrNull { it.isDigit() || it == '.' }, "$string at $i")
            }
        }
    }

    @Test
    fun commas() {
        assertEquals("-12,345.5", TextFormat.Decimal.format("-12345.5"))
        assertEquals("1,234", TextFormat.Integer.format("1.234"))
        // A '-' must not be grouped with the digits, e.g. "-100" -> "-,100", when the digit count is a multiple of 3.
        assertEquals("-100", TextFormat.Decimal.format("-100"))
        assertEquals("-123,456,789", TextFormat.Integer.format("-123456789"))
        assertEquals("-1,234.56", TextFormat.Decimal.reformat((-1234.56).toStringNoExponential()))
        assertEquals("1,000,000", TextFormat.Decimal.reformat(1_000_000.0.toStringNoExponential()))
    }

    @Test
    fun decimalFormat() {
        assertEquals("5.99", 5.99.toStringNoExponential())
        assertEquals("1224.54", 1224.54.toStringNoExponential())
        assertEquals("1224.542", 1224.542.toStringNoExponential())
        assertEquals("1224.5428", 1224.5428.toStringNoExponential())
        assertEquals("1224.54287", 1224.54287.toStringNoExponential())
        assertEquals("1224.542871", 1224.5428713.toStringNoExponential())
        assertEquals("1224", 1224.0.toStringNoExponential())
        assertEquals("112348224342", 112348224342.0.toStringNoExponential())
        assertEquals("0", 0.0.toStringNoExponential())
        // Claude: toLong() drops the sign under 1 and rem(1) keeps it, which once gave "0.-5".
        assertEquals("-0.5", (-0.5).toStringNoExponential())
        assertEquals("-5", (-5.0).toStringNoExponential())
        assertEquals("-1224.542871", (-1224.5428713).toStringNoExponential())
    }
}
