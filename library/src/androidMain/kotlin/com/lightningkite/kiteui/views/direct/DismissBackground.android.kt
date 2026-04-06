package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.ExperimentalKiteUi
import com.lightningkite.kiteui.OverrideOnly
import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*


@OptIn(ExperimentalKiteUi::class)
actual class DismissBackground actual constructor(context: ElementContext): NativeContainerElement(context) {
    init {
        themePipeline.add(ThemePipeline.Step.elementStyling, DismissSemantic)
    }

    override val native = FrameLayout(context.activity).apply {
        setOnClickListener {
            @Suppress("DEPRECATION")
            this@DismissBackground.context.dialogPageNavigator.clear()
        }
    }
    actual fun onClick(action: suspend () -> Unit) {
        val action = Action("Dismiss", Icon.close) { action() }
        native.setOnClickListener { _ ->
            action.startAction(this)
        }
    }

    @OptIn(OverrideOnly::class)
    override fun onStartup() {
        super.onStartup()
        children.forEach { it.underlyingNativeElement.native.isClickable = true }
    }
}