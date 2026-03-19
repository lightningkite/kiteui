package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


actual class DismissBackground actual constructor(context: ElementContext) : NativeContainerElement(context) {
    init {
        themeChoice += DismissSemantic
        native.tag = "div"
        native.classes.add("kiteui-stack")
        native.replaceEventListener("click") { dialogPageNavigator.clear() }
    }
    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        Frame.internalAddChildStack(this, index, element)
    }

    override fun willAddChild(element: Element) {
        super.willAddChild(element)
        element.native.addEventListener("click") { ev -> ev.stopImmediatePropagation() }
    }

    actual fun onClick(action: suspend () -> Unit): Unit {
        native.replaceEventListener("click") { launch { action() } }
    }
}
