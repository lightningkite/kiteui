package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.reactive.Constant
import com.lightningkite.kiteui.reactive.Property
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.h2
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.scrolls
import com.lightningkite.kiteui.views.direct.separator
import com.lightningkite.kiteui.views.direct.sizeConstraints
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.kiteui.views.direct.stack
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.important
import com.lightningkite.mppexampleapp.docs.article
import com.lightningkite.mppexampleapp.docs.example
import kotlin.time.Duration.Companion.milliseconds

@Routable("/")
class HomeScreen: Screen {
    override val title: Readable<String> get() = Constant("KiteUI")
    override fun ViewWriter.render(){
        article {
            centered - h1("KiteUI - Beautiful by Default")
            separator()
            text("In KiteUI, styling is beautiful without effort.  No styling or manual CSS is required to get beautiful layouts.  Just how it should be.")
            space()
            centered - h2("Goals")
            separator()
            text("- Web first - the web version should be comparable or better than React in performance, and generate reasonably small binaries.")
            text("- Readable - the code should be extremely easy to read and have the minimal amount of syntactical cruft.")
            text("- Multiplatform - the apps should compile to Android and iOS without issue.")
            text("- Native - the apps should use the native UI system of their given platform.")
            text("- Extendable - using native per-platform components and code should be easy.")
            text("- Kotlin-first - we use Kotlin conventions everywhere possible.")
            text("- Declarative - encode meaning into the system front-to-back, not the 'how'.")
            text("- Semantic themeing - style and content should be separated and bridged via meaning, not result.  It's not a 'red' button, it's a button that performs a dangerous action.")
            space()
            h2("Quick Sample")
            example("""
                val number = Property(0)
                col {
                    text("Here is a basic counter:")
                    row {
                        expanding - centered - text { ::content { number().toString() } }
                        col {
                            important - button {
                                text("+")
                                action = Action("Increment", Icon.add, frequencyCap = 0.milliseconds) {
                                    number.value++
                                }
                            }
                            important - button {
                                text("-")
                                action = Action("Decrement", Icon.remove, frequencyCap = 0.milliseconds) {
                                    number.value--
                                }
                            }
                        }
                    }
                }
            """.trimIndent()) {
                val number = Property(0)
                col {
                    text("Here is a basic counter:")
                    row {
                        expanding - centered - text { ::content { number().toString() } }
                        col {
                            important - button {
                                text("+")
                                action = Action("Increment", Icon.add, frequencyCap = 0.milliseconds) {
                                    number.value++
                                }
                            }
                            important - button {
                                text("-")
                                action = Action("Decrement", Icon.remove, frequencyCap = 0.milliseconds) {
                                    number.value--
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}