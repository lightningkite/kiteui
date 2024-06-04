package com.lightningkite.kiteui.views.direct

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.lightningkite.kiteui.views.*

actual fun DismissBackground.onClick(action: suspend () -> Unit) {
    native.setOnClickListener { _ ->
        launch { action() }
    }
}

@Suppress("ACTUAL_WITHOUT_EXPECT")
actual typealias NDismissBackground = ViewGroup

@ViewDsl
actual inline fun ViewWriter.dismissBackgroundActual(crossinline setup: DismissBackground.() -> Unit) = element(SlightlyModifiedFrameLayout(context)) {
    handleTheme(this, foreground = { it, view ->
        view.setBackgroundColor(it.background.closestColor().darken(0.5f).applyAlpha(alpha = 0.5f).toInt())
    }) {
        setOnClickListener {
            navigator.clear()
        }
        setup(DismissBackground(this))
        listNViews().forEach {
            it.isClickable = true
        }
    }
}