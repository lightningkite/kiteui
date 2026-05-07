package com.lightningkite.kiteui.models


fun Theme.Companion.y2026(primary: HSPColor = HSPColor(hue = 0.6.turns, saturation = 1f, brightness = 1f)): Theme =
    run {
        val backgroundGradient = LinearGradient(
            stops = listOf(
                GradientStop(0f, Color.gray(0.9f)),
                GradientStop(1f, primary.copy(brightness = 1f, saturation = 0.8f).toRGB()),
            ),
            angle = 0.turns,
            screenStatic = true
        )
        val cardBack = Color.gray(0.95f)
        val separator = Color.gray(0.6f)
        Theme(
            id = "y2026",
            foreground = Color.black,
            background = backgroundGradient,
            outline = separator,
            elevation = 2.px,
            cornerRadii = CornerRadii.Fixed(1.rem),
            cornerShape = CornerShape.Continuous,
            gap = 0.75.rem,
            padding = Edges(0.75.rem),
            semanticOverrides = SemanticOverrides(
                CardSemantic.override {
                    if (it.background != cardBack)
                        it.withBack(background = cardBack, elevation = 1.3.dp)
                    else
                        it.withBack(outlineWidth = 1.px)
                },
                HoverSemantic.override {
                    it.withBack(background = Color.white, elevation = 2.dp, transform = Transformation(translationY = -1.0))
                },
                DownSemantic.override {
                    it.withBack(background = Color.white, elevation = 1.dp)
                },
                FocusSemantic.override {
                    it.withBack(background = Color.white, elevation = 1.3.dp, outlineWidth = 2.dp, outline = primary.toRGB())
                },
                FieldSemantic.override {
                    it[CardSemantic]
                },
                BarSemantic.override { it.withoutBackButPadding },
                NavSemantic.override {
                    it.alter(
                        semanticOverrides = SemanticOverrides(
                            UnselectedSemantic.override {
                                it.withoutBack(foreground = it.foreground.applyAlpha(0.6f), iconOverride = it.foreground.applyAlpha(0.3f))
                            },
                            SelectedSemantic.override {
//                                it.withBack(cascading = false, outlineWidth = 1.dp)
                                it.withoutBack
                            },
                            HoverSemantic.override {
                                it.withBack(background = cardBack, elevation = 1.3.dp)
                            },
                        )
                    ).withoutBackButPadding
                },
                OuterSemantic.override { it.withoutBack(cascading = false, cornerRadii = CornerRadii.Fixed(0.px)) },
                MainContentSemantic.override { it.withoutBack },
//            InsetSemantic.override { it.withBack(background = it.background.backInvert()) },
                UnselectedSemantic.override { it.withBack },
                SelectedSemantic.override {
                    it.withBack(cascading = false, outlineWidth = 1.dp)
                },
                DialogSemantic.override {
                    it.withBack(
                        cascading = false,
                        outline = separator,
                        background = cardBack,
                        elevation = 4.dp
                    )
                },
                PopoverSemantic.override {
                    it.withBack(
                        cascading = false,
                        outline = separator,
                        background = cardBack,
                        elevation = 4.dp
                    )
                },
                ImportantSemantic.override {
                    it.withBack(
                        background = primary.toRGB(),
                        foreground = primary.toRGB().highlight(1f),
                        outlineWidth = 0.px
                    )
                },
                ListSemantic.override {
                    it.withoutBack(gap = 0.25.rem, cascading = false)
                },
                HeaderSizeSemantic(1).override {
                    it.withoutBack(font = it.font.copy(size = (HeaderSizeSemantic.lookup[level - 1]).rem, additionalLetterSpacing = (-1).dp))
                },
                HeaderSizeSemantic(2).override {
                    it.withoutBack(font = it.font.copy(size = (HeaderSizeSemantic.lookup[level - 1]).rem, additionalLetterSpacing = (-0.7).dp))
                },
            )
        )
    }