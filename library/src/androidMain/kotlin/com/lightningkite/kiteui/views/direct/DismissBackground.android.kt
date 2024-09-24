package com.lightningkite.kiteui.views.direct

import android.widget.FrameLayout
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.launchManualCancel
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.dialogScreenNavigator
import com.lightningkite.kiteui.navigation.screenNavigator
import com.lightningkite.kiteui.views.*


actual class DismissBackground actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        setOnClickListener {
            dialogScreenNavigator.clear()
        }
    }
    actual fun onClick(action: suspend () -> Unit) {
        native.setOnClickListener { _ ->
            launchManualCancel { action() }
        }
    }

    override fun applyBackground(theme: Theme, fullyApply: Boolean) {
        native.setBackgroundColor(theme.background.closestColor().copy(alpha = 0.5f).toInt())
    }
    override fun postSetup() {
        super.postSetup()
        children.forEach { it.native.isClickable = true }
    }
}