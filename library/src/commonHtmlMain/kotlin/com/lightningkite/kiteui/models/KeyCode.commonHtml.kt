package com.lightningkite.kiteui.models

actual typealias KeyCode = String
actual object KeyCodes {
    actual val left: KeyCode get() = "ArrowLeft"
    actual val right: KeyCode get() = "ArrowRight"
    actual val up: KeyCode get() = "ArrowUp"
    actual val down: KeyCode get() = "ArrowDown"
    actual fun letter(char: Char): KeyCode = "Key" + char.uppercase()
    actual fun num(digit: Int): KeyCode = "Digit$digit"
    actual fun numpad(digit: Int): KeyCode = "Numpad$digit"
    actual val space: KeyCode get() = "Space"
    actual val enter: KeyCode get() = "Enter"
    actual val tab: KeyCode get() = "Tab"
    actual val escape: KeyCode get() = "Escape"
    actual val leftCtrl: KeyCode get() = "ControlLeft"
    actual val rightCtrl: KeyCode get() = "ControlRight"
    actual val leftShift: KeyCode get() = "ShiftLeft"
    actual val rightShift: KeyCode get() = "ShiftRight"
    actual val leftAlt: KeyCode get() = "AltLeft"
    actual val rightAlt: KeyCode get() = "AltRight"
    actual val equals: KeyCode get() = "Equal"
    actual val dash: KeyCode get() = "Minus"
    actual val backslash: KeyCode get() = "Backslash"
    actual val leftBrace: KeyCode get() = "BracketLeft"
    actual val rightBrace: KeyCode get() = "BracketRight"
    actual val semicolon: KeyCode get() = "Semicolon"
    actual val comma: KeyCode get() = "Comma"
    actual val period: KeyCode get() = "Period"
}