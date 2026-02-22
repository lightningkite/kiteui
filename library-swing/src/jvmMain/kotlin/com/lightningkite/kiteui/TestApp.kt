package com.lightningkite.kiteui

import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.kiteui.models.px
import javax.swing.JFrame
import javax.swing.SwingUtilities
import java.awt.BorderLayout
import java.awt.Dimension
import kotlinx.coroutines.GlobalScope
import kotlin.coroutines.CoroutineContext

/**
 * Simple test application to verify the jvmSwing implementation works.
 *
 * This creates a basic UI demonstrating:
 * - Frame, Column, Space, Separator components
 * - TextView components with different content
 * - Button with click actions
 * - Layout spacing and alignment
 */
fun main() {
    SwingUtilities.invokeLater {
        // Create the JFrame
        val frame = JFrame("KiteUI Swing Test").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            preferredSize = Dimension(400, 500)
        }

        // Create RContext with the frame
        val context = RContext(frame)

        // Build UI using a root container ViewWriter
        val rootView = object : ViewWriter() {
            override val context: RContext = context
            override val representsView = null
            override val coroutineContext: CoroutineContext = GlobalScope.coroutineContext
            override fun willAddChild(view: com.lightningkite.kiteui.views.RView) {}
            override fun addChild(view: com.lightningkite.kiteui.views.RView) {
                // Add the root view to the frame's content pane
                frame.contentPane.removeAll()
                frame.contentPane.add(view.native, BorderLayout.CENTER)
                frame.contentPane.revalidate()
                frame.contentPane.repaint()
            }
        }

        // Build the UI
        rootView.apply {
            col {
                gap = 16.px

                // Header
                text {
                    content = "KiteUI Swing Test Application"
                }

                space(0.5)

                separator()

                space(0.5)

                // Description
                text {
                    content = "This is a test application demonstrating KiteUI on Swing."
                }

                space(1.0)

                // Counter section
                var clickCount = 0

                text {
                    content = "Button Click Test:"
                }

                space(0.5)

                val counterText = text {
                    content = "Clicks: $clickCount"
                }

                space(0.5)

                button {
                    text {
                        content = "Click Me!"
                    }
                    onClick {
                        clickCount++
                        counterText.content = "Clicks: $clickCount"
                        println("Button clicked! Count: $clickCount")
                    }
                }

                space(1.0)

                separator()

                space(1.0)

                // Multiple text views
                text {
                    content = "Text View 1: Default styling"
                }

                space(0.5)

                text {
                    content = "Text View 2: More content here"
                }

                space(0.5)

                text {
                    content = "Text View 3: Testing layout spacing"
                }

                space(1.0)

                separator()

                space(1.0)

                // ActivityIndicator test
                text {
                    content = "ActivityIndicator Test:"
                }

                space(0.5)

                activityIndicator()

                space(1.0)

                separator()

                space(1.0)

                // ProgressBar Test
                text {
                    content = "Progress Bar Test:"
                }

                space(0.5)

                val progressBar = progressBar {
                    ratio = 0.0f
                }

                space(0.5)

                button {
                    text {
                        content = "Increase Progress"
                    }
                    onClick {
                        progressBar.ratio = (progressBar.ratio + 0.1f).coerceIn(0f, 1f)
                        println("Progress: ${(progressBar.ratio * 100).toInt()}%")
                    }
                }

                space(0.5)

                button {
                    text {
                        content = "Reset Progress"
                    }
                    onClick {
                        progressBar.ratio = 0.0f
                        println("Progress reset to 0%")
                    }
                }

                space(1.0)

                separator()

                space(1.0)

                // Footer
                text {
                    content = "All components rendered successfully!"
                }
            }
        }

        // Show the frame
        frame.pack()
        frame.setLocationRelativeTo(null)  // Center on screen
        frame.isVisible = true

        println("KiteUI Swing Test App launched successfully!")
    }
}
