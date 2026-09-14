package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.models.KeyCodeWithModifiers
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope

@Deprecated("Use AppState instead", ReplaceWith("AppState.animationFrame", "com.lightningkite.readable.AppState")) public val AnimationFrame: Listenable get() = AppState.animationFrame
@Deprecated("Use AppState instead", ReplaceWith("AppState.windowInfo", "com.lightningkite.readable.AppState")) public val WindowInfo: ReactiveValue<WindowStatistics> get() = AppState.windowInfo
@Deprecated("Use AppState instead", ReplaceWith("AppState.inForeground", "com.lightningkite.readable.AppState")) public val InForeground: ReactiveValue<Boolean> get() = AppState.inForeground
@Deprecated("Use AppState instead", ReplaceWith("AppState.softInputOpen", "com.lightningkite.readable.AppState")) public val SoftInputOpen: ReactiveValue<Boolean> get() = AppState.softInputOpen

public expect object AppState {
    public val animationFrame: Listenable
    public val windowInfo: ReactiveValue<WindowStatistics>
    public val inForeground: ReactiveValue<Boolean>
    public val softInputOpen: ReactiveValue<Boolean>
    public fun keepScreenOn(scope: CoroutineScope)
    public fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): ()->Unit
}