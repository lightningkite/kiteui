package com.lightningkite.kiteui.models

import android.view.KeyEvent

actual typealias KeyCode = Int
actual object KeyCodes {
    actual val left: KeyCode get() = KeyEvent.KEYCODE_DPAD_LEFT
    actual val right: KeyCode get() = KeyEvent.KEYCODE_DPAD_RIGHT
    actual val up: KeyCode get() = KeyEvent.KEYCODE_DPAD_UP
    actual val down: KeyCode get() = KeyEvent.KEYCODE_DPAD_DOWN
    actual fun letter(char: Char): KeyCode = KeyEvent.KEYCODE_A + char.code
    actual fun num(digit: Int): KeyCode = KeyEvent.KEYCODE_0 + digit
    actual fun numpad(digit: Int): KeyCode = KeyEvent.KEYCODE_NUMPAD_0 + digit
    actual val space: KeyCode get() = KeyEvent.KEYCODE_SPACE
    actual val enter: KeyCode get() = KeyEvent.KEYCODE_ENTER
    actual val tab: KeyCode get() = KeyEvent.KEYCODE_TAB
    actual val escape: KeyCode get() = KeyEvent.KEYCODE_ESCAPE
    actual val leftCtrl: KeyCode get() = KeyEvent.KEYCODE_CTRL_LEFT
    actual val rightCtrl: KeyCode get() = KeyEvent.KEYCODE_CTRL_RIGHT
    actual val leftShift: KeyCode get() = KeyEvent.KEYCODE_SHIFT_LEFT
    actual val rightShift: KeyCode get() = KeyEvent.KEYCODE_SHIFT_RIGHT
    actual val leftAlt: KeyCode get() = KeyEvent.KEYCODE_ALT_LEFT
    actual val rightAlt: KeyCode get() = KeyEvent.KEYCODE_ALT_RIGHT
    actual val equals: KeyCode get() = KeyEvent.KEYCODE_EQUALS
    actual val dash: KeyCode get() = KeyEvent.KEYCODE_MINUS
    actual val backslash: KeyCode get() = KeyEvent.KEYCODE_BACKSLASH
    actual val leftBrace: KeyCode get() = KeyEvent.KEYCODE_LEFT_BRACKET
    actual val rightBrace: KeyCode get() = KeyEvent.KEYCODE_RIGHT_BRACKET
    actual val semicolon: KeyCode get() = KeyEvent.KEYCODE_SEMICOLON
    actual val comma: KeyCode get() = KeyEvent.KEYCODE_COMMA
    actual val period: KeyCode get() = KeyEvent.KEYCODE_PERIOD
}