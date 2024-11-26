package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.appTheme
import kotlin.random.Random

@Routable("themes")
object ThemesScreen : Screen {
    override fun ViewWriter.render() {
        col {
            h1 { content = "Theme Control" }
            card - col {
                h2 {
                    content = "Theme Sampling"
                }
                row {
                    expanding - space {}
                    padded - text { content = "Sample" }
                    card - text { content = "Card" }
                    important - text { content = "Important" }
                    critical - text { content = "Critical" }
                    expanding - space {}
                } in scrollsHorizontally
                row {
                    weight(1f) - space {}
                    warning - text { content = "Warning" }
                    danger - text { content = "Danger" }
                    affirmative - text { content = "Affirmative" }
                    weight(1f) - space {}
                } in scrollsHorizontally
            }
            card - stack {
                card - stack {
                    card - stack {
                        card - stack {
                            text("HI")
                        }
                    }
                }
            }
            col {
                h2 { content = "Randomly Generate Themes" }

                button {
                    h6 { content = "M1 Light" }
                    onClick {
                        appTheme set MaterialLikeTheme.randomLight().randomElevationAndCorners().randomTitleFontSettings()
                    }
                } in card
                button {
                    h6 { content = "M1 Dark" }
                    onClick {
                        appTheme set MaterialLikeTheme.randomDark().randomElevationAndCorners().randomTitleFontSettings()
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
                        appTheme set Theme.flat(id = "flat-${Random.nextInt()}", hue = a, saturation = 0.15f, accentHue = a + Angle.halfTurn, baseBrightness = 0.8f).copy(cornerRadii = CornerRadii.Constant(Random.nextDouble().rem)).randomTitleFontSettings()
                    }
                } in card
                button {
                    h6 { content = "Flat Dark" }
                    onClick {
                        val a = Angle(Random.nextFloat())
                        appTheme set Theme.flat(id = "flat-${Random.nextInt()}", hue = a, saturation = 0.5f, accentHue = a + Angle.halfTurn).copy(cornerRadii = CornerRadii.Constant(Random.nextDouble().rem)).randomTitleFontSettings()
                    }
                } in card
            } in card
        } in scrolls
    }
}