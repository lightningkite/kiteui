package com.lightningkite.kiteui.testing

import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Bundle
import android.view.View
import com.github.takahirom.roborazzi.captureRoboImage
import com.lightningkite.kiteui.KiteUiActivity
import com.lightningkite.kiteui.R
import com.lightningkite.kiteui.models.Theme
import com.lightningkite.kiteui.navigation.Page
import com.lightningkite.kiteui.navigation.PageNavigator
import com.lightningkite.kiteui.navigation.Routes
import com.lightningkite.kiteui.views.RView
import com.lightningkite.kiteui.views.ViewWriter
import com.lightningkite.kiteui.views.direct.frame
import com.lightningkite.reactive.context.*
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.GraphicsMode
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * Android implementation of TestHarness using Robolectric.
 *
 * Uses the KiteUiActivity pattern which is the standard way to
 * initialize KiteUI on Android.
 */
actual class TestHarness {
    actual val supported: Boolean = true
    private var controller: org.robolectric.android.controller.ActivityController<TestActivity>? = null
    private var rootView: RView? = null

    init {
        // Initialize AndroidAppContext as early as possible
        // This is required because Theme's static initialization accesses AndroidAppContext.oneRem
        try {
            com.lightningkite.kiteui.views.AndroidAppContext.applicationCtx
        } catch (e: UninitializedPropertyAccessException) {
            // Not initialized yet, initialize it now
            com.lightningkite.kiteui.views.AndroidAppContext.applicationCtx =
                RuntimeEnvironment.getApplication()
        }
    }

    /**
     * Test activity that holds the test UI.
     */
    class TestActivity : KiteUiActivity() {
        override val mainNavigator: PageNavigator = PageNavigator {
            Routes(listOf(), mapOf(), Page.Empty)
        }

        var testTheme: Theme = Theme(id = "test")
        var testContent: (ViewWriter.() -> Unit)? = null
        var rootView: RView? = null

        override val theme: ReactiveContext.() -> Theme = {
            testTheme
        }

        override fun onCreate(savedInstanceState: Bundle?) {
            super.onCreate(savedInstanceState)
            setTheme(R.style.Theme_Mppexample)
            val content = testContent
            if (content != null) {
                with(viewWriter) {
                    frame {
                        content()
                    }.also { rootView = it }
                }
            }
        }
    }

    actual fun render(theme: Theme, content: ViewWriter.() -> Unit): RView {
        val ctrl = Robolectric.buildActivity(TestActivity::class.java)
        controller = ctrl

        // CRITICAL FIX: Initialize AndroidAppContext BEFORE any view creation
        // This must happen before onCreate() because some view initialization code
        // accesses AndroidAppContext.res which depends on applicationCtx being set
        com.lightningkite.kiteui.views.AndroidAppContext.applicationCtx = ctrl.get().applicationContext

        ctrl.get().testTheme = theme
        ctrl.get().testContent = content
        ctrl.setup()

        // Get the root view that was captured in onCreate
        val root = ctrl.get().rootView
            ?: throw IllegalStateException("No root view found after setup")

        rootView = root
        return root
    }

    actual fun screenshot(name: String): ByteArray? {
        val root = rootView ?: return null
        return captureViewScreenshot(root.native, name)
    }

    actual fun screenshotView(view: RView, name: String): ByteArray? {
        return captureViewScreenshot(view.native, name)
    }

    private fun captureViewScreenshot(view: View, name: String): ByteArray? {
        try {
            // Roborazzi saves relative to current directory during test execution
            // Use absolute path to ensure we save where we expect
            val projectRoot = File(".").absoluteFile.parentFile
            val roborazziFile = File(projectRoot, "library/build/outputs/roborazzi/$name.png")
            roborazziFile.parentFile?.mkdirs()

            // Capture using Roborazzi - this actually renders the view
            view.captureRoboImage(roborazziFile.absolutePath)

            // Copy to our screenshots directory for easier access
            val screenshotFile = File(projectRoot, "library/local/screenshots/android/$name.png")
            screenshotFile.parentFile?.mkdirs()

            return if (roborazziFile.exists()) {
                val bytes = roborazziFile.readBytes()
                screenshotFile.writeBytes(bytes)
                println("Screenshot saved to: ${screenshotFile.absolutePath}")
                bytes
            } else {
                println("Roborazzi file not created: ${roborazziFile.absolutePath}")
                null
            }
        } catch (e: Exception) {
            println("Failed to capture screenshot: ${e.message}")
            e.printStackTrace()
            return null
        }
    }

    actual fun cleanup() {
        controller?.pause()
        controller?.stop()
        controller = null
        rootView = null
    }
}
