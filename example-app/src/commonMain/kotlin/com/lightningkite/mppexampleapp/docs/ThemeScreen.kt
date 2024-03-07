package com.lightningkite.mppexampleapp.docs

import com.lightningkite.rock.Routable
import com.lightningkite.rock.models.rem
import com.lightningkite.rock.reactive.*
import com.lightningkite.rock.views.*
import com.lightningkite.rock.views.direct.*


@Routable("docs/theme")
object ThemeScreen: DocScreen {
    override val covers: List<String> = listOf("theme", "Theme")

    override fun ViewWriter.render() {
        article {
            h1("Theme")
            text("The theme is a way to set the look and feel of the application.  It is a set of colors, fonts, and other properties that are used to style the application.")
            h2("Tweaking the Theme")
            text("The theme can be tweaked to change the look of the application.  This can be done by using the tweakTheme modifier.")
            example("""
                card - col {
                    text("This is the default color")
                    text("This is a different color") in tweakTheme { it.copy(foreground = Color(0.5f, 0f, 0f, 1f)) }
                }
            """.trimIndent()) {
                card - col {
                    text("This is the default color")
                    text("This is a different color")}
                }
            }
        }
    }