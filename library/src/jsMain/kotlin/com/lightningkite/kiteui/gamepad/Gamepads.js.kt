package com.lightningkite.kiteui.gamepad

import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.reactive.core.BasicListenable
import com.lightningkite.reactive.core.Listenable
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.browser.window
import org.w3c.dom.events.Event
import kotlin.math.min

actual object Gamepads {
    actual val maxGamepads: Int = 4
    
    private val gamepadStates = Array(maxGamepads) { Signal(GamepadState.DISCONNECTED) }
    private val _connectedGamepads = Signal(emptyList<Int>())
    private val _onConnectionChange = BasicListenable()
    
    private var polling = false
    private var pollListenerRemove: (() -> Unit)? = null
    
    actual fun gamepad(index: Int): ReactiveValue<GamepadState> {
        require(index in 0 until maxGamepads) { "Gamepad index $index out of range 0..$maxGamepads" }
        return gamepadStates[index]
    }
    
    actual val connectedGamepads: ReactiveValue<List<Int>> get() = _connectedGamepads
    actual val onConnectionChange: Listenable get() = _onConnectionChange
    
    init {
        // Listen for gamepad connect/disconnect events
        val connectHandler: (Event) -> Unit = { event ->
            val gamepad = event.asDynamic().gamepad
            val index = (gamepad.index as Number).toInt()
            if (index in 0 until maxGamepads) {
                updateConnectedList()
                _onConnectionChange.invokeAll()
            }
        }
        val disconnectHandler: (Event) -> Unit = { event ->
            val gamepad = event.asDynamic().gamepad
            val index = (gamepad.index as Number).toInt()
            if (index in 0 until maxGamepads) {
                gamepadStates[index].value = GamepadState.DISCONNECTED
                updateConnectedList()
                _onConnectionChange.invokeAll()
            }
        }
        window.addEventListener("gamepadconnected", connectHandler)
        window.addEventListener("gamepaddisconnected", disconnectHandler)
    }
    
    private fun updateConnectedList() {
        val gamepads = window.navigator.asDynamic().getGamepads() as? Array<dynamic>
        val connected = mutableListOf<Int>()
        gamepads?.forEachIndexed { index, pad ->
            if (index < maxGamepads && pad != null && pad.connected == true) {
                connected.add(index)
            }
        }
        _connectedGamepads.value = connected
    }
    
    actual fun poll() {
        val gamepads = window.navigator.asDynamic().getGamepads() as? Array<dynamic> ?: return
        
        for (index in 0 until min(gamepads.size, maxGamepads)) {
            val pad = gamepads[index]
            if (pad == null || pad.connected != true) {
                if (gamepadStates[index].value.connected) {
                    gamepadStates[index].value = GamepadState.DISCONNECTED
                }
                continue
            }
            
            val buttons = pad.buttons as Array<dynamic>
            val axes = pad.axes as Array<dynamic>
            
            gamepadStates[index].value = GamepadState(
                id = pad.id as String,
                name = pad.id as String,
                connected = true,
                leftStickX = dynamicToFloat(axes.getOrNull(0)),
                leftStickY = dynamicToFloat(axes.getOrNull(1)),
                rightStickX = dynamicToFloat(axes.getOrNull(2)),
                rightStickY = dynamicToFloat(axes.getOrNull(3)),
                leftTrigger = dynamicToFloat(buttons.getOrNull(6)?.value),
                rightTrigger = dynamicToFloat(buttons.getOrNull(7)?.value),
                dpadX = when {
                    buttons.getOrNull(14)?.pressed == true -> -1f  // D-pad left
                    buttons.getOrNull(15)?.pressed == true -> 1f   // D-pad right
                    else -> 0f
                },
                dpadY = when {
                    buttons.getOrNull(12)?.pressed == true -> -1f  // D-pad up
                    buttons.getOrNull(13)?.pressed == true -> 1f   // D-pad down
                    else -> 0f
                },
                buttonSouth = buttons.getOrNull(0)?.pressed == true,
                buttonEast = buttons.getOrNull(1)?.pressed == true,
                buttonWest = buttons.getOrNull(2)?.pressed == true,
                buttonNorth = buttons.getOrNull(3)?.pressed == true,
                leftBumper = buttons.getOrNull(4)?.pressed == true,
                rightBumper = buttons.getOrNull(5)?.pressed == true,
                leftStickButton = buttons.getOrNull(10)?.pressed == true,
                rightStickButton = buttons.getOrNull(11)?.pressed == true,
                startButton = buttons.getOrNull(9)?.pressed == true,
                selectButton = buttons.getOrNull(8)?.pressed == true,
                guideButton = buttons.getOrNull(16)?.pressed == true,
                timestamp = (pad.timestamp as? Number)?.toLong() ?: 0L
            )
        }
    }
    
    actual fun startPolling() {
        if (polling) return
        polling = true
        pollListenerRemove = AppState.animationFrame.addListener { poll() }
    }
    
    actual fun stopPolling() {
        polling = false
        pollListenerRemove?.invoke()
        pollListenerRemove = null
    }
}

private fun <T> Array<T>.getOrNull(index: Int): T? = if (index in indices) this[index] else null
private fun dynamicToFloat(value: dynamic): Float = (value as? Number)?.toFloat() ?: 0f
