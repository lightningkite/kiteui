package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


actual class DismissBackground actual constructor(context: RContext) : RView(context) {
    override val cannotBeCovered: Boolean get() = false
    init {
        native.tag = "div"
        native.classes.add("kiteui-stack")
        native.replaceEventListener("click") { dialogPageNavigator.clear() }
    }
    override fun internalAddChild(index: Int, view: RView) {
        super.internalAddChild(index, view)
        Frame.internalAddChildStack(this, index, view)
    }

    override fun addChild(view: RView) {
        super.addChild(view)
        view.native.addEventListener("click") { ev -> ev.stopImmediatePropagation() }
    }

    actual fun onClick(action: suspend () -> Unit): Unit {
        native.replaceEventListener("click") { launch { action() } }
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        return super.applyState(theme[DismissSemantic])
    }
}
