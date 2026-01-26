package com.lightningkite.kiteui.gamepad

import com.lightningkite.kiteui.reactive.AppState
import com.lightningkite.reactive.core.BasicListenable
import com.lightningkite.reactive.core.Listenable
import com.lightningkite.reactive.core.ReactiveValue
import com.lightningkite.reactive.core.Signal
import net.java.games.input.Component
import net.java.games.input.Controller
import net.java.games.input.ControllerEnvironment

/**
 * JVM/Swing implementation of Gamepads API using JInput.
 */
actual object Gamepads {
    actual val maxGamepads: Int = 4

    private val gamepadStates = Array(maxGamepads) { Signal(GamepadState.DISCONNECTED) }
    private val _connectedGamepads = Signal(emptyList<Int>())
    private val _onConnectionChange = BasicListenable()

    private var polling = false
    private var pollListenerRemove: (() -> Unit)? = null

    // Cache controllers to avoid recreating the environment each poll
    private var cachedControllers: List<Controller>? = null
    private var lastControllerScan = 0L
    private const val CONTROLLER_RESCAN_INTERVAL_MS = 2000L // Rescan every 2 seconds for hotplug

    actual fun gamepad(index: Int): ReactiveValue<GamepadState> {
        require(index in 0 until maxGamepads) { "Gamepad index $index out of range 0..$maxGamepads" }
        return gamepadStates[index]
    }

    actual val connectedGamepads: ReactiveValue<List<Int>> get() = _connectedGamepads
    actual val onConnectionChange: Listenable get() = _onConnectionChange

    private fun getGamepadControllers(): List<Controller> {
        val now = System.currentTimeMillis()
        if (cachedControllers == null || now - lastControllerScan > CONTROLLER_RESCAN_INTERVAL_MS) {
            lastControllerScan = now
            val oldCount = cachedControllers?.size ?: 0
            cachedControllers = try {
                ControllerEnvironment.getDefaultEnvironment().controllers
                    .filter { it.type == Controller.Type.GAMEPAD || it.type == Controller.Type.STICK }
            } catch (e: Exception) {
                // JInput may throw if native libraries aren't available
                emptyList()
            }
            if (cachedControllers?.size != oldCount) {
                updateConnectedList()
                _onConnectionChange.invokeAll()
            }
        }
        return cachedControllers ?: emptyList()
    }

    private fun updateConnectedList() {
        val controllers = cachedControllers ?: return
        val connected = (0 until minOf(controllers.size, maxGamepads)).toList()
        _connectedGamepads.value = connected

        // Clear disconnected states
        for (index in controllers.size until maxGamepads) {
            if (gamepadStates[index].value.connected) {
                gamepadStates[index].value = GamepadState.DISCONNECTED
            }
        }
    }

    actual fun poll() {
        val controllers = getGamepadControllers()

        for ((index, controller) in controllers.withIndex()) {
            if (index >= maxGamepads) break

            if (!controller.poll()) {
                // Controller disconnected or failed to poll
                if (gamepadStates[index].value.connected) {
                    gamepadStates[index].value = GamepadState.DISCONNECTED
                }
                continue
            }

            val components = controller.components.associateBy { it.identifier }

            // Map JInput component identifiers to GamepadState
            gamepadStates[index].value = GamepadState(
                id = controller.name,
                name = controller.name,
                connected = true,
                leftStickX = components[Component.Identifier.Axis.X]?.pollData ?: 0f,
                leftStickY = components[Component.Identifier.Axis.Y]?.pollData ?: 0f,
                rightStickX = components[Component.Identifier.Axis.RX]?.pollData
                    ?: components[Component.Identifier.Axis.Z]?.pollData ?: 0f,
                rightStickY = components[Component.Identifier.Axis.RY]?.pollData
                    ?: components[Component.Identifier.Axis.RZ]?.pollData ?: 0f,
                leftTrigger = normalizeTrigger(components[Component.Identifier.Axis.Z]?.pollData ?: -1f),
                rightTrigger = normalizeTrigger(components[Component.Identifier.Axis.RZ]?.pollData ?: -1f),
                dpadX = components[Component.Identifier.Axis.POV]?.let { povToX(it.pollData) } ?: 0f,
                dpadY = components[Component.Identifier.Axis.POV]?.let { povToY(it.pollData) } ?: 0f,
                buttonSouth = isButtonPressed(components[Component.Identifier.Button._0]),
                buttonEast = isButtonPressed(components[Component.Identifier.Button._1]),
                buttonWest = isButtonPressed(components[Component.Identifier.Button._2]),
                buttonNorth = isButtonPressed(components[Component.Identifier.Button._3]),
                leftBumper = isButtonPressed(components[Component.Identifier.Button._4]),
                rightBumper = isButtonPressed(components[Component.Identifier.Button._5]),
                leftStickButton = isButtonPressed(components[Component.Identifier.Button._8]),
                rightStickButton = isButtonPressed(components[Component.Identifier.Button._9]),
                startButton = isButtonPressed(components[Component.Identifier.Button._7]),
                selectButton = isButtonPressed(components[Component.Identifier.Button._6]),
                guideButton = isButtonPressed(components[Component.Identifier.Button._10]),
                timestamp = System.currentTimeMillis()
            )
        }
    }

    private fun isButtonPressed(component: Component?): Boolean {
        return component != null && component.pollData > 0.5f
    }

    // JInput triggers often report -1 to 1, we need 0 to 1
    private fun normalizeTrigger(value: Float): Float {
        return ((value + 1f) / 2f).coerceIn(0f, 1f)
    }

    // POV (D-pad) is reported as a single value representing direction
    // 0.0 = center, 0.25 = up, 0.5 = right, 0.75 = down, 1.0 = left
    // Diagonal values are in between (e.g., 0.125 = up-right)
    private fun povToX(pov: Float): Float {
        if (pov == Component.POV.OFF) return 0f
        return when {
            pov > 0.125f && pov < 0.625f -> 1f   // Right quadrant
            pov > 0.625f && pov < 0.875f -> 0f   // Down
            pov > 0.875f || pov < 0.125f -> 0f   // Up
            else -> -1f                          // Left quadrant
        }.let {
            // More precise calculation
            when {
                pov == Component.POV.OFF -> 0f
                pov in 0.125f..0.375f -> 1f  // Right
                pov in 0.625f..0.875f -> -1f // Left
                else -> 0f
            }
        }
    }

    private fun povToY(pov: Float): Float {
        if (pov == Component.POV.OFF) return 0f
        return when {
            pov == Component.POV.OFF -> 0f
            pov in 0.0f..0.125f || pov in 0.875f..1.0f -> -1f // Up
            pov in 0.375f..0.625f -> 1f // Down
            else -> 0f
        }
    }

    actual fun startPolling() {
        if (polling) return
        polling = true
        // Initial scan
        getGamepadControllers()
        pollListenerRemove = AppState.animationFrame.addListener { poll() }
    }

    actual fun stopPolling() {
        polling = false
        pollListenerRemove?.invoke()
        pollListenerRemove = null
    }
}
