package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*

@ViewDsl
fun ViewWriter.h1(text: String) = h1 { content = text }
@ViewDsl
fun ViewWriter.h2(text: String) = h2 { content = text }
@ViewDsl
fun ViewWriter.h3(text: String) = h3 { content = text }
@ViewDsl
fun ViewWriter.h4(text: String) = h4 { content = text }
@ViewDsl
fun ViewWriter.h5(text: String) = h5 { content = text }
@ViewDsl
fun ViewWriter.h6(text: String) = h6 { content = text }
@ViewDsl
fun ViewWriter.text(text: String) = text { content = text }
@ViewDsl
fun ViewWriter.subtext(text: String) = subtext { content = text }

// TODO: Button with working indicator

fun ViewWriter.confirmDanger(
    title: String,
    body: String,
    actionName: String = "OK",
    action: suspend () -> Unit
) {
    navigator.dialog.navigate(object : Screen {
        override val title: Readable<String> = Constant(title)
        override fun ViewWriter.render() {
            dismissBackground {
                centered - card - col {
                    h2(title)
                    text(body)
                    row {
                        button {
                            h6("Cancel")
                            onClick {
                                navigator.dismiss()
                            }
                        }
                        button {
                            h6(actionName)
                            onClick {
                                action()
                                navigator.dismiss()
                            }
                        } in danger
                    }
                }
            }
        }
    })
}
fun ViewWriter.alert(
    title: String,
    body: String,
) {
    navigator.dialog.navigate(object : Screen {
        override val title: Readable<String> = Constant(title)
        override fun ViewWriter.render() {
            dismissBackground {
                centered - card - col {
//                    ignoreInteraction = false
                    h2(title)
                    text(body)
                    row {
                        button {
                            h6("OK")
                            onClick {
                                navigator.dismiss()
                            }
                        } in danger
                    }
                }
            }
        }
    })
}