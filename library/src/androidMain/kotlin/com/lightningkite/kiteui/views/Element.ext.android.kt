package com.lightningkite.kiteui.views

import android.view.View
import com.lightningkite.kiteui.Log
import com.lightningkite.kiteui.debugMode
import com.lightningkite.kiteui.viewDebugTarget
import com.lightningkite.kiteui.views.Element

public var animationsEnabled: Boolean = true
public actual val Element.areAnimationsEnabled: Boolean get() = animationsEnabled
public actual inline fun Element.withoutAnimation(action: () -> Unit): Unit = native.withoutAnimation(action)

public inline fun View.withoutAnimation(action: () -> Unit) {
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

public inline fun View.debugPrint(get: () -> String) {
    if (debugMode && Element.Debugger.debugTarget?.native == this)
        Log.tag("viewDebugTarget").info(get())
}