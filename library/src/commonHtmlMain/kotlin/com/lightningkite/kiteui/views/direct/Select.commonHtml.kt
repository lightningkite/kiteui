package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import kotlin.time.Duration.Companion.milliseconds


actual class Select actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "select"
        native.classes.add("editable")
    }

    actual fun <T> bind(
        edits: Writable<T>,
        data: Readable<List<T>>,
        render: (T) -> String
    ) {
        var list: List<T> = listOf()
        reactiveScope {
            list = data()
            val v = edits()
            native.clearChildren()
            list.mapIndexed { index, it ->
                native.appendChild(FutureElement().apply {
                    tag = "option"
                    attributes.valueString = index.toString()
                    content = render(it)
                    attributes.selected = (it == v)
                })
            }
        }
        var alreadyHandled = false
        reactiveScope {
            val newValue = edits()
            val list = data()
            if (alreadyHandled) return@reactiveScope
            alreadyHandled = true
            val index = list.indexOf(newValue).toString()
            native.children.find { it.attributes.valueString == index }
            alreadyHandled = false
        }
        val setAction = Action("Set Value", Icon.send, frequencyCap = 0.milliseconds, ignoreRetryWhileRunning = true) {
            if(alreadyHandled) return@Action
            alreadyHandled = true
            native.attributes.valueString?.toIntOrNull()?.let { edits set list[it] }
            alreadyHandled = false
        }
        native.addEventListener("change") {
            setAction.startAction(this)
        }
    }

    actual var enabled: Boolean
        get() = !(native.attributes.disabled ?: false)
        set(value) { native.attributes.disabled = !value }
}

//fun HTMLElement.__resetContentToOptionList(options: List<WidgetOption>, selected: String) {
//    innerHTML = ""
//    for (item in options) appendChild((document.createElement("option") as HTMLOptionElement).apply {
//        this.value = item.key
//        this.innerText = item.display
//        this.selected = item.key == selected
//    })
//}
//fun HTMLElement.__selectOption(selected: String) {
//    children.let { (0..<it.length).map { index -> it.get(index) } }.forEach {
//        if(it is HTMLOptionElement) {
//            it.selected = it.value == selected
//        }
//    }
//}