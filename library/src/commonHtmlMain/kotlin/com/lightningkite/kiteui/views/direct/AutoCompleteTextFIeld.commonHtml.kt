package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.*
import com.lightningkite.readable.ImmediateWritable
import com.lightningkite.readable.Writable
import com.lightningkite.kiteui.views.*


actual class AutoCompleteTextField actual constructor(context: RContext) : RViewWithAction(context) {
    init {
        native.tag = "input"
        native.classes.add("editable")
    }
    actual val content: ImmediateWritable<String> = native.vprop("input", { attributes.valueString ?: "" }, { attributes.valueString = it })
    actual var keyboardHints: KeyboardHints = KeyboardHints()
        set(value) {
            field = value
            native.applyKeyboardHints(value)
        }
    init {
        native.addEventListener("keyup") { ev ->
            ev as KeyboardEvent
            if (ev.code == KeyCodes.enter) {
                action?.startAction(this)
            }
        }
    }
    inline var hint: String
        get() = native.attributes.placeholder ?: ""
        set(value) {
            native.attributes.placeholder = value
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
            native.style.fontSize = value.value.toString()
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