package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*

@Routable("sample/dialog")
object DialogSamplesScreen : Screen {
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
                    dialogScreenNavigator.navigate(DialogSampleScreen1)
                }
            }
        }
    }
}

@Routable("sample/dialog/1") object DialogSampleScreen1: Screen {
    override fun ViewWriter.render() {
        dismissBackground {
            centered - card - col {
                h2 { content = "Sample Dialog" }
                text { content = "This is a sample dialog." }
                row {
                    button {
                        text { content = "OK" }
                        onClick { dialogScreenNavigator.dismiss() }
                    } in card
                }
            }
        }
    }
}