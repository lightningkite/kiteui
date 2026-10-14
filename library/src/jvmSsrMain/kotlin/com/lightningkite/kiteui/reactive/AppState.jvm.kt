package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.kiteui.models.Dimension
import com.lightningkite.kiteui.models.KeyCodeWithModifiers
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.models.px
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.reactive.extensions.*
import com.lightningkite.reactive.lensing.*
import com.lightningkite.readable.*
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

public actual object AppState {
    internal val _animationFrame = BasicListenable()
    public actual val animationFrame: Listenable
        get() = _animationFrame
    internal val _windowInfo = Signal(WindowStatistics(1920.px, 1080.px, 1f))
    public actual val windowInfo: ReactiveValue<WindowStatistics>
        get() = _windowInfo
    internal val _inForeground = Signal(true)
    public actual val inForeground: ReactiveValue<Boolean>
        get() = _inForeground
    internal val _softInputOpen = Signal(false)
    public actual val softInputOpen: ReactiveValue<Boolean>
        get() = _softInputOpen

    internal val _systemLanguages = Signal(listOf(LanguageCode(Locale.getDefault().toLanguageTag())))
    public actual val systemLanguages: ReactiveValue<List<LanguageCode>>
        get() = _systemLanguages

    /**
     * There's no OS-level system language here - this target is server-side rendering, which may serve
     * many clients with different locales from a single JVM. Defaults to the JVM's default locale;
     * the hosting application should plug in the real value (e.g. per-request, from an incoming
     * Accept-Language header) by assigning this.
     */
    public var systemLanguagesOverride: List<LanguageCode>
        get() = _systemLanguages.value
        set(value) { _systemLanguages.value = value }

    public actual fun keepScreenOn(scope: CoroutineScope) {
        // Nothing to do; we're server-side
    }
    public actual fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): ()->Unit = {}
}
