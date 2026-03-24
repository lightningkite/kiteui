package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.FieldSemantic
import com.lightningkite.kiteui.models.ClickableSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlin.time.Duration.Companion.milliseconds


actual class Select actual constructor(context: RContext) : RView(context) {
    private var _driverSelectedDisplay: String? = null
    private var _driverSelectSetValue: (suspend (String) -> Unit)? = null
    override val driverValue: String? get() = _driverSelectedDisplay
    override val driverActions get() = super.driverActions + buildMap {
        _driverSelectSetValue?.let { setter -> put("setValue") { args: List<String> -> setter(args.joinToString(" ")); "OK" } }
    }
    init {
        native.tag = "select"
        native.classes.add("editable")
        native.classes.add("clickable")
    }

    actual fun <T> bind(
        edits: MutableReactive<T>,
        data: Reactive<List<T>>,
        render: (T) -> String
    ) {
        var list: List<T> = listOf()
        reactiveScope {
            list = data()
            val v = edits.state.getOrNull()
            native.clearChildren()
            list.mapIndexed { index, it ->
                native.appendChild(FutureElement().apply {
                    classes.add("kui")
                    tag = "option"
                    classes.add("checkResponsive")
                    attributes.valueString = index.toString()
                    content = render(it)
                    attributes.selected = (it == v)
                })
            }
        }
        var alreadyHandled = false
        reactiveScope {
            val newValue = edits()
            val list = data.state.getOrNull() ?: listOf()
            if (alreadyHandled) return@reactiveScope
            alreadyHandled = true
            val index = list.indexOf(newValue).toString()
            native.children.find { it.attributes.valueString == index }?.attributes?.selected = true
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
        // Driver support: track selected display and allow setValue
        reactiveScope {
            _driverSelectedDisplay = render(edits())
        }
        _driverSelectSetValue = { displayText ->
            val item = list.firstOrNull { render(it) == displayText }
                ?: throw com.lightningkite.kiteui.views.DriverActionException("No option matching '$displayText'")
            edits.set(item)
        }
    }

    actual var enabled: Boolean
        get() = !(native.attributes.disabled ?: false)
        set(value) { native.attributes.disabled = !value }

    override fun applyTheme(theme: ThemeAndBack) {
        val field = theme[FieldSemantic]
        val p = prevThemeClass
        val newClass = context.kiteUiCss.themeInteractive(field.theme)
        super.applyTheme(field)
        native.classes.add("transition")
        native.children.forEach { o ->
            p?.let { o.classes.remove(it) }
            o.classes.add(newClass)
        }
    }
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