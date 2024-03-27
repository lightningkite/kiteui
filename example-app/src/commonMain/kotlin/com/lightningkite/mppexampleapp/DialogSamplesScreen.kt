package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.navigation.KiteUiScreen
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("sample/dialog")
object DialogSamplesScreen : KiteUiScreen {
    override fun ViewWriter.render() {
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
                    navigator.dialog.navigate(DialogSampleScreen1)
                }
            }
        }
    }
}

@Routable("sample/dialog/1") object DialogSampleScreen1: KiteUiScreen {
    override fun ViewWriter.render() {
        dismissBackground {
            centered - card - col {
                h2 { content = "Sample Dialog" }
                text { content = "This is a sample dialog." }
                row {
                    button {
                        text { content = "OK" }
                        onClick { navigator.dismiss() }
                    } in card
                }
            }
        }
    }
}