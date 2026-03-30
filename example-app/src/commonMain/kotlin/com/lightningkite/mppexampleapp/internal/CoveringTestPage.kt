package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.*
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.models.rem
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.applySafeInsets
import com.lightningkite.kiteui.views.l2.children
import com.lightningkite.kiteui.views.l2.coordinatorFrame
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*

@Routable("covering-test")
object CoveringTestPage : Page {
    override val title: Reactive<String>
        get() = super.title

    override fun ElementWriter.CanAddTheme.render() {
        col {
            card.button {
                text("dialog")
                onClick {
                    context.dialog { close ->
                        sizeConstraints(width = 20.rem, height = 15.rem).col {
                            expanding.text("Heyo")
                            card.button {
                                text("Close")
                                onClick { close() }
                            }
                        }
                    }
                }
            }
            card.button {
                text("bottom sheet")
                onClick {
                    context.coordinatorFrame?.bottomSheet(blockBehind = false) { control ->
                        themed(DialogSemantic).col {
                            applySafeInsets()
                            centered.coordinatorDragHandle()
                            for (letter in 'A'..'C') {
                                card.text(letter.toString())
                            }
                            card.button {
                                text("Force close")
                                onClick {
                                    control.close()
                                }
                            }
                        }
                    }
                }
            }
            recyclerView {
                children(Constant(('A'..'Z').toList()), { it }) {
                    card.button {
                        text { ::content { it().toString() }}
                        onClick {
                            throw IllegalStateException("This should not be clickable.")
                        }
                    }
                }
            }
        }
    }
}
