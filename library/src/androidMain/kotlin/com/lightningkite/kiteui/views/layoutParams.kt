package com.lightningkite.kiteui.views

import android.view.ViewGroup
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.views.direct.DesiredSizeView

val Element.lparams: ViewGroup.LayoutParams
    get() {
        val parent = parent
        val parentElement = parent?.native
        if (parentElement is DesiredSizeView) return this.parent!!.lparams
        if (native.layoutParams != null) return native.layoutParams
        val newParams = parent?.underlyingNativeElement?.defaultLayoutParams() ?: run {
            Log.warn("No parent to identify LayoutParams type for a ${this::class.qualifiedName}")
            ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        native.layoutParams = newParams
        return newParams
    }