package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.InternalKiteUi
import com.lightningkite.kiteui.models.DismissSemantic
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.ThemeAndBack
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*


@InternalKiteUi
public actual class DismissBackground actual constructor(context: RContext): RView(context) {
    override val native: FrameLayout = FrameLayout(context.activity).apply {
        setOnClickListener {
            dialogPageNavigator.clear()
        }
    }
    public actual fun onClick(action: suspend () -> Unit) {
        val action = Action("Dismiss", Icon.close) { action() }
        native.setOnClickListener { _ ->
            action.startAction(this)
        }
    }

    override fun postSetup() {
        super.postSetup()
        children.forEach { it.native.isClickable = true }
    }

    override fun applyState(theme: ThemeAndBack): ThemeAndBack {
        return super.applyState(theme[DismissSemantic])
    }
}