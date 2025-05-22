package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.AriaRole
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.kiteui.views.*


actual class RadioToggleButton actual constructor(context: RContext) : RView(context), InputAccessibility {
    val input = FutureElement().apply {
        themeChoice += ClickableSemantic
        tag = "input"
        attributes.type = "radio"
        classes.add("checkResponsive")
        attributes.hidden = true
        style.display = "none"
        ariaRole = AriaRole.Radio
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
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }

    actual val checked: ImmediateWritable<Boolean> = input.vprop(
        "input",
        { attributes.checked == true },
        { value -> attributes.checked = value })
    init {
        checked.addListener {
            if(checked.value)
                native.classes.add("checked")
            else
                native.classes.remove("checked")
            native.setAttribute("aria-checked", checked.value.toString())
        }
    }

    actual inline var enabled: Boolean
        get() = input.attributes.disabled != true
        set(value) {
            input.attributes.disabled = !value
        }

    override var ariaRequired: Boolean? = null
        set(value) {
            field = value
            if (value == null) {
                input.setAttribute("aria-required", null)
            } else {
                input.setAttribute("aria-required", value.toString())
            }
        }
}
