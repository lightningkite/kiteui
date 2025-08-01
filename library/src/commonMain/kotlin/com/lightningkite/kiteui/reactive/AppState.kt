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

@Deprecated("Use AppState instead", ReplaceWith("AppState.animationFrame", "com.lightningkite.readable.AppState")) val AnimationFrame: Listenable get() = AppState.animationFrame
@Deprecated("Use AppState instead", ReplaceWith("AppState.windowInfo", "com.lightningkite.readable.AppState")) val WindowInfo: ReactiveValue<WindowStatistics> get() = AppState.windowInfo
@Deprecated("Use AppState instead", ReplaceWith("AppState.inForeground", "com.lightningkite.readable.AppState")) val InForeground: ReactiveValue<Boolean> get() = AppState.inForeground
@Deprecated("Use AppState instead", ReplaceWith("AppState.softInputOpen", "com.lightningkite.readable.AppState")) val SoftInputOpen: ReactiveValue<Boolean> get() = AppState.softInputOpen

expect object AppState {
    val animationFrame: Listenable
    val windowInfo: ReactiveValue<WindowStatistics>
    val inForeground: ReactiveValue<Boolean>
    val softInputOpen: ReactiveValue<Boolean>
    fun keepScreenOn(scope: CoroutineScope)
    fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): ()->Unit
}