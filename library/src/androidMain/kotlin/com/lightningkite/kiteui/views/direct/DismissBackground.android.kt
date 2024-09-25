package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.views.*


actual class DismissBackground actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        setOnClickListener {
            dialogScreenNavigator.clear()
        }
    }
    actual fun onClick(action: suspend () -> Unit) {
        native.setOnClickListener { _ ->
            launch { action() }
        }
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        val color = theme.background.closestColor()
        native.setBackgroundColor((if (fullyApply) color else color.withAlpha(0.5f)).toInt())
    }
    override fun postSetup() {
        super.postSetup()
        children.forEach { it.native.isClickable = true }
    }
}