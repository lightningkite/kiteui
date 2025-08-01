package com.lightningkite.kiteui.reactive

import com.lightningkite.signal.*
import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.dom.KeyboardEvent
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.views.direct.KeyCodeWithModifiers
import kotlinx.browser.document
import kotlinx.browser.window
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import org.w3c.dom.events.Event
import kotlin.js.Promise

public actual object AppState {
    public actual val animationFrame: Listenable
        get() = _AnimationFrame
    internal val _windowInfo = Property(
        WindowStatistics(
            width = window.innerWidth.px,
            height = window.innerHeight.px,
            density = window.devicePixelRatio.toFloat()
        )
    ).also {
        window.addEventListener("resize", { ev ->
            val newwidth = window.innerWidth.px
            val newheight = window.innerHeight.px
            if (it.value.width != newwidth || it.value.height != newheight) {
                it.value = WindowStatistics(
                    width = newwidth,
                    height = newheight,
                    density = 1f
                )
            }
        })
    }
    public actual val windowInfo: ImmediateReadable<WindowStatistics>
        get() = _windowInfo
    public actual val inForeground: ImmediateReadable<Boolean>
        get() = _InForeground
    internal val _softInputOpen = Property(false)
    public actual val softInputOpen: ImmediateReadable<Boolean>
        get() = _softInputOpen

    private var currentLock: WakeLockSentinel? = null
    private var currentLockCount = 0
    public actual fun keepScreenOn(scope: CoroutineScope) {
        if (currentLockCount++ == 0) {
            AppScope.launch {
                try {
                    currentLock =
                        (window.navigator.asDynamic().wakeLock.request("screen") as Promise<WakeLockSentinel>).await()
                } catch (e: Exception) {
                    ConsoleRoot.warn("Could not acquire screen lock - probably unsupported", e)
                }
            }
        }
        scope.onRemove {
            if (--currentLockCount == 0) {
                currentLock?.release()
                currentLock = null
            }
        }
    }

    public val _lastUniversalKeyboardInput: Signal<KeyCodeWithModifiers> = sharedProcess<KeyCodeWithModifiers> {
        val l = { ev: Event ->
            ev as KeyboardEvent
            emit(
                KeyCodeWithModifiers(
                    code = ev.code,
                    alt = ev.altKey,
                    ctrl = ev.ctrlKey,
                    shift = ev.shiftKey,
                    meta = ev.metaKey
                )
            )
        }
        window.addEventListener("keydown", l)
        try {
            suspendCancellableCoroutine { }
        } finally {
            window.removeEventListener("keydown", l)
        }
    }
    public actual fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): () -> Unit {
        val l = { ev: Event ->
            ev as KeyboardEvent
            if (handler(
                    KeyCodeWithModifiers(
                        code = ev.code,
                        alt = ev.altKey,
                        ctrl = ev.ctrlKey,
                        shift = ev.shiftKey,
                        meta = ev.metaKey
                    )
                )
            ) {
                ev.preventDefault()
                ev.stopImmediatePropagation()
            }
            Unit
        }
        window.addEventListener("keydown", l)
        return { window.removeEventListener("keydown", l) }
    }
}

public external interface WakeLockSentinel {
    public val released: Boolean
    public fun release(): Promise<Unit>
}

private object _AnimationFrame : Listenable {
    public override fun addListener(listener: () -> Unit): () -> Unit {
        var end = false
        var sub: (Double) -> Unit = {}
        sub = label@{
            listener()
            if (end) {
                // Done!
            } else {
                window.requestAnimationFrame(sub)
            }
        }
        window.requestAnimationFrame(sub)
        return {
            end = true
        }
    }
}

private object _InForeground : ImmediateReadable<Boolean> {
    public override val value: Boolean
        get() = (document.asDynamic().visibilityState as? String) != "hidden"

    public override fun addListener(listener: () -> Unit): () -> Unit {
        val l = { _: Event -> listener(); Unit }
        document.addEventListener("visibilitychange", l)
        return { document.removeEventListener("visibilitychange", l) }
    }
}