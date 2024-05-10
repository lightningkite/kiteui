package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.views.*


actual class AutoCompleteTextField actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    actual val content: Writable<String> = native.vprop("input", { attributes["value"] ?: "" }, { attributes["value"] = it })
    actual inline var keyboardHints: KeyboardHints
        get() = TODO()
        set(value) {
            native.attributes["type"] = when (value.type) {
                KeyboardType.Text -> "text"
                KeyboardType.Decimal -> "text"
                KeyboardType.Integer -> "text"
                KeyboardType.Phone -> "tel"
                KeyboardType.Email -> "text"
            }
            native.attributes["inputMode"] = when (value.type) {
                KeyboardType.Text -> "text"
                KeyboardType.Decimal -> "decimal"
                KeyboardType.Integer -> "numeric"
                KeyboardType.Phone -> "tel"
                KeyboardType.Email -> "email"
            }

            when (value.autocomplete) {
                AutoComplete.Email -> {
                    native.attributes["type"] = "email"
                    native.attributes["autocomplete"] = "email"
                }

                AutoComplete.Password -> {
                    native.attributes["type"] = "password"
                    native.attributes["autocomplete"] = "current-password"
                }

                AutoComplete.NewPassword -> {
                    native.attributes["type"] = "password"
                    native.attributes["autocomplete"] = "new-password"
                }

                AutoComplete.Phone -> {
                    native.attributes["autocomplete"] = "tel"
                }

                null -> {
                    native.attributes["autocomplete"] = "off"
                }
            }
        }
    actual var action: Action? = null
        set(value) {
            field = value
            native.events["keyup"] = if (value == null) null else { ev ->
                ev as KeyboardEvent
                if (ev.code == KeyCodes.enter) {
                    launchGlobal {
                        value.onSelect()
                    }
                }
            }
        }
    inline var hint: String
        get() = native.attributes["placeholder"] ?: ""
        set(value) {
            native.attributes["placeholder"] = value
        }
    var align: Align = Align.Start
        set(value) {
            native.style.textAlign = when (value) {
                Align.Start -> "start"
                Align.Center -> "center"
                Align.End -> "end"
                Align.Stretch -> "justify"
            }
        }
    var textSize: Dimension = 1.rem
        set(value) {
            field = value
            native.style.fontSize = value.value
        }

    actual var suggestions: List<String> = listOf()
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