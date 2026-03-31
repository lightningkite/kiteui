package com.lightningkite.kiteui.views

import android.view.View
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.debugMode
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.Element

var animationsEnabled: Boolean = true
actual val Element.areAnimationsEnabled: Boolean get() = animationsEnabled
actual inline fun Element.withoutAnimation(action: () -> Unit) = native.withoutAnimation(action)

inline fun View.withoutAnimation(action: () -> Unit) {
    if (!animationsEnabled) {
        action()
        return
    }
    try {
        animationsEnabled = false
        action()
    } finally {
        animationsEnabled = true
    }
}

inline fun View.debugPrint(get: () -> String) {
    if (debugMode && Element.Debugger.debugTarget?.native == this)
        Log.tag("viewDebugTarget").info(get())
}