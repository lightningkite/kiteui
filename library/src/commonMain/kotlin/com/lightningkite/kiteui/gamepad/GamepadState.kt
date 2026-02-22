package com.lightningkite.kiteui.gamepad

/**
 * Represents the current state of a gamepad controller.
 * All values are raw input data without high-level mapping.
 */
data class GamepadState(
    /** Unique identifier for this controller */
    val id: String,
    
    /** Human-readable name/description of the controller */
    val name: String,
    
    /** Whether the controller is currently connected */
    val connected: Boolean,
    
    // Axes (normalized ranges)
    /** Left stick X axis (-1.0 = left, 1.0 = right) */
    val leftStickX: Float,
    /** Left stick Y axis (-1.0 = up, 1.0 = down) */
    val leftStickY: Float,
    /** Right stick X axis (-1.0 = left, 1.0 = right) */
    val rightStickX: Float,
    /** Right stick Y axis (-1.0 = up, 1.0 = down) */
    val rightStickY: Float,
    /** Left trigger (0.0 = not pressed, 1.0 = fully pressed) */
    val leftTrigger: Float,
    /** Right trigger (0.0 = not pressed, 1.0 = fully pressed) */
    val rightTrigger: Float,
    
    // D-Pad as axis values
    /** D-Pad X axis (-1.0 = left, 0.0 = center, 1.0 = right) */
    val dpadX: Float,
    /** D-Pad Y axis (-1.0 = up, 0.0 = center, 1.0 = down) */
    val dpadY: Float,
    
    // Face buttons (using directional names for cross-platform consistency)
    /** Bottom face button (A on Xbox, X on PlayStation) */
    val buttonSouth: Boolean,
    /** Right face button (B on Xbox, Circle on PlayStation) */
    val buttonEast: Boolean,
    /** Left face button (X on Xbox, Square on PlayStation) */
    val buttonWest: Boolean,
    /** Top face button (Y on Xbox, Triangle on PlayStation) */
    val buttonNorth: Boolean,
    
    // Shoulder buttons
    val leftBumper: Boolean,
    val rightBumper: Boolean,
    
    // Stick buttons (pressing down on the sticks)
    val leftStickButton: Boolean,
    val rightStickButton: Boolean,
    
    // Menu buttons
    val startButton: Boolean,
    val selectButton: Boolean,
    
    // Guide/home button (may not be available on all platforms)
    val guideButton: Boolean,
    
    // Timestamp for delta calculations
    val timestamp: Long
) {
    companion object {
        val DISCONNECTED = GamepadState(
            id = "",
            name = "Disconnected",
            connected = false,
            leftStickX = 0f, leftStickY = 0f,
            rightStickX = 0f, rightStickY = 0f,
            leftTrigger = 0f, rightTrigger = 0f,
            dpadX = 0f, dpadY = 0f,
            buttonSouth = false, buttonEast = false,
            buttonWest = false, buttonNorth = false,
            leftBumper = false, rightBumper = false,
            leftStickButton = false, rightStickButton = false,
            startButton = false, selectButton = false,
            guideButton = false,
            timestamp = 0L
        )
    }
}

/**
 * Event representing a gamepad connection state change.
 */
data class GamepadConnectionEvent(
    val index: Int,
    val id: String,
    val name: String,
    val connected: Boolean
)
