package com.lightningkite.kiteui.models

actual typealias KeyCode = Int

actual object KeyCodes {
    actual val left: KeyCode = java.awt.event.KeyEvent.VK_LEFT
    actual val right: KeyCode = java.awt.event.KeyEvent.VK_RIGHT
    actual val up: KeyCode = java.awt.event.KeyEvent.VK_UP
    actual val down: KeyCode = java.awt.event.KeyEvent.VK_DOWN
    actual fun letter(char: Char): KeyCode = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(char.code)
    actual fun num(digit: Int): KeyCode = java.awt.event.KeyEvent.VK_0 + digit
    actual fun numpad(digit: Int): KeyCode = java.awt.event.KeyEvent.VK_NUMPAD0 + digit
    actual val space: KeyCode = java.awt.event.KeyEvent.VK_SPACE
    actual val enter: KeyCode = java.awt.event.KeyEvent.VK_ENTER
    actual val tab: KeyCode = java.awt.event.KeyEvent.VK_TAB
    actual val escape: KeyCode = java.awt.event.KeyEvent.VK_ESCAPE
    actual val leftCtrl: KeyCode = java.awt.event.KeyEvent.VK_CONTROL
    actual val rightCtrl: KeyCode = java.awt.event.KeyEvent.VK_CONTROL
    actual val leftShift: KeyCode = java.awt.event.KeyEvent.VK_SHIFT
    actual val rightShift: KeyCode = java.awt.event.KeyEvent.VK_SHIFT
    actual val leftAlt: KeyCode = java.awt.event.KeyEvent.VK_ALT
    actual val rightAlt: KeyCode = java.awt.event.KeyEvent.VK_ALT
    actual val equals: KeyCode = java.awt.event.KeyEvent.VK_EQUALS
    actual val dash: KeyCode = java.awt.event.KeyEvent.VK_MINUS
    actual val backslash: KeyCode = java.awt.event.KeyEvent.VK_BACK_SLASH
    actual val leftBrace: KeyCode = java.awt.event.KeyEvent.VK_BRACELEFT
    actual val rightBrace: KeyCode = java.awt.event.KeyEvent.VK_BRACERIGHT
    actual val semicolon: KeyCode = java.awt.event.KeyEvent.VK_SEMICOLON
    actual val comma: KeyCode = java.awt.event.KeyEvent.VK_COMMA
    actual val period: KeyCode = java.awt.event.KeyEvent.VK_PERIOD
}
