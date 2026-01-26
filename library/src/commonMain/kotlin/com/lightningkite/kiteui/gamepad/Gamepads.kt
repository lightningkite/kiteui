package com.lightningkite.kiteui.gamepad

import com.lightningkite.reactive.core.Listenable
import com.lightningkite.reactive.core.ReactiveValue

/**
 * Platform-agnostic API for accessing game controller input.
 * 
 * Gamepads are polled on each animation frame and their state is updated
 * in the corresponding [gamepad] reactive values.
 * 
 * Usage:
 * ```kotlin
 * // Start polling when needed
 * Gamepads.startPolling()
 * 
 * // Access gamepad state reactively
 * val pad = Gamepads.gamepad(0)
 * text { ::content { "Left Stick: (${pad().leftStickX}, ${pad().leftStickY})" } }
 * 
 * // Stop polling when done
 * Gamepads.stopPolling()
 * ```
 */
expect object Gamepads {
    /**
     * Maximum number of gamepads supported simultaneously.
     * Typically 4 on most platforms.
     */
    val maxGamepads: Int
    
    /**
     * Gets the reactive state for a specific gamepad index.
     * 
     * @param index The gamepad index (0 to maxGamepads-1)
     * @return A reactive value containing the current gamepad state
     */
    fun gamepad(index: Int): ReactiveValue<GamepadState>
    
    /**
     * Gets the list of all connected gamepad indices.
     * This is a reactive value that updates when controllers connect/disconnect.
     */
    val connectedGamepads: ReactiveValue<List<Int>>
    
    /**
     * Listenable that fires when a gamepad is connected or disconnected.
     */
    val onConnectionChange: Listenable
    
    /**
     * Polls all gamepads and updates their state.
     * This is called automatically on animation frame when polling is started,
     * but can be called manually for more frequent polling if needed.
     */
    fun poll()
    
    /**
     * Starts polling gamepads automatically on each animation frame.
     * This is typically called once during app initialization or when
     * entering a screen that needs gamepad input.
     * Safe to call multiple times (will only start once).
     */
    fun startPolling()
    
    /**
     * Stops automatic polling. Use when gamepad input is not needed
     * to save resources.
     */
    fun stopPolling()
}
