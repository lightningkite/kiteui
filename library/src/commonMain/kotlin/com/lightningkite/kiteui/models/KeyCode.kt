package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


public data class KeyCodeWithModifiers(val code: KeyCode, val alt: Boolean = false, val ctrl: Boolean = false, val shift: Boolean = false, val meta: Boolean = false)

public expect class KeyCode
public expect object KeyCodes {
    public val left: KeyCode
    public val right: KeyCode
    public val up: KeyCode
    public val down: KeyCode
    public fun letter(char: Char): KeyCode
    public fun num(digit: Int): KeyCode
    public fun numpad(digit: Int): KeyCode
    public val space: KeyCode
    public val enter: KeyCode
    public val tab: KeyCode
    public val escape: KeyCode
    public val leftCtrl: KeyCode
    public val rightCtrl: KeyCode
    public val leftShift: KeyCode
    public val rightShift: KeyCode
    public val leftAlt: KeyCode
    public val rightAlt: KeyCode
    public val equals: KeyCode
    public val dash: KeyCode
    public val backslash: KeyCode
    public val leftBrace: KeyCode
    public val rightBrace: KeyCode
    public val semicolon: KeyCode
    public val comma: KeyCode
    public val period: KeyCode
}

public object KeyCodeBuilder {

    public class Modifier(public val applyOn: (KeyCodeWithModifiers) -> KeyCodeWithModifiers) {
        public operator fun invoke(on: KeyCodeWithModifiers): KeyCodeWithModifiers = applyOn(on)
        public operator fun plus(code: KeyCode): KeyCodeWithModifiers = invoke(KeyCodeWithModifiers(code))
    }

    public val Ctrl: Modifier = Modifier { it.copy(ctrl = true) }
    public val Alt: Modifier = Modifier { it.copy(alt = true) }
    public val Shift: Modifier = Modifier { it.copy(shift = true) }
    public val Meta: Modifier = Modifier { it.copy(meta = true) }

    // This is needed to prevent the Set<Modifier>.plus(element) overload from shadowing desired behavior
    public class Modifiers(public val elements: MutableSet<Modifier>) {
        public operator fun plus(modifier: Modifier): Modifiers = apply { elements.add(modifier) }
        public operator fun invoke(keyCode: KeyCodeWithModifiers): KeyCodeWithModifiers = elements.fold(keyCode) { code, modifier -> modifier(code) }
        public operator fun plus(code: KeyCode): KeyCodeWithModifiers = invoke(KeyCodeWithModifiers(code))
    }

    public operator fun Modifier.plus(other: Modifier): Modifiers = Modifiers(mutableSetOf(this, other))
}

public fun keyCode(builder: KeyCodeBuilder.(codes: KeyCodes) -> KeyCodeWithModifiers): KeyCodeWithModifiers = builder(KeyCodeBuilder, KeyCodes)

public fun ViewWriter.onKeyCode(keyCode: KeyCodeWithModifiers, action: () -> Unit): () -> Unit =
    AppState.onUniversalKeyboard {
        if (it == keyCode) {
            action()
            true
        }
        else false
    }.also(::onRemove)