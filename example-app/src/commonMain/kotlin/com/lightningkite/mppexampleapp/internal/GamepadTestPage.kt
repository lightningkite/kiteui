package com.lightningkite.mppexampleapp.internal

import com.lightningkite.kiteui.Routable
import com.lightningkite.kiteui.gamepad.Gamepads
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.reactive.*
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.reactive.context.*
import com.lightningkite.reactive.core.*
import com.lightningkite.readable.*

@Routable("test/gamepad")
object GamepadTestPage : Page {

    override fun ViewWriter.render() {
        scrolling.col {
            // Start polling when the page is shown
            Gamepads.startPolling()
            onRemove { Gamepads.stopPolling() }
            h1 { content = "Gamepad Test" }

            text {
                ::content { "Connected controllers: ${Gamepads.connectedGamepads()}" }
            }

            separator()

            // Show details for each possible gamepad slot
            repeat(Gamepads.maxGamepads) { index ->
                card.col {
                    val pad = Gamepads.gamepad(index)

                    h3 { content = "Controller $index" }

                    text { ::content { "Connected: ${pad().connected}" } }
                    text { ::content { "Name: ${pad().name}" } }

                    separator()

                    row {
                        col {
                            h4 { content = "Left Stick" }
                            text { ::content { "X: ${formatFloat(pad().leftStickX)}" } }
                            text { ::content { "Y: ${formatFloat(pad().leftStickY)}" } }
                            text { ::content { "Button: ${pad().leftStickButton}" } }
                        }
                        col {
                            h4 { content = "Right Stick" }
                            text { ::content { "X: ${formatFloat(pad().rightStickX)}" } }
                            text { ::content { "Y: ${formatFloat(pad().rightStickY)}" } }
                            text { ::content { "Button: ${pad().rightStickButton}" } }
                        }
                    }

                    separator()

                    row {
                        col {
                            h4 { content = "Triggers" }
                            text { ::content { "Left: ${formatFloat(pad().leftTrigger)}" } }
                            text { ::content { "Right: ${formatFloat(pad().rightTrigger)}" } }
                        }
                        col {
                            h4 { content = "Bumpers" }
                            text { ::content { "Left: ${pad().leftBumper}" } }
                            text { ::content { "Right: ${pad().rightBumper}" } }
                        }
                    }

                    separator()

                    row {
                        col {
                            h4 { content = "D-Pad" }
                            text { ::content { "X: ${formatFloat(pad().dpadX)}" } }
                            text { ::content { "Y: ${formatFloat(pad().dpadY)}" } }
                        }
                        col {
                            h4 { content = "Face Buttons" }
                            text { ::content { "South (A): ${pad().buttonSouth}" } }
                            text { ::content { "East (B): ${pad().buttonEast}" } }
                            text { ::content { "West (X): ${pad().buttonWest}" } }
                            text { ::content { "North (Y): ${pad().buttonNorth}" } }
                        }
                    }

                    separator()

                    row {
                        col {
                            h4 { content = "Menu" }
                            text { ::content { "Start: ${pad().startButton}" } }
                            text { ::content { "Select: ${pad().selectButton}" } }
                            text { ::content { "Guide: ${pad().guideButton}" } }
                        }
                    }
                }
            }
        }
    }
}

// Helper for formatting floats - simple implementation
private fun formatFloat(value: Float): String {
    val intPart = value.toInt()
    val fracPart = ((value - intPart) * 100).toInt()
    val sign = if (value < 0 && intPart == 0) "-" else ""
    return "$sign$intPart.${kotlin.math.abs(fracPart).toString().padStart(2, '0')}"
}
