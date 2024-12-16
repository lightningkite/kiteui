package com.lightningkite.kiteui.utils

import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

inline fun numberAutocommaBackspace(
    dirty: String,
    selectionStart: Int,
    selectionEnd: Int = selectionStart,
    setResult: (String) -> Unit,
    setSelectionRange: (Int, Int) -> Unit
) {
    val clean = dirty.filter { it.isDigit() || it in setOf('.', '-') }
    var s = selectionStart.minus(dirty.substring(0, selectionStart).count { !it.isDigit() && it !in setOf('.', '-') })
    var e = selectionEnd.minus(dirty.substring(0, selectionEnd).count { !it.isDigit() && it !in setOf('.', '-') })
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
    val clean = dirty.filter { it.isDigit() || it in setOf('.', '-') }
    var s = selectionStart.minus(dirty.substring(0, selectionStart).count { !it.isDigit() && it !in setOf('.', '-') })
    var e = selectionEnd.minus(dirty.substring(0, selectionEnd).count { !it.isDigit() && it !in setOf('.', '-') })
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
) = repairFormatAndPosition(
    dirty = dirty,
    selectionStart = selectionStart,
    selectionEnd = selectionEnd,
    setResult = setResult,
    setSelectionRange = setSelectionRange,
    isRawData = { it.isDigit() || it in setOf('.', '-') },
    formatter = { clean: String ->
        val negative = clean.firstOrNull()?.equals('-') ?: false
        val decimal = clean.contains('.')
        val value = clean.filterNot { it == '-' }
        val preDecimal = value.substringBefore('.').reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
        val postDecimal = value.substringAfter('.', "").filterNot { it == '.' }
        when {
            negative and decimal -> "-$preDecimal.$postDecimal"
            negative -> "-$preDecimal"
            decimal -> "$preDecimal.$postDecimal"
            else -> preDecimal
        }
    }
)
//{
//    val startString = dirty
//    // Welcome to formatting HELL
//    val clean = startString.filter { it.isDigit() || it in setOf('.', '-') }
//    var decimal = clean.indexOf('.')
//    if (decimal == -1) decimal = clean.length
//    val s = selectionStart?.minus(dirty.substring(0, selectionStart).count { !it.isDigit() && it !in setOf('.', '-') })
//        ?.let { it - decimal }
//    val e =
//        selectionEnd?.minus(dirty.substring(0, selectionEnd).count { !it.isDigit() && it !in setOf('.', '-') })?.let { it - decimal }
//    val preDecimal = clean.substringBefore('.').reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
//    val postDecimal = clean.substringAfter('.', "")
//    val result = if (clean.contains('.')) "$preDecimal.$postDecimal" else preDecimal
//    val newDecimal = preDecimal.length
//    val adjust = { it: Int ->
//        val first = if (it == 0) newDecimal
//        else if (it > 0) newDecimal + it
//        else newDecimal + it + ((it + 1) / 3)
//        if(first > 0 && result[first-1] == ',') first - 1
//        else first
//    }
//    setResult(result)
//    if (s != null && e != null) {
//        setSelectionRange(adjust(s), adjust(e))
//    }
//}

//inline fun numberAutocommaRepair(
//    dirty: String,
//    selectionStart: Int? = null,
//    selectionEnd: Int? = selectionStart,
//    setResult: (String) -> Unit,
//    setSelectionRange: (Int, Int) -> Unit
//) = repairFormatAndPosition(
//    dirty,
//    selectionStart,
//    selectionEnd,
//    setResult,
//    setSelectionRange,
//    isRawData = { it.isDigit() || it == '.' },
//    formatter = { clean ->
//        val preDecimal = clean.substringBefore('.').reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
//        val postDecimal = clean.substringAfter('.', "")
//        if (clean.contains('.')) "$preDecimal.$postDecimal" else preDecimal
//    }
//)

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
    val clean = this.toStringNoExponential().filter { it.isDigit() || it in setOf('.', '-') }
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


inline fun StringBuilder.keyValue(key: String, value: Any?) = appendLine("$key: $value")

inline fun repairFormatAndPosition(
    dirty: String,
    selectionStart: Int? = null,
    selectionEnd: Int? = selectionStart,
    setResult: (String) -> Unit,
    setSelectionRange: (Int, Int) -> Unit,
    isRawData: (Char) -> Boolean,
    formatter: (clean: String) -> String,
) {
    // If you need to read and understand this, good luck.
    // This works by comparison of selection indexes relative to the number of 'data characters' in the dirty string

    val clean = dirty.filter(isRawData)
    val result = formatter(clean)

    // Finding the number of formatting characters in the selection range, and removing them from the selection index, resulting in the number of data characters in selection
    val startPosOnClean = selectionStart?.minus(dirty.substring(0, selectionStart).count { !isRawData(it) })
    val endPosOnClean = selectionEnd?.minus(dirty.substring(0, selectionEnd).count { !isRawData(it) })

    // With the formatted string, we loop through each character, counting each character, until we've encountered the number of data characters calculated in the previous step
    val startPosOnResult = startPosOnClean?.let {
        var remaining = it
        var count = 0
        while (remaining > 0) {
            if (count >= result.length) break
            if (isRawData(result[count++])) remaining -= 1
        }
        count
    }
    val endPosOnResult = endPosOnClean?.let {
        var remaining = it
        var count = 0
        while (remaining > 0) {
            if (count >= result.length) break
            if (isRawData(result[count++])) remaining -= 1
        }
        count
    }

    setResult(result)
    if (startPosOnResult != null && endPosOnResult != null) {
        setSelectionRange(startPosOnResult, endPosOnResult)
    }

//    println(
//        buildString {
//            append('\n')
//            appendLine("repairFormatAndPosition:")
//
//            keyValue("dirty", dirty)
//            keyValue("clean", clean)
//            keyValue("result", result)
//
//            keyValue("startPos", selectionStart)
//            keyValue("endPos", selectionEnd)
//            keyValue("startPosOnClean", startPosOnClean)
//            keyValue("endPosOnClean", endPosOnClean)
//            keyValue("startPosOnResult", startPosOnResult)
//            keyValue("endPosOnResult", endPosOnResult)
//            append('\n')
//        }
//    )
}