package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.Align
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.ElementWriter
import com.lightningkite.kiteui.views.card
import com.lightningkite.kiteui.views.centered
import com.lightningkite.kiteui.views.direct.button
import com.lightningkite.kiteui.views.direct.col
import com.lightningkite.kiteui.views.direct.h3
import com.lightningkite.kiteui.views.direct.onClick
import com.lightningkite.kiteui.views.direct.row
import com.lightningkite.kiteui.views.direct.shownWhen
import com.lightningkite.kiteui.views.direct.text
import com.lightningkite.kiteui.views.direct.toggleButton
import com.lightningkite.kiteui.views.expanding
import com.lightningkite.kiteui.views.important
import com.lightningkite.reactive.context.reactive
import com.lightningkite.reactive.core.BasicListenable
import com.lightningkite.reactive.core.Signal
import com.lightningkite.reactive.extensions.toggle

@Routable("/internal/test/repeat-modifiers")
object TestRepeatableModifiers : Page {
    val bool = Signal(true)

    override fun ElementWriter.CanAddTheme.render() {
        col {
            col {
                row {
                    centered.expanding.h3("shownWhen")
                    toggleButton {
                        text("Toggle")
                        checked bind bool
                    }
                }
                row {
                    expanding.shownWhen { bool() }.run {
                        repeat(5) {
                            card.text {
                                align = Align.Center
                                content = "Item ${it + 1}"
                            }
                        }
                    }
                }
                row {
                    val writer = expanding.shownWhen { bool() }
                    reactive {
                        rerunOn(bool)
                        clearChildren()
                        repeat(5) {
                            writer.card.text {
                                align = Align.Center
                                content = "Item ${it + 1}"
                            }
                        }
                    }
                }
            }
        }
    }
}