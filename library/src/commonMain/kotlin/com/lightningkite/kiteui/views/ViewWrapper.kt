package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RView

public abstract class ViewWrapper {
    public abstract fun view(): RView?
    public companion object: ViewWrapper() {
        override fun view(): RView? = null
    }
}
