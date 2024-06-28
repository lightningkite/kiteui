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
    val adjust =
        { it: Int -> if (it == 0) newDecimal else if (it > 0) newDecimal + it else newDecimal + it + ((it + 1) / 3) }
    setResult(result)
    if (s != null && e != null) {
        setSelectionRange(adjust(s), adjust(e))
    }
}

inline fun Double.toStringNoExponential(): String {
    val preDecimal = toLong().toString()
    val r = rem(1)
    if(r == 0.0) return preDecimal
    val availableDigits = 10 - preDecimal.length
    val postDecimal = r.times(10.0.pow(availableDigits)).roundToInt()
    if(postDecimal == 0) return preDecimal
    else return preDecimal + "." + postDecimal.toString().padStart(availableDigits, '0').trimEnd('0')
}

fun Double.commaString(): String {
    val clean = this.toStringNoExponential().filter { it.isDigit() || it == '.' }
    val preDecimal = clean.substringBefore('.').reversed().chunked(3) { it.reversed() }.reversed().joinToString(",")
    val postDecimal = clean.substringAfter('.', "")
    return if (clean.contains('.')) "$preDecimal.$postDecimal" else preDecimal
}