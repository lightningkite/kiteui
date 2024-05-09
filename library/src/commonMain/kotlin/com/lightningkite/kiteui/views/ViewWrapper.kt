package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RView

object ViewWrapper

inline operator fun ViewWrapper.contains(view: RView): Boolean {
    return true
}
inline operator fun ViewWrapper.contains(unit: Unit): Boolean {
    return true
}

inline operator fun ViewWrapper.contains(boolean: Boolean): Boolean {
    return true
}
