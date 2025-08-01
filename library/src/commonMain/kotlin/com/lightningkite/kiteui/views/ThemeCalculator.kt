package com.lightningkite.kiteui.views

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.ThemeDerivation

class ThemeCalculator(
    val getChildren: () -> Sequence<ThemeCalculator>,
    val appendToChoice: () -> ThemeDerivation? = { null },
    val applyTheme: (ThemeAndBack) -> Unit,
) {
    var themeTakeNonCascadingFromParent: Boolean = false
        set(value) {
            field = value
            refresh()
        }
    var themeChoice: ThemeDerivation = ThemeDerivation.Companion.none
        set(value) {
            field = value
            refresh()
        }
    var themeAndBack: ThemeAndBack = Theme.Companion.placeholder.withBack
        private set(value) {
            if (value != field) {
                field = value
                applyTheme(themeAndBack)
                for (child in getChildren()) {
//                    if (child.themeChoice !is ThemeChoice.Set)
                    child.onParentTheme(value)
                }
            }
        }

    var lastParentThemeAndBack: ThemeAndBack? = null
    fun refresh() {
        lastParentThemeAndBack?.let { onParentTheme(it) }
    }
    fun onParentTheme(parentThemeAndBack: ThemeAndBack) {
        if(themeChoice is ThemeDerivation.Set) return
        lastParentThemeAndBack = parentThemeAndBack
        val themeBorrowed = if(themeTakeNonCascadingFromParent) parentThemeAndBack.theme
        else parentThemeAndBack.theme.let { it.revert ?: it }
        val t = themeChoice(themeBorrowed) + (appendToChoice() ?: ThemeDerivation.none)
        themeAndBack = t
    }
}