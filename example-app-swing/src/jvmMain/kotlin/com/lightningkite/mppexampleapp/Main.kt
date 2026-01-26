package com.lightningkite.mppexampleapp

import com.lightningkite.kiteui.models.ThemeDerivation
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.mainPageNavigator
import com.lightningkite.kiteui.navigation.dialogPageNavigator
import com.lightningkite.kiteui.views.*
import com.lightningkite.kiteui.views.direct.*
import com.lightningkite.mppexampleapp.docs.LayoutPage
import com.lightningkite.mppexampleapp.internal.ControlsPage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.swing.JFrame
import javax.swing.SwingUtilities
import javax.swing.WindowConstants

fun main() {
    SwingUtilities.invokeLater {
        // Create main window
        val frame = JFrame("KiteUI Example App").apply {
            defaultCloseOperation = WindowConstants.EXIT_ON_CLOSE
            setSize(1200, 800)
            setLocationRelativeTo(null) // Center on screen
        }

        // Create KiteUI context
        val context = RContext(frame)

        // Create navigators
        val mainNavigator = PageNavigator { AutoRoutes }
        val dialogNavigator = PageNavigator { AutoRoutes }

        // Create ViewWriter with proper setup
        val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        val viewWriter = object : ViewWriter(), CoroutineScope by appScope {
            override val representsView: RView? = null
            override val context: RContext = context

            override fun willAddChild(view: RView) {
                // Apply base theme
                view::themeChoice { ThemeDerivation.SetAsBase(appTheme()) }
            }

            override fun addChild(view: RView) {
                // Set view as window content directly
                frame.contentPane.removeAll()
                frame.contentPane.add(view.native)
                frame.revalidate()
                frame.repaint()
            }
        }

        // Initialize the app with navigators
        try {
            with(viewWriter) {
                mainPageNavigator = mainNavigator
                dialogPageNavigator = dialogNavigator
                app(mainNavigator, dialogNavigator)
//                frame {
//                    with(LayoutPage) {
//                        render()
//                    }
//                }
//                col {
//                    text("A")
//                    text("B")
//                    text("C")
//                }
            }

            // Show the window
            frame.isVisible = true

            // Start test server for automation
            val testServer = SwingTestServer(frame)
            testServer.start()

            println("KiteUI Example App launched successfully on Swing!")
            println("Test server listening on port 18888")
        } catch (e: Exception) {
            System.err.println("Error initializing app:")
            e.printStackTrace()
            throw e
        }
    }
}
