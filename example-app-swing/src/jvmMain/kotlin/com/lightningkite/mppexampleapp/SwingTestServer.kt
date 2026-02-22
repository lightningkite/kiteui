package com.lightningkite.mppexampleapp

import java.awt.*
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.*
import java.net.ServerSocket
import java.net.Socket
import java.util.*
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.concurrent.thread

/**
 * A simple test server that allows external automation of the Swing app.
 * Listens on a port and accepts line-based commands.
 *
 * Commands:
 * - screenshot -> Returns base64 PNG of the window
 * - tree -> Returns component hierarchy as text
 * - find <text> -> Find components containing text, returns their bounds
 * - click <x> <y> -> Click at window-relative coordinates
 * - type <text> -> Type text into focused component
 * - focus <text> -> Find and focus component containing text
 * - bounds -> Returns window bounds
 * - quit -> Stop the test server
 */
class SwingTestServer(
    private val frame: JFrame,
    private val port: Int = 18888
) {
    private var running = false
    private var serverSocket: ServerSocket? = null

    fun start() {
        running = true
        thread(name = "SwingTestServer") {
            try {
                serverSocket = ServerSocket(port)
                println("SwingTestServer listening on port $port")

                while (running) {
                    try {
                        val client = serverSocket?.accept() ?: break
                        thread { handleClient(client) }
                    } catch (e: Exception) {
                        if (running) e.printStackTrace()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun stop() {
        running = false
        serverSocket?.close()
    }

    private fun handleClient(socket: Socket) {
        try {
            val reader = BufferedReader(InputStreamReader(socket.getInputStream()))
            val writer = PrintWriter(socket.getOutputStream(), true)

            val line = reader.readLine() ?: return
            val parts = line.trim().split(" ", limit = 2)
            val command = parts[0].lowercase()
            val args = parts.getOrNull(1) ?: ""

            val response = try {
                executeCommand(command, args)
            } catch (e: Exception) {
                "ERROR: ${e.message}"
            }

            writer.println(response)
            writer.println("END")

            socket.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun executeCommand(command: String, args: String): String {
        return when (command) {
            "screenshot" -> screenshot(args.ifBlank { null })
            "tree" -> componentTree()
            "find" -> findComponents(args)
            "click" -> click(args)
            "clickon" -> clickOnComponent(args)  // Click directly on a component by text
            "realclick" -> realClickOnComponent(args)  // Use Robot to click on component (tests real dispatch)
            "dispatchclick" -> dispatchClickOnComponent(args)  // Dispatch events directly to component (bypasses Robot)
            "diagnose" -> diagnoseClickPath(args)  // Diagnose what component would receive a click
            "type" -> typeText(args)
            "focus" -> focusComponent(args)
            "bounds" -> windowBounds()
            "scroll" -> scroll(args)  // Scroll by delta or to position
            "scrollon" -> scrollOnComponent(args)  // Find scrollable ancestor and scroll
            "wheel" -> mouseWheel(args)  // Simulate mouse wheel event
            "debugscroll" -> debugScrollPanes()  // Debug scroll pane info
            "dispatchwheel" -> dispatchWheelToScrollPane(args)  // Dispatch wheel directly
            "quit" -> { stop(); "OK" }
            "ping" -> "PONG"
            "moveto" -> moveTo(args)  // Move mouse to position without clicking (for hover testing)
            "movetoon" -> moveToOnComponent(args)  // Move mouse to component center (for hover testing)
            else -> "ERROR: Unknown command: $command"
        }
    }

    private fun screenshot(filePath: String?): String {
        return invokeAndWait {
            val contentPane = frame.contentPane
            val image = BufferedImage(contentPane.width, contentPane.height, BufferedImage.TYPE_INT_RGB)
            val g = image.createGraphics()
            contentPane.paint(g)
            g.dispose()

            if (filePath != null) {
                // Save to file
                val file = File(filePath)
                file.parentFile?.mkdirs()
                ImageIO.write(image, "png", file)
                "OK: Saved to $filePath"
            } else {
                // Return as base64
                val baos = ByteArrayOutputStream()
                ImageIO.write(image, "png", baos)
                val base64 = Base64.getEncoder().encodeToString(baos.toByteArray())
                "DATA:$base64"
            }
        }
    }

    private fun componentTree(): String {
        return invokeAndWait {
            val sb = StringBuilder()
            buildTree(frame.contentPane, 0, sb)
            sb.toString()
        }
    }

    private fun buildTree(component: Component, depth: Int, sb: StringBuilder) {
        val indent = "  ".repeat(depth)
        val bounds = component.bounds
        val text = getComponentText(component)
        val textPart = if (text.isNotEmpty()) " \"$text\"" else ""
        val visiblePart = if (!component.isVisible) " [hidden]" else ""

        sb.appendLine("$indent${component.javaClass.simpleName}$textPart (${bounds.x},${bounds.y} ${bounds.width}x${bounds.height})$visiblePart")

        if (component is Container) {
            for (child in component.components) {
                buildTree(child, depth + 1, sb)
            }
        }
    }

    private fun getComponentText(component: Component): String {
        return when (component) {
            is JLabel -> component.text ?: ""
            is JButton -> component.text ?: ""
            is JTextField -> component.text ?: ""
            is JTextArea -> component.text?.take(50) ?: ""
            is AbstractButton -> component.text ?: ""
            else -> ""
        }
    }

    private fun findComponents(text: String): String {
        if (text.isBlank()) return "ERROR: No search text provided"

        return invokeAndWait {
            val results = mutableListOf<String>()
            findComponentsRecursive(frame.contentPane, text.lowercase(), results)
            if (results.isEmpty()) {
                "NOTFOUND"
            } else {
                results.joinToString("\n")
            }
        }
    }

    private fun findComponentsRecursive(component: Component, searchText: String, results: MutableList<String>) {
        val text = getComponentText(component)
        if (text.lowercase().contains(searchText)) {
            val screenLoc = component.locationOnScreen
            val frameLoc = frame.contentPane.locationOnScreen
            val relX = screenLoc.x - frameLoc.x
            val relY = screenLoc.y - frameLoc.y
            results.add("${component.javaClass.simpleName} \"$text\" at ($relX,$relY) size ${component.width}x${component.height}")
        }

        if (component is Container) {
            for (child in component.components) {
                if (child.isVisible) {
                    findComponentsRecursive(child, searchText, results)
                }
            }
        }
    }

    private fun click(args: String): String {
        val parts = args.split(" ")
        if (parts.size < 2) return "ERROR: Usage: click <x> <y>"

        val x = parts[0].toIntOrNull() ?: return "ERROR: Invalid x coordinate"
        val y = parts[1].toIntOrNull() ?: return "ERROR: Invalid y coordinate"

        return invokeAndWait {
            // Ensure window has focus
            frame.toFront()
            frame.requestFocus()

            val frameLoc = frame.contentPane.locationOnScreen
            val screenX = frameLoc.x + x
            val screenY = frameLoc.y + y

            val robot = Robot()
            robot.delay(50)  // Small delay to ensure focus
            robot.mouseMove(screenX, screenY)
            robot.delay(20)
            robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
            robot.delay(20)
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
            robot.delay(50)  // Allow event processing

            "OK: Clicked at ($x, $y) screen=($screenX, $screenY)"
        }
    }

    private fun moveTo(args: String): String {
        val parts = args.split(" ")
        if (parts.size < 2) return "ERROR: Usage: moveto <x> <y>"

        val x = parts[0].toIntOrNull() ?: return "ERROR: Invalid x coordinate"
        val y = parts[1].toIntOrNull() ?: return "ERROR: Invalid y coordinate"

        return invokeAndWait {
            val frameLoc = frame.contentPane.locationOnScreen
            val screenX = frameLoc.x + x
            val screenY = frameLoc.y + y

            val robot = Robot()
            robot.mouseMove(screenX, screenY)
            robot.delay(100)  // Allow time for hover events to process

            "OK: Moved mouse to ($x, $y) screen=($screenX, $screenY)"
        }
    }

    private fun moveToOnComponent(searchText: String): String {
        if (searchText.isBlank()) return "ERROR: No search text provided"

        return invokeAndWait {
            val comp = findFirstComponent(frame.contentPane, searchText.lowercase())
                ?: return@invokeAndWait "ERROR: No component found containing '$searchText'"

            val compLoc = comp.locationOnScreen
            val centerX = compLoc.x + comp.width / 2
            val centerY = compLoc.y + comp.height / 2

            val robot = Robot()
            robot.mouseMove(centerX, centerY)
            robot.delay(100)  // Allow time for hover events to process

            "OK: Moved mouse to center of '${getComponentText(comp)}' at screen=($centerX, $centerY)"
        }
    }

    private fun typeText(text: String): String {
        if (text.isBlank()) return "ERROR: No text provided"

        return invokeAndWait {
            val robot = Robot()
            for (char in text) {
                typeChar(robot, char)
            }
            "OK: Typed ${text.length} characters"
        }
    }

    private fun typeChar(robot: Robot, char: Char) {
        val keyCode = java.awt.event.KeyEvent.getExtendedKeyCodeForChar(char.code)
        if (keyCode != java.awt.event.KeyEvent.VK_UNDEFINED) {
            val needsShift = char.isUpperCase() || "~!@#$%^&*()_+{}|:\"<>?".contains(char)
            if (needsShift) {
                robot.keyPress(java.awt.event.KeyEvent.VK_SHIFT)
            }
            try {
                robot.keyPress(keyCode)
                robot.keyRelease(keyCode)
            } catch (e: Exception) {
                // Some characters can't be typed directly
            }
            if (needsShift) {
                robot.keyRelease(java.awt.event.KeyEvent.VK_SHIFT)
            }
        }
        robot.delay(10)
    }

    private fun focusComponent(text: String): String {
        if (text.isBlank()) return "ERROR: No search text provided"

        return invokeAndWait {
            val component = findFirstComponent(frame.contentPane, text.lowercase())
            if (component != null) {
                component.requestFocusInWindow()
                "OK: Focused ${component.javaClass.simpleName}"
            } else {
                "NOTFOUND"
            }
        }
    }

    private fun findFirstComponent(component: Component, searchText: String): Component? {
        val text = getComponentText(component)
        if (text.lowercase().contains(searchText)) {
            return component
        }

        if (component is Container) {
            for (child in component.components) {
                if (child.isVisible) {
                    val found = findFirstComponent(child, searchText)
                    if (found != null) return found
                }
            }
        }
        return null
    }

    private fun windowBounds(): String {
        return invokeAndWait {
            val bounds = frame.contentPane.bounds
            "BOUNDS: ${bounds.width}x${bounds.height}"
        }
    }

    /**
     * Find all components matching text, then click the best one.
     * Prefers components whose parent JPanel has mouse listeners (like Link components).
     * Also handles checkbox siblings - when a label is found next to a checkbox, clicks the checkbox.
     */
    private fun clickOnComponent(text: String): String {
        if (text.isBlank()) return "ERROR: No search text provided"

        return invokeAndWait {
            // Find all matching components
            val allMatches = mutableListOf<Component>()
            findAllComponentsWithText(frame.contentPane, text.lowercase(), allMatches)

            if (allMatches.isEmpty()) {
                return@invokeAndWait "NOTFOUND: No component with text '$text'"
            }

            // Check if any match is a label with a checkbox or button sibling
            for (component in allMatches) {
                if (component is JLabel) {
                    val parent = component.parent
                    if (parent is Container) {
                        // Look for sibling JCheckBox or JButton
                        for (sibling in parent.components) {
                            when (sibling) {
                                is JCheckBox -> {
                                    sibling.doClick()
                                    return@invokeAndWait "OK: Clicked checkbox (label: '$text')"
                                }
                                is JButton -> {
                                    // KiteUI buttons have a JLabel for display and a JButton sibling for action
                                    sibling.doClick()
                                    return@invokeAndWait "OK: Clicked button sibling (label: '$text')"
                                }
                            }
                        }
                    }
                }
            }

            // Sort by priority: prefer components whose immediate parent is a JPanel with mouse listeners
            val sorted = allMatches.sortedByDescending { comp ->
                var score = 0
                var parent = comp.parent
                // Walk up a few levels looking for JPanel with listeners
                repeat(3) {
                    if (parent is JPanel && parent.mouseListeners.isNotEmpty()) {
                        score = 10 - it  // Higher score for closer panels
                    }
                    parent = parent?.parent
                }
                score
            }

            // Try each match until one works
            for (component in sorted) {
                // Check if component itself is a clickable control
                when (component) {
                    is AbstractButton -> {
                        component.doClick()
                        return@invokeAndWait "OK: Clicked ${component.javaClass.simpleName} (text: '$text')"
                    }
                }

                // Check if any ancestor is an AbstractButton (for text inside buttons)
                var buttonAncestor: Container? = component.parent
                while (buttonAncestor != null) {
                    if (buttonAncestor is AbstractButton) {
                        buttonAncestor.doClick()
                        return@invokeAndWait "OK: Clicked ${buttonAncestor.javaClass.simpleName} ancestor (text: '$text')"
                    }
                    buttonAncestor = buttonAncestor.parent
                }

                // Look for nearest ancestor JPanel with mouse listeners (Link components)
                var ancestor: Container? = component.parent
                while (ancestor != null) {
                    if (ancestor is JPanel && ancestor.mouseListeners.isNotEmpty()) {
                        // Found a JPanel with listeners - directly invoke the listener
                        val centerX = ancestor.width / 2
                        val centerY = ancestor.height / 2
                        val clickEvent = MouseEvent(
                            ancestor, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                            0, centerX, centerY, 1, false, MouseEvent.BUTTON1
                        )
                        // Directly call mouseClicked on all listeners
                        for (listener in ancestor.mouseListeners) {
                            listener.mouseClicked(clickEvent)
                        }
                        return@invokeAndWait "OK: Clicked on ${ancestor.javaClass.simpleName} via listener (text: '$text')"
                    }
                    ancestor = ancestor.parent
                }

                // Fallback: dispatch events up the hierarchy
                var target: Component? = component
                while (target != null) {
                    val centerX = target.width / 2
                    val centerY = target.height / 2

                    val pressEvent = MouseEvent(
                        target, MouseEvent.MOUSE_PRESSED, System.currentTimeMillis(),
                        MouseEvent.BUTTON1_DOWN_MASK, centerX, centerY, 1, false, MouseEvent.BUTTON1
                    )
                    val releaseEvent = MouseEvent(
                        target, MouseEvent.MOUSE_RELEASED, System.currentTimeMillis(),
                        MouseEvent.BUTTON1_DOWN_MASK, centerX, centerY, 1, false, MouseEvent.BUTTON1
                    )
                    val clickEvent = MouseEvent(
                        target, MouseEvent.MOUSE_CLICKED, System.currentTimeMillis(),
                        0, centerX, centerY, 1, false, MouseEvent.BUTTON1
                    )

                    target.dispatchEvent(pressEvent)
                    target.dispatchEvent(releaseEvent)
                    target.dispatchEvent(clickEvent)

                    if (clickEvent.isConsumed || target.mouseListeners.isNotEmpty()) {
                        return@invokeAndWait "OK: Clicked on ${target.javaClass.simpleName} (text search: '$text')"
                    }

                    target = target.parent
                }
            }
            "OK: Dispatched click for '$text' but no handler found"
        }
    }

    private fun findAllComponentsWithText(component: Component, searchText: String, results: MutableList<Component>) {
        val text = getComponentText(component)
        if (text.lowercase().contains(searchText)) {
            results.add(component)
        }
        if (component is Container) {
            for (child in component.components) {
                if (child.isVisible) {
                    findAllComponentsWithText(child, searchText, results)
                }
            }
        }
    }

    /**
     * Scroll command: scroll <delta_y> or scroll <x> <y>
     * Finds the first scroll pane in the content and scrolls it.
     */
    private fun scroll(args: String): String {
        val parts = args.split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return "ERROR: Usage: scroll <delta_y> or scroll <x> <y>"

        return invokeAndWait {
            val scrollPane = findFirstScrollPane(frame.contentPane)
                ?: return@invokeAndWait "ERROR: No scroll pane found"

            val viewport = scrollPane.viewport
            val currentPos = viewport.viewPosition

            if (parts.size == 1) {
                // Delta scroll
                val deltaY = parts[0].toIntOrNull() ?: return@invokeAndWait "ERROR: Invalid delta"
                val newY = (currentPos.y + deltaY).coerceIn(0, maxOf(0, viewport.view.height - viewport.height))
                viewport.viewPosition = Point(currentPos.x, newY)
                "OK: Scrolled to y=$newY"
            } else {
                // Absolute scroll
                val x = parts[0].toIntOrNull() ?: return@invokeAndWait "ERROR: Invalid x"
                val y = parts[1].toIntOrNull() ?: return@invokeAndWait "ERROR: Invalid y"
                viewport.viewPosition = Point(x.coerceAtLeast(0), y.coerceAtLeast(0))
                "OK: Scrolled to ($x, $y)"
            }
        }
    }

    /**
     * Find a component by text and scroll its ancestor scroll pane.
     * Usage: scrollon <text> [delta_y]
     */
    private fun scrollOnComponent(args: String): String {
        val parts = args.split(" ", limit = 2)
        if (parts.isEmpty()) return "ERROR: Usage: scrollon <text> [delta_y]"

        val searchText = parts[0].lowercase()
        val deltaY = parts.getOrNull(1)?.toIntOrNull() ?: 200

        return invokeAndWait {
            val component = findFirstComponent(frame.contentPane, searchText)
                ?: return@invokeAndWait "NOTFOUND: No component with text '$searchText'"

            // Find ancestor scroll pane
            var parent: Container? = component.parent
            while (parent != null) {
                if (parent is JScrollPane) {
                    val viewport = parent.viewport
                    val currentPos = viewport.viewPosition
                    val newY = (currentPos.y + deltaY).coerceIn(0, maxOf(0, viewport.view.height - viewport.height))
                    viewport.viewPosition = Point(currentPos.x, newY)
                    return@invokeAndWait "OK: Scrolled $deltaY from component '$searchText'"
                }
                parent = parent.parent
            }
            "ERROR: No scroll pane ancestor found for '$searchText'"
        }
    }

    /**
     * Simulate mouse wheel scroll using Robot.
     * Usage: wheel <clicks> or wheel <text> <clicks>
     * Negative clicks = scroll up, positive = scroll down
     */
    private fun mouseWheel(args: String): String {
        val parts = args.split(" ").filter { it.isNotBlank() }
        if (parts.isEmpty()) return "ERROR: Usage: wheel <clicks> or wheel <text> <clicks>"

        return invokeAndWait {
            frame.toFront()
            frame.requestFocus()

            val robot = Robot()
            robot.delay(50)

            if (parts.size == 1) {
                // Just scroll at current mouse position
                val clicks = parts[0].toIntOrNull() ?: return@invokeAndWait "ERROR: Invalid click count"
                robot.mouseWheel(clicks)
                robot.delay(100)
                "OK: Scrolled $clicks clicks at current position"
            } else {
                // Find component and scroll over it
                val searchText = parts[0].lowercase()
                val clicks = parts[1].toIntOrNull() ?: return@invokeAndWait "ERROR: Invalid click count"

                val component = findFirstComponent(frame.contentPane, searchText)
                    ?: return@invokeAndWait "NOTFOUND: No component with text '$searchText'"

                val compLoc = component.locationOnScreen
                val centerX = compLoc.x + component.width / 2
                val centerY = compLoc.y + component.height / 2

                robot.mouseMove(centerX, centerY)
                robot.delay(50)
                robot.mouseWheel(clicks)
                robot.delay(100)

                "OK: Scrolled $clicks clicks over '$searchText' at ($centerX, $centerY)"
            }
        }
    }

    private fun dispatchWheelToScrollPane(args: String): String {
        val clicks = args.toIntOrNull() ?: 3
        return invokeAndWait {
            val scrollPane = findFirstScrollPane(frame.contentPane)
                ?: return@invokeAndWait "ERROR: No scroll pane found"

            val viewport = scrollPane.viewport
            val beforeY = viewport.viewPosition.y

            // Create and dispatch wheel event directly to the scroll pane
            val wheelEvent = java.awt.event.MouseWheelEvent(
                scrollPane,
                java.awt.event.MouseWheelEvent.MOUSE_WHEEL,
                System.currentTimeMillis(),
                0,  // modifiers
                scrollPane.width / 2,  // x
                scrollPane.height / 2,  // y
                0,  // clickCount
                false,  // popup trigger
                java.awt.event.MouseWheelEvent.WHEEL_UNIT_SCROLL,
                3,  // scroll amount
                clicks  // wheel rotation
            )
            scrollPane.dispatchEvent(wheelEvent)

            // Give it time to process
            Thread.sleep(100)

            val afterY = viewport.viewPosition.y
            "OK: Dispatched wheel to scroll pane. Before Y=$beforeY, After Y=$afterY, Delta=${afterY - beforeY}"
        }
    }

    private fun debugScrollPanes(): String {
        return invokeAndWait {
            val result = StringBuilder()
            fun findScrollPanes(comp: Component, depth: Int = 0): Unit {
                val indent = "  ".repeat(depth)
                if (comp is JScrollPane) {
                    val viewport = comp.viewport
                    val view = viewport.view
                    result.appendLine("${indent}JScrollPane:")
                    result.appendLine("${indent}  viewportSize: ${viewport.size}")
                    result.appendLine("${indent}  viewSize: ${view?.size}")
                    result.appendLine("${indent}  viewPrefSize: ${view?.preferredSize}")
                    result.appendLine("${indent}  viewClass: ${view?.javaClass?.simpleName}")
                    result.appendLine("${indent}  vScrollPolicy: ${comp.verticalScrollBarPolicy}")
                    result.appendLine("${indent}  hScrollPolicy: ${comp.horizontalScrollBarPolicy}")
                    result.appendLine("${indent}  wheelScrollingEnabled: ${comp.isWheelScrollingEnabled}")
                    result.appendLine("${indent}  vScrollBarVisible: ${comp.verticalScrollBar?.isVisible}")
                    result.appendLine("${indent}  hScrollBarVisible: ${comp.horizontalScrollBar?.isVisible}")
                    if (view is javax.swing.Scrollable) {
                        result.appendLine("${indent}  tracksViewportWidth: ${view.scrollableTracksViewportWidth}")
                        result.appendLine("${indent}  tracksViewportHeight: ${view.scrollableTracksViewportHeight}")
                    } else {
                        result.appendLine("${indent}  NOT Scrollable interface")
                    }
                }
                if (comp is Container) {
                    for (child in comp.components) {
                        findScrollPanes(child, depth + 1)
                    }
                }
            }
            findScrollPanes(frame.contentPane)
            if (result.isEmpty()) "No scroll panes found" else result.toString()
        }
    }

    private fun findFirstScrollPane(component: Component): JScrollPane? {
        if (component is JScrollPane) return component
        if (component is Container) {
            for (child in component.components) {
                val found = findFirstScrollPane(child)
                if (found != null) return found
            }
        }
        return null
    }

    /**
     * Dispatch click events directly to a component found by text.
     * This bypasses Robot and native input, directly calling dispatchEvent on the component.
     * Useful for testing if the component's mouse listeners are working.
     */
    private fun dispatchClickOnComponent(text: String): String {
        if (text.isBlank()) return "ERROR: No search text provided"

        return invokeAndWait {
            val component = findFirstComponent(frame.contentPane, text.lowercase())
                ?: return@invokeAndWait "NOTFOUND: No component with text '$text'"

            val centerX = component.width / 2
            val centerY = component.height / 2
            val now = System.currentTimeMillis()

            // Create and dispatch mouse events directly to the component
            val pressEvent = MouseEvent(
                component,
                MouseEvent.MOUSE_PRESSED,
                now,
                MouseEvent.BUTTON1_DOWN_MASK,
                centerX, centerY,
                1,  // click count
                false,  // popup trigger
                MouseEvent.BUTTON1
            )

            val releaseEvent = MouseEvent(
                component,
                MouseEvent.MOUSE_RELEASED,
                now + 50,
                0,
                centerX, centerY,
                1,
                false,
                MouseEvent.BUTTON1
            )

            val clickEvent = MouseEvent(
                component,
                MouseEvent.MOUSE_CLICKED,
                now + 50,
                0,
                centerX, centerY,
                1,
                false,
                MouseEvent.BUTTON1
            )

            component.dispatchEvent(pressEvent)
            component.dispatchEvent(releaseEvent)
            component.dispatchEvent(clickEvent)

            "OK: Dispatched click events to '${component.javaClass.simpleName}'"
        }
    }

    /**
     * Use Robot to perform a REAL click on a component found by text.
     * This tests the actual Swing event dispatch path.
     */
    private fun realClickOnComponent(text: String): String {
        if (text.isBlank()) return "ERROR: No search text provided"

        return invokeAndWait {
            val component = findFirstComponent(frame.contentPane, text.lowercase())
                ?: return@invokeAndWait "NOTFOUND: No component with text '$text'"

            // Get screen coordinates of component center
            val screenLoc = component.locationOnScreen
            val centerX = screenLoc.x + component.width / 2
            val centerY = screenLoc.y + component.height / 2

            // Ensure window has focus
            frame.toFront()
            frame.requestFocus()

            val robot = Robot()
            robot.delay(100)
            robot.mouseMove(centerX, centerY)
            robot.delay(50)
            robot.mousePress(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
            robot.delay(20)
            robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_DOWN_MASK)
            robot.delay(100)

            "OK: Real click on '${component.javaClass.simpleName}' at screen ($centerX, $centerY)"
        }
    }

    /**
     * Diagnose what component would receive a click at a given position or on a text component.
     * Traces the component hierarchy and shows which component Swing would dispatch to.
     */
    private fun diagnoseClickPath(args: String): String {
        return invokeAndWait {
            val sb = StringBuilder()

            // Parse args - either "x y" coordinates or text to find
            val parts = args.split(" ")
            val (targetX: Int, targetY: Int) = if (parts.size >= 2 && parts[0].toIntOrNull() != null) {
                Pair(parts[0].toInt(), parts[1].toInt())
            } else {
                // Find component by text
                val component = findFirstComponent(frame.contentPane, args.lowercase())
                if (component == null) {
                    return@invokeAndWait "NOTFOUND: No component with text '$args'"
                }
                val screenLoc = component.locationOnScreen
                val frameLoc = frame.contentPane.locationOnScreen
                Pair(
                    screenLoc.x - frameLoc.x + component.width / 2,
                    screenLoc.y - frameLoc.y + component.height / 2
                )
            }

            sb.appendLine("=== CLICK DIAGNOSIS at ($targetX, $targetY) ===")
            sb.appendLine()

            // Trace through component hierarchy
            sb.appendLine("Component hierarchy at click point:")
            traceComponentAt(frame.contentPane, targetX, targetY, 0, sb)
            sb.appendLine()

            // Check what getComponentAt returns
            val deepest = frame.contentPane.getComponentAt(targetX, targetY)
            sb.appendLine("getComponentAt returns: ${deepest?.javaClass?.simpleName ?: "null"}")
            if (deepest != null) {
                sb.appendLine("  bounds: ${deepest.bounds}")
                sb.appendLine("  visible: ${deepest.isVisible}")
                sb.appendLine("  enabled: ${deepest.isEnabled}")
                sb.appendLine("  mouseListeners: ${deepest.mouseListeners.size}")
                deepest.mouseListeners.forEachIndexed { i, l ->
                    sb.appendLine("    [$i] ${l.javaClass.name}")
                }
            }
            sb.appendLine()

            // Check findComponentAt (recursive version)
            val found = (frame.contentPane as? Container)?.findComponentAt(targetX, targetY)
            sb.appendLine("findComponentAt returns: ${found?.javaClass?.simpleName ?: "null"}")
            if (found != null && found != deepest) {
                sb.appendLine("  bounds: ${found.bounds}")
                sb.appendLine("  mouseListeners: ${found.mouseListeners.size}")
            }
            sb.appendLine()

            // Walk up ancestry from deepest component and show mouse listeners
            sb.appendLine("Ancestor chain from deepest component:")
            var current: Component? = deepest
            var depth = 0
            while (current != null) {
                val indent = "  ".repeat(depth)
                val listeners = current.mouseListeners
                val hasListeners = listeners.isNotEmpty()
                val containsMethod = try {
                    current.javaClass.getMethod("contains", Int::class.java, Int::class.java)
                    val overridden = current.javaClass.getMethod("contains", Int::class.java, Int::class.java).declaringClass != Component::class.java
                    if (overridden) " [contains overridden]" else ""
                } catch (e: Exception) { "" }

                sb.appendLine("$indent${current.javaClass.simpleName}$containsMethod${if (hasListeners) " [${listeners.size} listeners]" else ""}")
                if (hasListeners) {
                    listeners.forEach { l ->
                        sb.appendLine("$indent  -> ${l.javaClass.name}")
                    }
                }
                current = current.parent
                depth++
            }

            sb.toString()
        }
    }

    private fun traceComponentAt(component: Component, x: Int, y: Int, depth: Int, sb: StringBuilder) {
        val indent = "  ".repeat(depth)
        val bounds = component.bounds
        val localX = x - bounds.x
        val localY = y - bounds.y

        // Check if point is in component's bounds
        val inBounds = localX >= 0 && localY >= 0 && localX < bounds.width && localY < bounds.height

        // Check what contains() returns
        val containsResult = try {
            component.contains(localX, localY)
        } catch (e: Exception) {
            false
        }

        if (!inBounds && !containsResult) return  // Not relevant to click path

        val text = getComponentText(component)
        val textPart = if (text.isNotEmpty()) " \"${text.take(20)}\"" else ""
        val containsPart = if (!containsResult && inBounds) " [contains=FALSE]" else ""
        val listenerPart = if (component.mouseListeners.isNotEmpty()) " [${component.mouseListeners.size} listeners]" else ""

        sb.appendLine("$indent${component.javaClass.simpleName}$textPart$containsPart$listenerPart")

        if (component is Container) {
            // Check children in reverse order (top to bottom in z-order)
            for (i in component.componentCount - 1 downTo 0) {
                val child = component.getComponent(i)
                if (child.isVisible) {
                    traceComponentAt(child, localX, localY, depth + 1, sb)
                }
            }
        }
    }

    private fun <T> invokeAndWait(action: () -> T): T {
        var result: T? = null
        var error: Exception? = null

        SwingUtilities.invokeAndWait {
            try {
                result = action()
            } catch (e: Exception) {
                error = e
            }
        }

        error?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return result as T
    }
}
