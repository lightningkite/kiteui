package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.KeyCodes
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.core.*
import com.lightningkite.kiteui.views.AiDriver


public actual class RadioToggleButton actual constructor(context: ElementContext) : NativeInteractiveContainerElement(context) {
    override val driverValue: String? get() = radioToggleDriverValue()
    override val driverActions: AiDriver.Actions get() = super.driverActions + radioToggleDriverActions()

    internal val input = FutureElement().apply {
        themeChoice += ClickableSemantic
        tag = "input"
        attributes.type = "radio"
        classes.add("checkResponsive")
        // Keep the native radio in the accessibility tree and keyboard-focusable, but hide it
        // visually (display:none / visibility:hidden would remove it from the a11y tree).
        setStyleProperty("position", "absolute")
        setStyleProperty("opacity", "0")
        setStyleProperty("width", "1px")
        setStyleProperty("height", "1px")
        setStyleProperty("margin", "-1px")
        setStyleProperty("overflow", "hidden")
        setStyleProperty("clip-path", "inset(50%)")
    }

    init {
        native.tag = "label"
        native.classes.add("kiteui-stack")
        native.classes.add("checkResponsive")
        native.classes.add("clickable")
        native.appendChild(input)
        input.addEventListener("keydown", { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                ev.preventDefault()
                input.click()
            }
        })
    }

    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        Frame.internalAddChildStack(this, index, element)
    }

    public actual val checked: MutableReactiveValue<Boolean> = input.vprop(
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
        }

    init {
        checked.addListener {
            if (checked.value) {
                native.classes.add("checked")
            } else {
                native.classes.remove("checked")
            }
        }
    }
}
