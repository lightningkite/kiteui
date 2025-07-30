package com.lightningkite.kiteui.models

import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*


data class KeyCodeWithModifiers(val code: KeyCode, val alt: Boolean = false, val ctrl: Boolean = false, val shift: Boolean = false, val meta: Boolean = false)

expect class KeyCode
expect object KeyCodes {
    val left: KeyCode
    val right: KeyCode
    val up: KeyCode
    val down: KeyCode
    fun letter(char: Char): KeyCode
    fun num(digit: Int): KeyCode
    fun numpad(digit: Int): KeyCode
    val space: KeyCode
    val enter: KeyCode
    val tab: KeyCode
    val escape: KeyCode
    val leftCtrl: KeyCode
    val rightCtrl: KeyCode
    val leftShift: KeyCode
    val rightShift: KeyCode
    val leftAlt: KeyCode
    val rightAlt: KeyCode
    val equals: KeyCode
    val dash: KeyCode
    val backslash: KeyCode
    val leftBrace: KeyCode
    val rightBrace: KeyCode
    val semicolon: KeyCode
    val comma: KeyCode
    val period: KeyCode
}

object KeyCodeBuilder {

    class Modifier(val applyOn: (KeyCodeWithModifiers) -> KeyCodeWithModifiers) {
        operator fun invoke(on: KeyCodeWithModifiers) = applyOn(on)
        operator fun plus(code: KeyCode) = invoke(KeyCodeWithModifiers(code))
    }

    val Ctrl = Modifier { it.copy(ctrl = true) }
    val Alt = Modifier { it.copy(alt = true) }
    val Shift = Modifier { it.copy(shift = true) }
    val Meta = Modifier { it.copy(meta = true) }

    // This is needed to prevent the Set<Modifier>.plus(element) overload from shadowing desired behavior
    class Modifiers(val elements: MutableSet<Modifier>) {
        operator fun plus(modifier: Modifier) = apply { elements.add(modifier) }
        operator fun invoke(keyCode: KeyCodeWithModifiers) = elements.fold(keyCode) { code, modifier -> modifier(code) }
        operator fun plus(code: KeyCode) = invoke(KeyCodeWithModifiers(code))
    }

    operator fun Modifier.plus(other: Modifier) = Modifiers(mutableSetOf(this, other))
}

fun keyCode(builder: KeyCodeBuilder.(codes: KeyCodes) -> KeyCodeWithModifiers) = builder(KeyCodeBuilder, KeyCodes)

fun ViewWriter.onKeyCode(keyCode: KeyCodeWithModifiers, action: () -> Unit) =
    AppState.onUniversalKeyboard {
        if (it == keyCode) {
            action()
            true
        }
        else false
    }.also(::onRemove)