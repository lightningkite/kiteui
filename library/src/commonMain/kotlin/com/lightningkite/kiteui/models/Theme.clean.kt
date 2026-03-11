package com.lightningkite.kiteui.models


fun Theme.Companion.clean(primary: Color?): Theme = run {
    val back = Color(red = 229f / 255f, green = 229f / 255f, blue = 234f / 255f, alpha = 1f)
    val defaultColor = Color(red = 0 / 255f, green = 122 / 255f, blue = 255 / 255f, alpha = 1f)
    val highlight = primary ?: defaultColor
    val separator = back.darken(0.1f)
    fun Paint.backInvert() = if (this == Color.white) back else Color.white
    Theme(
        id = "clean-${highlight.toInt()}",
        foreground = Color.black,
        background = Color.white,
        outline = separator,
        elevation = 0.px,
        cornerRadii = CornerRadii.AdaptiveToSpacing(0.5.rem),
        gap = 0.75.rem,
        padding = Edges(0.75.rem),
        semanticOverrides = SemanticOverrides(
            CardSemantic.override {
                if(it.background != Color.white)
                    it.withBack(background = it.background.backInvert(), foreground = Color.black)
                else
                    it.withBack(outlineWidth = 1.px)
            },
            FieldSemantic.override {
                it.withBack(
                    outline = separator,
                    outlineWidth = 1.px,
                    foreground = Color.black,
                    cornerRadii = CornerRadii.Fixed(0.5.rem)
                )
            },
            BarSemantic.override { it.withBack },
            NavSemantic.override { it.withBack },
            OuterSemantic.override { it.withBack(cascading = false, gap = 1.px, padding = Edges.ZERO, background = separator) },
            MainContentSemantic.override { it.withBack(cascading = false, cornerRadii = CornerRadii.AdaptiveToSpacing(0.px)) },
            InsetSemantic.override { it.withBack(background = it.background.backInvert()) },
            UnselectedSemantic.override { it.withBack },
            SelectedSemantic.override { it[CardSemantic] },
            DialogSemantic.override {
                it.withBack(
                    cascading = false,
                    outline = separator,
                    background = Color.white,
                    foreground = Color.black,
                    elevation = 4.dp
                )
            },
            PopoverSemantic.override {
                it.withBack(
                    cascading = false,
                    outline = separator,
                    background = Color.white,
                    foreground = Color.black,
                    elevation = 4.dp
                )
            },
            ImportantSemantic.override {
                it.withBack(
                    background = highlight,
                    foreground = if (highlight.perceivedBrightness > 0.4f) Color.white else Color.black,
                )
            },
            ListSemantic.override {
                it.copy(id = "lsts", background = back).withBack(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.75.rem),
                    gap = 1.px,
                    padding = Edges(0.px)
                )
            },
        )
    )
}