package com.lightningkite.kiteui.gamepad

import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.reactive.core.BasicListenable
import com.lightningkite.reactive.core.Listenable
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import platform.Foundation.NSDate
import platform.Foundation.NSNotification
import platform.Foundation.NSNotificationCenter
import platform.Foundation.date
import platform.Foundation.timeIntervalSince1970
import platform.GameController.GCController
import platform.GameController.GCControllerDidConnectNotification
import platform.GameController.GCControllerDidDisconnectNotification
import platform.darwin.NSObject
import platform.darwin.sel_registerName
import kotlin.math.min

@OptIn(ExperimentalForeignApi::class)
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

    private val observer = object : NSObject() {
        @Suppress("unused")
        @ObjCAction
        fun controllerDidConnect(notification: NSNotification?) {
            updateConnectedList()
            _onConnectionChange.invokeAll()
        }

        @Suppress("unused")
        @ObjCAction
        fun controllerDidDisconnect(notification: NSNotification?) {
            updateConnectedList()
            _onConnectionChange.invokeAll()
        }
    }

    init {
        NSNotificationCenter.defaultCenter.addObserver(
            observer = observer,
            selector = sel_registerName("controllerDidConnect:"),
            name = GCControllerDidConnectNotification,
            `object` = null
        )
        NSNotificationCenter.defaultCenter.addObserver(
            observer = observer,
            selector = sel_registerName("controllerDidDisconnect:"),
            name = GCControllerDidDisconnectNotification,
            `object` = null
        )
    }

    private fun updateConnectedList() {
        @Suppress("UNCHECKED_CAST")
        val controllers = GCController.controllers() as List<GCController>
        val connected = (0 until min(controllers.size, maxGamepads)).toList()
        _connectedGamepads.value = connected

        // Clear disconnected states
        for (index in controllers.size until maxGamepads) {
            if (gamepadStates[index].value.connected) {
                gamepadStates[index].value = GamepadState.DISCONNECTED
            }
        }
    }

    actual fun poll() {
        @Suppress("UNCHECKED_CAST")
        val controllers = GCController.controllers() as List<GCController>

        for ((index, controller) in controllers.withIndex()) {
            if (index >= maxGamepads) break

            val extendedGamepad = controller.extendedGamepad

            if (extendedGamepad != null) {
                gamepadStates[index].value = GamepadState(
                    id = controller.vendorName ?: "Controller $index",
                    name = controller.vendorName ?: "Controller $index",
                    connected = true,
                    leftStickX = extendedGamepad.leftThumbstick.xAxis.value,
                    leftStickY = -extendedGamepad.leftThumbstick.yAxis.value, // Invert Y
                    rightStickX = extendedGamepad.rightThumbstick.xAxis.value,
                    rightStickY = -extendedGamepad.rightThumbstick.yAxis.value, // Invert Y
                    leftTrigger = extendedGamepad.leftTrigger.value,
                    rightTrigger = extendedGamepad.rightTrigger.value,
                    dpadX = when {
                        extendedGamepad.dpad.left.pressed -> -1f
                        extendedGamepad.dpad.right.pressed -> 1f
                        else -> 0f
                    },
                    dpadY = when {
                        extendedGamepad.dpad.up.pressed -> -1f
                        extendedGamepad.dpad.down.pressed -> 1f
                        else -> 0f
                    },
                    buttonSouth = extendedGamepad.buttonA.pressed,
                    buttonEast = extendedGamepad.buttonB.pressed,
                    buttonWest = extendedGamepad.buttonX.pressed,
                    buttonNorth = extendedGamepad.buttonY.pressed,
                    leftBumper = extendedGamepad.leftShoulder.pressed,
                    rightBumper = extendedGamepad.rightShoulder.pressed,
                    leftStickButton = extendedGamepad.leftThumbstickButton?.pressed ?: false,
                    rightStickButton = extendedGamepad.rightThumbstickButton?.pressed ?: false,
                    startButton = extendedGamepad.buttonMenu.pressed,
                    selectButton = extendedGamepad.buttonOptions?.pressed ?: false,
                    guideButton = extendedGamepad.buttonHome?.pressed ?: false,
                    timestamp = (NSDate.date().timeIntervalSince1970 * 1000).toLong()
                )
            } else {
                // Basic gamepad profile (less buttons)
                val basicGamepad = controller.microGamepad
                if (basicGamepad != null) {
                    gamepadStates[index].value = GamepadState(
                        id = controller.vendorName ?: "Controller $index",
                        name = controller.vendorName ?: "Controller $index",
                        connected = true,
                        leftStickX = 0f, leftStickY = 0f,
                        rightStickX = 0f, rightStickY = 0f,
                        leftTrigger = 0f, rightTrigger = 0f,
                        dpadX = when {
                            basicGamepad.dpad.left.pressed -> -1f
                            basicGamepad.dpad.right.pressed -> 1f
                            else -> 0f
                        },
                        dpadY = when {
                            basicGamepad.dpad.up.pressed -> -1f
                            basicGamepad.dpad.down.pressed -> 1f
                            else -> 0f
                        },
                        buttonSouth = basicGamepad.buttonA.pressed,
                        buttonEast = false,
                        buttonWest = basicGamepad.buttonX.pressed,
                        buttonNorth = false,
                        leftBumper = false, rightBumper = false,
                        leftStickButton = false, rightStickButton = false,
                        startButton = basicGamepad.buttonMenu.pressed,
                        selectButton = false,
                        guideButton = false,
                        timestamp = (NSDate.date().timeIntervalSince1970 * 1000).toLong()
                    )
                } else {
                    gamepadStates[index].value = GamepadState.DISCONNECTED.copy(
                        id = controller.vendorName ?: "Controller $index",
                        name = controller.vendorName ?: "Controller $index",
                        connected = true
                    )
                }
            }
        }
    }

    actual fun startPolling() {
        if (polling) return
        polling = true
        updateConnectedList()
        pollListenerRemove = AppState.animationFrame.addListener { poll() }
    }

    actual fun stopPolling() {
        polling = false
        pollListenerRemove?.invoke()
        pollListenerRemove = null
    }
}
