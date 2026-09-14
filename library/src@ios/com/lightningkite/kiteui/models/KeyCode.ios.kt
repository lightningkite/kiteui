package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.objc.UIGestureRecognizerCustomPProtocol
import platform.UIKit.*

public actual typealias KeyCode = String

public actual object KeyCodes {
    public actual val left: KeyCode get() = UIKeyInputLeftArrow
    public actual val right: KeyCode get() = UIKeyInputRightArrow
    public actual val up: KeyCode get() = UIKeyInputUpArrow
    public actual val down: KeyCode get() = UIKeyInputDownArrow
    public actual fun letter(char: Char): KeyCode = char.lowercase()
    public actual fun num(digit: Int): KeyCode = digit.toString()
    public actual fun numpad(digit: Int): KeyCode = digit.toString()
    public actual val space: KeyCode get() = " "
    public actual val enter: KeyCode get() = "\n"
    public actual val tab: KeyCode get() = "\t"
    public actual val escape: KeyCode get() = UIKeyInputEscape
    public actual val leftCtrl: KeyCode get() = ""
    public actual val rightCtrl: KeyCode get() = ""
    public actual val leftShift: KeyCode get() = ""
    public actual val rightShift: KeyCode get() = ""
    public actual val leftAlt: KeyCode get() = ""
    public actual val rightAlt: KeyCode get() = ""
    public actual val equals: KeyCode get() = "="
    public actual val dash: KeyCode get() = "-"
    public actual val backslash: KeyCode get() = "\\"
    public actual val leftBrace: KeyCode get() = "["
    public actual val rightBrace: KeyCode get() = "]"
    public actual val semicolon: KeyCode get() = ";"
    public actual val comma: KeyCode get() = ","
    public actual val period: KeyCode get() = "."
}