package com.lightningkite.kiteui.views.direct

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
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds


actual class Select actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "select"
        native.classes.add("editable")
    }

    private var _accessibilityRenderedValue: String? = null  // by Claude
    private var _accessibilitySetter: ((String) -> Unit)? = null  // by Claude

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
            _accessibilityRenderedValue = render(newValue)  // by Claude
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
        _accessibilitySetter = { text ->  // by Claude
            @Suppress("UNCHECKED_CAST")
            val item = list.firstOrNull { render(it) == text }
                ?: throw IllegalArgumentException("No option matching '$text'")
            launch { edits set item }
        }
    }

    // by Claude
    override var accessibilityValue: String?
        get() = _accessibilityRenderedValue
        set(value) {
            val setter = _accessibilitySetter ?: throw IllegalStateException("Select not bound")
            setter(value ?: throw IllegalArgumentException("Cannot set null on Select"))
        }

    // by Claude - select supports click and setValue
    override val accessibilityActions: Set<String> get() = setOf("click", "setValue")
    override fun performAccessibilityAction(action: String, value: String?): String? = when (action) {
        "click" -> null  // click is handled at the native level
        "setValue" -> { accessibilityValue = value; null }
        else -> super.performAccessibilityAction(action, value)
    }

    actual var enabled: Boolean
        get() = !(native.attributes.disabled ?: false)
        set(value) { native.attributes.disabled = !value }

    override fun applyTheme(theme: ThemeAndBack) {
        val p = prevThemeClass
        val newClass = context.kiteUiCss.themeInteractive(theme.theme)
        super.applyTheme(theme)
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