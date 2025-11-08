package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.appTheme
import kotlin.random.Random

@Routable("themes")
object ThemesPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
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
            group.frame {
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

                ListSemantic.onNext.col {
                    ListSemantic.onNext.row {
                        expanding.button {
                            text("M1 Light")
                            onClick {
                                appTheme set MaterialLikeTheme.randomLight().randomElevationAndCorners()
                                    .randomTitleFontSettings()
                            }
                        }
                        expanding.button {
                            text("M1 Dark")
                            onClick {
                                appTheme set MaterialLikeTheme.randomDark().randomElevationAndCorners()
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
                    ListSemantic.onNext.row {
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
                                ).copy(id = "cri", cornerRadii = CornerRadii.Constant(Random.nextDouble().rem))
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
                                ).copy(id = "cri", cornerRadii = CornerRadii.Constant(Random.nextDouble().rem))
                                    .randomTitleFontSettings()
                            }
                        }
                    }
                    ListSemantic.onNext.row {
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
                                ).copy(id = "cri", cornerRadii = CornerRadii.Constant(Random.nextDouble().rem))
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
                                ).copy(id = "cri", cornerRadii = CornerRadii.Constant(Random.nextDouble().rem))
                                    .randomTitleFontSettings()
                            }
                        }
                    }
                    ListSemantic.onNext.row {
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
        }
    }
}