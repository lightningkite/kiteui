package com.lightningkite.kiteui.views.direct


import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.invoke
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.l2.icon

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NRadioButton = FrameLayoutToggleButton

@ViewDsl
actual inline fun ViewWriter.radioButtonActual(crossinline setup: RadioButton.() -> Unit): Unit {
    transitionNextView = ViewWriter.TransitionNextView.Yes
    themeFromLast {
        it.copy(
            outline = it.foreground,
            outlineWidth = maxOf(it.outlineWidth, 1.dp),
            spacing = it.spacing / 4,
            cornerRadii = CornerRadii.RatioOfSize(0.5f),
            selected = { this },
            unselected = { this },
        )
    } - radioToggleButton {
        icon(Icon.done, "") {
            ::visible.invoke { checked.await() }
        }
        setup(RadioButton(this.native))
    }
}

actual inline var RadioButton.enabled: Boolean
    get() = native.enabled
    set(value) { native.enabled = value }
actual val RadioButton.checked: Writable<Boolean> get() =  native.checkedWritable