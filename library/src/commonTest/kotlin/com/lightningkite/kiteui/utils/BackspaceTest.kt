package com.lightningkite.kiteui.utils

import kotlin.test.Test
import kotlin.test.assertEquals

// Covers the negative-number sign handling fixed in 94f1ebe6b. Before that fix,
// toStringNoExponential() and the commaString() family scrambled the '-' sign
// instead of preserving it; see the reasoning comments on each function.
class BackspaceTest {

    // Pre-fix: toLong() truncates -0.5 to 0, discarding the sign entirely, so the
    // fractional part below was computed from a negative remainder against a
    // "positive" whole part and produced "0.-5" instead of "-0.5".
    @Test
    fun negativeFractionSmallerThanOneKeepsItsSign() {
        assertEquals("-0.5", (-0.5).toStringNoExponential())
    }

    // Pre-fix: rem(1) on a negative dividend returns a negative remainder, which fed
    // straight into postDecimal and produced "-1224.-542871" instead of the sign
    // being applied once to the whole formatted string.
    @Test
    fun negativeNumberWithFractionalPartFormatsCorrectly() {
        assertEquals("-1224.542871", (-1224.5428713).toStringNoExponential())
    }

    // A whole negative number happens to survive toLong() with its sign intact, so
    // this isn't a regression case, but it locks in the guarantee going forward.
    @Test
    fun negativeWholeNumberFormatsCorrectly() {
        assertEquals("-5", (-5.0).toStringNoExponential())
    }

    // Pre-fix: reversing "-100" before chunking into groups of 3 put the '-' in the
    // same group as the last digit, e.g. "-100" -> "-,100" (see fix's own comment).
    // This only shows up when the digit count is a multiple of 3.
    @Test
    fun intCommaStringDoesNotMergeMinusSignIntoFirstGroup() {
        assertEquals("-100", (-100).commaString())
        assertEquals("-123,456", (-123456).commaString())
    }

    @Test
    fun longCommaStringDoesNotMergeMinusSignIntoFirstGroup() {
        assertEquals("-123,456,789", (-123456789L).commaString())
    }

    @Test
    fun doubleCommaStringDoesNotMergeMinusSignIntoFirstGroup() {
        assertEquals("-100", (-100.0).commaString())
    }

    // Exercises the full pipeline: toStringNoExponential's sign fix feeding into
    // commaString's own sign-splitting fix.
    @Test
    fun doubleCommaStringFormatsNegativeDecimalsCorrectly() {
        assertEquals("-1,234.56", (-1234.56).commaString())
    }

    // Sanity checks that the refactor didn't disturb the unsigned case.
    @Test
    fun positiveCommaStringIsUnaffectedByTheSignFix() {
        assertEquals("1,234", 1234.commaString())
        assertEquals("1,234", 1234L.commaString())
        assertEquals("1,234.56", 1234.56.commaString())
    }

    @Test
    fun zeroFormatsWithoutASignForEitherFunction() {
        assertEquals("0", 0.0.toStringNoExponential())
        assertEquals("0", 0.commaString())
    }
}
