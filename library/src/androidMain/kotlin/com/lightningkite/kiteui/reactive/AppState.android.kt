package com.lightningkite.kiteui.reactive

import android.view.WindowManager
import com.lightningkite.kiteui.LogRoot
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyCodeWithModifiers
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.AndroidAppContext
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CancellationException
import com.lightningkite.kiteui.views.direct.KeyCodeWithModifiers
import com.lightningkite.signal.*
import kotlinx.coroutines.CoroutineScope

public actual object AppState {
    internal val _animationFrame = BasicListenable()
    public actual val animationFrame: Listenable
        get() = _animationFrame
    internal val _windowInfo = Signal(WindowStatistics(Dimension(1920f), Dimension(1080f), 1f))
    actual val windowInfo: ReactiveValue<WindowStatistics>
        get() = _windowInfo
    internal val _inForeground = Signal(true)
    actual val inForeground: ReactiveValue<Boolean>
        get() = _inForeground
    internal val _softInputOpen = Signal(false)
    actual val softInputOpen: ReactiveValue<Boolean>
        get() = _softInputOpen

    private var currentLockCount = 0
    public actual fun keepScreenOn(scope: CoroutineScope) {
        if(currentLockCount++ == 0) {
            try {
                AndroidAppContext.activityCtx?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } catch (e: CancellationException) {
                throw e
            } catch(e: Exception) {
                LogRoot.warn("Could not acquire screen lock - probably unsupported", e)
            }
        }
        scope.onRemove {
            if(--currentLockCount == 0) {
                AndroidAppContext.activityCtx?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
    public actual fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): ()->Unit = {}
}
