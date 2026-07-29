package com.lightningkite.kiteui.views.direct

import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.views.*
import kotlinx.coroutines.launch


@OptIn(ExperimentalKiteUi::class)
public actual class DismissBackground actual constructor(context: ElementContext) : NativeContainerElement(context) {
    init {
        native.tag = "div"
        native.classes.add("kiteui-stack")
    }
    override fun nativeAddChild(index: Int, element: Element) {
        super.nativeAddChild(index, element)
        Frame.internalAddChildStack(this, index, element)
    }

    override fun nativeWillAddChild(element: Element) {
        element.native.addEventListener("click") { ev -> ev.stopImmediatePropagation() }
    }

    public actual fun onClick(action: suspend () -> Unit) {
        native.replaceEventListener("click") { launch { action() } }
    }

    init {
        themePipeline.add(
            ThemePipeline.Step.elementStyling,
            DismissSemantic
        )
    }
}
