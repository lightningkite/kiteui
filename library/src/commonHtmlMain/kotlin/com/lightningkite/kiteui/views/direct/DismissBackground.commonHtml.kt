package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


actual class DismissBackground actual constructor(context: RContext) : RView(context) {
    init {
        native.tag = "div"
        native.classes.add("dismissBackground")
        native.classes.add("kiteui-stack")
        native.replaceEventListener("click") { dialogScreenNavigator.clear() }
    }
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Stack.internalAddChildStack(this, index, view)
    }

    override fun addChild(view: RView) {
        super.addChild(view)
        view.native.addEventListener("click") { ev -> ev.stopImmediatePropagation() }
    }

    actual fun onClick(action: suspend () -> Unit): Unit {
        native.replaceEventListener("click") { launch { action() } }
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        native.classes.removeAll { it.startsWith("t-") }
        native.classes.addAll(context.kiteUiCss.themeInteractive(theme))

        native.setStyleProperty("--parentSpacing", parentSpacing.value)
    }
}
