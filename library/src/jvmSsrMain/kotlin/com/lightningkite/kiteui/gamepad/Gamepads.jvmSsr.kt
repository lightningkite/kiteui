package com.lightningkite.kiteui.gamepad

import com.lightningkite.reactive.core.BasicListenable
import com.lightningkite.reactive.core.Listenable
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal

/**
 * Server-side rendering stub - no gamepad support on server.
 */
actual object Gamepads {
    actual val maxGamepads: Int = 0
    
    private val emptyState = Signal(GamepadState.DISCONNECTED)
    private val emptyList = Signal(emptyList<Int>())
    private val emptyListenable = BasicListenable()
    
    actual fun gamepad(index: Int): ReactiveValue<GamepadState> = emptyState
    actual val connectedGamepads: ReactiveValue<List<Int>> get() = emptyList
    actual val onConnectionChange: Listenable get() = emptyListenable
    
    actual fun poll() {}
    actual fun startPolling() {}
    actual fun stopPolling() {}
}
