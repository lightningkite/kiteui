package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyCodeWithModifiers
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CoroutineScope
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.QuartzCore.CADisplayLink
import platform.UIKit.UIApplication
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification
import platform.UIKit.UIScreen
import platform.darwin.NSObject
import platform.darwin.sel_registerName



actual object AppState {
    internal val _animationFrame = BasicListenable()
    actual val animationFrame: Listenable
        get() = _animationFrame
    private val handle = object: NSObject() {
        @ObjCAction
        fun onFrame() {
            _animationFrame.invokeAll()
        }
    }
    init {
        CADisplayLink.displayLinkWithTarget(handle, sel_registerName("onFrame")).addToRunLoop(NSRunLoop.currentRunLoop, forMode = NSRunLoopCommonModes)
    }
    internal val _windowInfo = Signal(WindowStatistics(
        width = Dimension(UIScreen.mainScreen.bounds.useContents { size.width }),
        height = Dimension(UIScreen.mainScreen.bounds.useContents { size.height }),
        density = UIScreen.mainScreen.scale.toFloat()
    ))
    actual val windowInfo: ReactiveValue<WindowStatistics>
        get() = _windowInfo
    val _inForeground = Signal(true)
    actual val inForeground: ReactiveValue<Boolean>
        get() = _inForeground
    actual val softInputOpen: ReactiveValue<Boolean> get() = _SoftInputOpen

    private var currentLockCount = 0
    actual fun keepScreenOn(scope: CoroutineScope) {
        if(currentLockCount++ == 0) {
            UIApplication.sharedApplication.idleTimerDisabled = true
        }
        scope.onRemove {
            if(--currentLockCount == 0) {
                UIApplication.sharedApplication.idleTimerDisabled = false
            }
        }
    }
    actual fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): ()->Unit = {}
}


private object _SoftInputOpen : ReactiveValue<Boolean>, MutableReactive<Boolean> {
    private val listeners = ArrayList<() -> Unit>()
    override var value: Boolean = false
        set(value) {
            if(field != value) {
                field = value
                listeners.invokeAllSafe()
            }
        }

    override fun addListener(listener: () -> Unit): () -> Unit {
        listeners.add(listener)
        return {
            val pos = listeners.indexOfFirst { it === listener }
            if (pos != -1) {
                listeners.removeAt(pos)
            }
        }
    }


    val observer: NSObject = object: NSObject() {
        @ObjCAction fun keyboardWillShowNotification() {
            value = true
        }
        @ObjCAction fun keyboardWillHideNotification() {
            value = false
        }
    }
    init {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = observer,
            selector = sel_registerName("keyboardWillShowNotification"),
            name = UIKeyboardWillShowNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = observer,
            selector = sel_registerName("keyboardWillHideNotification"),
            name = UIKeyboardWillHideNotification,
            `object` = null
        )
    }
    override suspend infix fun set(value: Boolean) {
        this.value = value
    }
}