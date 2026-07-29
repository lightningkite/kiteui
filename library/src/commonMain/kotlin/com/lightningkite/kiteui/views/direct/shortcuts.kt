@file:OptIn(ExperimentalContracts::class)

package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.dialog
import com.lightningkite.reactive.context.ReactiveContext
import com.lightningkite.reactive.core.MutableReactive
import com.lightningkite.reactive.core.Reactive
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@ViewDsl
public inline fun ElementWriter.subtext(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += SubtextSemantic
        setup(this)
    }
}

@ViewDsl
public inline fun ElementWriter.h1(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H1Semantic
        setup(this)
    }
}

@ViewDsl
public inline fun ElementWriter.h2(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H2Semantic
        setup(this)
    }
}

@ViewDsl
public inline fun ElementWriter.h3(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H3Semantic
        setup(this)
    }
}

@ViewDsl
public inline fun ElementWriter.h4(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H4Semantic
        setup(this)
    }
}

@ViewDsl
public inline fun ElementWriter.h5(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H5Semantic
        setup(this)
    }
}

@ViewDsl
public inline fun ElementWriter.h6(crossinline setup: TextView.() -> Unit = {}): TextView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return text {
        themeChoice += HeaderSemantic + H6Semantic
        setup(this)
    }
}

@ViewDsl
public fun ElementWriter.h1(text: String, align: Align? = null): TextView = h1 {
    content = text
    this.align = align
}

@ViewDsl
public fun ElementWriter.h2(text: String, align: Align? = null): TextView = h2 {
    content = text
    this.align = align
}

@ViewDsl
public fun ElementWriter.h3(text: String, align: Align? = null): TextView = h3 {
    content = text
    this.align = align
}

@ViewDsl
public fun ElementWriter.h4(text: String, align: Align? = null): TextView = h4 {
    content = text
    this.align = align
}

@ViewDsl
public fun ElementWriter.h5(text: String, align: Align? = null): TextView = h5 {
    content = text
    this.align = align
}

@ViewDsl
public fun ElementWriter.h6(text: String, align: Align? = null): TextView = h6 {
    content = text
    this.align = align
}

@ViewDsl
public fun ElementWriter.text(text: String, align: Align? = null): TextView = text {
    content = text
    this.align = align
}

@ViewDsl
public fun ElementWriter.subtext(text: String, align: Align? = null): TextView = subtext {
    content = text
    this.align = align
}

@ViewDsl
public fun ElementWriter.checkbox(checked: MutableReactive<Boolean>): Checkbox = checkbox { this.checked bind checked }

@ViewDsl
public fun ElementWriter.radioButton(checked: MutableReactive<Boolean>): RadioButton = radioButton { this.checked bind checked }

@ViewDsl
public fun ElementWriter.progressBar(ratio: Reactive<Float>): ProgressBar = progressBar { ::ratio bind ratio }

inline fun <T> ElementWriter.swapping(
    crossinline transition: (T) -> ScreenTransition = { ScreenTransition.Fade },
    crossinline current: ReactiveContext.() -> T,
    crossinline views: ViewWriter.(T) -> Unit
): SwapView {
    return swapView {
        swapping(transition, current, views)
    }
}


@ViewDsl
public inline fun ElementWriter.icon(icon: Icon, description: String, setup: IconView.() -> Unit = {}): IconView {
    contract { callsInPlace(setup, InvocationKind.EXACTLY_ONCE) }
    return icon {
        source = icon
        this.description = description
        setup(this)
    }
}


// TODO: Button with working indicator

public fun ElementContext.confirmDanger(
    title: String,
    body: String,
    actionName: String = "OK",
    cancelName: String = "Cancel",
    action: suspend () -> Unit
) {
    dialog(true) { closer ->
        col {
            h2(title)
            text(body)
            row {
                expanding.buttonTheme.button {
                    centered.text(cancelName)
                    onClick {
                        closer()
                    }
                }
                expanding.danger.buttonTheme.button {
                    centered.text(actionName)
                    onClick(actionName) {
                        action()
                        closer()
                    }
                }
            }
        }
    }
}

public fun ElementContext.alert(
    title: String,
    body: String,
) {
    dialog { closer ->
        col {
            h2(title)
            text(body)
            row {
                expanding.danger.button {
                    centered.h6("OK")
                    onClick {
                        closer()
                    }
                }
            }
        }
    }
}


@Deprecated("Use a shared action instead")
public fun Button.onClickAssociatedField(
    field: TextInput,
    title: String = "Submit",
    icon: Icon = Icon.done,
    action: suspend () -> Unit
) {
    val action = Action(
        title = title,
        icon = icon,
    ) { action() }
    field.action = action
    this.action = action
}

@Deprecated("Use a shared action instead")
public fun Button.onClickAssociatedField(
    field: NumberInput,
    title: String = "Submit",
    icon: Icon = Icon.done,
    action: suspend () -> Unit
) {
    val action = Action(
        title = title,
        icon = icon,
    ) { action() }
    field.action = action
    this.action = action
}
