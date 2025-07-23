package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Build
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.WeakReference
import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.checkLeakAfterDelay
import com.lightningkite.kiteui.leaks
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ScreenTransitions
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.requestFile
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewModifiable
import com.lightningkite.readable.Constant
import com.lightningkite.readable.Property
import com.lightningkite.readable.Readable
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.animateIn
import com.lightningkite.kiteui.views.animateOut
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.RowOrCol
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.coordinatorDragHandle
import com.lightningkite.kiteui.views.direct.dismissBackground
import com.lightningkite.kiteui.views.direct.h1
import com.lightningkite.kiteui.views.direct.h2
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.separator
import com.lightningkite.kiteui.views.direct.sizeConstraints
import com.lightningkite.kiteui.views.direct.space
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.important
import com.lightningkite.kiteui.views.l2.applySafeInsets
import com.lightningkite.kiteui.views.l2.coordinatorFrame
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.overlayWriter
import com.lightningkite.kiteui.views.withoutAnimation
import com.lightningkite.mppexampleapp.docs.article
import com.lightningkite.mppexampleapp.docs.example
import com.lightningkite.readable.onRemove
import kotlin.time.Duration.Companion.milliseconds

@Routable("/")
class HomePage: Page {
    override val title: Readable<String> get() = Constant("KiteUI")
    override fun ViewWriter.render(): ViewModifiable = run {
        return article {
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
            text("- Semantic theming - style and content should be separated and bridged via meaning, not result.  It's not a 'red' button, it's a button that performs a dangerous action.")
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
            h1("Getting Started")
            text("TODO")
            space()
            text("Version: ${Build.version}")
        }
    }
}
