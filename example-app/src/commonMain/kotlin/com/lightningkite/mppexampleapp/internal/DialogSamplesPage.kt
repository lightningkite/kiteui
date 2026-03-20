package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.models.DialogSemantic
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.views.l2.applySafeInsets
import com.lightningkite.kiteui.views.l2.coordinatorFrame
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.kiteui.views.l2.field
import com.lightningkite.kiteui.views.themed

@Routable("sample/dialog")
object DialogSamplesPage : Page {
    override fun ViewWriter.render(): Unit = run {
        col {
            h1 { content = "Dialog Samples" }

            button {
                h6("Confirm Test")
                onClick {
                    confirmDanger("Delete", body = "Delete this item?") {
                        println("Delete!")
                    }
                }
            }
            button {
                h6 { content = "Launch Test Dialog" }
                onClick {
                    dialogPageNavigator.navigate(DialogSampleScreen1)
                }
            }
            button {
                h6 { content = "Launch edit dialog" }
                onClick {
                    dialog { close ->
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
                    coordinatorFrame!!.bottomSheet(startState = BottomSheetState.PARTIALLY_EXPANDED) {
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
                    coordinatorFrame!!.leftSlidingPanel {
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
                    coordinatorFrame!!.rightSlidingPanel {
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

@Routable("sample/dialog/1") object DialogSampleScreen1: Page {
    override fun ViewWriter.render(): Unit = run {
        dismissBackground {
            centered.card.col {
                h2 { content = "Sample Dialog" }
                text { content = "This is a sample dialog." }
                row {
                    card.button {
                        text { content = "OK" }
                        onClick { dialogPageNavigator.dismiss() }
                    }
                }
            }
        }
    }
}