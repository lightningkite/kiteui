package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.KeyCodes
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*


actual class ToggleButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    override val driverValue: String? get() = toggleDriverValue()
    override val driverActions get() = super.driverActions + toggleDriverActions()
    val input = FutureElement().apply {
        themeChoice += ClickableSemantic
        tag = "input"
        attributes.type = "checkbox"
        classes.add("checkResponsive")
        classes.add("kui")
        attributes.hidden = true
        style.display = "none"
    }

    init {
        native.tag = "label"
        native.classes.add("kiteui-stack")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
        native.attributes.tabIndex = 0
        native.addEventListener("keydown", { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.space || ev.code == KeyCodes.enter) {
                ev.preventDefault()
            }
        })
        native.addEventListener("keyup", { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.space || ev.code == KeyCodes.enter) {
                input.click()
                ev.preventDefault()
            }
        })
        native.appendChild(input)
    }
    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        Frame.internalAddChildStack(this, index, element)
    }

    actual val checked: MutableReactiveValue<Boolean> = input.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value }
    )

    init {
        checked.addListener {
            if (checked.value)
                native.classes.add("checked")
            else
                native.classes.remove("checked")
        }.also(::onRemove)
    }

    override var enabled: Boolean
        get() = input.attributes.disabled != true
        set(value) {
            input.attributes.disabled = !value
        }
}
