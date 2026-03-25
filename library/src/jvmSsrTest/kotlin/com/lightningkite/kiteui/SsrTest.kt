package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.models.flat2
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.models.turns
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.field
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.setMain
import java.io.File
import kotlin.test.Test

class SsrTest {
    init {
        Dispatchers.setMain(Dispatchers.IO)
    }
    @Test
    fun test() {
        val context = RContext("/")
        val writer = Frame(context)
        with(writer) {
            Theme.flat2("f2", hue = 0.6.turns).onNext.scrolling.col {
                centered.sizeConstraints(width = 50.rem).card.col {
                    centered.h1("Welcome to my Website")
                    centered.text("I hope you like it!")
                }
                centered.sizeConstraints(width = 50.rem).card.col {
                    centered.h2("Form")
                    field("Email") {
                        textInput {  }
                    }
                    field("Phone Number") {
                        textInput {  }
                    }
                    centered.important.button { text("Submit") }
                }
            }
        }
        fun File.write(action: Appendable.() -> Unit) = bufferedWriter().use { action(it) }
        File("build/test.html").write {
            appendLine("""
                <!DOCTYPE html>
                <html lang="en">
                <head>
                    <meta charset="UTF-8">
                    <meta name="viewport" content="width=device-width,initial-scale=1">
                    <title>JS Client</title>
                    <base href="/">
                    <link rel="preconnect" href="https://fonts.googleapis.com">
                    <link rel="stylesheet" href="/experimental.css" />
                    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                    <link rel="stylesheet" href="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/styles/atom-one-dark.min.css">
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/highlight.min.js"></script>
                    <script src="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.9.0/languages/kotlin.min.js"></script>
                    ${context.dynamicCss.headElements.joinToString("\n")}
                    <style>
                        /* http://meyerweb.com/eric/tools/css/reset/
                   v2.0 | 20110126
                   License: none (public domain)
                */

                        html, body, div, span, applet, object, iframe,
                        h1, h2, h3, h4, h5, h6, p, blockquote, pre,
                        a, abbr, acronym, address, big, cite, code,
                        del, dfn, em, img, ins, kbd, q, s, samp,
                        small, strike, strong, sub, sup, tt, var,
                        b, u, i, center,
                        dl, dt, dd, ol, ul, li,
                        fieldset, form, label, legend,
                        table, caption, tbody, tfoot, thead, tr, th, td,
                        article, aside, canvas, details, embed,
                        figure, figcaption, footer, header, hgroup,
                        menu, nav, output, ruby, section, summary,
                        time, mark, audio, video {
                            margin: 0;
                            padding: 0;
                            border: 0;
                            font-size: 100%;
                            font: inherit;
                            vertical-align: baseline;
                        }

                        /* HTML5 display-role reset for older browsers */
                        article, aside, details, figcaption, figure,
                        footer, header, hgroup, menu, nav, section {
                            display: block;
                        }

                        body {
                            line-height: 1;
                        }

                        ol, ul {
                            list-style: none;
                        }

                        blockquote, q {
                            quotes: none;
                        }

                        blockquote:before, blockquote:after,
                        q:before, q:after {
                            content: '';
                            content: none;
                        }

                        table {
                            border-collapse: collapse;
                            border-spacing: 0;
                        }
                        
                        ${context.dynamicCss.emit()}

                    </style>
                </head>
                <body>
            """.trimIndent())
            writer.children[0].native.render(this)
            appendLine("""
                </body>
                </html>
            """.trimIndent())
        }
    }
}