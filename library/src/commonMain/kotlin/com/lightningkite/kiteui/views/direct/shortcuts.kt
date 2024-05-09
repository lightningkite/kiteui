package com.lightningkite.kiteui.views.direct

import ViewWriter
import com.lightningkite.kiteui.contains
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.Action
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*

@ViewDsl
inline fun RView.h1(crossinline setup: HeaderView.()->Unit = {}) = header(1, setup)
@ViewDsl
inline fun RView.h2(crossinline setup: HeaderView.()->Unit = {}) = header(2, setup)
@ViewDsl
inline fun RView.h3(crossinline setup: HeaderView.()->Unit = {}) = header(3, setup)
@ViewDsl
inline fun RView.h4(crossinline setup: HeaderView.()->Unit = {}) = header(4, setup)
@ViewDsl
inline fun RView.h5(crossinline setup: HeaderView.()->Unit = {}) = header(5, setup)
@ViewDsl
inline fun RView.h6(crossinline setup: HeaderView.()->Unit = {}) = header(6, setup)
@ViewDsl
fun RView.h1(text: String) = h1 { content = text }
@ViewDsl
fun RView.h2(text: String) = h2 { content = text }
@ViewDsl
fun RView.h3(text: String) = h3 { content = text }
@ViewDsl
fun RView.h4(text: String) = h4 { content = text }
@ViewDsl
fun RView.h5(text: String) = h5 { content = text }
@ViewDsl
fun RView.h6(text: String) = h6 { content = text }
@ViewDsl
fun RView.text(text: String) = text { content = text }
@ViewDsl
fun RView.subtext(text: String) = subtext { content = text }

// TODO: Button with working indicator

fun RView.confirmDanger(
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
fun RView.alert(
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


fun Button.onClickAssociatedField(
    field: TextField,
    title: String = "Submit",
    icon: Icon = Icon.done,
    action: suspend () -> Unit
) {
    field.action = Action(
        title = title,
        icon = icon,
        onSelect = {
            this@onClickAssociatedField.calculationContext.launchManualCancel(action)
        }
    )
    onClick { action() }
}
fun Button.onClickAssociatedField(
    field: NumberField,
    title: String = "Submit",
    icon: Icon = Icon.done,
    action: suspend () -> Unit
) {
    field.action = Action(
        title = title,
        icon = icon,
        onSelect = {
            this@onClickAssociatedField.calculationContext.launchManualCancel(action)
        }
    )
    onClick { action() }
}
