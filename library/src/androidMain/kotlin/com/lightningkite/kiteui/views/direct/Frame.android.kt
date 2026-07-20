package com.lightningkite.kiteui.views.direct

import android.view.ViewGroup
import android.widget.FrameLayout
import com.lightningkite.kiteui.views.ElementContext
import com.lightningkite.kiteui.views.NativeContainerElement

public actual class Frame actual constructor(context: ElementContext) : NativeContainerElement(context) {
    override val native = FrameLayout(context.activity)
    override fun defaultLayoutParams(): ViewGroup.LayoutParams =
        FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
}