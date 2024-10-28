package com.lightningkite.kiteui

fun String.splitParens(
    delimiter: Char = ',',
    start: Char = '(',
    end: Char = ')',
    startingAt: Int = 0,
): List<String> {
    val result = mutableListOf<String>()
    var index = startingAt
    if(this[index] != start) throw IllegalArgumentException()
    var depth = 1
    val section = StringBuilder()
    while(++index < length) {
        val current = this[index]
        when(current) {
            start -> {
                depth++
                section.append(current)
            }
            end -> {
                if(--depth == 0) {
                    result += section.toString()
                    section.clear()
                    break
                }
                else section.append(current)
            }
            delimiter -> {
                if(depth == 1) {
                    result += section.toString()
                    section.clear()
                } else section.append(current)
            }
            else -> section.append(current)
        }
    }
    return result
}
fun String.afterParens(
    start: Char = '(',
    end: Char = ')',
    startingAt: Int = 0,
): Int {
    var index = startingAt
    if(this[index] != start) throw IllegalArgumentException()
    var depth = 1
    while(++index < length) {
        val current = this[index]
        when(current) {
            start -> {
                depth++
            }
            end -> {
                if(--depth == 0) {
                    break
                }
            }
            else -> {}
        }
    }
    return index + 1
}