package com.lightningkite.kiteui.reactive

import android.content.Context
import android.os.PowerManager
import android.view.WindowManager
import com.lightningkite.kiteui.ConsoleRoot
import com.lightningkite.kiteui.launchGlobal
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.views.AndroidAppContext
import kotlinx.coroutines.CoroutineScope
import kotlin.time.Duration.Companion.days

actual object AppState {
    internal val _animationFrame = BasicListenable()
    actual val animationFrame: Listenable
        get() = _animationFrame
    internal val _windowInfo = Property(WindowStatistics(Dimension(1920f), Dimension(1080f), 1f))
    actual val windowInfo: ImmediateReadable<WindowStatistics>
        get() = _windowInfo
    internal val _inForeground = Property(true)
    actual val inForeground: ImmediateReadable<Boolean>
        get() = _inForeground
    internal val _softInputOpen = Property(false)
    actual val softInputOpen: ImmediateReadable<Boolean>
        get() = _softInputOpen

    private var currentLockCount = 0
    actual fun keepScreenOn(scope: CoroutineScope) {
        if(currentLockCount++ == 0) {
            try {
                AndroidAppContext.activityCtx?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            } catch(e: Exception) {
                ConsoleRoot.warn("Could not acquire screen lock - probably unsupported", e)
            }
        }
        scope.onRemove {
            if(--currentLockCount == 0) {
                AndroidAppContext.activityCtx?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
