package com.lightningkite.kiteui.reactive

import com.lightningkite.kiteui.locale.LanguageCode
import com.lightningkite.kiteui.models.KeyCodeWithModifiers
import com.lightningkite.kiteui.models.WindowStatistics
import com.lightningkite.kiteui.models.px
import com.lightningkite.reactive.core.*
import kotlinx.coroutines.CoroutineScope
import java.util.Locale

// The window root (KiteUiRoot) keeps these up to date.
public actual object AppState {
    internal val _animationFrame = BasicListenable()
    public actual val animationFrame: Listenable get() = _animationFrame
    internal val _windowInfo = Signal(WindowStatistics(1024.px, 768.px, 1f))
    public actual val windowInfo: ReactiveValue<WindowStatistics> get() = _windowInfo
    internal val _inForeground = Signal(true)
    public actual val inForeground: ReactiveValue<Boolean> get() = _inForeground
    // Desktop keyboards are physical.
    public actual val softInputOpen: ReactiveValue<Boolean> = Constant(false)
    public actual val systemLanguages: ReactiveValue<List<LanguageCode>> =
        Constant(listOf(LanguageCode(Locale.getDefault().toLanguageTag())))

    // TODO: keep the display awake (e.g. during video); needs an OS power assertion, which the JDK has no API for.
    public actual fun keepScreenOn(scope: CoroutineScope) {}

    internal val keyboardHandlers = ArrayList<(KeyCodeWithModifiers) -> Boolean>()
    public actual fun onUniversalKeyboard(handler: (KeyCodeWithModifiers) -> Boolean): () -> Unit {
        keyboardHandlers.add(handler)
        return { keyboardHandlers.remove(handler) }
    }
}
