package com.lightningkite.kiteui.utils

import kotlin.math.pow
import kotlin.math.roundToInt

inline fun numberAutocommaBackspace(
    dirty: String,
    selectionStart: Int,
    selectionEnd: Int = selectionStart,
    setResult: (String) -> Unit,
    setSelectionRange: (Int, Int) -> Unit
) {
    val clean = dirty.filter { it.isDigit() || it == '.' }
    var s = selectionStart.minus(dirty.substring(0, selectionStart).count { !it.isDigit() && it != '.' })
    var e = selectionEnd.minus(dirty.substring(0, selectionEnd).count { !it.isDigit() && it != '.' })
    if (s == e) s -= 1
    if (s >= 0) {
        numberAutocommaRepair(
            clean.removeRange(s, e),
            s,
            setResult = setResult,
            setSelectionRange = setSelectionRange
        )
    } else {
    }
}

inline fun numberAutocommaDelete(
    dirty: String,
    selectionStart: Int,
    selectionEnd: Int = selectionStart,
    setResult: (String) -> Unit,
    setSelectionRange: (Int, Int) -> Unit
) {
    val clean = dirty.filter { it.isDigit() || it == '.' }
    var s = selectionStart.minus(dirty.substring(0, selectionStart).count { !it.isDigit() && it != '.' })
    var e = selectionEnd.minus(dirty.substring(0, selectionEnd).count { !it.isDigit() && it != '.' })
    if (s == e) e += 1
    if (e <= clean.length) {
        numberAutocommaRepair(
            clean.removeRange(s, e),
            s,
            setResult = setResult,
            setSelectionRange = setSelectionRange
        )
    } else {
    }
}

inline fun numberAutocommaRepair(
    dirty: String,
    selectionStart: Int? = null,
    selectionEnd: Int? = selectionStart,
    setResult: (String) -> Unit,
    setSelectionRange: (Int, Int) -> Unit
) {
    val startString = dirty
    // Welcome to formatting HELL
    val clean = startString.filter { it.isDigit() || it == '.' }
    var decimal = clean.indexOf('.')
    if (decimal == -1) decimal = clean.length
    val s = selectionStart?.minus(dirty.substring(0, selectionStart).count { !it.isDigit() && it != '.' })
        ?.let { it - decimal }
    val e =
        selectionEnd?.minus(dirty.substring(0, selectionEnd).count { !it.isDigit() && it != '.' })?.let { it - decimal }
    val preDecimal = clean.substringBefore('.').reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
    val postDecimal = clean.substringAfter('.', "")
    val result = if (clean.contains('.')) "$preDecimal.$postDecimal" else preDecimal
    val newDecimal = preDecimal.length
    val adjust = { it: Int ->
        val first = if (it == 0) newDecimal
        else if (it > 0) newDecimal + it
        else newDecimal + it + ((it + 1) / 3)
        if(first > 0 && result[first-1] == ',') first - 1
        else first
    }
    setResult(result)
    if (s != null && e != null) {
        setSelectionRange(adjust(s), adjust(e))
    }
}

fun Double.toStringNoExponential(): String {
    val preDecimal = toLong().toString()
    val r = rem(1)
    if (r == 0.0) return preDecimal
    val availableDigits = 10 - preDecimal.length
    val postDecimal = r.times(10.0.pow(availableDigits)).roundToInt()
    if (postDecimal == 0) return preDecimal
    else return preDecimal + "." + postDecimal.toString().padStart(availableDigits, '0').trimEnd('0')
}

fun Double.commaString(): String {
    val clean = this.toStringNoExponential().filter { it.isDigit() || it == '.' }
    val preDecimal = clean.substringBefore('.').reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
    val postDecimal = clean.substringAfter('.', "")
    return if (clean.contains('.')) "$preDecimal.$postDecimal" else preDecimal
}
fun Int.commaString(): String {
    return toString().substringBefore('.').reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
}
fun Long.commaString(): String {
    return toString().substringBefore('.').reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
}


// Phone-number formatting
inline fun CharSequence.substringOrNull(startIndex: Int, endIndex: Int): String? {
    if (startIndex !in indices) return null
    if (endIndex > length) return substring(startIndex, length)

    return substring(startIndex, endIndex)
}

inline fun String.formatUSPhoneNumber(): String {
    val clean = filter { it.isDigit() }
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

inline fun USPhoneNumberRepair(
    dirty: String,
    selectionStart: Int? = null,
    selectionEnd: Int? = selectionStart,
    setResult: (String) -> Unit,
    setSelectionRange: (Int, Int) -> Unit
) {
    // Welcome to formatting hell-V2!
    val clean = dirty.filter { it.isDigit() }

    val startPosOnClean = selectionStart?.minus(dirty.substring(0, selectionStart).count { !it.isDigit() })
    val endPosOnClean = selectionEnd?.minus(dirty.substring(0, selectionEnd).count { !it.isDigit() })

    val result = clean.formatUSPhoneNumber()

    val resultUpToStart = startPosOnClean?.let { pos ->
        clean.substring(0, pos).formatUSPhoneNumber().dropLastWhile { !it.isDigit() }
    }
    val resultUpToEnd = endPosOnClean?.let { pos ->
        clean.substring(0, pos).formatUSPhoneNumber().dropLastWhile { !it.isDigit() }
    }
    setResult(result)
    if (resultUpToStart != null && resultUpToEnd != null) {
        setSelectionRange(resultUpToStart.length, resultUpToEnd.length)
    }
}

inline fun autoRepairFormatAndPosition(
    dirty: String,
    selectionStart: Int? = null,
    selectionEnd: Int? = selectionStart,
    setResult: (String) -> Unit,
    setSelectionRange: (Int, Int) -> Unit,
    isRawData: (Char) -> Boolean,
    formatter: (clean: String) -> String,
) {
    val clean = dirty.filter(isRawData)

    val startPosOnClean = selectionStart?.minus(dirty.substring(0, selectionStart).count { !isRawData(it) })
    val endPosOnClean = selectionEnd?.minus(dirty.substring(0, selectionEnd).count { !isRawData(it) })

    val result = formatter(clean)

    val resultUpToStart = startPosOnClean?.let { pos ->
        formatter(clean.substring(0, pos)).dropLastWhile { !isRawData(it) }
    }
    val resultUpToEnd = endPosOnClean?.let { pos ->
        formatter(clean.substring(0, pos)).dropLastWhile { !isRawData(it) }
    }
    setResult(result)
    if (resultUpToStart != null && resultUpToEnd != null) {
        setSelectionRange(resultUpToStart.length, resultUpToEnd.length)
    }
}