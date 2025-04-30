package com.lightningkite.kiteui.models

data class SemanticOverride<T : Semantic>(val semantic: T, val derivation: T.(Theme) -> ThemeAndBack): ThemeDerivation {
    override fun invoke(theme: Theme): ThemeAndBack = derivation(semantic, theme)
}

