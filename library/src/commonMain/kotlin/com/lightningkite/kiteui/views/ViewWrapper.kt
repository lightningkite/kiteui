package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RView

abstract class ViewWrapper {
    abstract fun view(): RView?
    companion object: ViewWrapper() {
        override fun view(): RView? = null
    }
}
