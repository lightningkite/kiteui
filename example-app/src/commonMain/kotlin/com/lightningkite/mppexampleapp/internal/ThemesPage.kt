package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.themed
import com.lightningkite.mppexampleapp.appTheme
import kotlin.random.Random

// MaterialLikeTheme.randomLight/randomDark are deprecated with no direct successor (Theme.material
// only replaces the plain constructor); these mirror that same randomization for the "M1" demo buttons.
private fun randomM1Light(): Theme {
    val hue = Random.nextFloat().turns
    val saturation = Random.nextFloat() * 0.5f + 0.25f
    val value = Random.nextFloat() * 0.5f + 0.25f
    return Theme.material(
        id = "materialRandomLight-${Random.nextInt()}",
        primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
        secondary = HSVColor(hue = hue + Angle.halfTurn, saturation = 1f - saturation, value = 1f - value).toRGB(),
    )
}

private fun randomM1Dark(): Theme {
    val hue = Random.nextFloat().turns
    val saturation = Random.nextFloat() * 0.5f + 0.25f
    val value = Random.nextFloat() * 0.5f + 0.25f
    return Theme.material(
        id = "materialRandomDark-${Random.nextInt()}",
        foreground = Color.white,
        background = Color.gray(0.2f),
        primary = HSVColor(hue = hue, saturation = saturation, value = value).toRGB(),
        secondary = HSVColor(hue = hue + Angle.halfTurn, saturation = 1f - saturation, value = 1f - value).toRGB(),
    )
}

@Routable("themes")
object ThemesPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        scrolling.col {
            h1 { content = "Theme Control" }
            group.col {
                h2 {
                    content = "Theme Sampling"
                }
                scrollingHorizontally.row {
                    expanding.space {}
                    padded.text { content = "Sample" }
                    card.text { content = "Card" }
                    important.text { content = "Important" }
                    critical.text { content = "Critical" }
                    expanding.space {}
                }
                scrollingHorizontally.row {
                    weight(1f).space {}
                    warning.text { content = "Warning" }
                    danger.text { content = "Danger" }
                    affirmative.text { content = "Affirmative" }
                    weight(1f).space {}
                }
            }
            group.col {
                h2 { content = "Nested Card Test" }
                card.frame {
                    card.frame {
                        card.frame {
                            text("HI")
                        }
                    }
                }
            }
            group.col {
                h2 { content = "Theme Picker" }

                themed(ListSemantic).col {
                    themed(ListSemantic).row {
                        expanding.button {
                            text("M1 Light")
                            onClick {
                                appTheme set randomM1Light().randomElevationAndCorners()
                                    .randomTitleFontSettings()
                            }
                        }
                        expanding.button {
                            text("M1 Dark")
                            onClick {
                                appTheme set randomM1Dark().randomElevationAndCorners()
                                    .randomTitleFontSettings()
                            }
                        }
                        expanding.button {
                            text("M3 Light")
                            onClick {
                                appTheme set M3Theme.randomLight().randomElevationAndCorners().randomTitleFontSettings()
                            }
                        }
                        expanding.button {
                            text("M3 Dark")
                            onClick {
                                appTheme set M3Theme.randomDark().randomElevationAndCorners().randomTitleFontSettings()
                            }
                        }
                    }
                    themed(ListSemantic).row {
                        expanding.button {
                            text("Flat Light")
                            onClick {
                                val a = Angle(Random.nextFloat())
                                appTheme set Theme.flat(
                                    id = "flat-${Random.nextInt()}",
                                    hue = a,
                                    saturation = 0.15f,
                                    accentHue = a + Angle.halfTurn,
                                    baseBrightness = 0.8f
                                ).copy(id = "cri", cornerRadii = CornerRadii.AdaptiveToSpacing(Random.nextDouble().rem))
                                    .randomTitleFontSettings()
                            }
                        }
                        expanding.button {
                            text("Flat Dark")
                            onClick {
                                val a = Angle(Random.nextFloat())
                                appTheme set Theme.flat(
                                    id = "flat-${Random.nextInt()}",
                                    hue = a,
                                    saturation = 0.5f,
                                    accentHue = a + Angle.halfTurn
                                ).copy(id = "cri", cornerRadii = CornerRadii.AdaptiveToSpacing(Random.nextDouble().rem))
                                    .randomTitleFontSettings()
                            }
                        }
                    }
                    themed(ListSemantic).row {
                        expanding.button {
                            text("Flat2 Light")
                            onClick {
                                val a = Angle(Random.nextFloat())
                                appTheme set Theme.flat2(
                                    id = "flat2-${Random.nextInt()}",
                                    hue = a,
                                    saturation = 0.15f,
                                    accentHue = a + Angle.halfTurn,
                                    baseBrightness = 0.8f
                                ).copy(id = "cri", cornerRadii = CornerRadii.AdaptiveToSpacing(Random.nextDouble().rem))
                                    .randomTitleFontSettings()
                            }
                        }
                        expanding.button {
                            text("Flat2 Dark")
                            onClick {
                                val a = Angle(Random.nextFloat())
                                appTheme set Theme.flat2(
                                    id = "flat2-${Random.nextInt()}",
                                    hue = a,
                                    saturation = 0.5f,
                                    accentHue = a + Angle.halfTurn
                                ).copy(id = "cri", cornerRadii = CornerRadii.AdaptiveToSpacing(Random.nextDouble().rem))
                                    .randomTitleFontSettings()
                            }
                        }
                    }
                    themed(ListSemantic).row {
                        expanding.button {
                            text("ShadCN-Like Light")
                            onClick {
                                appTheme set Theme.shadCnLike(
                                    id = "shadcnlike-light",
                                    background = Color.white,
                                )
                            }
                        }
                        expanding.button {
                            text("ShadCN-Like Dark")
                            onClick {
                                appTheme set Theme.shadCnLike(
                                    id = "shadcnlike-dark",
                                    background = Color.gray(0.05f),
                                )
                            }
                        }
                    }
                    button {
                        text("Clean (iOS like)")
                        onClick {
                            val a = Angle(Random.nextFloat())
                            appTheme set Theme.clean(null)
                        }
                    }
                }
            }


            group.col {
                h2 { content = "Colors" }
                with(ColorTestPage) {
                    changeAppBackgroundColor()
                }
            }
        }
    }
}