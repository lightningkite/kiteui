package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.awaitOnce
import com.lightningkite.kiteui.views.*


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
        println("BIND STARTED")
        reactiveScope {
            list = data.await()
            val v = edits.await()
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
            val newValue = edits.await()
            val list = data.await()
            if(alreadyHandled) return@reactiveScope
            alreadyHandled = true
            val index = list.indexOf(newValue).toString()
            native.children.find { it.attributes.valueString == index }
            alreadyHandled = false
        }
        native.addEventListener("change") {
            launch {
                if(alreadyHandled) return@launch
                alreadyHandled = true
                native.attributes.valueString?.toIntOrNull()?.let { edits set list[it] }
                alreadyHandled = false
            }
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