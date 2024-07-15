package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.WindowStatistics

@Deprecated("Use AppState instead", ReplaceWith("AppState.animationFrame", "com.lightningkite.kiteui.reactive.AppState")) val AnimationFrame: Listenable get() = AppState.animationFrame
@Deprecated("Use AppState instead", ReplaceWith("AppState.windowInfo", "com.lightningkite.kiteui.reactive.AppState")) val WindowInfo: ImmediateReadable<WindowStatistics> get() = AppState.windowInfo
@Deprecated("Use AppState instead", ReplaceWith("AppState.inForeground", "com.lightningkite.kiteui.reactive.AppState")) val InForeground: ImmediateReadable<Boolean> get() = AppState.inForeground
@Deprecated("Use AppState instead", ReplaceWith("AppState.softInputOpen", "com.lightningkite.kiteui.reactive.AppState")) val SoftInputOpen: ImmediateReadable<Boolean> get() = AppState.softInputOpen

expect object AppState {
    val animationFrame: Listenable
    val windowInfo: ImmediateReadable<WindowStatistics>
    val inForeground: ImmediateReadable<Boolean>
    val softInputOpen: ImmediateReadable<Boolean>
}