package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.ViewWrapper
import com.lightningkite.kiteui.models.Color
import com.lightningkite.kiteui.models.GradientStop
import com.lightningkite.kiteui.models.ImportantSemantic
import com.lightningkite.kiteui.models.LinearGradient
import com.lightningkite.kiteui.models.Semantic
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.models.degrees
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.appTheme
import com.lightningkite.mppexampleapp.defaultTheme


@Routable("docs/theming")
object ThemingScreen : DocScreen {
    override val covers: List<String> = listOf("theming", "Semantic", "theme", "Theme", "style", "css")

    override fun ViewWriter.render() {
        article {
            h1("Theming")
            text("KiteUI takes an extremely opinionated approach to styling your application, the purpose of which is to enable sweeping styling changes late in development with ease.")
            text("To accomplish this, styling is written in terms of meaning instead of application.  For example,")
            example(
                """
                col {
                    button { centered - text("I'm not that important of an action.") }
                    important - button { centered - text("Pay attention to me!") }
                    danger - button { centered - text("I'm dangerous!") }
                    button {
                        centered - text("Change to a random theme")
                        onClick { appTheme.value = Theme.random() }
                    }
                    button {
                        centered - text("Reset theme")
                        onClick { appTheme.value = defaultTheme }
                    }
                }
            """.trimIndent()
            ) {
                col {
                    button { centered - text("I'm not that important of an action.") }
                    important - button { centered - text("Pay attention to me!") }
                    danger - button { centered - text("I'm dangerous!") }
                    button {
                        centered - text("Change to a random theme")
                        onClick { appTheme.value = Theme.random() }
                    }
                    button {
                        centered - text("Reset theme")
                        onClick { appTheme.value = defaultTheme }
                    }
                }
            }
            text("Try changing the theme using the buttons above - you'll notice that all of the elements adjust accordingly.")
            text("The best test of your KiteUI code is that you should be able to alter the theme freely and still have it look good.")
            space()

            h2("Semantics")
            text("Modifiers like 'important' and 'danger' are references to semantics, or in other words, stylistic meanings.")
            text("Exactly what a given modifier does is dependent on your theme.")
            text("You can define your own semantics, but here are some built-in ones:")
            card - col {
                card - stack { text("card - used for creating light separations in UI groups") }
                fieldTheme - stack { text("fieldTheme - used for indicating a field") }
                bar - stack { text("bar - used to theme the top bar in navigation") }
                nav - stack { text("nav - used to theme navigational widgets, such as bottom or side bars") }
                important - stack { text("important - used to draw attention to a particular element on a page") }
                critical - stack { text("critical - used to draw the maximal amount of attention to a particular element.  Typically, a maximum of one will be present per page.") }
                warning - stack { text("warning - used to indicate that there is some warning you need to pay attention to") }
                danger - stack { text("danger - used to indicate that this element is dangerous to use") }
                affirmative - stack { text("affirmative - used to indicate success") }
                emphasize - stack { text("emphasize - used to mildly draw attention to some text") }
            }
            text("These modifiers carry down to their child elements.  Note that the above modifiers are correcting their child elements' text color.")
            space()

            h2("Defining Your Own Semantics")
            example(
                """
                data object InvertedSemantic: Semantic {
                    override val key: String = "invert"
                    override fun default(theme: Theme): ThemeAndBack = theme.copy(
                        id = key,
                        background = theme.background.map { it.invert() },
                        outline = theme.outline.map { it.invert() },
                        foreground = theme.foreground.map { it.invert() },
                    ).withBack
                }
                @ViewModifierDsl3
                inline val ViewWriter.inverted: ViewWrapper get() = InvertedSemantic.onNext
                
                //...
                
                inverted - col {
                    h3("This block is inverted.")
                    card - col {
                        text("Some carded content inside it")
                        text("Watch how themes carry down.")
                    }
                    inverted - text("Inverting again here")
                }
            """.trimIndent().also { println("'$it'") }) {
                inverted - col {
                    h3("This block is inverted.")
                    card - col {
                        text("Some carded content inside it")
                        text("Watch how themes carry down.")
                    }
                    inverted - text("Inverting again here")
                }
            }
            space()

            h2("Defining Your Application's Theme")
            text("TODO - for now, take a look at Theme.flat for a good sample implementation.")
            text("The key is that you need to think about each semantic as a modifier, not a replacement of styles.")
        }
    }
}

data object InvertedSemantic: Semantic {
    override val key: String = "invert"
    override fun default(theme: Theme): ThemeAndBack = theme.copy(
        id = key,
        background = theme.background.map { it.invert() },
        outline = theme.outline.map { it.invert() },
        foreground = theme.foreground.map { it.invert() },
    ).withBack
}
@ViewModifierDsl3
inline val ViewWriter.inverted: ViewWrapper get() = InvertedSemantic.onNext