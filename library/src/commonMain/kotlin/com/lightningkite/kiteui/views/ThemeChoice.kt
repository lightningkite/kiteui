package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Theme

sealed interface ThemeChoice {
    data class Set(val theme: Theme): ThemeChoice
    data class Derive(val derivation: (Theme) -> Theme?): ThemeChoice
//        data class ControlStateDerive(val derivation: (Theme) -> Theme): ThemeChoice
}

operator fun ThemeChoice?.plus(other: ThemeChoice?): ThemeChoice? {
    return when(this) {
        is ThemeChoice.Derive -> when(other) {
            is ThemeChoice.Derive -> ThemeChoice.Derive {
                val s = derivation(it) ?: it
                other.derivation(s)
            }
            is ThemeChoice.Set -> other
            null -> this
        }
        is ThemeChoice.Set -> when(other) {
            is ThemeChoice.Derive -> ThemeChoice.Set(other.derivation(this.theme) ?: this.theme)
            is ThemeChoice.Set -> other
            null -> this
        }
        null -> other
    }
}