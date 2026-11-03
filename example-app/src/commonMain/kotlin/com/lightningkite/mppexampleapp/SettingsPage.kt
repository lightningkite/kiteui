package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.themed
import com.lightningkite.mppexampleapp.appTheme
import com.lightningkite.reactive.extensions.equalTo
import kotlin.random.Random

@Routable("settings")
class SettingsPage: Page {
    override fun ElementWriter.CanAddTheme.render() {
        frame {
            align(Align.Center, Align.Stretch).sizedBox(SizeConstraints(width = 80.rem)).scrolling.col  {
                group.col {
                    h2 { content = "Nav Style" }
                    rowCollapsingToColumn(50.rem) {
                        NavVariant.entries.map { variant ->
                            radioToggleButton {
                                centered.text(variant.display)
                                checked bind navVariant.equalTo(variant)
                            }
                        }
                    }
                }
                group.col {
                    h2 { content = "Theme Picker" }

                    scrollingHorizontally.row {
                        expanding.space {}
                        centered.padded.text { content = "Sample" }
                        centered.card.text { content = "Card" }
                        centered.important.text { content = "Important" }
                        centered.critical.text { content = "Critical" }
                        expanding.space {}
                    }
                    scrollingHorizontally.row {
                        weight(1f).space {}
                        centered.warning.text { content = "Warning" }
                        centered.danger.text { content = "Danger" }
                        centered.affirmative.text { content = "Affirmative" }
                        weight(1f).space {}
                    }
                    card.frame {
                        card.frame {
                            card.frame {
                                text("Nested Cards")
                            }
                        }
                    }

                    themed(ListSemantic).col {
                        // Every built-in factory takes (id, background, accent, ...), so switching
                        // design language is a matter of swapping the function name. The accent is
                        // randomized to show that each theme derives its whole palette from it.
                        themed(ListSemantic).row {
                            expanding.button {
                                text("M2 Light")
                                onClick {
                                    appTheme set Theme.material("m2-l-${Random.nextInt()}", accent = randomAccent())
                                }
                            }
                            expanding.button {
                                text("M2 Dark")
                                onClick {
                                    appTheme set Theme.material(
                                        "m2-d-${Random.nextInt()}",
                                        background = Color.fromHexString("#121212"),
                                        accent = randomAccent(),
                                    )
                                }
                            }
                            expanding.button {
                                text("M3 Light")
                                onClick {
                                    appTheme set Theme.material3("m3-l-${Random.nextInt()}", accent = randomAccent())
                                }
                            }
                            expanding.button {
                                text("M3 Dark")
                                onClick {
                                    appTheme set Theme.material3(
                                        "m3-d-${Random.nextInt()}",
                                        background = Color.fromHexString("#141218"),
                                        accent = randomAccent(),
                                    )
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
                            // Off-white rather than white: cards are made of white, so a pure white
                            // canvas leaves them nothing to rise to. See Theme.shadCnLike.
                            expanding.button {
                                text("ShadCN-Like Light")
                                onClick {
                                    appTheme set Theme.shadCnLike(
                                        id = "shadcnlike-light",
                                        background = Color.fromHexString("#FAFAFA"),
                                    )
                                }
                            }
                            expanding.button {
                                text("ShadCN-Like Dark")
                                onClick { appTheme set Theme.shadCnLike(id = "shadcnlike-dark") }
                            }
                            // Same theme with a brand color as the primary - the neutrals pick up its
                            // hue too, so the whole page shifts, not just the buttons.
                            expanding.button {
                                text("ShadCN-Like Blue")
                                onClick {
                                    appTheme set Theme.shadCnLike(
                                        id = "shadcnlike-blue",
                                        background = Color.fromHexString("#FAFAFA"),
                                        accent = Color.fromHexString("#2563EB"),
                                    )
                                }
                            }
                        }
                        themed(ListSemantic).row {
                            expanding.button {
                                text("Light")
                                onClick { appTheme set Theme.light() }
                            }
                            // Same theme, different accent: the neutrals are tints of the accent hue,
                            // so swapping it recolors the page and every surface, not just the buttons.
                            expanding.button {
                                text("Light (plum)")
                                onClick {
                                    appTheme set Theme.light(
                                        id = "light-plum",
                                        accent = Color.fromHexString("#6B2D5B"),
                                    )
                                }
                            }
                        }
                        themed(ListSemantic).row {
                            expanding.button {
                                text("Clean Light")
                                onClick { appTheme set Theme.clean("clean-light") }
                            }
                            expanding.button {
                                text("Clean Dark")
                                onClick { appTheme set Theme.clean("clean-dark", background = Color.black) }
                            }
                        }
                    }
                }
            }
        }
    }


    /** A saturated mid-tone in a random hue - the shape of a usable brand color. */
    private fun randomAccent(): Color = HSVColor(
        hue = Random.nextFloat().turns,
        saturation = Random.nextFloat() * 0.4f + 0.55f,
        value = Random.nextFloat() * 0.35f + 0.5f,
    ).toRGB()
}