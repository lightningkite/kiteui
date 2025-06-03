package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.objc.UIGestureRecognizerCustomPProtocol

actual typealias KeyCode = String

actual object KeyCodes {
    actual val left: KeyCode get() = UIKeyInputLeftArrow
    actual val right: KeyCode get() = UIKeyInputRightArrow
    actual val up: KeyCode get() = UIKeyInputUpArrow
    actual val down: KeyCode get() = UIKeyInputDownArrow
    actual fun letter(char: Char): KeyCode = char.lowercase()
    actual fun num(digit: Int): KeyCode = digit.toString()
    actual fun numpad(digit: Int): KeyCode = digit.toString()
    actual val space: KeyCode get() = " "
    actual val enter: KeyCode get() = "\n"
    actual val tab: KeyCode get() = "\t"
    actual val escape: KeyCode get() = UIKeyInputEscape
    actual val leftCtrl: KeyCode get() = ""
    actual val rightCtrl: KeyCode get() = ""
    actual val leftShift: KeyCode get() = ""
    actual val rightShift: KeyCode get() = ""
    actual val leftAlt: KeyCode get() = ""
    actual val rightAlt: KeyCode get() = ""
    actual val equals: KeyCode get() = "="
    actual val dash: KeyCode get() = "-"
    actual val backslash: KeyCode get() = "\\"
    actual val leftBrace: KeyCode get() = "["
    actual val rightBrace: KeyCode get() = "]"
    actual val semicolon: KeyCode get() = ";"
    actual val comma: KeyCode get() = ","
    actual val period: KeyCode get() = "."
}