package com.lightningkite.kiteui

internal fun String.splitParens(
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
internal fun String.afterParens(
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

/**
 * Checks if a position in the string is inside a string literal.
 * Handles both regular strings ("...") and triple-quoted strings ("""...""").
 */
internal fun String.isInsideStringLiteral(position: Int): Boolean {
    var i = 0
    var inString = false
    var stringDelimiter = '"'
    var isTripleQuoted = false
    var escapeNext = false

    while (i < position && i < length) {
        if (!inString) {
            // Check for start of triple-quoted string
            if (i + 2 < length && this[i] == '"' && this[i + 1] == '"' && this[i + 2] == '"') {
                inString = true
                stringDelimiter = '"'
                isTripleQuoted = true
                i += 3
                continue
            }
            // Check for start of regular string
            else if (this[i] == '"' || this[i] == '\'') {
                inString = true
                stringDelimiter = this[i]
                isTripleQuoted = false
                escapeNext = false
                i++
                continue
            }
        } else {
            // Inside a string
            if (isTripleQuoted) {
                // Check for end of triple-quoted string
                if (i + 2 < length && this[i] == '"' && this[i + 1] == '"' && this[i + 2] == '"') {
                    inString = false
                    isTripleQuoted = false
                    i += 3
                    continue
                }
            } else {
                // Regular string handling
                if (escapeNext) {
                    escapeNext = false
                } else if (this[i] == '\\') {
                    escapeNext = true
                } else if (this[i] == stringDelimiter) {
                    inString = false
                    isTripleQuoted = false
                }
            }
        }
        i++
    }

    return inString
}