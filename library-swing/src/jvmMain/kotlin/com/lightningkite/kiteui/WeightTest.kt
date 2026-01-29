package com.lightningkite.kiteui

import com.lightningkite.kiteui.models.*
import com.lightningkite.kiteui.views.RContext
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.*
import kotlinx.coroutines.GlobalScope
import javax.swing.JFrame
import javax.swing.SwingUtilities
import java.awt.BorderLayout
import java.awt.Dimension
import kotlin.coroutines.CoroutineContext

/**
 * Test application to verify weight behavior in rows.
 * Two views with weight(1f) should each take exactly half the available width.
 */
fun main() {
    // Enable layout debugging
    System.setProperty("kiteui.debug.layout", "true")

    SwingUtilities.invokeLater {
        val frame = JFrame("Weight Test").apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            preferredSize = Dimension(800, 400)
        }

        val context = RContext(frame)

        val rootView = object : ViewWriter() {
            override val context: RContext = context
            override val representsView = null
            override val coroutineContext: CoroutineContext = GlobalScope.coroutineContext
            override fun willAddChild(view: com.lightningkite.kiteui.views.RView) {}
            override fun addChild(view: com.lightningkite.kiteui.views.RView) {
                frame.contentPane.removeAll()
                frame.contentPane.add(view.native, BorderLayout.CENTER)
                frame.contentPane.revalidate()
                frame.contentPane.repaint()
            }
        }

        // Build test UI
        val views = mutableListOf<com.lightningkite.kiteui.views.RView>()

        rootView.apply {
            row {
                // First weighted view - should take 50% of width
                weight(1f).col {
                    views.add(this)
                    text {
                        content = "View 1 (weight: 1)"
                        align = Align.Center
                    }
                }

                // Second weighted view - should take 50% of width
                weight(1f).col {
                    views.add(this)
                    text {
                        content = "View 2 (weight: 1)"
                        align = Align.Center
                    }
                }
            }
        }

        val view1 = views[0]
        val view2 = views[1]

        // Use fixed size instead of pack() to ensure we have space to test
        frame.setSize(800, 400)
        frame.setLocationRelativeTo(null)
        frame.isVisible = true

        // Wait for layout to complete and then verify
        SwingUtilities.invokeLater {
            SwingUtilities.invokeLater {
                // Give it an extra frame to ensure layout is complete
                val containerWidth = frame.contentPane.width
                val view1Width = view1.native.width
                val view2Width = view2.native.width

                println("=== Weight Test Results ===")
                println("Container width: $containerWidth")
                println("View 1 width: $view1Width (${(view1Width.toDouble() / containerWidth * 100).toInt()}%)")
                println("View 2 width: $view2Width (${(view2Width.toDouble() / containerWidth * 100).toInt()}%)")
                println("Expected: ~${containerWidth / 2 - 8} each (50%)")

                val tolerance = 5 // Allow 5px tolerance
                val expectedWidth = containerWidth / 2 - 8
                val test1Passed = Math.abs(view1Width - expectedWidth) <= tolerance
                val test2Passed = Math.abs(view2Width - expectedWidth) <= tolerance

                println("\nTest Results:")
                println("View 1 correct size: ${if (test1Passed) "✓ PASS" else "✗ FAIL"}")
                println("View 2 correct size: ${if (test2Passed) "✓ PASS" else "✗ FAIL"}")

                if (test1Passed && test2Passed) {
                    println("\n✓✓✓ ALL TESTS PASSED ✓✓✓")
                    // Exit with success
                    frame.dispose()
                    System.exit(0)
                } else {
                    println("\n✗✗✗ TESTS FAILED ✗✗✗")
                    println("Debug info:")
                    println("  view1.native type: ${view1.native.javaClass.simpleName}")
                    println("  view2.native type: ${view2.native.javaClass.simpleName}")
                    if (view1.native is javax.swing.JComponent) {
                        println("  view1 weight: ${(view1.native as javax.swing.JComponent).weight}")
                        println("  view1 preferredSize: ${view1.native.preferredSize}")
                        println("  view1 maximumSize: ${view1.native.maximumSize}")
                    }
                    if (view2.native is javax.swing.JComponent) {
                        println("  view2 weight: ${(view2.native as javax.swing.JComponent).weight}")
                        println("  view2 preferredSize: ${view2.native.preferredSize}")
                        println("  view2 maximumSize: ${view2.native.maximumSize}")
                    }

                    // Exit with failure
                    frame.dispose()
                    System.exit(1)
                }
            }
        }

        // Safety timeout - kill the test after 5 seconds if verification hasn't completed
        java.util.Timer().schedule(object : java.util.TimerTask() {
            override fun run() {
                println("\n✗✗✗ TEST TIMEOUT ✗✗✗")
                frame.dispose()
                System.exit(2)
            }
        }, 5000)
    }
}
