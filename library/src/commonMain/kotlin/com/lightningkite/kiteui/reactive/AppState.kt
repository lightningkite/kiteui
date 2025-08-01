package com.lightningkite.kiteui.reactive

import com.lightningkite.signal.*
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.views.direct.KeyCodeWithModifiers
import kotlinx.coroutines.CoroutineScope

@Deprecated("Use AppState instead", ReplaceWith("AppState.animationFrame", "com.lightningkite.readable.AppState"))
public val AnimationFrame: Listenable get() = AppState.animationFrame
@Deprecated("Use AppState instead", ReplaceWith("AppState.windowInfo", "com.lightningkite.readable.AppState"))
public val WindowInfo: ImmediateReadable<WindowStatistics> get() = AppState.windowInfo
@Deprecated("Use AppState instead", ReplaceWith("AppState.inForeground", "com.lightningkite.readable.AppState"))
public val InForeground: ImmediateReadable<Boolean> get() = AppState.inForeground
@Deprecated("Use AppState instead", ReplaceWith("AppState.softInputOpen", "com.lightningkite.readable.AppState"))
public val SoftInputOpen: ImmediateReadable<Boolean> get() = AppState.softInputOpen

public expect object AppState {
    public val animationFrame: Listenable
    public val windowInfo: ImmediateReadable<WindowStatistics>
    public val inForeground: ImmediateReadable<Boolean>
    public val softInputOpen: ImmediateReadable<Boolean>
    public fun keepScreenOn(scope: CoroutineScope)
    public fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): ()->Unit
}