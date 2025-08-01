package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.signal.ImmediateWritable
import com.lightningkite.signal.Writable
import com.lightningkite.kiteui.views.*


public actual class AutoCompleteTextField public actual constructor(context: RContext) : RViewWithAction(context) {
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    public actual val content: ImmediateWritable<String> = native.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
    public actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            native.attributes.type = when (value.type) {
                KeyboardType.Text -> "text"
                KeyboardType.Decimal -> "text"
                KeyboardType.Integer -> "text"
                KeyboardType.Phone -> "tel"
                KeyboardType.Email -> "text"
            }
            native.attributes.inputMode = when (value.type) {
                KeyboardType.Text -> "text"
                KeyboardType.Decimal -> "decimal"
                KeyboardType.Integer -> "numeric"
                KeyboardType.Phone -> "tel"
                KeyboardType.Email -> "email"
            }

            when (value.autocomplete) {
                AutoComplete.Email -> {
                    native.attributes.type = "email"
                    native.attributes.autocomplete = "email"
                }

                AutoComplete.Password -> {
                    native.attributes.type = "password"
                    native.attributes.autocomplete = "current-password"
                }

                AutoComplete.NewPassword -> {
                    native.attributes.type = "password"
                    native.attributes.autocomplete = "new-password"
                }

                AutoComplete.Phone -> {
                    native.attributes.autocomplete = "tel"
                }
                AutoComplete.OneTimeCode, null -> {
                    native.attributes.autocomplete = "off"
                }
            }
        }
    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
    }
    public inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
        }
    public var align: Align = Align.Start
        set(value) {
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    public var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value.toString()
        }

    public actual var suggestions: List<String> = listOf()
        set(value) {
            field = value
            // TODO
//            val listId = native.attributes.get("list") ?: run {
//                val newId = "datalist" + Random.nextInt(0, Int.MAX_VALUE)
//                document.body!!.appendChild((document.createElement("datalist") as HTMLDataListElement).apply {
//                    id = newId
//                })
//                native.setAttribute("list", newId)
//                newId
//            }
//            document.getElementById(listId)?.let { it as? HTMLElement }?.apply {
//                __resetContentToOptionList(value.map { WidgetOption(it, it) }, this@suggestions.native.value)
//            }
        }
}