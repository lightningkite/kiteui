package com.lightningkite.kiteui.models

import android.view.KeyEvent
import com.lightningkite.kiteui.InternalKiteUi

public actual typealias KeyCode = Int
@InternalKiteUi
public actual object KeyCodes {
    public actual val left: KeyCode get() = KeyEvent.KEYCODE_DPAD_LEFT
    public actual val right: KeyCode get() = KeyEvent.KEYCODE_DPAD_RIGHT
    public actual val up: KeyCode get() = KeyEvent.KEYCODE_DPAD_UP
    public actual val down: KeyCode get() = KeyEvent.KEYCODE_DPAD_DOWN
    public actual fun letter(char: Char): KeyCode = KeyEvent.KEYCODE_A + char.code
    public actual fun num(digit: Int): KeyCode = KeyEvent.KEYCODE_0 + digit
    public actual fun numpad(digit: Int): KeyCode = KeyEvent.KEYCODE_NUMPAD_0 + digit
    public actual val space: KeyCode get() = KeyEvent.KEYCODE_SPACE
    public actual val enter: KeyCode get() = KeyEvent.KEYCODE_ENTER
    public actual val tab: KeyCode get() = KeyEvent.KEYCODE_TAB
    public actual val escape: KeyCode get() = KeyEvent.KEYCODE_ESCAPE
    public actual val leftCtrl: KeyCode get() = KeyEvent.KEYCODE_CTRL_LEFT
    public actual val rightCtrl: KeyCode get() = KeyEvent.KEYCODE_CTRL_RIGHT
    public actual val leftShift: KeyCode get() = KeyEvent.KEYCODE_SHIFT_LEFT
    public actual val rightShift: KeyCode get() = KeyEvent.KEYCODE_SHIFT_RIGHT
    public actual val leftAlt: KeyCode get() = KeyEvent.KEYCODE_ALT_LEFT
    public actual val rightAlt: KeyCode get() = KeyEvent.KEYCODE_ALT_RIGHT
    public actual val equals: KeyCode get() = KeyEvent.KEYCODE_EQUALS
    public actual val dash: KeyCode get() = KeyEvent.KEYCODE_MINUS
    public actual val backslash: KeyCode get() = KeyEvent.KEYCODE_BACKSLASH
    public actual val leftBrace: KeyCode get() = KeyEvent.KEYCODE_LEFT_BRACKET
    public actual val rightBrace: KeyCode get() = KeyEvent.KEYCODE_RIGHT_BRACKET
    public actual val semicolon: KeyCode get() = KeyEvent.KEYCODE_SEMICOLON
    public actual val comma: KeyCode get() = KeyEvent.KEYCODE_COMMA
    public actual val period: KeyCode get() = KeyEvent.KEYCODE_PERIOD
}