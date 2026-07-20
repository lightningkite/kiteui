package com.lightningkite.kiteui.models


public fun Theme.Companion.clean(primary: Color?): Theme = run {
    val back = Color(red = 229f / 255f, green = 229f / 255f, blue = 234f / 255f, alpha = 1f)
    val defaultColor = Color(red = 0 / 255f, green = 122 / 255f, blue = 255 / 255f, alpha = 1f)
    val highlight = primary ?: defaultColor
    val separator = back.darken(0.1f)
    val white = Color.gray(0.95f)
    val black = Color.gray(0.1f)
    fun Paint.backInvert() = if (this == white) back else white
    Theme(
        id = "clean-${highlight.toInt()}",
        foreground = black,
        background = white,
        outline = separator,
        elevation = 0.px,
        cornerRadii = CornerRadii.Fixed(1.rem),
        cornerShape = CornerShape.Continuous,
        gap = 1.rem,
        padding = Edges(1.rem),
        semanticOverrides = SemanticOverrides(
            CardSemantic.override {
                if (it.background != white)
                    it.withBack(background = it.background.backInvert(), foreground = black)
                else
                    it.withBack(outlineWidth = 1.px)
            },
            FieldSemantic.override {
                it.withBack(
                    outline = separator,
                    outlineWidth = 1.px,
                    foreground = black,
                    cornerRadii = CornerRadii.Fixed(1.rem),
                )
            },
            BarSemantic.override { it.withBack(cascading = false, cornerRadii = CornerRadii.Fixed(0.px)) },
            NavSemantic.override { it.withBack(cascading = false, cornerRadii = CornerRadii.Fixed(0.px)) },
            OuterSemantic.override {
                it.withBack(
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(0.px),
                    gap = 1.px,
                    padding = Edges.ZERO,
                    background = separator
                )
            },
            MainContentSemantic.override { it.withBack(cascading = false, cornerRadii = CornerRadii.Fixed(0.px)) },
            InsetSemantic.override { it.withBack(background = it.background.backInvert()) },
            UnselectedSemantic.override { it.withBack },
            SelectedSemantic.override { it[CardSemantic] },
            DialogSemantic.override {
                it.withBack(
                    cascading = false,
                    outline = separator,
                    background = white,
                    foreground = black,
                    elevation = 4.dp
                )
            },
            PopoverSemantic.override {
                it.withBack(
                    cascading = false,
                    outline = separator,
                    background = white,
                    foreground = black,
                    elevation = 4.dp
                )
            },
            ImportantSemantic.override {
                it.withBack(
                    background = highlight,
                    foreground = if (highlight.perceivedBrightness > 0.4f) white else black,
                    outlineWidth = 0.px
                )
            },
            ListSemantic.override {
                it.copy(
                    id = "directlist",
                    cascading = false,
                    cornerRadii = CornerRadii.Fixed(1.rem),
                    gap = 1.px,
                    padding = Edges(0.px),
                ).withBack(
                    semanticOverrides = SemanticOverrides(
                        ListSemantic.override {
                            it.withBack(
                                cascading = false,
                                cornerRadii = CornerRadii.Fixed(1.rem),
                                outlineWidth = 0.px,
                                gap = 1.px,
                                padding = Edges(0.px),
                                semanticOverrides = SemanticOverrides(
                                    ListSemantic.override { it.withoutBack },
                                )
                            )
                        },
                    )
                )
            },
        )
    )
}