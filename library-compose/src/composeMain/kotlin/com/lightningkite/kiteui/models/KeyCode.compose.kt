package com.lightningkite.kiteui.models

import androidx.compose.ui.input.key.Key

// Compose's Key is a value class, which can't actualize an expect class, so a KeyCode is its Key.keyCode.
public actual typealias KeyCode = Long
public actual object KeyCodes {
    public actual val left: KeyCode get() = Key.DirectionLeft.keyCode
    public actual val right: KeyCode get() = Key.DirectionRight.keyCode
    public actual val up: KeyCode get() = Key.DirectionUp.keyCode
    public actual val down: KeyCode get() = Key.DirectionDown.keyCode
    public actual fun letter(char: Char): KeyCode = letters[char.uppercaseChar() - 'A'].keyCode
    public actual fun num(digit: Int): KeyCode = digits[digit].keyCode
    public actual fun numpad(digit: Int): KeyCode = numpadDigits[digit].keyCode
    public actual val space: KeyCode get() = Key.Spacebar.keyCode
    public actual val enter: KeyCode get() = Key.Enter.keyCode
    public actual val tab: KeyCode get() = Key.Tab.keyCode
    public actual val escape: KeyCode get() = Key.Escape.keyCode
    public actual val leftCtrl: KeyCode get() = Key.CtrlLeft.keyCode
    public actual val rightCtrl: KeyCode get() = Key.CtrlRight.keyCode
    public actual val leftShift: KeyCode get() = Key.ShiftLeft.keyCode
    public actual val rightShift: KeyCode get() = Key.ShiftRight.keyCode
    public actual val leftAlt: KeyCode get() = Key.AltLeft.keyCode
    public actual val rightAlt: KeyCode get() = Key.AltRight.keyCode
    public actual val equals: KeyCode get() = Key.Equals.keyCode
    public actual val dash: KeyCode get() = Key.Minus.keyCode
    public actual val backslash: KeyCode get() = Key.Backslash.keyCode
    public actual val leftBrace: KeyCode get() = Key.LeftBracket.keyCode
    public actual val rightBrace: KeyCode get() = Key.RightBracket.keyCode
    public actual val semicolon: KeyCode get() = Key.Semicolon.keyCode
    public actual val comma: KeyCode get() = Key.Comma.keyCode
    public actual val period: KeyCode get() = Key.Period.keyCode

    private val letters = listOf(
        Key.A, Key.B, Key.C, Key.D, Key.E, Key.F, Key.G, Key.H, Key.I, Key.J, Key.K, Key.L, Key.M,
        Key.N, Key.O, Key.P, Key.Q, Key.R, Key.S, Key.T, Key.U, Key.V, Key.W, Key.X, Key.Y, Key.Z,
    )
    private val digits = listOf(Key.Zero, Key.One, Key.Two, Key.Three, Key.Four, Key.Five, Key.Six, Key.Seven, Key.Eight, Key.Nine)
    private val numpadDigits = listOf(
        Key.NumPad0, Key.NumPad1, Key.NumPad2, Key.NumPad3, Key.NumPad4, Key.NumPad5, Key.NumPad6, Key.NumPad7, Key.NumPad8, Key.NumPad9,
    )
}
