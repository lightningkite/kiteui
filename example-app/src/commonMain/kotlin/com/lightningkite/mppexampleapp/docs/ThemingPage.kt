package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.Routable

import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.DownSemantic
import com.lightningkite.kiteui.models.HoverSemantic
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.Transformation
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.titledSection
import com.lightningkite.mppexampleapp.appTheme
import com.lightningkite.mppexampleapp.defaultTheme
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.collections.mapOf


@Routable("docs/theming")
object ThemingPage : DocPage {
    override val covers: List<String> = listOf("theming", "Semantic", "theme", "Theme", "style", "css")

    override fun ViewWriter.render(): Unit = run {
        article {
            titledSection("Theming") {

                text("KiteUI takes an extremely opinionated approach to styling your application, the purpose of which is to enable sweeping styling changes late in development with ease.")
                text("To accomplish this, styling is written in terms of meaning instead of application.  For example,")
                example(
                """
                    col {
                        button { centered.text("I'm not that important of an action.") }
                        important.button { centered.text("Pay attention to me!") }
                        danger.button { centered.text("I'm dangerous!") }
                        button {
                            centered.text("Change to a random theme")
                            onClick { appTheme.value = Theme.random() }
                        }
                        button {
                            centered.text("Reset theme")
                            onClick { appTheme.value = defaultTheme }
                        }
                    }
                """.trimIndent()
                ) {
                    col {
                        button { centered.text("I'm not that important of an action.") }
                        important.button { centered.text("Pay attention to me!") }
                        danger.button { centered.text("I'm dangerous!") }
                        button {
                            centered.text("Change to a random theme")
                            onClick { appTheme.value = Theme.random() }
                        }
                        button {
                            centered.text("Reset theme")
                            onClick { appTheme.value = defaultTheme }
                        }
                    }
                }
                text("Try changing the theme using the buttons above - you'll notice that all of the elements adjust accordingly.")
                text("The best test of your KiteUI code is that you should be able to alter the theme freely and still have it look good.")
                space()

                titledSection("Semantics") {
                    text("Modifiers like 'important' and 'danger' are references to semantics, or in other words, stylistic meanings.")
                    text("Exactly what a given modifier does is dependent on your theme.")
                    emphasized.text("Semantics are allowed to be cascading such that they affect their children!")
                    text("Note that the above modifiers are correcting their child elements' text color.")
                    text("You can define your own semantics, but here are some built-in ones:")
                    card.col {
                        card.frame { text("card - used for creating light separations in UI groups") }
                        fieldTheme.frame { text("fieldTheme - used for indicating a field") }
                        bar.frame { text("bar - used to theme the top bar in navigation") }
                        nav.frame { text("nav - used to theme navigational widgets, such as bottom or side bars") }
                        important.frame { text("important - used to draw attention to a particular element on a page") }
                        critical.frame { text("critical - used to draw the maximal amount of attention to a particular element.  Typically, a maximum of one will be present per page.") }
                        warning.frame { text("warning - used to indicate that there is some warning you need to pay attention to") }
                        danger.frame { text("danger - used to indicate that this element is dangerous to use") }
                        affirmative.frame { text("affirmative - used to indicate success") }
                        emphasize.frame { text("emphasize - used to mildly draw attention to some text") }
                    }
                    titledSection("Semantics are Nestable") {
                        text("Applying the same semantic multiple times might not have identical effects.  This is on purpose.  For example, here's multiple layers of cards:")
                        example("""
                            card.col {
                                text("Layer 1")
                                card.col {
                                    text("Layer 2")
                                    card.col {
                                        text("Layer 3")
                                    }
                                }
                            }
                        """.trimIndent()) {
                            card.col {
                                text("Layer 1")
                                card.col {
                                    text("Layer 2")
                                    card.col {
                                        text("Layer 3")
                                    }
                                }
                            }
                        }
                        text("This allows elements to show a reasonable style regardless of the context they are placed in.")
                        text("Another example is what happens when you put a card inside an already important layout:")
                        example("""
                            important.col {
                                text("Important")
                                card.col {
                                    text("Important + Card")
                                }
                            }
                        """.trimIndent()) {
                            important.col {
                                text("Important")
                                card.col {
                                    text("Important + Card")
                                }
                            }
                        }
                        text("A good way to debug why certain elements look the way they do is by inspecting them using web tools.")
                        text("These elements have a class in the format 't-themeName-semantic-semantic-semantic...' which indicates what theme and semantics have been applied.")
                    }
                }

                titledSection("Defining Your Own Semantics") {
                    text("You can define your own semantics as well.")
                    text("Here, we will define a semantic that will invert the color palette.")
                    example(
                        """
                        data object InvertedSemantic : Semantic("invert") {
                            override fun ThemeBuilder.apply() {
                                background = background.map { it.invert() }
                                outline = outline.map { it.invert() }
                                foreground = foreground.map { it.invert() }
                            }
                        }
                        @ViewModifierDsl3
                        inline val ViewWriter.inverted: ViewWriter get() = InvertedSemantic.onNext
                        
                        //...
                        
                        inverted.col {
                            h3("This block is inverted.")
                            card.col {
                                text("Some carded content inside it")
                                text("Watch how themes carry down.")
                            }
                            inverted.text("Inverting again here")
                        }
                        """.trimIndent().also { println("'$it'") }) {
                        inverted.col {
                            h3("This block is inverted.")
                            card.col {
                                text("Some carded content inside it")
                                text("Watch how themes carry down.")
                            }
                            inverted.text("Inverting again here")
                        }
                    }
                }

                titledSection("Defining Your Application's Theme") {
                    text("TODO - for now, take a look at Theme.flat for a good sample implementation.")
                    text("The key is that you need to think about each semantic as a modifier, not a replacement of styles.")
                }
            }
        }
    }
}

data object GlassSemantic : Semantic("glass") {
    override fun default(theme: Theme): ThemeAndBack {
        return theme.withBack(
            background = Color.gray.withAlpha(0.25f),
            blurBackground = 1.rem,
            semanticOverrides = mapOf(
                HoverSemantic to {
                    it.withBack(transform = Transformation(scaleX = 1.2, scaleY = 1.2))
                },
                DownSemantic to {
                    it.withBack(transform = Transformation(scaleX = 0.9, scaleY = 0.9))
                },
            )
        )
    }
}

data object AnimatedEmphasis1Semantic : Semantic("ae1s") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        cascading = false,
        transform = Transformation(scaleX = 1.1, scaleY = 1.1)
    )
}
data object AnimatedEmphasis2Semantic : Semantic("ae2s") {
    override fun default(theme: Theme): ThemeAndBack = theme.withBack(
        cascading = false,
        transform = Transformation(scaleX = 0.9, scaleY = 0.9)
    )
}

data object InvertedSemantic : Semantic("invert") {
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        background = theme.background.map { it.invert() },
        outline = theme.outline.map { it.invert() },
        foreground = theme.foreground.map { it.invert() },
    ).withBack
}

@ViewModifierDsl3
inline val ViewWriter.inverted: ViewWriter get() = InvertedSemantic.onNext