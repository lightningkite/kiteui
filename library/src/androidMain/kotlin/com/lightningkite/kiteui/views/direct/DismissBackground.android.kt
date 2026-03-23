package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*


actual class DismissBackground actual constructor(context: ElementContext): NativeContainerElement(context) {
    init {
        elementSpecificTheming += ElementSpecificTheming { DismissSemantic }
    }

    override val native = FrameLayout(context.activity).apply {
        setOnClickListener {
            dialogPageNavigator.clear()
        }
    }
    actual fun onClick(action: suspend () -> Unit) {
        val action = Action("Dismiss", Icon.close) { action() }
        native.setOnClickListener { _ ->
            action.startAction(this)
        }
    }

    override fun startup() {
        super.startup()
        children.forEach { it.underlyingNativeElement.native.isClickable = true }
    }
}