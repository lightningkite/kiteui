package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.afterTimeout
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.WindowStatistics
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.cinterop.useContents
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSRunLoop
import platform.Foundation.NSRunLoopCommonModes
import platform.QuartzCore.CADisplayLink
import platform.UIKit.UIKeyboardWillHideNotification
import platform.UIKit.UIKeyboardWillShowNotification
import platform.UIKit.UIScreen
import platform.darwin.NSObject
import platform.darwin.sel_registerName


@OptIn(ExperimentalForeignApi::class)
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
    internal val _windowInfo = Property(WindowStatistics(
        width = Dimension(UIScreen.mainScreen.bounds.useContents { size.width }),
        height = Dimension(UIScreen.mainScreen.bounds.useContents { size.height }),
        density = UIScreen.mainScreen.scale.toFloat()
    ))
    actual val windowInfo: ImmediateReadable<WindowStatistics>
        get() = _windowInfo
    internal val _inForeground = Property(true)
    actual val inForeground: ImmediateReadable<Boolean>
        get() = _inForeground
    actual val softInputOpen: ImmediateReadable<Boolean> get() = _SoftInputOpen
}

@OptIn(ExperimentalForeignApi::class)
@Suppress("ACTUAL_WITHOUT_EXPECT")
private object _SoftInputOpen : ImmediateReadable<Boolean>, Writable<Boolean> {
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

    @OptIn(BetaInteropApi::class)
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