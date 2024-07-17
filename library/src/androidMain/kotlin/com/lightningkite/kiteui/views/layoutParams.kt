package com.lightningkite.kiteui.views

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.HorizontalScrollView
import android.widget.LinearLayout
import android.widget.ScrollView
import androidx.core.widget.NestedScrollView
import com.lightningkite.kiteui.views.direct.DesiredSizeView
import com.lightningkite.kiteui.views.direct.SimplifiedLinearLayout
import com.lightningkite.kiteui.views.direct.SlightlyModifiedLinearLayout

val RView.lparams: ViewGroup.LayoutParams
    get() {
        val parent = parent
        val parentElement = parent?.native
        if (parentElement is DesiredSizeView) return this.parent!!.lparams
        if (native.layoutParams != null) return native.layoutParams
        val newParams = parent?.defaultLayoutParams() ?: run {
            println("No parent to identify LayoutParams type for a ${this::class.qualifiedName}")
            ViewGroup.MarginLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        native.layoutParams = newParams
        return newParams
    }