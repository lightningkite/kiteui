package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.models.Icon
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.reactive.Action
import com.lightningkite.kiteui.views.*


actual class DismissBackground actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        setOnClickListener {
            dialogScreenNavigator.clear()
        }
    }
    actual fun onClick(action: suspend () -> Unit) {
        val action = Action("Dismiss", Icon.close, action = action)
        native.setOnClickListener { _ ->
            action.startAction(this)
        }
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        val color = theme.background.closestColor()
        native.setBackgroundColor(color.applyAlpha(0.5f).toInt())
    }
    override fun postSetup() {
        super.postSetup()
        children.forEach { it.native.isClickable = true }
    }
}