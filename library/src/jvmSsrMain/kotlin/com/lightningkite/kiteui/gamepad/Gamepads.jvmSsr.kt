package com.lightningkite.kiteui.gamepad

import com.lightningkite.reactive.core.BasicListenable
import com.lightningkite.reactive.core.Listenable
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal

/**
 * Server-side rendering stub - no gamepad support on server.
 */
public actual object Gamepads {
    public actual val maxGamepads: Int = 0
    
    private val emptyState = Signal(GamepadState.DISCONNECTED)
    private val emptyList = Signal(emptyList<Int>())
    private val emptyListenable = BasicListenable()
    
    public actual fun gamepad(index: Int): ReactiveValue<GamepadState> = emptyState
    public actual val connectedGamepads: ReactiveValue<List<Int>> get() = emptyList
    public actual val onConnectionChange: Listenable get() = emptyListenable
    
    public actual fun poll() {}
    public actual fun startPolling() {}
    public actual fun stopPolling() {}
}
