package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("sample/dialog")
object DialogSamplesPage : Page {
    override fun ViewWriter.render(): ViewModifiable = run {
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
                h6 { content = "Launch Test Bottom Sheet" }
                onClick {
                    openBottomSheet {
                        col {
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
    override fun ViewWriter.render(): ViewModifiable = run {
        dismissBackground {
            centered - card - col {
                h2 { content = "Sample Dialog" }
                text { content = "This is a sample dialog." }
                row {
                    button {
                        text { content = "OK" }
                        onClick { dialogPageNavigator.dismiss() }
                    } in card
                }
            }
        }
    }
}