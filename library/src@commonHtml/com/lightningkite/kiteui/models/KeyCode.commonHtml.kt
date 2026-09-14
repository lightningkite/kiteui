package com.lightningkite.kiteui.models

public actual typealias KeyCode = String
public actual object KeyCodes {
    public actual val left: KeyCode get() = "ArrowLeft"
    public actual val right: KeyCode get() = "ArrowRight"
    public actual val up: KeyCode get() = "ArrowUp"
    public actual val down: KeyCode get() = "ArrowDown"
    public actual fun letter(char: Char): KeyCode = "Key" + char.uppercase()
    public actual fun num(digit: Int): KeyCode = "Digit$digit"
    public actual fun numpad(digit: Int): KeyCode = "Numpad$digit"
    public actual val space: KeyCode get() = "Space"
    public actual val enter: KeyCode get() = "Enter"
    public actual val tab: KeyCode get() = "Tab"
    public actual val escape: KeyCode get() = "Escape"
    public actual val leftCtrl: KeyCode get() = "ControlLeft"
    public actual val rightCtrl: KeyCode get() = "ControlRight"
    public actual val leftShift: KeyCode get() = "ShiftLeft"
    public actual val rightShift: KeyCode get() = "ShiftRight"
    public actual val leftAlt: KeyCode get() = "AltLeft"
    public actual val rightAlt: KeyCode get() = "AltRight"
    public actual val equals: KeyCode get() = "Equal"
    public actual val dash: KeyCode get() = "Minus"
    public actual val backslash: KeyCode get() = "Backslash"
    public actual val leftBrace: KeyCode get() = "BracketLeft"
    public actual val rightBrace: KeyCode get() = "BracketRight"
    public actual val semicolon: KeyCode get() = "Semicolon"
    public actual val comma: KeyCode get() = "Comma"
    public actual val period: KeyCode get() = "Period"
}