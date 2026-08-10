package com.lightningkite.kiteui.gamepad

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.reactive.core.BasicListenable
import com.lightningkite.reactive.core.Listenable
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal

public actual object Gamepads {
    public actual val maxGamepads: Int = 4
    
    private val gamepadStates = Array(maxGamepads) { Signal(GamepadState.DISCONNECTED) }
    private val _connectedGamepads = Signal(emptyList<Int>())
    private val _onConnectionChange = BasicListenable()
    
    private var polling = false
    private var pollListenerRemove: (() -> Unit)? = null
    
    // Map from device ID to our index
    private val deviceIdToIndex = mutableMapOf<Int, Int>()
    
    public actual fun gamepad(index: Int): ReactiveValue<GamepadState> {
        require(index in 0 until maxGamepads) { "Gamepad index $index out of range 0..$maxGamepads" }
        return gamepadStates[index]
    }
    
    public actual val connectedGamepads: ReactiveValue<List<Int>> get() = _connectedGamepads
    public actual val onConnectionChange: Listenable get() = _onConnectionChange
    
    private fun isGamepad(device: InputDevice): Boolean {
        val sources = device.sources
        return (sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
               (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
    }
    
    private fun scanDevices() {
        val gamepadDeviceIds = InputDevice.getDeviceIds()
            .toList()
            .mapNotNull { id -> InputDevice.getDevice(id) }
            .filter { device -> isGamepad(device) }
            .map { device -> device.id }
            .toSet()

        // Remove disconnected devices
        deviceIdToIndex.keys.toList().forEach { deviceId ->
            if (!gamepadDeviceIds.contains(deviceId)) {
                val index = deviceIdToIndex.remove(deviceId)
                if (index != null) {
                    gamepadStates[index].value = GamepadState.DISCONNECTED
                }
            }
        }

        // Add new devices
        for (deviceId in gamepadDeviceIds) {
            if (!deviceIdToIndex.containsKey(deviceId) && deviceIdToIndex.size < maxGamepads) {
                val nextIndex = (0 until maxGamepads).firstOrNull { idx ->
                    !deviceIdToIndex.values.contains(idx)
                } ?: continue
                deviceIdToIndex[deviceId] = nextIndex

                // Initialize state with device name
                val device = InputDevice.getDevice(deviceId)
                if (device != null) {
                    gamepadStates[nextIndex].value = GamepadState.DISCONNECTED.copy(
                        id = deviceId.toString(),
                        name = device.name,
                        connected = true
                    )
                }
            }
        }

        updateConnectedList()
    }
    
    private fun updateConnectedList() {
        val connected = deviceIdToIndex.values.sorted()
        if (connected != _connectedGamepads.value) {
            _connectedGamepads.value = connected
            _onConnectionChange.invokeAll()
        }
    }
    
    public actual fun poll() {
        scanDevices()
    }
    
    /**
     * Call from Activity.onGenericMotionEvent to update gamepad axis values.
     * 
     * Example usage in your Activity:
     * ```kotlin
     * override fun onGenericMotionEvent(event: MotionEvent): Boolean {
     *     if (Gamepads.handleMotionEvent(event)) return true
     *     return super.onGenericMotionEvent(event)
     * }
     * ```
     */
    public fun handleMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) != InputDevice.SOURCE_JOYSTICK &&
            (event.source and InputDevice.SOURCE_GAMEPAD) != InputDevice.SOURCE_GAMEPAD) {
            return false
        }
        
        val index = deviceIdToIndex[event.deviceId] ?: return false
        val current = gamepadStates[index].value
        
        gamepadStates[index].value = current.copy(
            leftStickX = event.getAxisValue(MotionEvent.AXIS_X),
            leftStickY = event.getAxisValue(MotionEvent.AXIS_Y),
            rightStickX = event.getAxisValue(MotionEvent.AXIS_Z),
            rightStickY = event.getAxisValue(MotionEvent.AXIS_RZ),
            leftTrigger = event.getAxisValue(MotionEvent.AXIS_LTRIGGER),
            rightTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER),
            dpadX = event.getAxisValue(MotionEvent.AXIS_HAT_X),
            dpadY = event.getAxisValue(MotionEvent.AXIS_HAT_Y),
            timestamp = event.eventTime
        )
        return true
    }
    
    /**
     * Call from Activity.onKeyDown to update gamepad button states.
     * 
     * Example usage in your Activity:
     * ```kotlin
     * override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
     *     if (Gamepads.handleKeyDown(keyCode, event)) return true
     *     return super.onKeyDown(keyCode, event)
     * }
     * ```
     */
    public fun handleKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        return handleKeyEvent(keyCode, event.deviceId, true)
    }
    
    /**
     * Call from Activity.onKeyUp to update gamepad button states.
     * 
     * Example usage in your Activity:
     * ```kotlin
     * override fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
     *     if (Gamepads.handleKeyUp(keyCode, event)) return true
     *     return super.onKeyUp(keyCode, event)
     * }
     * ```
     */
    public fun handleKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        return handleKeyEvent(keyCode, event.deviceId, false)
    }
    
    private fun handleKeyEvent(keyCode: Int, deviceId: Int, pressed: Boolean): Boolean {
        val index = deviceIdToIndex[deviceId] ?: return false
        val current = gamepadStates[index].value
        
        val updated = when (keyCode) {
            KeyEvent.KEYCODE_BUTTON_A -> current.copy(buttonSouth = pressed)
            KeyEvent.KEYCODE_BUTTON_B -> current.copy(buttonEast = pressed)
            KeyEvent.KEYCODE_BUTTON_X -> current.copy(buttonWest = pressed)
            KeyEvent.KEYCODE_BUTTON_Y -> current.copy(buttonNorth = pressed)
            KeyEvent.KEYCODE_BUTTON_L1 -> current.copy(leftBumper = pressed)
            KeyEvent.KEYCODE_BUTTON_R1 -> current.copy(rightBumper = pressed)
            KeyEvent.KEYCODE_BUTTON_THUMBL -> current.copy(leftStickButton = pressed)
            KeyEvent.KEYCODE_BUTTON_THUMBR -> current.copy(rightStickButton = pressed)
            KeyEvent.KEYCODE_BUTTON_START -> current.copy(startButton = pressed)
            KeyEvent.KEYCODE_BUTTON_SELECT -> current.copy(selectButton = pressed)
            KeyEvent.KEYCODE_BUTTON_MODE -> current.copy(guideButton = pressed)
            KeyEvent.KEYCODE_DPAD_UP -> current.copy(dpadY = if (pressed) -1f else 0f)
            KeyEvent.KEYCODE_DPAD_DOWN -> current.copy(dpadY = if (pressed) 1f else 0f)
            KeyEvent.KEYCODE_DPAD_LEFT -> current.copy(dpadX = if (pressed) -1f else 0f)
            KeyEvent.KEYCODE_DPAD_RIGHT -> current.copy(dpadX = if (pressed) 1f else 0f)
            else -> return false
        }
        
        gamepadStates[index].value = updated.copy(timestamp = System.currentTimeMillis())
        return true
    }
    
    public actual fun startPolling() {
        if (polling) return
        polling = true
        scanDevices()
        pollListenerRemove = AppState.animationFrame.addListener { poll() }
    }
    
    public actual fun stopPolling() {
        polling = false
        pollListenerRemove?.invoke()
        pollListenerRemove = null
    }
}
