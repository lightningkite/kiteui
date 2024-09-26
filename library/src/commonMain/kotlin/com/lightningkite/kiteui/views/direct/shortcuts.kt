package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.navigation.Screen
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlin.contracts.InvocationKind
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

@ViewDsl
@OptIn(ExperimentalContracts::class)
inline fun ViewWriter.subtext(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += SubtextSemantic
        setup(this)
    }
}

@ViewDsl
@OptIn(ExperimentalContracts::class)
inline fun ViewWriter.h1(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H1Semantic
        setup(this)
    }
}

@ViewDsl
@OptIn(ExperimentalContracts::class)
inline fun ViewWriter.h2(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H2Semantic
        setup(this)
    }
}

@ViewDsl
@OptIn(ExperimentalContracts::class)
inline fun ViewWriter.h3(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H3Semantic
        setup(this)
    }
}

@ViewDsl
@OptIn(ExperimentalContracts::class)
inline fun ViewWriter.h4(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H4Semantic
        setup(this)
    }
}

@ViewDsl
@OptIn(ExperimentalContracts::class)
inline fun ViewWriter.h5(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H5Semantic
        setup(this)
    }
}

@ViewDsl
@OptIn(ExperimentalContracts::class)
inline fun ViewWriter.h6(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H6Semantic
        setup(this)
    }
}

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
    dialogScreenNavigator.navigate(object : Screen {
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
                                screenNavigator.dismiss()
                            }
                        }
                        button {
                            h6(actionName)
                            onClick {
                                action()
                                screenNavigator.dismiss()
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
    dialogScreenNavigator.navigate(object : Screen {
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
                                screenNavigator.dismiss()
                            }
                        } in danger
                    }
                }
            }
        }
    })
}


fun Button.onClickAssociatedField(
    field: TextInput,
    title: String = "Submit",
    icon: Icon = Icon.done,
    action: suspend () -> Unit
) {
    var going = false
    suspend fun guarded() {
        if (going) return@guarded
        going = true
        try {
            action()
        } finally {
            going = false
        }
    }
    field.action = Action(
        title = title,
        icon = icon,
        onSelect = {
            this@onClickAssociatedField.launchManualCancel(::guarded)
        }
    )
    onClick(::guarded)
}

fun Button.onClickAssociatedField(
    field: NumberInput,
    title: String = "Submit",
    icon: Icon = Icon.done,
    action: suspend () -> Unit
) {
    var going = false
    suspend fun guarded() {
        if (going) return@guarded
        going = true
        try {
            action()
        } finally {
            going = false
        }
    }
    field.action = Action(
        title = title,
        icon = icon,
        onSelect = {
            this@onClickAssociatedField.launchManualCancel(::guarded)
        }
    )
    onClick(::guarded)
}
