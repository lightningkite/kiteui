package com.lightningkite.kiteui.models

public data class SemanticOverride<T : Semantic>(val semantic: T, val derivation: T.(Theme) -> ThemeAndBack): ThemeDerivation {
    public override fun invoke(theme: Theme): ThemeAndBack = derivation(semantic, theme)
}

