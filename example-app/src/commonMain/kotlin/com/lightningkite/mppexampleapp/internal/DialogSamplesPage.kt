package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.direct.confirmDanger
import com.lightningkite.kiteui.views.l2.applySafeInsets
import com.lightningkite.kiteui.views.l2.coordinatorFrame
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.l2.label
import com.lightningkite.kiteui.views.themed
import com.lightningkite.reactive.core.Signal

@Routable("sample/dialog")
object DialogSamplesPage : Page {
    override fun ElementWriter.CanAddTheme.render(): Unit = run {
        col {
            h1 { content = "Dialog Samples" }

            button {
                h6("Confirm Test")
                onClick {
                    context.confirmDanger("Delete", body = "Delete this item?") {
                        println("Delete!")
                    }
                }
            }
            button {
                h6 { content = "Launch Test Dialog" }
                onClick {
                    context.dialog { close ->
                        card.col {
                            h2 { content = "Sample Dialog" }
                            text { content = "This is a sample dialog." }
                            row {
                                card.button {
                                    text { content = "OK" }
                                    onClick { close() }
                                }
                            }
                        }
                    }
                }
            }
            button {
                h6 { content = "Launch edit dialog" }
                onClick {
                    context.dialog { close ->
                        col {
                            text("INPUT TIME!")
                            field("Field") {
                                textInput {  }
                            }
                            button {
                                text("OK")
                                onClick { close() }
                            }
                        }
                    }
                }
            }
            button {
                h6 { content = "Launch Test Bottom Sheet Old" }
                onClick {
                    openBottomSheet {
                        col {
                            h2("Bottom sheet")
                            text("bottom text")
                        }
                    }
                }
            }
            button {
                h6 { content = "Launch Test bottomSheet" }
                onClick {
                    context.coordinatorFrame.bottomSheet(startState = BottomSheetState.PARTIALLY_EXPANDED) {
                        themed(DialogSemantic).col {
                            applySafeInsets()
                            centered.coordinatorDragHandle()
                            button {
                                text("Close")
                                onClick { it.close() }
                            }
                            h2("Bottom sheet")
                            text("bottom text")
                        }
                    }
                }
            }
            button {
                h6 { content = "Launch Test leftSlidingPanel" }
                onClick {
                    context.coordinatorFrame.leftSlidingPanel {
                        themed(DialogSemantic).col {
                            button {
                                text("Close")
                                onClick { it.close() }
                            }
                            h2("Bottom sheet")
                            text("bottom text")
                        }
                    }
                }
            }
            button {
                h6 { content = "Launch Test rightSlidingPanel" }
                onClick {
                    context.coordinatorFrame.rightSlidingPanel {
                        themed(DialogSemantic).col {
                            button {
                                text("Close")
                                onClick { it.close() }
                            }
                            h2("Bottom sheet")
                            text("bottom text")
                        }
                    }
                }
            }
        }
    }
}