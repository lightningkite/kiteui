package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.WidgetOption
import com.lightningkite.kiteui.reactive.Readable
import com.lightningkite.kiteui.reactive.Writable
import com.lightningkite.kiteui.reactive.await
import com.lightningkite.kiteui.reactive.awaitOnce
import com.lightningkite.kiteui.views.ViewDsl
import ViewWriter
import com.lightningkite.kiteui.views.launch
import com.lightningkite.kiteui.views.reactiveScope
import org.w3c.dom.HTMLSelectElement

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NSelect = HTMLSelectElement

@ViewDsl
actual inline fun ViewWriter.selectActual(crossinline setup: Select.() -> Unit): Unit =
    themedElementEditable<NSelect>("select") { setup(Select(this)) }
//    themedElementClickable<NSelect>("select") { setup(Select(this)) }

actual fun <T> Select.bind(
    edits: Writable<T>,
    data: Readable<List<T>>,
    render: (T) -> String
) {
    var list: List<T> = listOf()
    reactiveScope {
        list = data.await()
        native.__resetContentToOptionList(
            list.mapIndexed { index, t ->
                WidgetOption(index.toString(), render(t))
            },
            list.indexOf(edits.awaitOnce()).toString()
        )
    }
    var alreadyHandled = false
    reactiveScope {
        val newValue = edits.await()
        val list = data.await()
        if(alreadyHandled) return@reactiveScope
        alreadyHandled = true
        native.__selectOption(list.indexOf(newValue).toString())
        alreadyHandled = false
    }
    native.onchange = {
        launch {
            if(alreadyHandled) return@launch
            alreadyHandled = true
            native.value.toIntOrNull()?.let { edits set list[it] }
            alreadyHandled = false
        }
    }
}