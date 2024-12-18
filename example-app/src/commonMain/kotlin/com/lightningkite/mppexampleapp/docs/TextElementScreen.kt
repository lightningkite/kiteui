package com.lightningkite.mppexampleapp.docs

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("docs/text")
object TextElementScreen : DocScreen {
    override val covers: List<String> = listOf("Text", "text", "subtext", "h1", "h2", "h3")

    override fun ViewWriter.render() {
        article {
            h1("Text Elements")
            text("KiteUI has a number of text elements to help you display text in a variety of ways.")
            h2("Text")
            text("The most basic text element is the text element.  It simply displays text.")
            example("text(\"Hello, world!\")") {
                text("Hello, world!")
            }
            h2("Subtext")
            text("Subtext is a smaller, less important text element.")
            example("subtext(\"This is a subtext element.\")") {
                subtext("This is a subtext element.")
            }
            h2("Headers")
            text("KiteUI has the standard header elements from HTML, h1, h2, h3, h4, h5, and h6.")
            example("h1(\"This is an h1 header\")") {
                h1("This is an h1 header")
            }
            example("h2(\"This is an h2 header\")") {
                h2("This is an h2 header")
            }
            example("h3(\"This is an h3 header\")") {
                h3("This is an h3 header")
            }
            example("h4(\"This is an h4 header\")") {
                h4("This is an h4 header")
            }
            example("h5(\"This is an h5 header\")") {
                h5("This is an h5 header")
            }
            example("h6(\"This is an h6 header\")") {
                h6("This is an h6 header")
            }
            h2("How themes interact with text")
            text("Text elements are styled by the theme.  You can change the theme to change the style of the text.")
            example(
                """
                col {
                    tweakTheme { it.copy(body = it.body.copy(bold = true)) } - text("Bold Text")
                    tweakTheme { it.copy(foreground = Color.red) } - text("Red Text")
                }
                """.trimIndent()
            ) {
                col {
                    tweakTheme { it.copy(body = it.font.copy(bold = true)) } - text("Bold Text")
                    tweakTheme { it.copy(foreground = Color.red) } - text("Red Text")
                }
            }
            text("Common style tweaks are available via some shortcuts.")
            example(
                """
                col {
                    bold - text("Bold Text")
                    italic - text("Italic Text")
                    allCaps - text("All Caps Text")
                    strikethrough - text("Strikethrough Text")
                    underline - text("Underline Text")
                }
                """.trimIndent()
            ) {
                col {
                    bold - text("Bold Text")
                    italic - text("Italic Text")
                    allCaps - text("All Caps Text")
                    strikethrough - text("Strikethrough Text")
                    underline - text("Underline Text")
                }
            }
            text("Custom weights are available too.")
            example(
                """
                col {
                    for(weight in 900 downTo 100) {
                        tweakTheme { it.copy(body = it.body.copy(weight = weight)) } - text("Weight ${'$'}weight")
                    }
                }
                """.trimIndent()
            ) {
                col {
                    for(weight in 900 downTo 100 step 100) {
                        tweakTheme { it.copy(font = it.font.copy(weight = weight)) } - text("Weight $weight")
                    }
                }
            }
            text("It's gross, but you can assign some level of HTML too.")
            example(
                """
                text {
                    setBasicHtmlContent(""${'"'}
                        <h1>Hello!</h1>
                        <p>Welcome to my website from 2000.</p>
                        <ol>
                        <li> Item One</li>
                        <li> Item Two</li>
                        <li> Item Three</li>
                        </ol>
                        <a href="https://google.com"> Open Google </a>
                    ""${'"'}.trimIndent())
                }
                """.trimIndent()
            ) {
                text {
                    setBasicHtmlContent("""
                        <h1>Hello!</h1>
                        <p>Welcome to my website from 2000.</p>
                        <ol>
                        <li> Item One</li>
                        <li> Item Two</li>
                        <li> Item Three</li>
                        </ol>
                        <a href="https://google.com"> Open Google </a>
                    """.trimIndent())
                }
            }
        }
    }

}