package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.KeyCodes
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*


actual class RadioToggleButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    override val driverValue: String? get() = radioToggleDriverValue()
    override val driverActions get() = super.driverActions + radioToggleDriverActions()

    val input = FutureElement().apply {
        themeChoice += ClickableSemantic
        tag = "input"
        attributes.type = "radio"
        classes.add("checkResponsive")
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

    override var enabled: Boolean
        get() = (native.attributes.disabled != true || input.attributes.disabled != true)
        set(value) {
            input.attributes.disabled = !value
            input.setAttribute("aria-disabled", if (value) null else "true")
            native.attributes.disabled = !value
            native.setAttribute("aria-disabled", if (value) null else "true")
        }

    init {
        native.setAttribute("role", "radio")
        native.setAttribute("aria-checked", "false")
        checked.addListener {
            if (checked.value) {
                native.classes.add("checked")
                native.setAttribute("aria-checked", "true")
            } else {
                native.classes.remove("checked")
                native.setAttribute("aria-checked", "false")
            }
        }
    }
}
