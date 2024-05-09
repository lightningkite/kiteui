package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.content.res.ColorStateList
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.lightningkite.kiteui.launch
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.views.*


actual class DismissBackground actual constructor(context: RContext): RView(context) {
    override val native = FrameLayout(context.activity).apply {
        setOnClickListener {
            navigator.clear()
        }
    }
    actual fun onClick(action: suspend () -> Unit) {
        native.setOnClickListener { _ ->
            launch { action() }
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