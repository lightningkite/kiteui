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



public actual object AppState {
    internal val _animationFrame = BasicListenable()
    public actual val animationFrame: Listenable
        get() = _animationFrame
    private val handle = object: NSObject() {
        @ObjCAction
        fun onFrame() {
            refreshWindowInfo()
            _animationFrame.invokeAll()
        }
    }

    /**
     * The window's current size, preferring the app's own window over the physical screen.
     *
     * On a device that can show two apps at once - Split View, Slide Over, Stage Manager - the
     * screen keeps its full size while the app gets a fraction of it, so reading the screen would
     * report a width the app does not have.
     */
    private fun currentWindowStatistics(): WindowStatistics {
        val bounds = UIApplication.sharedApplication.keyWindow?.bounds ?: UIScreen.mainScreen.bounds
        return WindowStatistics(
            width = Dimension(bounds.useContents { size.width }),
            height = Dimension(bounds.useContents { size.height }),
            density = UIScreen.mainScreen.scale.toFloat(),
        )
    }

    /**
     * Republishes [windowInfo] when the window has actually changed size.
     *
     * Driven from the display link that is already running rather than from a rotation
     * notification, because rotation is only one of the ways this changes: Split View, Slide Over,
     * Stage Manager resizing and a window moving between displays all do too, and several of them
     * report a stale size at notification time. Comparing two `WindowStatistics` values costs a
     * `CGRect` read per frame and cannot miss a cause.
     *
     * Before this, `_windowInfo` was written exactly once - at initialisation - and never again, so
     * every reader of it was pinned to whatever the launch orientation happened to be.
     */
    // internal rather than private so a test can drive it without a running display link, which a
    // test binary has no frames to produce.
    internal fun refreshWindowInfo() {
        val now = currentWindowStatistics()
        if (now != _windowInfo.value) _windowInfo.value = now
    }
    init {
        CADisplayLink.displayLinkWithTarget(handle, sel_registerName("onFrame")).addToRunLoop(NSRunLoop.currentRunLoop, forMode = NSRunLoopCommonModes)
    }
    internal val _windowInfo = Signal(WindowStatistics(
        width = Dimension(UIScreen.mainScreen.bounds.useContents { size.width }),
        height = Dimension(UIScreen.mainScreen.bounds.useContents { size.height }),
        density = UIScreen.mainScreen.scale.toFloat()
    ))
        // Kept as the screen rather than currentWindowStatistics(): at object-initialisation time
        // the app has no key window yet, and refreshWindowInfo() replaces this on the first frame.
    public actual val windowInfo: ReactiveValue<WindowStatistics>
        get() = _windowInfo
    public val _inForeground: Signal<Boolean> = Signal(true)
    public actual val inForeground: ReactiveValue<Boolean>
        get() = _inForeground
    public actual val softInputOpen: ReactiveValue<Boolean> get() = _SoftInputOpen

    private var currentLockCount = 0
    public actual fun keepScreenOn(scope: CoroutineScope) {
        if(currentLockCount++ == 0) {
            UIApplication.sharedApplication.idleTimerDisabled = true
        }
        scope.onRemove {
            if(--currentLockCount == 0) {
                UIApplication.sharedApplication.idleTimerDisabled = false
            }
        }
    }
    public actual fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): ()->Unit = {}
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