package com.lightningkite.kiteui.reactive

import com.lightningkite.readable.*
import com.lightningkite.kiteui.models.WindowStatistics
import kotlinx.coroutines.CoroutineScope

@Deprecated("Use AppState instead", ReplaceWith("AppState.animationFrame", "com.lightningkite.readable.AppState")) val AnimationFrame: Listenable get() = AppState.animationFrame
@Deprecated("Use AppState instead", ReplaceWith("AppState.windowInfo", "com.lightningkite.readable.AppState")) val WindowInfo: ImmediateReadable<WindowStatistics> get() = AppState.windowInfo
@Deprecated("Use AppState instead", ReplaceWith("AppState.inForeground", "com.lightningkite.readable.AppState")) val InForeground: ImmediateReadable<Boolean> get() = AppState.inForeground
@Deprecated("Use AppState instead", ReplaceWith("AppState.softInputOpen", "com.lightningkite.readable.AppState")) val SoftInputOpen: ImmediateReadable<Boolean> get() = AppState.softInputOpen

expect object AppState {
    val animationFrame: Listenable
    val windowInfo: ImmediateReadable<WindowStatistics>
    val inForeground: ImmediateReadable<Boolean>
    val softInputOpen: ImmediateReadable<Boolean>
    fun keepScreenOn(scope: CoroutineScope)
}