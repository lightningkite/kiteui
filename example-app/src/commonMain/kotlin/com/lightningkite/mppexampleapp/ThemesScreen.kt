package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import kotlin.random.Random

@Routable("themes")
object ThemesScreen : Screen {
    override fun ViewWriter.render() {
        col {
            h1 { content = "Theme Control" }
            col {
                h2 { content = "Theme Sampling" }
                row {
                    space {} in weight(1f)
                    text { content = "Sample" } in padded
                    text { content = "Card" } in card
                    text { content = "Important" } in hasPopover {
                        card - col {
                            text {
                                content = "Pop over!"
                            }
                            button {
                                text("Dismiss")
                                onClick {
                                    it.close()
                                }
                            }
                        }
                    } in important
                    text { content = "Critical" } in critical
                    space {} in weight(1f)
                } in scrollsHorizontally
                row {
                    space {} in weight(1f)
                    text { content = "Warning" } in warning
                    text { content = "Danger" } in danger
                    text { content = "Affirmitive" } in affirmative
                    space {} in weight(1f)
                } in scrollsHorizontally
            } in card
            col {
                h2 { content = "Randomly Generate Themes" }

                button {
                    h6 { content = "M1 Light" }
                    onClick {
                        appTheme set MaterialLikeTheme.randomLight().randomElevationAndCorners()
                            .randomTitleFontSettings()
                    }
                } in card
                button {
                    h6 { content = "M1 Dark" }
                    onClick {
                        appTheme set MaterialLikeTheme.randomDark().randomElevationAndCorners()
                            .randomTitleFontSettings()
                    }
                } in card
                button {
                    h6 { content = "M3 Light" }
                    onClick {
                        appTheme set M3Theme.randomLight().randomElevationAndCorners().randomTitleFontSettings()
                    }
                } in card
                button {
                    h6 { content = "M3 Dark" }
                    onClick {
                        appTheme set M3Theme.randomDark().randomElevationAndCorners().randomTitleFontSettings()
                    }
                } in card
                button {
                    h6 { content = "Flat Light" }
                    onClick {
                        val a = Angle(Random.nextFloat())
                        appTheme set Theme.flat(hue = a, saturation = 0.15f, accentHue = a + Angle.halfTurn, baseBrightness = 0.8f)
                            .copy(
                                cornerRadii = CornerRadii.RatioOfSpacing(Random.nextFloat())
                            )
                            .randomTitleFontSettings()
                    }
                } in card
                button {
                    h6 { content = "Flat Dark" }
                    onClick {
                        val a = Angle(Random.nextFloat())
                        appTheme set Theme.flat(hue = a, saturation = 0.5f, accentHue = a + Angle.halfTurn)
                            .copy(
                                cornerRadii = CornerRadii.RatioOfSpacing(Random.nextFloat())
                            )
                            .randomTitleFontSettings()
                    }
                } in card
            } in card
        } in scrolls
    }
}